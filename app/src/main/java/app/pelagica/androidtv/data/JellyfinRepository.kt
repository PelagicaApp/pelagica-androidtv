package app.pelagica.androidtv.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import app.pelagica.androidtv.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.authenticationApi
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.discovery.RecommendedServerInfo
import org.jellyfin.sdk.discovery.RecommendedServerInfoScore
import org.jellyfin.sdk.discovery.RecommendedServerIssue
import org.jellyfin.sdk.model.ClientInfo

data class DiscoveredServer(val name: String, val address: String)

data class Server(val address: String, val name: String)

data class Session(val server: Server, val username: String)

class JellyfinException(message: String) : Exception(message)

/** Wraps the Jellyfin SDK and persists the current server and access token. */
class JellyfinRepository(context: Context) {
    private val jellyfin = createJellyfin {
        this.context = context.applicationContext
        clientInfo = ClientInfo(name = "Pelagica", version = BuildConfig.VERSION_NAME)
    }

    /** Shared API client, pointed at the current server and authenticated after sign-in. */
    val api: ApiClient = jellyfin.createApi()

    private val prefs = context.getSharedPreferences("jellyfin_session", Context.MODE_PRIVATE)

    fun discoverServers(): Flow<DiscoveredServer> =
        jellyfin.discovery.discoverLocalServers()
            .map { DiscoveredServer(name = it.name, address = it.address) }

    /**
     * Resolves user input like "jellyfin.local" into a reachable server URL, trying
     * https/http and the default ports. Throws [JellyfinException] if none work.
     */
    suspend fun connect(input: String): Server {
        val addresses = runCatching { jellyfin.discovery.getAddressCandidates(input) }
            .onFailure { Log.w(TAG, "Failed to parse server address", it) }
            .getOrNull()
        if (addresses.isNullOrEmpty()) throw JellyfinException("Invalid server address")
        val candidates = runCatching { jellyfin.discovery.getRecommendedServers(addresses) }
            .getOrElse {
                Log.w(TAG, "Server discovery failed", it)
                throw JellyfinException("Could not reach the server")
            }

        candidates.forEach { Log.d(TAG, "${it.address}: ${it.score} ${it.issues}") }

        val best = BEST_SCORES.firstNotNullOfOrNull { score ->
            candidates.firstOrNull { it.score == score }
        } ?: throw JellyfinException(candidates.describeFailure())

        val server = Server(
            address = best.address,
            name = best.systemInfo.getOrNull()?.serverName ?: best.address,
        )
        api.update(baseUrl = server.address, accessToken = null)
        prefs.edit {
            putString(KEY_ADDRESS, server.address)
            putString(KEY_SERVER_NAME, server.name)
            remove(KEY_TOKEN)
            remove(KEY_USERNAME)
        }
        return server
    }

    suspend fun signIn(username: String, password: String): Session {
        val server = savedServer() ?: throw JellyfinException("No server selected")
        val result = try {
            api.authenticationApi.authenticateUserByName(username, password).content
        } catch (e: InvalidStatusException) {
            throw JellyfinException(
                if (e.status == 401) "Invalid username or password" else "Sign in failed (HTTP ${e.status})"
            )
        } catch (e: Exception) {
            throw JellyfinException("Could not reach the server")
        }

        val token = result.accessToken ?: throw JellyfinException("Server did not return an access token")
        val name = result.user?.name ?: username
        api.update(accessToken = token)
        prefs.edit {
            putString(KEY_TOKEN, token)
            putString(KEY_USERNAME, name)
        }
        return Session(server, name)
    }

    /**
     * Restores the previous session. Returns the saved server (without a session) when
     * the stored token has been revoked, or null when nothing was saved.
     */
    suspend fun restore(): Pair<Server, Session?>? {
        val server = savedServer() ?: return null
        val token = prefs.getString(KEY_TOKEN, null)
        val username = prefs.getString(KEY_USERNAME, null)
        api.update(baseUrl = server.address, accessToken = token)
        if (token == null || username == null) return server to null

        return try {
            val user = api.userApi.getCurrentUser().content
            server to Session(server, user.name ?: username)
        } catch (e: InvalidStatusException) {
            if (e.status == 401) {
                clearToken()
                server to null
            } else {
                server to Session(server, username)
            }
        } catch (e: Exception) {
            // Server unreachable right now; keep the session and let later calls retry.
            server to Session(server, username)
        }
    }

    suspend fun signOut() {
        runCatching { api.sessionApi.reportSessionEnded() }
        clearToken()
    }

    private fun clearToken() {
        api.update(accessToken = null)
        prefs.edit {
            remove(KEY_TOKEN)
            remove(KEY_USERNAME)
        }
    }

    private fun savedServer(): Server? {
        val address = prefs.getString(KEY_ADDRESS, null) ?: return null
        return Server(address, prefs.getString(KEY_SERVER_NAME, null) ?: address)
    }

    private fun Collection<RecommendedServerInfo>.describeFailure(): String =
        when (val issue = firstNotNullOfOrNull { it.firstIssueOrNull() }) {
            is RecommendedServerIssue.UnsupportedServerVersion -> "Server version ${issue.version} is not supported"
            is RecommendedServerIssue.InvalidProductName,
            is RecommendedServerIssue.MissingSystemInfo,
            RecommendedServerIssue.MissingVersion -> "This doesn't look like a Jellyfin server"
            is RecommendedServerIssue.SecureConnectionFailed -> "Secure connection failed"
            else -> "Could not reach the server"
        }

    private companion object {
        const val TAG = "JellyfinRepository"
        val BEST_SCORES = listOf(
            RecommendedServerInfoScore.GREAT,
            RecommendedServerInfoScore.GOOD,
            RecommendedServerInfoScore.OK,
        )

        const val KEY_ADDRESS = "server_address"
        const val KEY_SERVER_NAME = "server_name"
        const val KEY_TOKEN = "access_token"
        const val KEY_USERNAME = "username"
    }
}

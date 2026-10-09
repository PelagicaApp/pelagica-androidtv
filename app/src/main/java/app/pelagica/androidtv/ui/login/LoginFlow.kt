package app.pelagica.androidtv.ui.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.pelagica.androidtv.data.JellyfinRepository
import app.pelagica.androidtv.data.Server
import app.pelagica.androidtv.data.Session
import app.pelagica.androidtv.ui.components.Spinner
import app.pelagica.androidtv.ui.theme.Background
import app.pelagica.androidtv.ui.theme.TextMuted
import kotlinx.coroutines.launch

private sealed interface LoginStep {
    data object Restoring : LoginStep
    data object SelectServer : LoginStep
    data class LoginMethod(val server: Server) : LoginStep
    data class QuickConnect(val server: Server) : LoginStep
    data class Credentials(val server: Server) : LoginStep
    data class SignedIn(val session: Session) : LoginStep
}

@Composable
fun LoginFlow(jellyfin: JellyfinRepository) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf<LoginStep>(LoginStep.Restoring) }

    LaunchedEffect(Unit) {
        val restored = jellyfin.restore()
        step = when {
            restored == null -> LoginStep.SelectServer
            restored.second != null -> LoginStep.SignedIn(restored.second!!)
            else -> LoginStep.LoginMethod(restored.first)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "loginStep",
        ) { current ->
            when (current) {
                LoginStep.Restoring -> Spinner(color = TextMuted)
                LoginStep.SelectServer -> ServerScreen(
                    jellyfin = jellyfin,
                    onConnected = { step = LoginStep.LoginMethod(it) },
                )
                is LoginStep.LoginMethod -> LoginMethodScreen(
                    server = current.server,
                    onQuickConnect = { step = LoginStep.QuickConnect(current.server) },
                    onPassword = { step = LoginStep.Credentials(current.server) },
                    onChangeServer = { step = LoginStep.SelectServer },
                )
                is LoginStep.QuickConnect -> QuickConnectScreen(
                    jellyfin = jellyfin,
                    server = current.server,
                    onSignedIn = { step = LoginStep.SignedIn(it) },
                    onBack = { step = LoginStep.LoginMethod(current.server) },
                )
                is LoginStep.Credentials -> CredentialsScreen(
                    jellyfin = jellyfin,
                    server = current.server,
                    onSignedIn = { step = LoginStep.SignedIn(it) },
                    onBack = { step = LoginStep.LoginMethod(current.server) },
                )
                is LoginStep.SignedIn -> SignedInScreen(
                    session = current.session,
                    onSignOut = {
                        scope.launch {
                            jellyfin.signOut()
                            step = LoginStep.LoginMethod(current.session.server)
                        }
                    },
                )
            }
        }
    }
}

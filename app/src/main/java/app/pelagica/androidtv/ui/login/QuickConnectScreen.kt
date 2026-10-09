package app.pelagica.androidtv.ui.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.pelagica.androidtv.data.JellyfinRepository
import app.pelagica.androidtv.data.QuickConnectCode
import app.pelagica.androidtv.data.Server
import app.pelagica.androidtv.data.Session
import app.pelagica.androidtv.ui.components.ButtonStyle
import app.pelagica.androidtv.ui.components.PelagicaButton
import app.pelagica.androidtv.ui.components.QrCode
import app.pelagica.androidtv.ui.components.Spinner
import app.pelagica.androidtv.ui.theme.OnBackground
import app.pelagica.androidtv.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
internal fun QuickConnectScreen(
    jellyfin: JellyfinRepository,
    server: Server,
    onSignedIn: (Session) -> Unit,
    onBack: () -> Unit,
) {
    var request by remember { mutableStateOf<QuickConnectCode?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // Bumped to request a fresh code after a failure.
    var attempt by remember { mutableStateOf(0) }
    val backFocus = remember { FocusRequester() }

    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { backFocus.requestFocus() }

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(attempt) {
        error = null
        request = null
        runCatching {
            val code = jellyfin.initiateQuickConnect()
            request = code
            while (!jellyfin.isQuickConnectAuthorized(code.secret)) delay(QUICK_CONNECT_POLL_MS)
            jellyfin.signInWithQuickConnect(code.secret)
        }
            .onSuccess(onSignedIn)
            .onFailure { error = it.message }
    }

    LoginScaffold(title = "Quick Connect", subtitle = server.name) {
        val current = request
        when {
            error != null -> Unit
            current == null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Spinner(color = TextMuted)
            }
            else -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QrCode(content = jellyfin.quickConnectUrl(server, current.code), codeSize = 160.dp)
                Column {
                    Text(
                        text = current.code,
                        color = OnBackground,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 8.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Scan the QR code, or enter this code under Quick Connect " +
                            "in your Jellyfin settings on ${server.address.substringAfter("://")}",
                        color = TextMuted,
                        fontSize = 16.sp,
                    )
                }
            }
        }
        ErrorText(error)
        Spacer(Modifier.height(12.dp))
        if (error != null) {
            PelagicaButton(
                text = "Try Again",
                onClick = { attempt++ },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        PelagicaButton(
            text = "Back",
            onClick = onBack,
            style = ButtonStyle.Secondary,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(backFocus),
        )
    }
}

private const val QUICK_CONNECT_POLL_MS = 2_000L

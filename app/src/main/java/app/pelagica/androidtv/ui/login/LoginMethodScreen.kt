package app.pelagica.androidtv.ui.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import app.pelagica.androidtv.data.Server
import app.pelagica.androidtv.ui.components.ButtonStyle
import app.pelagica.androidtv.ui.components.PelagicaButton

@Composable
internal fun LoginMethodScreen(
    server: Server,
    onQuickConnect: () -> Unit,
    onPassword: () -> Unit,
    onChangeServer: () -> Unit,
) {
    val quickConnectFocus = remember { FocusRequester() }

    BackHandler(onBack = onChangeServer)
    LaunchedEffect(Unit) { quickConnectFocus.requestFocus() }

    LoginScaffold(title = "Sign In", subtitle = server.name) {
        PelagicaButton(
            text = "Sign in with Quick Connect",
            onClick = onQuickConnect,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(quickConnectFocus),
        )
        PelagicaButton(
            text = "Sign in with Password",
            onClick = onPassword,
            style = ButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        PelagicaButton(
            text = "Use a Different Server",
            onClick = onChangeServer,
            style = ButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

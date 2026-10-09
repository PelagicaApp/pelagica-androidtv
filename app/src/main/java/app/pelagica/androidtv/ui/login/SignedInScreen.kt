package app.pelagica.androidtv.ui.login

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import app.pelagica.androidtv.data.Session
import app.pelagica.androidtv.ui.components.PelagicaButton

@Composable
internal fun SignedInScreen(session: Session, onSignOut: () -> Unit) {
    val signOutFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { signOutFocus.requestFocus() }

    LoginScaffold(title = "Welcome, ${session.username}", subtitle = "Connected to ${session.server.name}") {
        PelagicaButton(
            text = "Sign Out",
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(signOutFocus),
        )
    }
}

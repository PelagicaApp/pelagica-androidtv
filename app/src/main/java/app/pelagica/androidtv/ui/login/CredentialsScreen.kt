package app.pelagica.androidtv.ui.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.pelagica.androidtv.data.JellyfinRepository
import app.pelagica.androidtv.data.Server
import app.pelagica.androidtv.data.Session
import app.pelagica.androidtv.ui.components.ButtonStyle
import app.pelagica.androidtv.ui.components.PelagicaButton
import app.pelagica.androidtv.ui.components.PelagicaTextField
import kotlinx.coroutines.launch

@Composable
internal fun CredentialsScreen(
    jellyfin: JellyfinRepository,
    server: Server,
    onSignedIn: (Session) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var signingIn by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val usernameFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val signInFocus = remember { FocusRequester() }

    fun signIn() {
        if (signingIn) return
        if (username.isBlank()) {
            error = "Enter your username"
            return
        }
        error = null
        signingIn = true
        scope.launch {
            runCatching { jellyfin.signIn(username.trim(), password) }
                .onSuccess(onSignedIn)
                .onFailure { error = it.message }
            signingIn = false
        }
    }

    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { usernameFocus.requestFocus() }

    LoginScaffold(title = "Sign In", subtitle = server.name) {
        FieldLabel("Username")
        PelagicaTextField(
            value = username,
            onValueChange = { username = it; error = null },
            placeholder = "Username",
            focusRequester = usernameFocus,
            imeAction = ImeAction.Next,
            onImeAction = { passwordFocus.requestFocus() },
            modifier = Modifier.fillMaxWidth(),
        )
        FieldLabel("Password")
        PelagicaTextField(
            value = password,
            onValueChange = { password = it; error = null },
            placeholder = "Password",
            focusRequester = passwordFocus,
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = { signInFocus.requestFocus() },
            modifier = Modifier.fillMaxWidth(),
        )
        ErrorText(error)
        Spacer(Modifier.height(4.dp))
        PelagicaButton(
            text = if (signingIn) "Signing In…" else "Sign In",
            onClick = ::signIn,
            loading = signingIn,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(signInFocus),
        )
        PelagicaButton(
            text = "Back",
            onClick = onBack,
            style = ButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

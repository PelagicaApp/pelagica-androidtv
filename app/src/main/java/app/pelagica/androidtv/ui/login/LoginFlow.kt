package app.pelagica.androidtv.ui.login

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.pelagica.androidtv.data.DiscoveredServer
import app.pelagica.androidtv.data.JellyfinRepository
import app.pelagica.androidtv.data.Server
import app.pelagica.androidtv.data.Session
import app.pelagica.androidtv.ui.components.ButtonStyle
import app.pelagica.androidtv.ui.components.PelagicaButton
import app.pelagica.androidtv.ui.components.PelagicaLogo
import app.pelagica.androidtv.ui.components.PelagicaTextField
import app.pelagica.androidtv.ui.components.Spinner
import app.pelagica.androidtv.ui.components.formWidth
import app.pelagica.androidtv.ui.theme.Background
import app.pelagica.androidtv.ui.theme.ErrorRed
import app.pelagica.androidtv.ui.theme.OnBackground
import app.pelagica.androidtv.ui.theme.TextMuted
import kotlinx.coroutines.launch

private sealed interface LoginStep {
    data object Restoring : LoginStep
    data object SelectServer : LoginStep
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
            else -> LoginStep.Credentials(restored.first)
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
                    onConnected = { step = LoginStep.Credentials(it) },
                )
                is LoginStep.Credentials -> CredentialsScreen(
                    jellyfin = jellyfin,
                    server = current.server,
                    onSignedIn = { step = LoginStep.SignedIn(it) },
                    onBack = { step = LoginStep.SelectServer },
                )
                is LoginStep.SignedIn -> SignedInScreen(
                    session = current.session,
                    onSignOut = {
                        scope.launch {
                            jellyfin.signOut()
                            step = LoginStep.Credentials(current.session.server)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LoginScaffold(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.formWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PelagicaLogo()
        Spacer(Modifier.height(16.dp))
        Text(text = title, color = OnBackground, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(text = subtitle, color = TextMuted, fontSize = 16.sp)
        }
        Spacer(Modifier.height(28.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 6.dp, top = 4.dp),
    )
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) {
        Text(
            text = error,
            color = ErrorRed,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun ServerScreen(jellyfin: JellyfinRepository, onConnected: (Server) -> Unit) {
    val scope = rememberCoroutineScope()
    var address by rememberSaveable { mutableStateOf("") }
    var connecting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var discovered by remember { mutableStateOf<List<DiscoveredServer>>(emptyList()) }
    var scanning by remember { mutableStateOf(true) }
    val addressFocus = remember { FocusRequester() }

    fun connect(target: String) {
        if (connecting) return
        if (target.isBlank()) {
            error = "Enter a server address"
            return
        }
        error = null
        connecting = true
        scope.launch {
            runCatching { jellyfin.connect(target.trim()) }
                .onSuccess(onConnected)
                .onFailure { error = it.message }
            connecting = false
        }
    }

    LaunchedEffect(Unit) { addressFocus.requestFocus() }
    LaunchedEffect(Unit) {
        runCatching {
            jellyfin.discoverServers().collect { server ->
                if (discovered.none { it.address == server.address }) discovered = discovered + server
            }
        }
        scanning = false
    }

    LoginScaffold(title = "Pelagica") {
        FieldLabel("Server Address")
        PelagicaTextField(
            value = address,
            onValueChange = { address = it; error = null },
            placeholder = "jellyfin.example.com",
            focusRequester = addressFocus,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go,
            onImeAction = { connect(address) },
            modifier = Modifier.fillMaxWidth(),
        )
        ErrorText(error)
        PelagicaButton(
            text = if (connecting) "Connecting…" else "Connect to Server",
            onClick = { connect(address) },
            loading = connecting,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Servers on your network",
                color = TextMuted,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
            )
            if (scanning) {
                Spinner(color = TextMuted)
                Text(
                    text = "Scanning…",
                    color = TextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        discovered.forEach { server ->
            PelagicaButton(
                onClick = {
                    address = server.address
                    connect(server.address)
                },
                style = ButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server.name,
                        color = OnBackground,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = server.address.substringAfter("://"), color = TextMuted, fontSize = 14.sp)
                }
            }
        }
        if (!scanning && discovered.isEmpty()) {
            Text(
                text = "No servers found",
                color = TextMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun CredentialsScreen(
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

@Composable
private fun SignedInScreen(session: Session, onSignOut: () -> Unit) {
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

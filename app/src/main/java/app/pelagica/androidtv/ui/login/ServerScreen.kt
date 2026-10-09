package app.pelagica.androidtv.ui.login

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.pelagica.androidtv.data.DiscoveredServer
import app.pelagica.androidtv.data.JellyfinRepository
import app.pelagica.androidtv.data.Server
import app.pelagica.androidtv.ui.components.ButtonStyle
import app.pelagica.androidtv.ui.components.PelagicaButton
import app.pelagica.androidtv.ui.components.PelagicaTextField
import app.pelagica.androidtv.ui.components.Spinner
import app.pelagica.androidtv.ui.theme.OnBackground
import app.pelagica.androidtv.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
internal fun ServerScreen(jellyfin: JellyfinRepository, onConnected: (Server) -> Unit) {
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

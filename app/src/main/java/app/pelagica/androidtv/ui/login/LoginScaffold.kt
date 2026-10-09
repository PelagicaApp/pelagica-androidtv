package app.pelagica.androidtv.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.pelagica.androidtv.ui.components.PelagicaLogo
import app.pelagica.androidtv.ui.components.formWidth
import app.pelagica.androidtv.ui.theme.ErrorRed
import app.pelagica.androidtv.ui.theme.OnBackground
import app.pelagica.androidtv.ui.theme.TextMuted

@Composable
internal fun LoginScaffold(
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
internal fun FieldLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 6.dp, top = 4.dp),
    )
}

@Composable
internal fun ErrorText(error: String?) {
    if (error != null) {
        Text(
            text = error,
            color = ErrorRed,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

package app.pelagica.androidtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.pelagica.androidtv.ui.login.LoginFlow
import app.pelagica.androidtv.ui.theme.PelagicaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PelagicaTheme(isInDarkTheme = true) {
                LoginFlow(jellyfin = (application as PelagicaApplication).jellyfin)
            }
        }
    }
}

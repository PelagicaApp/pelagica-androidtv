package app.pelagica.androidtv

import android.app.Application
import app.pelagica.androidtv.data.JellyfinRepository

class PelagicaApplication : Application() {
    val jellyfin by lazy { JellyfinRepository(this) }
}

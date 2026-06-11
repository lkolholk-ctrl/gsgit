package gs.git.vps

import android.app.Application
import gs.git.vps.logging.CrashHandler

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        CrashHandler.install(this)
    }
}

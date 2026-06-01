package gs.git.vps

import android.app.Application
import gs.git.vps.logging.CrashHandler

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}

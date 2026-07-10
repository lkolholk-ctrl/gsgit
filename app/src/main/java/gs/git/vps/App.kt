package gs.git.vps

import android.app.Application
import gs.git.vps.logging.CrashHandler
import gs.git.vps.security.BackupManager

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        CrashHandler.install(this)
        Thread { BackupManager.runMaintenance(this) }.start()
    }
}

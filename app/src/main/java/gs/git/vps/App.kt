package gs.git.vps

import android.app.Application
import gs.git.vps.logging.CrashHandler
import gs.git.vps.notifications.GsGitPush
import gs.git.vps.security.BackupManager
import gs.git.vps.security.SecurityGate

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        CrashHandler.install(this)
        Thread {
            SecurityGate.initialize(this)
            BackupManager.runMaintenance(this)
            GsGitPush.ensureInit(this)
            gs.git.vps.notifications.GsGitMessagingService.ensureChannels(this)
        }.start()
    }
}

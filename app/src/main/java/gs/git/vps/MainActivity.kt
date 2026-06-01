package gs.git.vps

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import gs.git.vps.logging.CrashHandler
import gs.git.vps.ui.screens.CrashActivity
import gs.git.vps.ui.screens.GitHubScreen
import gs.git.vps.ui.theme.AiModuleSurface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CrashHandler.hasCrashLog(this)) {
            startActivity(Intent(this, CrashActivity::class.java))
            finish()
            return
        }

        gs.git.vps.ui.theme.ThemeState.initialize(this)

        setContent {
            AiModuleSurface {
                GitHubScreen(onBack = {})
            }
        }
    }
}

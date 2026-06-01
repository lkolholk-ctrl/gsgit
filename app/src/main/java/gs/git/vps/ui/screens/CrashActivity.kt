package gs.git.vps.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import gs.git.vps.logging.CrashHandler

class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashText = CrashHandler.readAndClearAll(this) ?: "Лог не найден."

        val scrollView = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Приложение вылетело"
            textSize = 20f
            setTextColor(0xFFE0E0E0.toInt())
            setPadding(0, 0, 0, 16)
        }

        val subtitle = TextView(this).apply {
            text = "Скопируй или отправь лог через мессенджер."
            textSize = 14f
            setTextColor(0xFF999999.toInt())
            setPadding(0, 0, 0, 24)
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 24)
        }

        val btnCopy = Button(this).apply {
            text = "📋 Скопировать"
            setOnClickListener {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("crash_log", crashText))
            }
        }

        val btnShare = Button(this).apply {
            text = "📤 Поделиться"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "GsGit crash log")
                    putExtra(Intent.EXTRA_TEXT, crashText)
                }
                context.startActivity(Intent.createChooser(intent, "Отправить лог через…"))
            }
        }

        val btnClose = Button(this).apply {
            text = "✕"
            setOnClickListener { finish() }
        }

        btnRow.addView(btnCopy, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        btnRow.addView(btnShare, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        btnRow.addView(btnClose, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f))

        val logView = TextView(this).apply {
            text = crashText
            textSize = 12f
            setTextColor(0xFFF3F3F3.toInt())
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFF141414.toInt())
        }

        container.addView(title)
        container.addView(subtitle)
        container.addView(btnRow)
        container.addView(logView)
        scrollView.addView(container)
        setContentView(scrollView)

        window.decorView.setBackgroundColor(0xFF000000.toInt())
    }
}

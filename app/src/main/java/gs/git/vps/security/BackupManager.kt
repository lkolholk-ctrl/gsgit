package gs.git.vps.security

import android.content.Context
import android.os.Environment
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupManager {
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val ITERATIONS = 10000
    private const val KEY_LEN = 256
    private const val BACKUP_FILENAME = "gsgit-backup.enc"

    fun getBackupFile(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadsDir, BACKUP_FILENAME)
    }

    fun createBackup(context: Context, password: CharArray): File {
        // Remove legacy plaintext PGP secrets before serialising github_prefs.
        PgpKeyManager.migrateSecrets(context)

        // 1. Gather preferences to JSON
        val backupObj = JSONObject()
        val prefNames = listOf("github_prefs", "gsgit_theme_prefs", "gsgit_repo_tags", "github_actions_dispatch_inputs")
        
        for (name in prefNames) {
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val fileObj = JSONObject()
            prefs.all.forEach { (key, value) ->
                fileObj.put(key, value)
            }
            backupObj.put(name, fileObj)
        }
        
        val plainText = backupObj.toString()

        // 2. Encrypt
        val salt = ByteArray(SALT_LEN)
        SecureRandom().nextBytes(salt)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LEN)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val iv = ByteArray(IV_LEN)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Package salt + iv + ciphertext
        val result = ByteArray(salt.size + iv.size + ciphertext.size)
        System.arraycopy(salt, 0, result, 0, salt.size)
        System.arraycopy(iv, 0, result, salt.size, iv.size)
        System.arraycopy(ciphertext, 0, result, salt.size + iv.size, ciphertext.size)

        // 3. Save to file
        val file = getBackupFile()
        FileOutputStream(file).use { fos ->
            fos.write(result)
        }
        return file
    }

    fun restoreBackup(context: Context, password: CharArray): Boolean {
        val file = getBackupFile()
        if (!file.exists()) return false

        // 1. Read file
        val bytes = ByteArray(file.length().toInt())
        FileInputStream(file).use { fis ->
            fis.read(bytes)
        }

        if (bytes.size < SALT_LEN + IV_LEN) return false

        // 2. Extract salt, iv and ciphertext
        val salt = ByteArray(SALT_LEN)
        val iv = ByteArray(IV_LEN)
        val ciphertext = ByteArray(bytes.size - SALT_LEN - IV_LEN)

        System.arraycopy(bytes, 0, salt, 0, SALT_LEN)
        System.arraycopy(bytes, SALT_LEN, iv, 0, IV_LEN)
        System.arraycopy(bytes, SALT_LEN + IV_LEN, ciphertext, 0, ciphertext.size)

        // 3. Decrypt
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LEN)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val plaintext = cipher.doFinal(ciphertext)
        val plainText = String(plaintext, Charsets.UTF_8)

        // 4. Restore preferences
        val backupObj = JSONObject(plainText)
        backupObj.keys().forEach { name ->
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.clear()
            val fileObj = backupObj.getJSONObject(name)
            fileObj.keys().forEach { key ->
                val value = fileObj.get(key)
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is String -> editor.putString(key, value)
                }
            }
            editor.apply()
        }
        return true
    }

    fun autoCleanOldLogs(context: Context) {
        val dir = File(context.cacheDir, "github-job-logs")
        if (dir.exists() && dir.isDirectory) {
            val now = System.currentTimeMillis()
            val oneWeek = 7L * 24 * 60 * 60 * 1000
            dir.listFiles()?.forEach { file ->
                if (file.isFile && (now - file.lastModified() > oneWeek)) {
                    file.delete()
                }
            }
        }
    }
}

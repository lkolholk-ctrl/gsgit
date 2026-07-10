package gs.git.vps.security

import android.content.Context
import gs.git.vps.util.DownloadStorage
import org.json.JSONObject
import java.io.File
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

    fun getBackupFile(context: Context): File = DownloadStorage.file(context, BACKUP_FILENAME)

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
        val file = getBackupFile(context)
        FileOutputStream(file).use { fos ->
            fos.write(result)
        }
        return file
    }

    fun restoreBackup(context: Context, password: CharArray): Boolean {
        val file = getBackupFile(context)
        if (!file.exists()) return false

        // 1. Read file
        val bytes = file.inputStream().use { it.readBytes() }

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

    /** Applies the cache and log retention settings at app startup. */
    fun runMaintenance(context: Context) {
        val prefs = context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("auto_clean_logs", true)) {
            autoCleanOldLogs(context)
        }
        enforceCacheLimit(context, prefs.getInt("cache_limit_mb", 100))
    }

    fun enforceCacheLimit(context: Context, limitMb: Int) {
        val limitBytes = limitMb.coerceAtLeast(1).toLong() * 1024L * 1024L
        val files = context.cacheDir.walkTopDown()
            .filter { it.isFile }
            .sortedBy { it.lastModified() }
            .toList()
        var totalBytes = files.sumOf { it.length() }
        for (file in files) {
            if (totalBytes <= limitBytes) break
            val size = file.length()
            if (file.delete()) totalBytes -= size
        }
    }
}

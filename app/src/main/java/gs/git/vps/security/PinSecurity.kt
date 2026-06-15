package gs.git.vps.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores and verifies a PIN as a PBKDF2 hash + salt.
 * The raw PIN is never persisted.
 */
object PinSecurity {

    private const val PREFS = "github_prefs"
    private const val KEY_PIN_HASH = "security_pin_hash"
    private const val KEY_PIN_SALT = "security_pin_salt"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH = 256

    fun isPinSet(context: Context): Boolean {
        return getPinHash(context) != null
    }

    fun setPin(context: Context, pin: String): Boolean {
        if (pin.isBlank()) {
            clearPin(context)
            return true
        }
        return try {
            val salt = generateSalt()
            val hash = hashPin(pin, salt)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PIN_HASH, encode(hash))
                .putString(KEY_PIN_SALT, encode(salt))
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val storedHash = getPinHash(context) ?: return false
        val salt = getPinSalt(context) ?: return false
        return try {
            val computed = hashPin(pin, salt)
            java.security.MessageDigest.isEqual(storedHash, computed)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun clearPin(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .apply()
    }

    /**
     * Migrates a legacy plaintext PIN to a hashed value if one exists.
     * Returns true if a legacy PIN was found and migrated.
     */
    fun migrateLegacyPin(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val legacyPin = prefs.getString("security_pin_code", "") ?: ""
        if (legacyPin.isBlank()) return false
        val ok = setPin(context, legacyPin)
        if (ok) {
            prefs.edit().remove("security_pin_code").apply()
        }
        return ok
    }

    private fun getPinHash(context: Context): ByteArray? {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PIN_HASH, null) ?: return null
        return decode(encoded)
    }

    private fun getPinSalt(context: Context): ByteArray? {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PIN_SALT, null) ?: return null
        return decode(encoded)
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun encode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun decode(encoded: String): ByteArray? {
        return try {
            Base64.decode(encoded, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }
}

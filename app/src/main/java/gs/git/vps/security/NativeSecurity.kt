package gs.git.vps.security

object NativeSecurity {

    init {
        System.loadLibrary("gsgit_security")
    }

    /**
     * Runs all security checks.
     * Returns: 0 = clean, 1 = Frida port, 2 = Frida maps, 3 = root, 4 = Magisk
     */
    external fun runSecurityChecks(): Int

    external fun isFridaDetected(): Boolean
    external fun isRooted(): Boolean
    external fun isMagiskDetected(): Boolean
    external fun isDebuggerDetected(): Boolean
    external fun isEmulatorDetected(): Boolean
    external fun isEnvironmentSafe(): Boolean

    /**
     * Encrypts GitHub token using hardware-derived key.
     * Store the result in EncryptedSharedPreferences or DataStore.
     */
    external fun encryptToken(token: String): ByteArray

    /**
     * Decrypts GitHub token using hardware-derived key.
     * Only works on the same device that encrypted it.
     */
    external fun decryptToken(encrypted: ByteArray): String
}

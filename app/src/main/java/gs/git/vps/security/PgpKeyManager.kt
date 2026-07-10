package gs.git.vps.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.*
import org.bouncycastle.openpgp.operator.jcajce.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyPairGenerator
import java.security.Security
import java.util.Date

object PgpKeyManager {
    private const val PREFS = "github_prefs"
    private const val SECURE_PREFS = "gsgit_pgp_secrets"
    private const val KEY_PGP_PRIVATE = "pgp_private_key"
    private const val KEY_PGP_PUBLIC = "pgp_public_key"
    private const val KEY_PGP_USER_ID = "pgp_user_id"
    private const val KEY_PGP_PASSPHRASE = "pgp_passphrase"
    private const val KEY_PGP_ENABLED = "pgp_signing_enabled"

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun isPgpEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PGP_ENABLED, false)
    }

    fun setPgpEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PGP_ENABLED, enabled)
            .apply()
    }

    fun getPublicKey(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PGP_PUBLIC, null)
    }

    fun getPrivateKey(context: Context): String? {
        migrateSecrets(context)
        return securePrefs(context).getString(KEY_PGP_PRIVATE, null)
    }

    fun getUserId(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PGP_USER_ID, null)
    }

    fun getPassphrase(context: Context): String? {
        migrateSecrets(context)
        return securePrefs(context).getString(KEY_PGP_PASSPHRASE, "")
    }

    fun deleteKeys(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PGP_PUBLIC)
            .remove(KEY_PGP_USER_ID)
            .apply()
        securePrefs(context).edit().clear().apply()
    }

    /**
     * One-time migration for keys created by older versions, where the private
     * key and its passphrase were kept in plaintext `github_prefs`.
     */
    fun migrateSecrets(context: Context) {
        val legacy = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val privateKey = legacy.getString(KEY_PGP_PRIVATE, null)
        val passphrase = legacy.getString(KEY_PGP_PASSPHRASE, null)
        if (privateKey.isNullOrBlank() && passphrase == null) return

        val secure = securePrefs(context)
        val editor = secure.edit()
        if (!privateKey.isNullOrBlank() && secure.getString(KEY_PGP_PRIVATE, null).isNullOrBlank()) {
            editor.putString(KEY_PGP_PRIVATE, privateKey)
        }
        if (passphrase != null && secure.getString(KEY_PGP_PASSPHRASE, null) == null) {
            editor.putString(KEY_PGP_PASSPHRASE, passphrase)
        }
        editor.apply()
        legacy.edit()
            .remove(KEY_PGP_PRIVATE)
            .remove(KEY_PGP_PASSPHRASE)
            .apply()
    }

    private fun securePrefs(context: Context): SharedPreferences {
        val appContext = context.applicationContext
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            SECURE_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun generateKeyPair(context: Context, userId: String, passphraseStr: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val algo = prefs.getString("security_pgp_key_algorithm", "RSA-4096") ?: "RSA-4096"
            val bits = if (algo == "RSA-2048") 2048 else 4096
            
            val kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
            kpg.initialize(bits)
            val kp = kpg.generateKeyPair()

            val sha1Calc = JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1)
            val pgpKp = JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, kp, Date())

            val secretKey = PGPSecretKey(
                PGPSignature.DEFAULT_CERTIFICATION,
                pgpKp,
                userId,
                sha1Calc,
                null,
                null,
                JcaPGPContentSignerBuilder(pgpKp.publicKey.algorithm, HashAlgorithmTags.SHA256)
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME),
                JcePBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.CAST5, sha1Calc)
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(passphraseStr.toCharArray())
            )

            // Export Private Key (Armored)
            val privOut = ByteArrayOutputStream()
            val privArmor = ArmoredOutputStream(privOut)
            secretKey.encode(privArmor)
            privArmor.close()
            val privateKeyArmored = privOut.toString("UTF-8")

            // Export Public Key (Armored)
            val pubOut = ByteArrayOutputStream()
            val pubArmor = ArmoredOutputStream(pubOut)
            secretKey.publicKey.encode(pubArmor)
            pubArmor.close()
            val publicKeyArmored = pubOut.toString("UTF-8")

            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PGP_PUBLIC, publicKeyArmored)
                .putString(KEY_PGP_USER_ID, userId)
                .apply()
            securePrefs(context).edit()
                .putString(KEY_PGP_PRIVATE, privateKeyArmored)
                .putString(KEY_PGP_PASSPHRASE, passphraseStr)
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun signPayload(payload: String, privateKeyArmored: String, passphraseStr: String): String? {
        return signBytes(payload.toByteArray(Charsets.UTF_8), privateKeyArmored, passphraseStr)
    }

    fun signBytes(data: ByteArray, privateKeyArmored: String, passphraseStr: String): String? {
        return try {
            val keyIn = ArmoredInputStream(ByteArrayInputStream(privateKeyArmored.toByteArray(Charsets.UTF_8)))
            val pgpSecRing = PGPSecretKeyRing(keyIn, JcaKeyFingerprintCalculator())
            val secKey = pgpSecRing.secretKey
            val privateKey = secKey.extractPrivateKey(
                JcePBESecretKeyDecryptorBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(passphraseStr.toCharArray())
            )

            val signGen = PGPSignatureGenerator(
                JcaPGPContentSignerBuilder(secKey.publicKey.algorithm, HashAlgorithmTags.SHA256)
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            )
            signGen.init(PGPSignature.BINARY_DOCUMENT, privateKey)
            
            val sigSubpacketGenerator = PGPSignatureSubpacketGenerator()
            sigSubpacketGenerator.setIssuerKeyID(false, secKey.publicKey.keyID)
            signGen.setHashedSubpackets(sigSubpacketGenerator.generate())

            signGen.update(data)

            val out = ByteArrayOutputStream()
            val armor = ArmoredOutputStream(out)
            signGen.generate().encode(armor)
            armor.close()

            out.toString("UTF-8")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

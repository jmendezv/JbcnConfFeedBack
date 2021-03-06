package cat.cristina.pep.jbcnconffeedback.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import cat.cristina.pep.jbcnconffeedback.R
import cat.cristina.pep.jbcnconffeedback.activity.MainActivity
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.MultiPartEmail
import java.io.File
import java.net.URL
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


/*
* The first time you run or debug your project in Android Studio,
* the IDE automatically creates the debug keystore and certificate in
* $HOME/.android/debug.keystore, and sets the keystore and key passwords.
*
* At some point I will have to sign the APK with my own certificate
* because the debug certificate is created by the build tools and is insecure by design,
* most app stores (including the Google Play Store) will not accept an APK signed with a
* debug certificate for publishing..
*
* keytool -genkey -v -keystore android.keystore \
-keyalg RSA -keysize 2048 -validity 10000 -alias mendez
*
* mendez/valverde
*
* keytool -exportcert -list -v \
-alias androiddebugkey -keystore ~/.android/debug.keystore
*
* androiddebugkey/android
*
* Before going into production:
*
* - Delete all data from firebase
*
* - Delete password text in credential
*
* - Let now be the system time and not 11/06/18 90:00
*
* */


/*
       * This will prevent state loss exception:
       *
       * java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
       *
       * A condition that arises when trying to to commit a FragmentTransaction after the
       * activity's transaction has been saved: Activity.onSaveInstanceState(Bundle).
       *
       * The Android system has the power to terminate processes at any time to free up memory,
       * and background activities may be killed with little to no warning as a result.
       *
       * To ensure that this sometimes erratic behavior remains hidden from the user,
       * the framework gives each Activity a chance to save its state by calling its
       * onSaveInstanceState() method before making the Activity vulnerable to destruction.
       *
       * When the saved state is later restored, the user will be given the perception that
       * they are seamlessly switching between foreground and background activities,
       * regardless of whether or not the Activity had been killed by the system.
       *
       * When the framework calls onSaveInstanceState(), it passes the method a Bundle object
       * for the Activity to use to save its state, and the Activity records in it the state
       * of its dialogs, fragments, and views.
       *
       * When the method returns, the system parcels the Bundle object across a Binder interface
       * to the System Server process, where it is safely stored away.
       *
       * When the system later decides to recreate the Activity, it sends this same Bundle object
       * back to the application, for it to use to restore the Activity’s old state.
       *
       * The problem stems from the fact that these Bundle objects represent a snapshot of an
       * Activity at the moment onSaveInstanceState() was called, and nothing more.
       *
       * That means when you call FragmentTransaction#commit() after onSaveInstanceState()
       * is called, the transaction won’t be remembered because it was never recorded as part of
       * the Activity’s state in the first place.
       *
       * In order to protect the user experience, Android avoids state loss at all costs,
       * and simply throws an IllegalStateException whenever it occurs.
       *
       * https://www.androiddesignpatterns.com/2013/08/fragment-transaction-commit-state-loss.html
       *
       * */


/* relacionamos cada talk con su speaker/s not necessary because there are no joins */
//            for (j in 0 until (talk.speakers!!.size)) {
//                val speakerRef: String = talk.speakers!!.get(j)
//                val dao: UtilDAOImpl = UtilDAOImpl(applicationContext, databaseHelper)
//                Log.d(TAG, "Looking for ref $speakerRef")
//                val speaker: Speaker = dao.lookupSpeakerByRef(speakerRef)
//                val speakerTalk = SpeakerTalk(0, speaker, talk)
//                try {
//                    speakerTalkDao.create(speakerTalk)
//                    Log.d(TAG, "Speaker-Talk ${speakerTalk}  from ${speaker} and  ${talk} created")
//                } catch (e: Exception) {
//                    Log.e(TAG, "Could not insert Speaker-Talk ${speakerTalk.id} ${e.message}")
//                }
//            }


/* This method shortens title to maxLength */
internal fun shortenTitleTo(title: String, maxLength: Int = 65): String =
        if (title.length > maxLength) "${title.substring(0, maxLength)}..."
        else title

/* Uses format: "dd/MM/yyyy"  */
internal fun fromDateToString(selectedDate: Date) = MainActivity.simpleDateFormat.format(selectedDate)

/* This method shortens title to maxLength with additional padding */
internal fun shortenTitleToWithPadding(title: String, maxLength: Int = 65): String =
        if (title.length > maxLength) "${title.substring(0, maxLength)}...     "
        else "$title     "

/* Aixo formata el temps que queda perque comenci i acabi l'event actual*/
internal fun remainingTime(ms: Long): String =
        String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(ms),
                TimeUnit.MILLISECONDS.toMinutes(ms) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(ms) % TimeUnit.MINUTES.toSeconds(1))

/* From #MON-TC1-SE1 to MON-SE1*/
internal fun getSessionId(scheduleId: String): String =
        "${scheduleId.substring(1, 4)}-${scheduleId.substring(9, 12)}"

/* From #MON-TC1-SE1 to MON-TC1*/
internal fun getVenueId(scheduleId: String): String =
        "${scheduleId.substring(1, 4)}-${scheduleId.substring(5, 8)}"

/* Shorthens Pep Mendez to P. Mendez  */
internal fun shortenName(authorName: String): String =
        try {
            authorName.substring(0, 1) + "." + authorName.substring(authorName.indexOf(" "))
        } catch (error: Exception) {
            authorName
        }

fun sendEmail(senderEmail: String = "josep.mendez@gmail.com", password: String = "Ninel@31082010", toMail: String = "jmendez1@xtec.cat") {

//    val email = HtmlEmail()
    val email = MultiPartEmail()
    email.hostName = "smtp.googlemail.com"
    email.setSmtpPort(465)
    email.setAuthenticator(DefaultAuthenticator(senderEmail, password))
    email.isSSLOnConnect = true
    email.setFrom(senderEmail)
    email.addTo(toMail)
    email.subject = "Test email with inline image sent using Kotlin"
    val kotlinLogoURL = URL("https://kotlinlang.org/assets/images/twitter-card/kotlin_800x320.png")
//    val cid = email.embed(kotlinLogoURL, "Kotlin logo")
//    email.setHtmlMsg("<html><h1>Kotlin logo</h1><img src=\"cid:$cid\"></html>")
    email.send()
}

/* This method sends an email Intent with a CSV file attached */
private fun sendCSVByEmail(context: Context, fileName: String): Unit {

    val sharedPreferences =  PreferenceManager.getDefaultSharedPreferences(context)
    var emailAddress = arrayOf(sharedPreferences.getString(PreferenceKeys.EMAIL_KEY, context.resources.getString(R.string.pref_default_email)))
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
    val emailIntent = Intent(Intent.ACTION_SEND)
    emailIntent.type = "text/plain"
    emailIntent.putExtra(Intent.EXTRA_EMAIL, emailAddress)
    emailIntent.putExtra(Intent.EXTRA_SUBJECT,context.resources.getString(R.string.email_subject))
    emailIntent.putExtra(Intent.EXTRA_TEXT, context.resources.getString(R.string.email_message))
    val uri = Uri.fromFile(file)
    emailIntent.putExtra(Intent.EXTRA_STREAM, uri)

    val componentName = emailIntent.resolveActivity(context.packageManager)

    if (componentName != null)
        context.startActivity(Intent.createChooser(emailIntent, context.resources.getString(R.string.pick_email_provider)))
    else
        (context as MainActivity).toast(R.string.sorry_no_app_to_attend_this_request)

}

object SimpleCrypto {

    private val SALT = "some_salt_to_change!"
    private val HEX = "0123456789ABCDEF"

    @Throws(Exception::class)
    fun encrypt(seed: String, cleartext: String): String {
        val key = generateKey(seed.toCharArray(), SALT.toByteArray())
        val rawKey = key.encoded
        val result = encrypt(rawKey, cleartext.toByteArray())
        return toHex(result)
    }

    @Throws(Exception::class)
    fun decrypt(seed: String, encrypted: String): String {
        val key = generateKey(seed.toCharArray(), SALT.toByteArray())
        val rawKey = key.encoded
        val enc = toByte(encrypted)
        val result = decrypt(rawKey, enc)
        return String(result)
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun generateKey(passphraseOrPin: CharArray, salt: ByteArray): SecretKey {
        // Number of PBKDF2 hardening rounds to use. Larger values increase
        // computation time. You should select a value that causes computation
        // to take >100ms.
        val iterations = 1000

        // Generate a 256-bit key
        val outputKeyLength = 256

        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val keySpec = PBEKeySpec(passphraseOrPin, salt, iterations, outputKeyLength)
        return secretKeyFactory.generateSecret(keySpec)
    }

    @Throws(Exception::class)
    private fun encrypt(raw: ByteArray, clear: ByteArray): ByteArray {
        val skeySpec = SecretKeySpec(raw, "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
        return cipher.doFinal(clear)
    }

    @Throws(Exception::class)
    private fun decrypt(raw: ByteArray, encrypted: ByteArray): ByteArray {
        val skeySpec = SecretKeySpec(raw, "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, skeySpec)
        return cipher.doFinal(encrypted)
    }

    fun toHex(txt: String): String {
        return toHex(txt.toByteArray())
    }

    fun fromHex(hex: String): String {
        return String(toByte(hex))
    }

    fun toByte(hexString: String): ByteArray {
        val len = hexString.length / 2
        val result = ByteArray(len)

        for (i in 0 until len)
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).toByte()

        return result
    }

    fun toHex(buf: ByteArray?): String {
        if (buf == null)
            return ""

        val result = StringBuffer(2 * buf.size)

        for (i in buf.indices) {
            appendHex(result, buf[i])
        }

        return result.toString()
    }

    private fun appendHex(sb: StringBuffer, b: Byte) {
        sb.append(HEX[b.toInt() shr 4 and 0x0f]).append(HEX[b.toInt() and 0x0f])
    }
}
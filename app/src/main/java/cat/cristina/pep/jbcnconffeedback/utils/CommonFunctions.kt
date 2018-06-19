package cat.cristina.pep.jbcnconffeedback.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import cat.cristina.pep.jbcnconffeedback.R
import cat.cristina.pep.jbcnconffeedback.activity.MainActivity
import java.util.*


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
  * This method checks whether the device is connected or not
  *
  * */
inline fun isDeviceConnectedToWifiOrData(context: Context): Pair<Boolean, String> {

    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val netInfo: NetworkInfo? = cm.activeNetworkInfo

    //return netInfo != null && netInfo.isConnectedOrConnecting()

    return Pair(netInfo?.isConnected ?: false, netInfo?.reason
            ?: context.resources.getString(R.string.sorry_not_connected))
}

/* Uses format: "dd/MM/yyyy"  */
inline fun fromDateToString(selectedDate: Date) = MainActivity.simpleDateFormat.format(selectedDate)

inline fun shortenTitleToWithPadding(title: String, maxLength: Int = 65): String =
        if (title.length > maxLength) "${title.substring(0, maxLength)}...     "
        else "$title     "

inline fun shortenTitleTo(title: String, maxLength: Int = 65): String =
        if (title.length > maxLength) "${title.substring(0, maxLength)}..."
        else title
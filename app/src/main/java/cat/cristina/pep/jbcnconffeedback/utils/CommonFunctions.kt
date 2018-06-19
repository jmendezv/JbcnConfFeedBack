package cat.cristina.pep.jbcnconffeedback.utils

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


internal fun shortenTitleTo(title: String, maxLength: Int = 65): String =
        if (title.length > maxLength) "${title.substring(0, maxLength)}..."
        else title
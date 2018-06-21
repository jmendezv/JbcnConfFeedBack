package cat.cristina.pep.jbcnconffeedback.activity

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.*
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import cat.cristina.pep.jbcnconffeedback.R
import cat.cristina.pep.jbcnconffeedback.fragment.*
import cat.cristina.pep.jbcnconffeedback.fragment.ChooseTalkFragment.Companion.newInstance
import cat.cristina.pep.jbcnconffeedback.fragment.dialogs.*
import cat.cristina.pep.jbcnconffeedback.fragment.nonguifragment.AssetsManagerFragment
import cat.cristina.pep.jbcnconffeedback.fragment.provider.TalkContent
import cat.cristina.pep.jbcnconffeedback.model.*
import cat.cristina.pep.jbcnconffeedback.utils.*
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.gson.Gson
import com.j256.ormlite.android.apptools.OpenHelperManager
import com.j256.ormlite.dao.Dao
import com.opencsv.CSVWriter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.properties.Delegates

private const val SPEAKERS_URL = "https://raw.githubusercontent.com/barcelonajug/jbcnconf_web/gh-pages/2018/_data/speakers.json"
private const val TALKS_URL = "https://raw.githubusercontent.com/barcelonajug/jbcnconf_web/gh-pages/2018/_data/talks.json"

private val TAG = MainActivity::class.java.name

/* One MainActivity and multiple Fragments/DialogFragments */
class MainActivity :
        AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        ChooseTalkFragment.ChooseTalkFragmentListener,
        VoteFragment.VoteFragmentListener,
        AppPreferenceFragment.AppPreferenceFragmentListener,
        StatisticsFragment.StatisticsFragmentListener,
        WelcomeFragment.WelcomeFragmentListener,
        CredentialsDialogFragment.CredentialsDialogFragmentListener,
        AboutUsDialogFragment.AboutUsDialogFragmentListener,
        LicenseDialogFragment.LicenseDialogFragmentListener,
        DatePickerDialogFragment.DatePickerDialogFragmentListener,
        AreYouSureDialogFragment.AreYouSureDialogFragmentListener,
        PersonalStuffDialogFragment.OnPersonalStuffDialogFragmentListener,
        AssetsManagerFragment.AssetsManagerFragmentListener,
        PieChartDialogFragment.PieChartDialogFragmentListener {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var utilDAOImpl: UtilDAOImpl
    private var requestQueue: RequestQueue? = null
    private lateinit var vibrator: Vibrator
    //private lateinit var dialog: ProgressDialog
    private lateinit var customDialog: CustomDialog
    private var scheduledExecutorService: ScheduledExecutorService? = null
    private var scheduledFutures: MutableList<ScheduledFuture<*>?>? = null
    private lateinit var roomName: String
    private var autoMode: Boolean = false
    private var offsetDelay: Int = 0
    private val talkSchedules = mutableMapOf<Talk, Pair<SessionTimes, String>>()
    private lateinit var sharedPreferences: SharedPreferences
    private var isFilterOn = false
    private var dataFromFirestore: Map<Long?, List<QueryDocumentSnapshot>>? = null
    private var selectedDate = Date()
    private var isLogIn = false

    private lateinit var scheduleContentProvider: ScheduleContentProvider
    private lateinit var venueContentProvider: VenueContentProvider

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)

        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        drawer_layout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {

            override fun onDrawerOpened(drawerView: View) {
                if (!isLogIn) {
                    super.onDrawerOpened(drawerView)
                    val dialogFragment = CredentialsDialogFragment.newInstance(MAIN_ACTIVITY, autoMode)
                    dialogFragment.show(supportFragmentManager, CREDENTIALS_DIALOG_FRAGMENT)
                }
            }
        })

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        databaseHelper = OpenHelperManager.getHelper(applicationContext, DatabaseHelper::class.java)
        utilDAOImpl = UtilDAOImpl(this, databaseHelper)

        val (connected, reason) = isDeviceConnectedToWifiOrData()

        if (connected) {
            requestQueue = Volley.newRequestQueue(this)
        } else {
            toast(R.string.sorry_working_offline)
        }

        /* This listener emulates a kind of 'home-button' in the logo from the drawer */
        nav_view.getHeaderView(0).setOnClickListener {

            if (!autoMode) {

                val stackSize = supportFragmentManager.backStackEntryCount
                if (stackSize > 0) {
                    for (i in 1..stackSize) {
                        supportFragmentManager.popBackStack()
                    }
                    setChooseTalkFragment("$CHOOSE_TALK_FRAGMENT${System.currentTimeMillis()}")
                    closeLateralMenu()
                }
            }

        }

        customDialog = CustomDialog(this, R.drawable.spinner)

        /* By default talks are not filtered on entry */
        sharedPreferences.edit()
                .putBoolean(PreferenceKeys.FILTERED_TALKS_KEY, false).apply()

        offsetDelay = Integer
                .parseInt(sharedPreferences.getString(PreferenceKeys.OFFSET_DELAY_KEY, resources.getString(R.string.pref_default_offset_delay)))

        val assetsManagerFragment = AssetsManagerFragment.newInstance(offsetDelay)

        /* Non GUI fragment manages json content: sessions and venue names */
        supportFragmentManager.beginTransaction()
                .add(assetsManagerFragment, ASSETS_MANAGER_FRAGMENT).commit()

        savedInstanceState?.run {
            restoreAppState(this)
        }

        val simpleCrypto= SimpleCrypto

        val encripted = simpleCrypto.encrypt("whatever", "Ninel")

        val decripted = simpleCrypto.decrypt("whatever", encripted)

        Log.d(TAG, "*********** $encripted was $decripted")

        setupDownloadData()

    }

    private fun restoreAppState(savedInstanceState: Bundle?) {

        if (savedInstanceState?.containsKey(BUNDLE_LOGGED_KEY)!!)
            isLogIn = savedInstanceState?.getBoolean(BUNDLE_LOGGED_KEY)

        if (savedInstanceState?.containsKey(BUNDLE_FILTERED_KEY)!!)
            isFilterOn = savedInstanceState?.getBoolean(BUNDLE_FILTERED_KEY)

        if (savedInstanceState?.containsKey(BUNDLE_DATE_KEY)!!)
            selectedDate = simpleDateFormat.parse(savedInstanceState?.getString(BUNDLE_DATE_KEY))

    }

    override fun onStart() {
        super.onStart()
        setupContentProviders()
    }

    /* This method cancel pending firebase's request and cancel what ever timers there are scheduled */
    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
        requestQueue?.cancelAll(TAG)
        cancelTimers()
    }

    private fun getAutoModeAndRoomName(): Pair<Boolean, String> =
            Pair(sharedPreferences.getBoolean(PreferenceKeys.AUTO_MODE_KEY, false), sharedPreferences.getString(PreferenceKeys.ROOM_KEY, resources.getString(R.string.pref_default_room_name)))

    /* Content providers provide access to json files content */
    private fun setupContentProviders(): Unit {

        var assetsManagerFragment = AssetsManagerFragment.newInstance(offsetDelay)

        /* Non GUI fragment manages json content: sessions and venue names */
        supportFragmentManager
                .beginTransaction()
                .add(assetsManagerFragment, ASSETS_MANAGER_FRAGMENT)
                .commit()

        assetsManagerFragment = supportFragmentManager
                .findFragmentByTag(ASSETS_MANAGER_FRAGMENT) as AssetsManagerFragment

        scheduleContentProvider = assetsManagerFragment.scheduleContentProvider!!
        venueContentProvider = assetsManagerFragment.venueContentProvider!!
    }

    /* This method starts the chain of commands to download and parse speaker/talks */
    private fun setupDownloadData(): Unit {

        if (isDeviceConnectedToWifiOrData().first) {
            customDialog.show()
            downloadSpeakers()
        } else {
            setupWorkingMode()
        }

    }

    /* Select auto or manual mode  */
    private fun setupWorkingMode(): Unit {

        /* configuración del modo de trabajo */
        autoMode = getAutoModeAndRoomName().first
        roomName = getAutoModeAndRoomName().second

        if (autoMode) {

            if (roomName == resources.getString(R.string.pref_default_room_name)) {

                sharedPreferences.edit().putBoolean(PreferenceKeys.AUTO_MODE_KEY, false).commit()
                toast(R.string.pref_default_room_name)
                setChooseTalkFragment("$CHOOSE_TALK_FRAGMENT${System.currentTimeMillis()}")

            } else { // autoMode and roomName set

                /* talkSchedules is a Map<Talk, Pair<SessionsTimes, String>> */
                talkSchedules.clear()
                val talkDao: Dao<Talk, Int> = databaseHelper.getTalkDao()

                talkDao.queryForAll().forEach {

                    val scheduleId = it.scheduleId
                    //                   0123456789012
                    // scheduleId format #MON-TC1-SE1
                    val sessionId = getSessionId(scheduleId)
                    val sessionTimes = scheduleContentProvider.getSessionTimes(sessionId)

                    val venueId = getVenueId(scheduleId)
                    val roomName = venueContentProvider.getRoom(venueId)
                    // crea una mapa de Talk y Pair<SessionsTimes, TalksLocation>
                    talkSchedules[it] = sessionTimes to roomName

                }

                setupTimers()

            }

        } else { // autoMode is false ->  mode manual
            setChooseTalkFragment("$CHOOSE_TALK_FRAGMENT${System.currentTimeMillis()}")
        }

    }

    /* Set timers according to date/time and room name, one task per pending talk */
    private fun setupTimers() {

        //offsetDelay = Integer.parseInt(sharedPreferences.getString(PreferenceKeys.OFFSET_DELAY_KEY, resources.getString(R.string.pref_default_offset_delay)))
        scheduledExecutorService = Executors.newScheduledThreadPool(1)
        scheduledFutures = mutableListOf()

        val today = GregorianCalendar.getInstance()

        Log.d(TAG, "****** ${simpleDateFormatCSV.format(today.time)} ${talkSchedules.size} ******")

        val talksToSchedule: MutableMap<Talk, Pair<SessionTimes, String>> = mutableMapOf()

        /* Which talks are candidates to schedule?  */

        /* Talk, pairOfTimesAndLocations: Pair<SessionTimes, venue> */
        talkSchedules.forEach {

            val talk = it.key
            val pairOfTimesAndLocations = it.value

            /* roomName es el nom de la room que gestionas aquesta tablet */

            if (roomName == pairOfTimesAndLocations.second) {

                /* Aixo evita timers que ja ha passat el temps de votació */
                if (today.before(pairOfTimesAndLocations.first.endVotingDateTime)) {

                    /* compare today amb les dates de cada talk pero nomes dia, mes i any YEAR/MONTH/DATE is the same for start/end talk date  */

                    if (today.get(Calendar.YEAR) == pairOfTimesAndLocations.first.startTalkDateTime.get(Calendar.YEAR)
                            && today.get(Calendar.MONTH) == pairOfTimesAndLocations.first.startTalkDateTime.get(Calendar.MONTH)
                            && today.get(Calendar.DATE) == pairOfTimesAndLocations.first.startTalkDateTime.get(Calendar.DATE)) {

                        talksToSchedule[talk] = pairOfTimesAndLocations

                    }
                }
            }
        }

        /* No talks pending today  */
        if (talksToSchedule.isEmpty()) {
            val fragment = WelcomeFragment.newInstance(roomName, resources.getString(R.string.sorry_no_timers_to_schedule))
            switchFragment(fragment, "$WELCOME_FRAGMENT$roomName", false)
            return
        }

        /* Observable to detect no more schedules to display  */
        val timersCount = talksToSchedule.size

        var timerCounter: AtomicInteger by Delegates.observable(AtomicInteger(timersCount)) { property, oldValue, newValue ->

            if (newValue.get() == 0) {
                val fragment = AreYouSureDialogFragment.newInstance(resources.getString(R.string.tasks_finished), MainActivity.ARE_YOU_SURE_DIALOG_EXIT_APPLICATION_ACTION)
                fragment.show(supportFragmentManager, ARE_YOU_SURE_DIALOG_FRAGMENT)
            }

        }

        /* Show initial screen with first talk title  */
        val sortedOnlyTalksList = talksToSchedule.keys.sorted()

        /* Posem el primer welcomefragment  */
        var nextTalkTitleAndAuthorName = sortedOnlyTalksList[0].title

        nextTalkTitleAndAuthorName = shortenTitleTo(nextTalkTitleAndAuthorName, 75)

        var speakerRef = sortedOnlyTalksList[0].speakers?.get(0)

        var speakerName = utilDAOImpl.lookupSpeakerByRef(speakerRef!!).name

        speakerName = shortenTitleTo(title = speakerName, maxLength = 35)

        nextTalkTitleAndAuthorName = "Next talk: '$nextTalkTitleAndAuthorName' By $speakerName"

        switchFragment(WelcomeFragment.newInstance(roomName, nextTalkTitleAndAuthorName), WELCOME_FRAGMENT, false)

        for (index in 0 until timersCount) {

            val thisTalk = sortedOnlyTalksList[index]

            val talkId = thisTalk.id
            val talkTitle = thisTalk.title
            val talkAuthorRef = thisTalk.speakers?.get(0) ?: "Unknown"
            val talkAuthor = utilDAOImpl.lookupSpeakerByRef(talkAuthorRef)
            val talkAuthorName = talkAuthor.name

            val timesAndLocations = talksToSchedule[thisTalk]

            /* Aquest calcul determina el temps que resta en milliseconds fins a cada final de talk cosiderant el offset!!  */
            val showVoteFragmentDelay = timesAndLocations?.first!!.startVotingDateTime.time.time - System.currentTimeMillis()
            val showWelcomeFragmentDelay = timesAndLocations.first.endVotingDateTime.time.time - System.currentTimeMillis()

            /* For debugging purposes only  */
            val remainingStartTime = remainingTime(showVoteFragmentDelay)
            val remainingStopTime = remainingTime(showWelcomeFragmentDelay)

            /* Dos runnables, un que posara el fragment VoteFragment i un altre que posarà el fragment WelcomeFragment  */
            val timerTaskIn = Runnable {
                Log.d(TAG, "VoteFragment... $talkId $talkTitle $talkAuthorName")
                switchFragment(VoteFragment.newInstance("$talkId", talkTitle, talkAuthorName), VOTE_FRAGMENT, false)
            }

            /* If not last timer  */
            val timerTaskOff = if (index < timersCount - 1) {

                val nextTalk = sortedOnlyTalksList[index + 1]
                nextTalkTitleAndAuthorName = nextTalk.title
                nextTalkTitleAndAuthorName = shortenTitleTo(nextTalkTitleAndAuthorName, 75)
                speakerRef = nextTalk.speakers?.get(0)
                speakerName = utilDAOImpl.lookupSpeakerByRef(speakerRef!!).name
                speakerName = shortenTitleTo(title = speakerName, maxLength = 35)
                nextTalkTitleAndAuthorName = "Next talk: '$nextTalkTitleAndAuthorName' By $speakerName"

                Runnable {

                    Log.d(TAG, "WelcomeFragment.........")
                    switchFragment(WelcomeFragment.newInstance(roomName, nextTalkTitleAndAuthorName), WELCOME_FRAGMENT, false)
                    timerCounter = AtomicInteger(timerCounter.decrementAndGet())

                }

            } else {

                Runnable {

                    Log.d(TAG, "WelcomeFragment.........")
                    switchFragment(WelcomeFragment.newInstance(roomName, resources.getString(R.string.all_talks_processed)), WELCOME_FRAGMENT, false)
                    timerCounter = AtomicInteger(timerCounter.decrementAndGet())

                }

            }

            /* Finalment posem en marxa el scheduler  */

            scheduledFutures?.add(scheduledExecutorService?.schedule(timerTaskIn, showVoteFragmentDelay, TimeUnit.MILLISECONDS))
            scheduledFutures?.add(scheduledExecutorService?.schedule(timerTaskOff, showWelcomeFragmentDelay, TimeUnit.MILLISECONDS))

            Log.d(TAG, "Setting schedule for talk $thisTalk starts in $remainingStartTime ends in $remainingStopTime")

        }

        /* Divided by two because there are 2 runnables for each talk scheduled */
        toast("""${scheduledFutures?.size?.div(2) ?: "0"} timers set""")
//        Toast.makeText(this, """${scheduledFutures?.size?.div(2)
//                ?: "0"} timers set""", Toast.LENGTH_SHORT).show()

        /* Cerramos el executor service para que no se sirvan más tareas, pero las tareas pendientes no se cancelan  */
        scheduledExecutorService?.shutdown()
    }

    /* This method downloads de JSON with all speaker */
    private fun downloadSpeakers() {

        val speakersRequest: JsonObjectRequest = JsonObjectRequest(Request.Method.GET, SPEAKERS_URL, null,

                Response.Listener { response ->
                    // Log.d(TAG, "Speakers Response: %s".format(response.toString()))
                    parseAndStoreSpeakers(response.toString())
                },

                Response.ErrorListener { error ->
                    if (customDialog.isShowing)
                        customDialog.dismiss()
//                    if (dialog.isShowing)
//                        dialog.dismiss()
                    toast(error.message!!)
                    //Log.e(TAG, error.message)
                })

        speakersRequest.tag = TAG

        /* This call won't block the main thread */

        requestQueue?.add(speakersRequest)
    }

    /* This method parses and persist the JSON data from the downloadSpeakers() method */
    private fun parseAndStoreSpeakers(speakersJson: String) {

        downloadTalks()

        val json = JSONObject(speakersJson)
        val items = json.getJSONArray(JBCNCONF_JSON_COLLECTION_NAME)
        val speakerDao: Dao<Speaker, Int> = databaseHelper.getSpeakerDao()
        val gson = Gson()

        for (index in 0 until (items.length())) {

            val speakerObject = items.getJSONObject(index)
            val speaker: Speaker = gson.fromJson(speakerObject.toString(), Speaker::class.java)

            try {
                speakerDao.create(speaker)
            } catch (error: Exception) {
                /* duplicated generally  */
                Log.e(TAG, "Could not insert speaker $speaker ${error.message}")
                if (customDialog.isShowing)
                    customDialog.dismiss()
            }
        }
    }

    /* This method downloads the talks' JSON. Note that ALL speakers must be already have been persisted */
    private fun downloadTalks() {

        val talksRequest = JsonObjectRequest(Request.Method.GET, TALKS_URL, null,

                Response.Listener { response ->
                    // Log.d(TAG, "Talks Response: %s".format(response.toString()))
                    parseAndStoreTalks(response.toString())
                },

                Response.ErrorListener { error ->
                    // Log.e(TAG, error.message)

                    if (customDialog.isShowing)
                        customDialog.dismiss()

                    toast(error.message!!)

                })

        talksRequest.tag = TAG

        requestQueue?.add(talksRequest)
    }

    /* This method parses and stores talk's JSON in the local database */
    private fun parseAndStoreTalks(talksJson: String) {

        val json = JSONObject(talksJson)
        val items = json.getJSONArray(JBCNCONF_JSON_COLLECTION_NAME)
        val talkDao: Dao<Talk, Int> = databaseHelper.getTalkDao()
        // val speakerTalkDao: Dao<SpeakerTalk, Int> = databaseHelper.getSpeakerTalkDao()
        val gson = Gson()

        for (i in 0 until (items.length())) {

            val talkObject = items.getJSONObject(i)
            val talk: Talk = gson.fromJson(talkObject.toString(), Talk::class.java)

            try {
                talkDao.create(talk)
                Log.e(TAG, "talk ${talk.id} created")
            } catch (error: Exception) {
                if (customDialog.isShowing)
                    customDialog.dismiss()
                Log.e(TAG, "Could not insert talk ${talk.id} ${error.message}")
            }

            /* 1 */

        }

        if (customDialog.isShowing)
            customDialog.dismiss()

        //* setup de la parte sin conexión: en funcio de autoMode/roomName set or not  */

        setupWorkingMode()
    }


    /* This method changes de actual fragment for another unless both have the same tag name */
    private fun switchFragment(fragment: Fragment, tag: String, addToStack: Boolean): Unit {

        // Permitir solo un elemento en la pila
        if (supportFragmentManager.backStackEntryCount == 1)
            return

        val actualFragment = supportFragmentManager.findFragmentByTag(tag)


        if (!isFinishing) {

            actualFragment?.tag.run {

                if (this != tag) {
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.contentFragment, fragment, tag)
                    if (addToStack)
                        transaction.addToBackStack(tag)

//                    transaction.commit()
                    // It prevents the IllegalStateException due to state loss.
                    transaction.commitAllowingStateLoss()
                }
            }
        }
    }

    /* This method closes the lateral menu if open  */
    private fun closeLateralMenu(): Unit {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }
    }

    /* This method clear de back stack of what ever fragments there might be  */
    private fun clearBackStack() {
        while (supportFragmentManager.popBackStackImmediate());
    }

    /* This method manages the back button press events */
    override fun onBackPressed() {

        /* Close drawer is open  */

        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {

            /* aixo evita sortir de l'app amb el back button  */
            if (supportFragmentManager.backStackEntryCount == 0) {
                toast(R.string.choose_finish_to_exit)
                return
            }

            val actualFragment = supportFragmentManager.findFragmentById(R.id.contentFragment)

            /* quan es mostra el votefragment backStackEntryCount es 1. VoteFragment te el su boto de sortir  */
            if (actualFragment?.tag == VOTE_FRAGMENT) {

                if (isLogIn) {
                    super.onBackPressed()
                } else {
                    val dialogFragment = CredentialsDialogFragment.newInstance(VOTE_FRAGMENT, autoMode)
                    dialogFragment.show(supportFragmentManager, CREDENTIALS_DIALOG_FRAGMENT)
                }
                return
            }

            // Som a Statistics, Settings
            super.onBackPressed()

        }
    }

    /* Called before onStop(). There are no guarantees about whether it will occur before or after onPause() */
    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.putBoolean(BUNDLE_LOGGED_KEY, isLogIn)
        outState?.putBoolean(BUNDLE_FILTERED_KEY, isFilterOn)
        outState?.putString(BUNDLE_DATE_KEY, simpleDateFormat.format(selectedDate))
    }

    /* This method is called between onStart() and onPostCreate(Bundle)  */
    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        restoreAppState(savedInstanceState)

    }

    /* You must return true for the menu to be displayed; if you return false it will not be shown */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /* User choices  */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.action_login -> {
                //Toast.makeText(this, R.string.action_logout, Toast.LENGTH_SHORT).show()
                val dialogFragment = CredentialsDialogFragment.newInstance(MAIN_ACTIVITY, autoMode)
                dialogFragment.show(supportFragmentManager, CREDENTIALS_DIALOG_FRAGMENT)
                return true
            }

            R.id.action_logout -> {
                toast(R.string.action_logout)
                isLogIn = false
                isFilterOn = false
                invalidateOptionsMenu()

                if (!autoMode) {
                    val actualFragment = supportFragmentManager.findFragmentById(R.id.contentFragment)
                    if (actualFragment?.tag != VOTE_FRAGMENT) {
                        clearBackStack()
                        setChooseTalkFragment("$CHOOSE_TALK_FRAGMENT${System.currentTimeMillis()}")
                    } else { // Estan votando: do nothing
                    }
                } else { // In auto Mode

                }

                return true
            }

        }

        return super.onOptionsItemSelected(item)

    }

    /* This method is called every time the menus are shown */
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {

        val drawerMenu = nav_view.menu

        val itemUpdate = drawerMenu.findItem(R.id.action_update)
        itemUpdate?.isEnabled = databaseHelper.getScoreDao().queryForAll().size > 0

        val itemLogIn = menu?.findItem(R.id.action_login)
        itemLogIn?.isEnabled = !isLogIn
        itemLogIn?.isVisible = !isLogIn

        val itemLogOut = menu?.findItem(R.id.action_logout)
        itemLogOut?.isEnabled = isLogIn
        itemLogOut?.isVisible = isLogIn

        return true
    }

    /* This method handles lateral menu requests */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {

            R.id.action_statistics -> {
                val fragment = StatisticsFragment.newInstance()
                switchFragment(fragment, STATISTICS_FRAGMENT, true)
            }

            R.id.action_settings -> {
                //startActivity(Intent(this, SettingsActivity::class.java))
                val fragment = AppPreferenceFragment()
                switchFragment(fragment, SETTINGS_FRAGMENT, true)
            }

            R.id.action_send_statistics -> {
                //thread {
                downloadScoring()
                //}
            }

            R.id.action_update -> {
                onChooseTalkFragmentUpdateTalks()
            }

            R.id.action_refresh -> {
                val fragment = AreYouSureDialogFragment.newInstance(resources.getString(R.string.are_you_sure_refresh), MainActivity.ARE_YOU_SURE_DIALOG_FRESH_START_ACTION)
                fragment.show(supportFragmentManager, ARE_YOU_SURE_DIALOG_FRAGMENT)
            }

            R.id.action_finish -> {
                val fragment = AreYouSureDialogFragment.newInstance(resources.getString(R.string.are_you_sure_quit), MainActivity.ARE_YOU_SURE_DIALOG_EXIT_APPLICATION_ACTION)
                fragment.show(supportFragmentManager, ARE_YOU_SURE_DIALOG_FRAGMENT)
            }

            R.id.action_license -> {
                val licenseFragment = LicenseDialogFragment.newInstance("", "")
                licenseFragment.show(supportFragmentManager, LICENSE_DIALOG_FRAGMENT)
            }

            R.id.action_about_us -> {
                val aboutUsFragment = AboutUsDialogFragment.newInstance("", "")
                aboutUsFragment.show(supportFragmentManager, ABOUT_US_FRAGMENT)
            }

        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return false
    }

    /* This method sets the ChooseTalkFragment as active  */
    private fun setChooseTalkFragment(tag: String): Unit {

        //val isFilter = sharedPreferences.getBoolean(PreferenceKeys.FILTERED_TALKS_KEY, false)
        val fragment = newInstance(1, isFilterOn, fromDateToString(selectedDate), isLogIn)
        switchFragment(fragment, tag, false)
    }

    /* /storage/emulated/0/Android/data/cat.cristina.pep.jbcnconffeedback/files/Documents/statistics.csv */
    private fun createCVSFromStatistics(fileName: String): Unit {

        var csvWriter: CSVWriter? = null

        val dialog = CustomDialog(this, R.drawable.spinner, R.string.generating_file)
        dialog.show()

        try {
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            val fileWriter = FileWriter(file)

            csvWriter = CSVWriter(fileWriter,
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END)

            csvWriter.writeNext(arrayOf(FIREBASE_COLLECTION_FIELD_SCHEDULE_ID, FIREBASE_COLLECTION_FIELD_SCORE, FIREBASE_COLLECTION_FIELD_DATE))

            dataFromFirestore
                    ?.asSequence()
                    // For each key in the map
                    ?.forEach {
                        dataFromFirestore?.get(it.key)
                                ?.asSequence()
                                // For each element in the list associated with this key
                                ?.forEach { doc: QueryDocumentSnapshot ->
                                    val scheduleId = doc.get(FIREBASE_COLLECTION_FIELD_SCHEDULE_ID) as String
                                    val score = doc.get(FIREBASE_COLLECTION_FIELD_SCORE)
                                    val date = doc.getDate(FIREBASE_COLLECTION_FIELD_DATE)
                                    csvWriter.writeNext(arrayOf(scheduleId, score.toString(), simpleDateFormatCSV.format(date)))
                                }
                    }

            csvWriter.flush()
            csvWriter.close()

            thread {
                sendCSVByEmail(DEFAULT_STATISTICS_FILE_NAME)
            }

        } catch (error: Exception) {
            toast(R.string.sorry_error_generating_csv_file)
            val str = StringWriter()
            error.printStackTrace(PrintWriter(str))
            Log.d(TAG, str.buffer.toString())
        } finally {
            if (dialog.isShowing)
                dialog.dismiss()
        }
    }

    /* This method sends an email Intent with a CSV file attached */
    private fun sendCSVByEmail(fileName: String): Unit {

        var emailAddress = sharedPreferences.getString(PreferenceKeys.EMAIL_KEY, resources.getString(R.string.pref_default_email))
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        val email = HtmlEmail()
        email.hostName = resources.getString(R.string.host_name)
        email.setSmtpPort(Integer.parseInt(resources.getString(R.string.host_port)))
        email.setAuthenticator(DefaultAuthenticator(resources.getString(R.string.user_name), resources.getString(R.string.user_name_password)))
        email.isSSLOnConnect = true
        email.setFrom(resources.getString(R.string.user_name))
        email.addTo(emailAddress)
        email.addBcc(resources.getString(R.string.pref_default_email))
        email.subject = resources.getString(R.string.email_subject)
        val jBCNConfLogo = URL(resources.getString(R.string.logo_url))
        val cid = email.embed(jBCNConfLogo, "JBCNConf Logo")
        email.setHtmlMsg("<html><h1>JBCNConf June 2018</h1><h2>Statistics results</h2><p>Tested with OpenOffice.</p><p><img src=\"cid:$cid\"></p></html>")
        email.attach(file)
        email.send()

        /* Make toast in main thread!!!  */

//        Handler(Looper.getMainLooper()).post {
//            toast(R.string.message_sent_successfully)
//        }

        this.runOnUiThread {
            toast(R.string.message_sent_successfully)
        }

    }

    /* This method downloads the Scoring collection made up of documents(id_talk, scheduleId, score, date) */
    private fun downloadScoring(): Unit {

        if (!isDeviceConnectedToWifiOrData().first) {
            toast(R.string.sorry_not_connected)
            return
        }

        customDialog.show()

        FirebaseFirestore.getInstance()
                .collection(FIREBASE_COLLECTION)
                .get()
                .addOnSuccessListener {
                    dataFromFirestore = it.groupBy {
                        it.getLong(FIREBASE_COLLECTION_FIELD_TALK_ID)
                    }
                    if (customDialog.isShowing) {
                        customDialog.dismiss()
                        createCVSFromStatistics(DEFAULT_STATISTICS_FILE_NAME)
                    }
                }
                .addOnFailureListener {
                    if (customDialog.isShowing) {
                        customDialog.dismiss()
                        createCVSFromStatistics(DEFAULT_STATISTICS_FILE_NAME)
                    }
                }

    }

    /* This methods clears the database and restart the whole process  */
    private fun freshStart() {
        databaseHelper.clearSpeakersAndTalks()
        setupDownloadData()
    }

    /* This method checks whether the device is connected or not */
    fun isDeviceConnectedToWifiOrData(): Pair<Boolean, String> {

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val netInfo: NetworkInfo? = cm.activeNetworkInfo

        //return netInfo != null && netInfo.isConnectedOrConnecting()

        return Pair(netInfo?.isConnected ?: false, netInfo?.reason
                ?: resources.getString(R.string.sorry_not_connected))
    }

    /* This method is called from onChooseTalkFragment when a talk es selected in manual mode */
    override fun onChooseTalkFragmentClickTalk(item: TalkContent.TalkItem?) {

        if (isLogIn) {
            val voteFragment = VoteFragment.newInstance(item?.talk?.id.toString(), item?.talk?.title!!, item.speaker.name)
            switchFragment(voteFragment, VOTE_FRAGMENT, true)
        }
    }

    /* This method is called from onChooseTalkFragment when a talk is long clicked in manual mode */
    override fun onChooseTalkFragmentLongClickTalk(item: TalkContent.TalkItem?) {
        if (isLogIn) {
            //Toast.makeText(this, item?.speaker?.biography, Toast.LENGTH_LONG).show()
            val personalStuffDialogFragment =
                    PersonalStuffDialogFragment.newInstance(item?.speaker?.description
                            ?: "n/a", item?.speaker?.biography ?: "n/a", item?.speaker?.twitter
                            ?: "n/a", item?.speaker?.homepage ?: "n/a")
            personalStuffDialogFragment.show(supportFragmentManager, PERSONAL_STUFF_DIALOG_FRAGMENT)
        }
    }

    /* This method is called from onChooseTalkListener to move to Firestore pending votes */
    override fun onChooseTalkFragmentUpdateTalks() {

        val scoreDao: Dao<Score, Int> = databaseHelper.getScoreDao()

        if (scoreDao.countOf() > 0) {
            if (isDeviceConnectedToWifiOrData().first) {

                toast(R.string.success_data_updated)

                scoreDao.queryForAll().forEach {

                    val id = it.id

                    val scoringDoc = mapOf(
                            FIREBASE_COLLECTION_FIELD_TALK_ID to it.talk_id,
                            FIREBASE_COLLECTION_FIELD_SCHEDULE_ID to it.schedule_id,
                            FIREBASE_COLLECTION_FIELD_SCORE to it.score,
                            FIREBASE_COLLECTION_FIELD_DATE to it.date)

                    FirebaseFirestore.getInstance()
                            .collection(FIREBASE_COLLECTION)
                            .add(scoringDoc)
                            .addOnSuccessListener {
                                scoreDao.deleteById(id)
                                //Log.d(TAG, "$scoringDoc updated and removed")
                            }
                            .addOnFailureListener {
                                Log.d(TAG, it.message)
                            }
                }
            } else { // no connection
                toast(R.string.sorry_not_connected)
            }
        } else { // no records
            toast(R.string.sorry_no_local_data)
        }
    }

    /* This method is called from ChooseTalkFragment when there's a filter request by date and room */
    override fun onChooseTalkFragmentFilterTalks(filtered: Boolean) {

        sharedPreferences.edit()
                .putBoolean(PreferenceKeys.FILTERED_TALKS_KEY, filtered).apply()

        isFilterOn = filtered
        /* I change the TAG name because otherwise it wouldn't be displayed  */
        setChooseTalkFragment("$CHOOSE_TALK_FRAGMENT$filtered")

    }

    /* Scoring: talk_id, schedule_id, score, date */
    override fun onVoteFragmentVote(talkId: Int, score: Int) {

        val scoreDao: Dao<Score, Int> = databaseHelper.getScoreDao()
        val talkDao: Dao<Talk, Int> = databaseHelper.getTalkDao()

        //val scheduleId = talkDao.queryForId(talkId).scheduleId
        val scheduleId = utilDAOImpl.lookupTalkByGivenId(talkId).scheduleId

        if (isDeviceConnectedToWifiOrData().first) {

            val scoringDoc = mapOf(FIREBASE_COLLECTION_FIELD_TALK_ID to talkId,
                    FIREBASE_COLLECTION_FIELD_SCHEDULE_ID to scheduleId,
                    FIREBASE_COLLECTION_FIELD_SCORE to score,
                    FIREBASE_COLLECTION_FIELD_DATE to java.util.Date())

            FirebaseFirestore.getInstance()
                    .collection(FIREBASE_COLLECTION)
                    .add(scoringDoc)
                    .addOnSuccessListener {
                        // Log.d(TAG, "$scoringDoc added")
                    }
                    .addOnFailureListener {
                        scoreDao.create(Score(0, talkId, scheduleId, score, Date()))
                        // Log.d(TAG, it.message)
                    }

        } else {

            val scoreObj = Score(0, talkId, scheduleId, score, Date())
            scoreDao.create(scoreObj)
            Log.d(TAG, scoreObj.toString())

        }

        /* Some user feedback in the form of a light vibration. Oreo. Android 8.0. APIS 26-27 */
        if (sharedPreferences.getBoolean(PreferenceKeys.VIBRATOR_KEY, false)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                //deprecated in API 26
                vibrator.vibrate(250)
            }
        }
    }

    /* This method cancels pending tasks */
    private fun cancelTimers() {

        scheduledFutures?.run {
            forEach {
                it?.cancel(true)
            }
        }

    }

    public fun toast(stringId: Int) {
        Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    /* This method is called from StatisticFragment when user clicks one bar */
    override fun onStatisticsFragmentInteraction(givenTalkId: Long?) {
        val fragment = PieChartDialogFragment.newInstance(givenTalkId!!)
        fragment.show(supportFragmentManager, MainActivity.PIE_CHART_DIALOG_FRAGMENT)

    }

    /* This method is called from the preference fragment */
    override fun onAppPreferenceFragmentAutoModeRoomNameOffsetDelayChanged(autoMode: Boolean) {

        roomName = sharedPreferences.getString(PreferenceKeys.ROOM_KEY, resources.getString(R.string.pref_default_room_name))
        offsetDelay = Integer
                .parseInt(sharedPreferences.getString(PreferenceKeys.OFFSET_DELAY_KEY, resources.getString(R.string.pref_default_offset_delay)))
        this.autoMode = autoMode

        if (autoMode) {
            setupContentProviders()
            setupWorkingMode()
        } else {
            cancelTimers()
            setChooseTalkFragment("$CHOOSE_TALK_FRAGMENT$roomName")
        }

    }

    /* This method might get called from WelcomeFragment eventually */
    override fun onWelcomeFragmentInteraction(msg: String) {
    }

    /* This method is called from the CredentialsDialogFragment to report the user interaction */
    override fun onCredentialsDialogFragmentInteraction(answer: Int) {

        when (answer) {

            Dialog.BUTTON_POSITIVE -> {
                toast(R.string.action_login)
                isLogIn = true
                invalidateOptionsMenu()
                if (!autoMode) {
                    setChooseTalkFragment("$CHOOSE_TALK_FRAGMENT$answer")
                }
            }

            Dialog.BUTTON_NEGATIVE -> {
                closeLateralMenu()
            }

            Dialog.BUTTON_NEUTRAL -> {
            }

        }
    }

    /* This method is called from AboutUsDialogFragment */
    override fun onAboutUsDialogFragmentInteraction(msg: String) {
        closeLateralMenu()
    }

    /* This method is called from LicenseDialogFragment */
    override fun onLicenseDialogFragmentInteraction(msg: String) {
        closeLateralMenu()
    }

    /* This method is called from DatePickerDialogFragment */
    override fun onDatePikerDialogFragmentInteraction(newDate: String) {

        selectedDate = simpleDateFormat.parse(newDate)
        val hour = GregorianCalendar().get(Calendar.HOUR_OF_DAY)
        val minutes = GregorianCalendar().get(Calendar.MINUTE)
        // A la fecha le añado lo milisegundos de la hora
        selectedDate.time = selectedDate.time + ((hour * 60 + minutes) * 60 * 1_000)

        setChooseTalkFragment("$CHOOSE_TALK_FRAGMENT$newDate")

    }

    /* This method is called from AreYouSureDialogFragment */
    override fun onAreYouSureDialogFragmentInteraction(resp: Int) {

        when (resp) {

            ARE_YOU_SURE_DIALOG_EXIT_APPLICATION_RESPONSE -> {
                finishAndRemoveTask()
            }

            ARE_YOU_SURE_DIALOG_FRESH_START_RESPONSE -> {
                freshStart()
                toast(R.string.data_refreshed)
            }

            ARE_YOU_SURE_DIALOG_CANCEL_RESPONSE -> {
            }

        }
    }

    /* This method might get called from PersonalStuffDialogFragment eventually */
    override fun onPersonalStuffDialogFragmentInteraction(msg: String) {

    }

    /* This method might get called from PersonalStuffDialogFragment eventually */
    override fun onAssetsManagerFragmentInteraction(msg: String) {
    }

    /* This method might get called from PieChartDialogFragmentInteraction eventually */
    override fun onPieChartDialogFragmentInteraction(msg: String) {
    }

    /* Static-like Kotlin style declarations */
    companion object {

        const val URL_SPEAKERS_IMAGES = "http://www.jbcnconf.com/2018/"
        const val DEFAULT_STATISTICS_FILE_NAME = "scoring.csv"

        const val MAIN_ACTIVITY = "MainActivity"
        const val CHOOSE_TALK_FRAGMENT = "ChooseTalkFragment"
        const val STATISTICS_FRAGMENT = "StatisticsFragment"
        const val CREDENTIALS_DIALOG_FRAGMENT = "CredentialsDialogFragment"
        const val VOTE_FRAGMENT = "VoteFragment"
        const val WELCOME_FRAGMENT = "WelcomeFragment"
        const val SETTINGS_FRAGMENT = "SettingsFragment"
        const val ASSETS_MANAGER_FRAGMENT = "AssetsManagerFragment"
        const val ABOUT_US_FRAGMENT = "AboutUsFragment"
        const val LICENSE_DIALOG_FRAGMENT = "LicenseDialogFragment"
        const val DATE_PICKER_FRAGMENT = "DatePickerFragment"
        const val ARE_YOU_SURE_DIALOG_FRAGMENT = "AreYouSureDialogFragment"
        const val PERSONAL_STUFF_DIALOG_FRAGMENT = "PersonalStuffDialogFragment"
        const val PIE_CHART_DIALOG_FRAGMENT = "PieChartDialogFragment"


        const val BUNDLE_LOGGED_KEY = "logged_key"
        const val BUNDLE_FILTERED_KEY = "filtered_key"
        const val BUNDLE_DATE_KEY = "date_key"

        // const val JBCNCONF_JSON_SCHEDULES_FILE_NAME = "schedules.json"
        const val JBCNCONF_JSON_SCHEDULES_FILE_NAME = "schedules_fake.json"
        const val JBCNCONF_JSON_VENUES_FILE_NAME = "venues.json"
        const val JBCNCONF_JSON_COLLECTION_NAME = "items"

        const val FIREBASE_COLLECTION = "Scoring"
        const val FIREBASE_COLLECTION_FIELD_TALK_ID = "talk_id"
        const val FIREBASE_COLLECTION_FIELD_SCHEDULE_ID = "schedule_id"
        const val FIREBASE_COLLECTION_FIELD_SCORE = "score"
        const val FIREBASE_COLLECTION_FIELD_DATE = "date"


        const val ARE_YOU_SURE_DIALOG_EXIT_APPLICATION_ACTION = 0X1
        const val ARE_YOU_SURE_DIALOG_FRESH_START_ACTION = 0X2

        const val ARE_YOU_SURE_DIALOG_CANCEL_RESPONSE = 0X0
        const val ARE_YOU_SURE_DIALOG_EXIT_APPLICATION_RESPONSE = 0X1
        const val ARE_YOU_SURE_DIALOG_FRESH_START_RESPONSE = 0X2

        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val simpleDateFormatCSV = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    }

}
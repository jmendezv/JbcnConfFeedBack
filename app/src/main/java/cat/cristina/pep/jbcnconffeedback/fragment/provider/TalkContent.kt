package cat.cristina.pep.jbcnconffeedback.fragment.provider

import android.content.Context
import android.support.v7.preference.PreferenceManager
import cat.cristina.pep.jbcnconffeedback.R
import cat.cristina.pep.jbcnconffeedback.activity.MainActivity
import cat.cristina.pep.jbcnconffeedback.fragment.nonguifragment.AssetsManagerFragment
import cat.cristina.pep.jbcnconffeedback.model.DatabaseHelper
import cat.cristina.pep.jbcnconffeedback.model.Speaker
import cat.cristina.pep.jbcnconffeedback.model.Talk
import cat.cristina.pep.jbcnconffeedback.model.UtilDAOImpl
import cat.cristina.pep.jbcnconffeedback.utils.PreferenceKeys
import cat.cristina.pep.jbcnconffeedback.utils.ScheduleContentProvider
import cat.cristina.pep.jbcnconffeedback.utils.VenueContentProvider
import com.j256.ormlite.android.apptools.OpenHelperManager
import com.j256.ormlite.dao.Dao
import java.util.*

private val TAG = TalkContent::class.java.name

class TalkContent(val context: Context, val date: Date) {

    private var databaseHelper: DatabaseHelper = OpenHelperManager.getHelper(context, DatabaseHelper::class.java)
    private var utilDAOImpl: UtilDAOImpl
    private var speakerDao: Dao<Speaker, Int>
    private var talkDao: Dao<Talk, Int>
    //private var speakerTalkDao: Dao<SpeakerTalk, Int>

    private var scheduleContentProvider: ScheduleContentProvider
    private var venueContentProvider: VenueContentProvider

    /**
     * An array of talk items.
     */
    val ITEMS: MutableList<TalkItem> = mutableListOf()
    val ITEMS_FILTERED_BY_DATE_AND_ROOM_NAME: MutableList<TalkItem> = mutableListOf()

    /**
     * A map of pairs <scheduleId, TalkItem
     */
    val ITEM_MAP: MutableMap<String, TalkItem> = HashMap()

    init {

        val assetsManagerFragment = (context as MainActivity)
                .supportFragmentManager
                .findFragmentByTag(MainActivity.ASSETS_MANAGER_FRAGMENT) as AssetsManagerFragment

        scheduleContentProvider = assetsManagerFragment.scheduleContentProvider
        venueContentProvider = assetsManagerFragment.venueContentProvider

        utilDAOImpl = UtilDAOImpl(context, databaseHelper)
        talkDao = databaseHelper.getTalkDao()
        speakerDao = databaseHelper.getSpeakerDao()
        //speakerTalkDao = databaseHelper.getSpeakerTalkDao()

        var i = 1
        talkDao
                .queryForAll()
                .sorted()
//                .stream() // API Level 24
//                // by scheduleId
//                .sorted()
                .forEach {
                    addItem(createTalkItem(i++, it))
                }


    }

    private fun addItem(item: TalkItem) {

        /* ITEMS contiene todos  */
        ITEMS.add(item)

        /* Cada entrada de ITEM_MAP contiene un scheduleId y un talkitem   */
        ITEM_MAP.put(item.talk.scheduleId, item)

        val today = GregorianCalendar()
        today.time = date

        val scheduleId = item.talk.scheduleId
        val sessionId = "${scheduleId.substring(1, 4)}-${scheduleId.substring(9, 12)}"
        val venueId = "${scheduleId.substring(1, 4)}-${scheduleId.substring(5, 8)}"

        //val session = SessionsTimes.valueOf("${scheduleId.substring(1, 4)}_${scheduleId.substring(9, 12)}")
        val session = scheduleContentProvider.getSessionTimes(sessionId)

        //val location = TalksLocations.valueOf("${scheduleId.substring(1, 4)}_${scheduleId.substring(5, 8)}")
        val location = venueContentProvider.getRoom(venueId)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val roomName =
                sharedPreferences?.getString(PreferenceKeys.ROOM_KEY, context.resources.getString(R.string.pref_default_room_name))
        if (today.get(Calendar.DATE) == session.startTalkDateTime.get(Calendar.DATE)
                && today.get(Calendar.MONTH) == session.startTalkDateTime.get(Calendar.MONTH)
                && today.get(Calendar.YEAR) == session.startTalkDateTime.get(Calendar.YEAR)
                && roomName == location) {
            ITEMS_FILTERED_BY_DATE_AND_ROOM_NAME.add(item)
        }
    }

    private fun createTalkItem(position: Int, talk: Talk): TalkItem {
        val speakerRef: String = talk.speakers?.get(0) ?: "null"
        return TalkItem(position.toString(), talk, utilDAOImpl.lookupSpeakerByRef(speakerRef))
    }

    /* A talk item */
    data class TalkItem(val id: String, val talk: Talk, val speaker: Speaker)
}

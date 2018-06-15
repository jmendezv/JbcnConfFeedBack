package cat.cristina.pep.jbcnconffeedback

import android.os.Build.VERSION_CODES.LOLLIPOP
import cat.cristina.pep.jbcnconffeedback.model.*
import com.j256.ormlite.android.apptools.OpenHelperManager
import com.j256.ormlite.dao.Dao
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.junit.After


@RunWith(RobolectricGradleTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(LOLLIPOP), packageName = "cat.cristina.pep.jbcnconffeedback")
class UtilDAOImplTest {

    private lateinit var dao: UtilDAOImpl
    private lateinit var speakers: List<Speaker>
    private lateinit var talks: List<Talk>
    private var databaseHelper: DatabaseHelper = getDatabaseHelper()
    private lateinit var speakerDao: Dao<Speaker, Int>
    private lateinit var talkDao: Dao<Talk, Int>
    private lateinit var speakerTalkDao: Dao<SpeakerTalk, Int>

    @Before
    fun beforeEachTest(): Unit {
        dao = UtilDAOImpl(RuntimeEnvironment.application, databaseHelper)
        //populate()
    }

    @After
    fun afterEachTest() {
        dao.onDestroy()
    }

    @Test
    fun checkThatTereAreTalks() {
        talks = dao.lookupTalks()
        Assert.assertTrue(talks.size > 0)
    }

    @Test
    fun checkThatTereAreSpeakers() {
        speakers = dao.lookupSpeakers()
        Assert.assertTrue(speakers.size > 0)
    }

    @Test
    fun checkLookupSpeakersByRef() {
        val michelSchudel = dao.lookupSpeakerByRef("TWljaGVsU2NodWRlbG1pY2hlbC5zY2h1ZGVsQGdtYWlsLmNvbQ==")
        Assert.assertEquals("Michel Schudel", michelSchudel.name)
    }

    @Test
    fun checkLookupSpeakersListForTalkIsNotEmpty() {
        talks = dao.lookupTalks()
        val speakers: List<Speaker> = dao.lookupSpeakersForTalk(talks.get(0))
        Assert.assertTrue(speakers.isNotEmpty())
    }

    @Test
    fun checkLookupSpeakersListForTalkHasCorrectSpeaker() {
        talks = dao.lookupTalks()
        val speakers: List<Speaker> = dao.lookupSpeakersForTalk(talks.get(1))
        Assert.assertEquals("Mercedes Wyss", speakers.get(0).name)
    }

    private fun getDatabaseHelper(): DatabaseHelper {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(RuntimeEnvironment.application, DatabaseHelper::class.java)
        }
        return databaseHelper
    }



}
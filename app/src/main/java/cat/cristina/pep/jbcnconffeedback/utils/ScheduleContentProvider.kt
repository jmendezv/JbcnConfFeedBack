package cat.cristina.pep.jbcnconffeedback.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.json.JSONObject
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

private const val OFFSET = 15

private val TAG = ScheduleContentProvider::class.java.name

class ScheduleContentProvider(val context: Context, private val fileName: String) {

    private var scheduleMap: MutableMap<String, Pair<Calendar, Calendar>> = mutableMapOf()

    private data class Schedule(val id: String, val start: Date, val end: Date)

    init {
        processData(readData(fileName))
    }

    public fun getSessionTimes(id: String): SessionTimes =
            SessionTimes(getStartTalkDateTime(id), getEndTalkDateTime(id), OFFSET)

    /*
    * id format is 'MON-SE1'
    * */
    private fun getStartTalkDateTime(id: String): Calendar {
        //Log.d(TAG, "ERROR $id")
        return scheduleMap[id]!!.first
    }

    private fun getEndTalkDateTime(id: String): Calendar {
        return scheduleMap[id]!!.second
    }

    private fun getStartScheduleDateTime(id: String): Calendar {
        val calendar = getEndTalkDateTime(id)
        calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) - OFFSET)
        return calendar
    }

    private fun getEndScheduleDateTime(id: String): Calendar {
        val calendar = getEndTalkDateTime(id)
        calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + OFFSET)
        return calendar
    }


    private fun readData(fileName: String) =
            context.assets.open(fileName).bufferedReader().readText()


    private fun processData(jsonOfSchedules: String): Unit {

        val jsonObject = JSONObject(jsonOfSchedules)
        val schedules = jsonObject.getJSONArray(SCHEDULE_PROVIDER_JSON_COLLECTION_NAME)
        val gson = GsonBuilder()
                .setDateFormat("dd/MM/yyyy HH:mm")
                //.setPrettyPrinting()
                .create()

        for (index in 0 until (schedules.length())) {
            val scheduleObject = schedules.getJSONObject(index)
            val schedule: Schedule = gson.fromJson(scheduleObject.toString(), Schedule::class.java)
            val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val start = GregorianCalendar()
            start.time = schedule.start
            val end = GregorianCalendar()
            end.time = schedule.end
            scheduleMap[schedule.id] = start to end
            //Log.d(TAG, "${schedule.id} ${simpleDateFormat.format(scheduleMap[schedule.id]?.first?.time)}  ${simpleDateFormat.format(scheduleMap[schedule.id]?.second?.time)}")
        }


    }

    companion object {
        const val SCHEDULE_PROVIDER_JSON_COLLECTION_NAME = "schedules"
    }

}
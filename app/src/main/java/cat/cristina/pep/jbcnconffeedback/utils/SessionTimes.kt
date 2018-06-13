package cat.cristina.pep.jbcnconffeedback.utils

import java.util.*

data class SessionTimes(
        val startTalkDateTime: Calendar,
        val endTalkDateTime: Calendar,
        val offsetInMinutes: Int = 15) {

    var startScheduleDateTime: Calendar
    var endScheduleDateTime: Calendar

    init {
        startScheduleDateTime = endTalkDateTime
        startScheduleDateTime
                .set(Calendar.MINUTE, startScheduleDateTime.get(Calendar.MINUTE) - offsetInMinutes)
        endScheduleDateTime = endTalkDateTime
        endScheduleDateTime
                .set(Calendar.MINUTE, endScheduleDateTime.get(Calendar.MINUTE) + offsetInMinutes)
    }
}

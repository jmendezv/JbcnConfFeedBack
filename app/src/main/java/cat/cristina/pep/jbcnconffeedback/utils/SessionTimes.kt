package cat.cristina.pep.jbcnconffeedback.utils

import java.util.*

data class SessionTimes(
        val startTalkDateTime: Calendar,
        val endTalkDateTime: Calendar,
        private val offsetInMinutes: Int = 15) {

    var startVotingDateTime: Calendar = GregorianCalendar()
    var endVotingDateTime: Calendar = GregorianCalendar()

    init {
        startVotingDateTime.time = endTalkDateTime.time
        startVotingDateTime
                .add(Calendar.MINUTE, -offsetInMinutes)

        endVotingDateTime.time = endTalkDateTime.time
        endVotingDateTime
                .add(Calendar.MINUTE, offsetInMinutes)
    }
}

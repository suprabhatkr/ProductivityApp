package com.example.productivityapp.service

import java.time.LocalTime
import kotlin.random.Random

internal data class ReminderCopy(
    val title: String,
    val message: String,
)

internal enum class ReminderEvent {
    WATER_FIRST_DRINK,
    WATER_INTERVAL,
    RUN_HALF,
    RUN_NINETY,
    STEP_HALF,
    STEP_NINETY,
    STEP_EVENING,
    SLEEP_BEDTIME,
    SLEEP_NAP_STARTED,
    SLEEP_WAKE_FOLLOW_UP,
}

internal object HealthReminderContent {
    fun pick(
        event: ReminderEvent,
        now: LocalTime = LocalTime.now(),
        bedtimeLabel: String = "",
        durationLabel: String = "",
        remainingSteps: Int = 0,
    ): ReminderCopy {
        val variants = when (event) {
            ReminderEvent.WATER_FIRST_DRINK -> listOf(
                ReminderCopy("Start your hydration early", "You have not logged your first drink yet. A glass of water now makes the rest of the day feel easier."),
                ReminderCopy("Your water ring is waiting", "No drink has been recorded yet today. Start with a quick sip and build momentum before the afternoon."),
                ReminderCopy("Small sip, strong start", "A first glass now is the easiest way to stay steady through the day. Log your water when you are ready."),
                ReminderCopy("Hydration check-in", "You have not started your water goal yet. Begin with one simple drink and let the ring move with you."),
                ReminderCopy("Good morning, drink first", "The first water entry is still missing. A quick drink now helps you avoid catching up later."),
            )

            ReminderEvent.WATER_INTERVAL -> listOf(
                ReminderCopy("Time for another drink", "It has been about 90 minutes since your last water entry. A short refill keeps your pace comfortable."),
                ReminderCopy("Keep the hydration rhythm going", "Your last drink was a while ago. Another glass now will help you stay on track without rushing later."),
                ReminderCopy("A gentle water nudge", "You have been away from water for about an hour and a half. Take a moment for a few refreshing sips."),
                ReminderCopy("Hydration gap spotted", "Your ring has been quiet for a while. A small drink now keeps the day feeling lighter and steadier."),
                ReminderCopy("Refill break", "The last drink was logged a little while ago. This is a good moment to top up before the next stretch of the day."),
            )

            ReminderEvent.RUN_HALF -> listOf(
                ReminderCopy("Halfway there on your run", "You have crossed the halfway mark. Keep your rhythm steady and let this second half build on the first."),
                ReminderCopy("Strong pace so far", "Half of your run goal is done. Stay smooth, stay relaxed, and keep carrying this momentum forward."),
                ReminderCopy("Run checkpoint reached", "You are past the halfway point. Nice work. A calm finish will make this run feel even better."),
                ReminderCopy("Momentum is on your side", "Half the distance is already behind you. Keep going and turn this into a strong complete session."),
                ReminderCopy("Keep that stride alive", "You have hit 50 percent of your run goal. Settle in and keep pressing through the next stretch."),
            )

            ReminderEvent.RUN_NINETY -> listOf(
                ReminderCopy("Almost at your run goal", "You are around 90 percent done. Just a little more and this run is yours."),
                ReminderCopy("Final stretch", "You are so close to finishing the run goal. One more steady push will close it out."),
                ReminderCopy("Run goal in sight", "You have nearly finished the ring. Keep moving for a little longer and enjoy the finish."),
                ReminderCopy("You are right there", "Only a small stretch remains on this run. Hold your pace and bring it home."),
                ReminderCopy("Just a little bit more", "You have already done the hard part. A short final effort will complete the run goal."),
            )

            ReminderEvent.STEP_HALF -> listOf(
                ReminderCopy("Halfway on your step goal", "You have reached 50 percent of today's steps. Keep stacking small walks and the ring will keep moving."),
                ReminderCopy("Good step pace so far", "Half the step target is done. A couple more short walks can take you a long way from here."),
                ReminderCopy("Step checkpoint reached", "You are at the halfway mark. Nice progress. Stay active and keep the ring growing."),
                ReminderCopy("Steady movement is working", "You have crossed half of today's steps. Keep going. The second half can come quicker than you think."),
                ReminderCopy("Momentum for the day", "Your step ring is halfway full. Keep that movement alive and carry it into the rest of the day."),
            )

            ReminderEvent.STEP_NINETY -> listOf(
                ReminderCopy("Step goal almost done", "You are about 90 percent of the way there. A short walk can finish the ring."),
                ReminderCopy("You are very close", "Only a little more movement is left for today's steps. This is a great time for a final push."),
                ReminderCopy("Final steps ahead", "The ring is nearly complete. One more walk or a few minutes on your feet can close it out."),
                ReminderCopy("Just a little bit more", "You are right near the step goal now. Keep moving and enjoy the finish."),
                ReminderCopy("Step ring in sight", "You have almost completed today's steps. A small final effort will do the job."),
            )

            ReminderEvent.STEP_EVENING -> listOf(
                ReminderCopy("Evening step reminder", "It is 8 PM and your step ring still needs a little help. A short walk now can finish the day strong."),
                ReminderCopy("Close the step ring tonight", "There are still $remainingSteps steps left for today. A relaxed evening walk can wrap it up."),
                ReminderCopy("Your steps are calling", "Today's step goal is still open. If you have a little energy left, now is a great time to finish it."),
                ReminderCopy("One last walk could do it", "You still have $remainingSteps steps to go. A quick evening loop can help you close the ring."),
                ReminderCopy("Finish today's movement goal", "The step ring is not complete yet. A few more steps tonight will make the day feel complete."),
            )

            ReminderEvent.SLEEP_BEDTIME -> listOf(
                ReminderCopy("Sleep reminder for tonight", "Your usual bedtime is around $bedtimeLabel. Start winding down now so sleep feels easier when it is time."),
                ReminderCopy("Half an hour until bedtime", "Your target sleep time is close. Dim the pace of the evening and give yourself a softer landing into rest."),
                ReminderCopy("Start your sleep routine", "Bedtime is about 30 minutes away at $bedtimeLabel. A calm transition now can help tonight feel smoother."),
                ReminderCopy("Prepare for a good night", "You are getting close to your planned sleep time. Slow things down now and let rest become the next step."),
                ReminderCopy("Evening wind-down reminder", "Bedtime is approaching. A quiet half hour now can make it easier to hit your sleep goal tonight."),
            )

            ReminderEvent.SLEEP_NAP_STARTED -> listOf(
                ReminderCopy("Nap started", "Your nap timer has started. Rest well and check back in when you are up."),
                ReminderCopy("Nap mode is on", "You have started a nap. Enjoy the break and let the app help you track it."),
                ReminderCopy("Taking a recharge break", "Your nap is now running. Rest for a bit and return when you feel refreshed."),
                ReminderCopy("Nap tracked successfully", "The nap timer is active now. Settle in and let yourself recover."),
                ReminderCopy("Quick rest in progress", "Your nap has started. Enjoy the pause and check the app when you wake up."),
            )

            ReminderEvent.SLEEP_WAKE_FOLLOW_UP -> listOf(
                ReminderCopy("How did your sleep feel?", "Your sleep session ended at ${durationLabel.ifBlank { "the latest checkpoint" }}. Review the quality when you are ready and keep your sleep history useful."),
                ReminderCopy("Morning sleep check-in", "You are up now. Take a moment to review your sleep quality and see how it matched your goal."),
                ReminderCopy("Sleep session complete", "Your latest sleep window has been recorded. A quick review now will make tonight's pattern easier to understand."),
                ReminderCopy("Nice work getting rest", "Your sleep session is saved. Check the quality or see whether you reached your goal before the day gets busy."),
                ReminderCopy("Wake-up follow-up", "Now that you are awake, log how your sleep felt and see how close you were to the target."),
            )
        }
        val seed = now.toSecondOfDay() + event.ordinal * 31
        return variants[Random(seed).nextInt(variants.size)]
    }
}

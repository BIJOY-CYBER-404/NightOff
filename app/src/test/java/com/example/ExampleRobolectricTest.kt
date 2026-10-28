package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.NightSchedule
import com.example.model.SleepAction
import com.example.scheduler.AlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NightOff", appName)
    }

    @Test
    fun `verify default sleep actions`() {
        val actions = SleepAction.defaultActions()
        assertTrue(actions.contains(SleepAction.STOP_MEDIA))
        assertTrue(actions.contains(SleepAction.LOCK_SCREEN))
        assertTrue(actions.contains(SleepAction.MUTE_SOUND))
    }

    @Test
    fun `verify quick timer computation`() {
        val target = System.currentTimeMillis() + 30 * 60 * 1000L
        val schedule = NightSchedule(
            title = "Quick 30",
            hour = 0,
            minute = 0,
            isQuickTimer = true,
            targetTimeMillis = target
        )
        val computed = AlarmScheduler.computeNextTriggerMillis(schedule)
        assertEquals(target, computed)
    }
}

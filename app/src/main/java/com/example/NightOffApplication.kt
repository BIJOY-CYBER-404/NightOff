package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.ScheduleRepository
import com.example.util.NotificationHelper

class NightOffApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        ScheduleRepository(
            context = this,
            scheduleDao = database.scheduleDao(),
            executionLogDao = database.executionLogDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}

package com.studyplanner.app

import android.app.Application
import com.studyplanner.app.data.FocusSessionManager
import com.studyplanner.app.notify.FocusNotification
import com.studyplanner.app.notify.NotificationScheduler

class StudyPlannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationScheduler.createChannels(this)
        FocusNotification.createChannel(this)
        FocusSessionManager.initialize(this)
    }
}

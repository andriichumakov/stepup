package com.example.stepupapp.managers

class AppConfigPreferencesManager(
    storage: PreferencesStorage,
    userIdProvider: UserIdProvider = AuthUserIdProvider()
) : BasePreferencesManager(storage, userIdProvider) {
    
    companion object {
        private const val KEY_SETUP_COMPLETED = "setup_completed"
        private const val KEY_SHOW_STEP_COUNTER_NOTIFICATION = "show_step_counter_notification"
        private const val TAG = "AppConfigPreferencesManager"
    }
    
    fun isSetupCompleted(): Boolean {
        return storage.getBoolean(KEY_SETUP_COMPLETED, false)
    }
    
    fun setSetupCompleted(completed: Boolean = true) {
        storage.putBoolean(KEY_SETUP_COMPLETED, completed)
        logDebug(TAG, "Setup completed set to: $completed")
    }
    
    fun shouldShowStepCounterNotification(): Boolean {
        return storage.getBoolean(KEY_SHOW_STEP_COUNTER_NOTIFICATION, true)
    }
    
    fun setStepCounterNotificationVisibility(show: Boolean) {
        storage.putBoolean(KEY_SHOW_STEP_COUNTER_NOTIFICATION, show)
        logDebug(TAG, "Step counter notification visibility set to: $show")
    }
} 
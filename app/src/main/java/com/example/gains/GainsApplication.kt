package com.example.gains

import android.app.Application
import com.example.gains.data.DefaultDataRepository
import com.example.gains.data.GainsDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class GainsApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { GainsDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { DefaultDataRepository(database.gainsDao()) }
    val settingsManager by lazy { com.example.gains.data.SettingsManager(this) }
}

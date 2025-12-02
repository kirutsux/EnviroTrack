package com.ecocp.capstoneenvirotrack

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

class MyApplication : Application() {
    val smrDataStore: DataStore<Preferences> by preferencesDataStore(name = "emb_smr_ai_cache")
}

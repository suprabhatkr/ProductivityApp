package com.example.productivityapp.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Top-level extension property that uses the official preferencesDataStore delegate.
 * Declaring the delegate at the top-level ensures a singleton DataStore per process
 * for the given name ("user_prefs"). Use `context.userPreferencesDataStore` to access it.
 */
val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")


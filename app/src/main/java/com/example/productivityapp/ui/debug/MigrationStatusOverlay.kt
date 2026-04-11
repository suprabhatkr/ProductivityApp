package com.example.productivityapp.ui.debug

import android.content.Context
// don't rely on generated BuildConfig here; detect debuggable at runtime
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.productivityapp.datastore.profile.EncryptedProtoUserProfileStore
import com.example.productivityapp.datastore.profile.SharedPreferencesLegacyProfileReader
import com.example.productivityapp.datastore.profile.UserProfileMigrationCoordinator
import com.example.productivityapp.datastore.profile.MigrationDiagnostics

/**
 * Small debug-only migration status surface.
 * Visible only when BuildConfig.DEBUG is true. It reads the secure profile file and
 * the legacy shared-preferences snapshot and displays a brief status to aid manual QA.
 */
@Composable
fun MigrationStatusOverlay(appContext: Context) {
    // guard early if not a debug build (use runtime debuggable flag for robustness)
    val isDebuggable = (appContext.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    if (!isDebuggable) return

    val status = remember { mutableStateOf("Checking migration status...") }
    val scope = rememberCoroutineScope()

    // Build diagnostics helpers once from context
    val secureFile = appContext.filesDir.resolve("secure_user_profile.pb")
    val secureStore = EncryptedProtoUserProfileStore.create(secureFile)
    val legacyReader = SharedPreferencesLegacyProfileReader.fromContext(appContext)
    val coordinator = UserProfileMigrationCoordinator(secureStore, legacyReader, migrationEnabled = true)
    val diagnostics = MigrationDiagnostics(secureStore, legacyReader, coordinator)

    LaunchedEffect(appContext.filesDir) {
        try {
            val secure = try {
                secureStore.read()
            } catch (t: Throwable) {
                null
            }

            val legacy = try {
                legacyReader.readLegacyProfile()
            } catch (t: Throwable) {
                null
            }

            val secureState = secure?.migrationState?.name ?: "(no-secure-file)"
            val migratedAt = secure?.migratedAtEpochMs ?: 0L
            val legacyPresent = if (legacy != null) "present" else "none"

            status.value = "Secure: $secureState (migratedAt=$migratedAt)\nLegacy: $legacyPresent"
        } catch (e: Throwable) {
            status.value = "migration-check-error: ${e.localizedMessage}"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
            Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(8.dp),
            color = Color(0x88000000)
        ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = status.value,
                        color = Color.White,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        Button(onClick = {
                            scope.launch {
                                val result = try {
                                    diagnostics.triggerMigration()
                                } catch (t: Throwable) {
                                    null
                                }
                                status.value = "trigger-migration: ${result?.javaClass?.simpleName ?: "error"}"
                            }
                        }) {
                            Text(text = "Run Migration")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(onClick = {
                            scope.launch {
                                try {
                                    diagnostics.clearSecureStore()
                                    status.value = "secure-store-cleared"
                                } catch (t: Throwable) {
                                    status.value = "clear-secure-error: ${t.localizedMessage}"
                                }
                            }
                        }) {
                            Text(text = "Delete Secure File")
                        }
                    }
                }
        }
    }
}






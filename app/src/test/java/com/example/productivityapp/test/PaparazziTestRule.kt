package com.example.productivityapp.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.example.productivityapp.ui.theme.ProductivityAppTheme
import org.junit.Rule
import org.junit.ClassRule
import org.junit.rules.TestRule
import org.robolectric.util.ReflectionHelpers

private fun ensureRobolectricFingerprint() {
    try {
        if (android.os.Build.FINGERPRINT == null) {
            ReflectionHelpers.setStaticField(android.os.Build::class.java, "FINGERPRINT", "robolectric")
        }
    } catch (_: Throwable) {
    }
}

@Suppress("unused")
private val forceRobolectricFingerprint = run {
    ensureRobolectricFingerprint()
    Unit
}

/**
 * Simple Compose test helper that wraps setContent with the app theme and exposes
 * a Compose test rule for JVM unit tests (Robolectric-based).
 */
open class ComposeTestRuleHolder {
    // Underlying compose rule instance that we will delegate to. Keep this
    // concrete reference so we can call setContent(...) from helpers.
    private val delegateComposeRule: ComposeContentTestRule by lazy {
        ensureRobolectricFingerprint()
        createComposeRule()
    }

    // A small rule that ensures Build.FINGERPRINT is non-null before other
    // rules (notably the Compose rule) are applied. JUnit applies rules in
    // field-declaration order, so declare this first.
    @get:Rule
    val fingerprintRule: TestRule = TestRule { base, _ ->
        ensureRobolectricFingerprint()
        base
    }

    @get:Rule
    val composeTestRule: ComposeContentTestRule
        get() {
            ensureRobolectricFingerprint()
            return delegateComposeRule
        }

    companion object {
        // A class-level rule runs earlier than instance rules and will be
        // applied before the Compose test rule. This helps ensure
        // android.os.Build.FINGERPRINT is initialized before Compose testing
        // internals read it.
        @JvmField
        @ClassRule
        val fingerprintClassRule: TestRule = TestRule { base, _ ->
            try {
                ensureRobolectricFingerprint()
            } catch (_: Throwable) {
            }
            base
        }
    }

    protected fun setThemedContent(content: @Composable () -> Unit) {
        // Some Compose UI test internals check android.os.Build.FINGERPRINT and
        // call toLowerCase on it. In some Robolectric/JVM test environments this
        // value can be null which leads to an NPE. Ensure it is non-null to
        // keep Compose testing idling strategies happy.
        ensureRobolectricFingerprint()

        delegateComposeRule.setContent {
            ProductivityAppTheme {
                content()
            }
        }
    }
}

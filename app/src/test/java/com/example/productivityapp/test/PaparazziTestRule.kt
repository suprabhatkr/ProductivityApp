package com.example.productivityapp.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.productivityapp.ui.theme.ProductivityAppTheme
import org.junit.Rule
import org.junit.ClassRule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Simple Compose test helper that wraps setContent with the app theme and exposes
 * a Compose test rule for JVM unit tests (Robolectric-based).
 */
open class ComposeTestRuleHolder {
    // Create a TestRule wrapper that ensures android.os.Build.FINGERPRINT is
    // non-null before the actual Compose test rule initializes. Some Compose
    // testing internals inspect Build.FINGERPRINT during rule application and
    // can NPE in certain Robolectric/JVM environments.
    // Underlying compose rule instance that we will delegate to. Keep this
    // concrete reference so we can call setContent(...) from helpers.
    private val delegateComposeRule = createComposeRule()

    // A small rule that ensures Build.FINGERPRINT is non-null before other
    // rules (notably the Compose rule) are applied. JUnit applies rules in
    // field-declaration order, so declare this first.
    @get:Rule
    val fingerprintRule: TestRule = TestRule { base, _ ->
        try {
            val buildClass = android.os.Build::class.java
            val field = buildClass.getDeclaredField("FINGERPRINT")
            field.isAccessible = true
            val current = field.get(null) as? String
            if (current == null) {
                field.set(null, "robolectric")
            }
        } catch (_: Throwable) {
        }
        base
    }

    @get:Rule
    val composeTestRule = delegateComposeRule

    companion object {
        // A class-level rule runs earlier than instance rules and will be
        // applied before the Compose test rule. This helps ensure
        // android.os.Build.FINGERPRINT is initialized before Compose testing
        // internals read it.
        @JvmField
        @ClassRule
        val fingerprintClassRule: TestRule = TestRule { base, _ ->
            try {
                val buildClass = android.os.Build::class.java
                val field = buildClass.getDeclaredField("FINGERPRINT")
                field.isAccessible = true
                val current = field.get(null) as? String
                if (current == null) {
                    field.set(null, "robolectric")
                }
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
        try {
            val buildClass = android.os.Build::class.java
            val field = buildClass.getDeclaredField("FINGERPRINT")
            field.isAccessible = true
            val current = field.get(null) as? String
            if (current == null) {
                field.set(null, "robolectric")
            }
        } catch (_: Throwable) {
            // best-effort; if this fails, tests may still fail and the original
            // exception will surface. Swallow exceptions here to avoid masking
            // other setup failures.
        }

        delegateComposeRule.setContent {
            ProductivityAppTheme {
                content()
            }
        }
    }
}



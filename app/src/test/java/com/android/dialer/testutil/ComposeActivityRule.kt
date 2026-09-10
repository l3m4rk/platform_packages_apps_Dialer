package com.android.dialer.testutil

import android.content.ComponentName
import androidx.activity.ComponentActivity
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Registers [ComponentActivity] with the shadow package manager.
 *
 * Compose's test rules launch a bare [ComponentActivity], but a Robolectric test running with
 * `@Config(manifest = Config.NONE)` — which every test here must, because Robolectric rejects the
 * packaged manifest's `minSdkVersion 37` — has no manifest declaring it. Apply this with
 * `order = 0` so it runs before the compose rule.
 */
fun composeActivityRule(): TestRule = TestRule { base, _ ->
    object : Statement() {
        override fun evaluate() {
            val application = RuntimeEnvironment.getApplication()
            shadowOf(application.packageManager).addActivityIfNotPresent(
                ComponentName(application, ComponentActivity::class.java),
            )
            base.evaluate()
        }
    }
}

package de.egril.defender.iam

import androidx.activity.ComponentActivity
import org.publicvalue.multiplatform.oidc.appsupport.AndroidCodeAuthFlowFactory

/**
 * Application-scoped singleton that holds the [AndroidCodeAuthFlowFactory] instance.
 *
 * The factory must live as long as the application because the login flow can survive
 * Activity recreation (the factory attaches to the Activity's lifecycle via
 * [registerActivity], which must be called from [ComponentActivity.onCreate] on every
 * creation).
 */
object AndroidIamFlowProvider {

    /**
     * Single factory instance for the lifetime of the process.
     * `useWebView = false` → Chrome Custom Tabs (recommended for security).
     */
    val factory: AndroidCodeAuthFlowFactory = AndroidCodeAuthFlowFactory(useWebView = false)

    /**
     * Must be called from [ComponentActivity.onCreate] so the factory can attach to the
     * current activity's lifecycle and handle the OAuth2 redirect intent.
     */
    fun registerActivity(activity: ComponentActivity) {
        factory.registerActivity(activity)
    }
}

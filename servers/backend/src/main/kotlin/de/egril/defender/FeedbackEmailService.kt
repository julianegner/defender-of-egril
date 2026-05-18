package de.egril.defender

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import java.util.Properties

private val feedbackEmailLogger = LoggerFactory.getLogger("FeedbackEmail")

private data class FeedbackEmailConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val from: String,
    val to: String,
    val startTlsEnabled: Boolean
)

internal object FeedbackEmailService {

    fun sendFeedbackNotification(
        request: FeedbackSubmissionRequest,
        feedbackType: FeedbackType,
        userId: String?,
        userName: String?,
        hasScreenshot: Boolean
    ) {
        val config = loadConfig() ?: return

        val properties = Properties().apply {
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", config.startTlsEnabled.toString())
        }
        val session = Session.getInstance(
            properties,
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication =
                    PasswordAuthentication(config.username, config.password)
            }
        )

        runCatching {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(config.from))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.to))
                subject = "[Defender Feedback] ${feedbackType.name} (${request.feedbackId})"
                setText(
                    buildString {
                        appendLine("New player feedback received:")
                        appendLine()
                        appendLine("feedbackId: ${request.feedbackId}")
                        appendLine("feedbackType: ${feedbackType.name}")
                        appendLine("bugTypes: ${request.bugTypes.joinToString(",").ifBlank { "-" }}")
                        appendLine("sourceContext: ${request.sourceContext ?: "-"}")
                        appendLine("platform: ${request.platform}")
                        appendLine("platformLong: ${request.platformLong ?: "-"}")
                        appendLine("platformExtended: ${request.platformExtended ?: "-"}")
                        appendLine("osName: ${request.osName ?: "-"}")
                        appendLine("versionName: ${request.versionName ?: "-"}")
                        appendLine("commitHash: ${request.commitHash ?: "-"}")
                        appendLine("userId: ${userId ?: "-"}")
                        appendLine("userName: ${userName ?: "-"}")
                        appendLine("contactEmail: ${request.contactEmail ?: "-"}")
                        appendLine("gameLevelName: ${request.gameLevelName ?: "-"}")
                        appendLine("gameTurnNumber: ${request.gameTurnNumber?.toString() ?: "-"}")
                        appendLine("hasScreenshot: $hasScreenshot")
                        appendLine("hasGameLog: ${!request.gameLog.isNullOrBlank()}")
                        appendLine()
                        appendLine("Message:")
                        appendLine(request.message)
                    }
                )
            }
            Transport.send(message)
            feedbackEmailLogger.info("Feedback notification email sent for feedbackId=${request.feedbackId}")
        }.onFailure { ex ->
            feedbackEmailLogger.warn("Failed to send feedback notification email: ${ex.message}")
        }
    }

    private fun loadConfig(): FeedbackEmailConfig? {
        val host = System.getenv("FEEDBACK_SMTP_HOST").orEmpty().trim()
        val portStr = System.getenv("FEEDBACK_SMTP_PORT").orEmpty().trim()
        val username = System.getenv("FEEDBACK_SMTP_USERNAME").orEmpty().trim()
        val password = System.getenv("FEEDBACK_SMTP_PASSWORD").orEmpty().trim()
        val from = System.getenv("FEEDBACK_EMAIL_FROM").orEmpty().trim()
        val to = System.getenv("FEEDBACK_EMAIL_TO").orEmpty().trim()
        val startTlsEnabled = (System.getenv("FEEDBACK_SMTP_STARTTLS") ?: "true").equals("true", ignoreCase = true)

        if (host.isBlank() || portStr.isBlank() || username.isBlank() || password.isBlank() || from.isBlank() || to.isBlank()) {
            feedbackEmailLogger.info("Feedback email config incomplete; email notification is disabled")
            return null
        }
        val port = portStr.toIntOrNull() ?: run {
            feedbackEmailLogger.warn("Feedback email config invalid: FEEDBACK_SMTP_PORT is not a valid integer")
            return null
        }
        return FeedbackEmailConfig(
            host = host,
            port = port,
            username = username,
            password = password,
            from = from,
            to = to,
            startTlsEnabled = startTlsEnabled
        )
    }
}

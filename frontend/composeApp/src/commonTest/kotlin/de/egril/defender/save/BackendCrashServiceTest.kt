package de.egril.defender.save

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class BackendCrashServiceTest {

    @Test
    fun buildCrashReportUploadJson_includesRequiredFields() {
        val payload = buildCrashReportUploadJson(
            CrashReportSubmitRequest(
                crashId = "123e4567-e89b-12d3-a456-426614174000",
                errorType = "java.lang.IllegalStateException",
                errorMessage = "something broke",
                stackTrace = "at de.egril.defender.Foo.bar(Foo.kt:42)",
                gameLog = "turn=7",
                settingsJson = serializeSettingsJson(mapOf("darkMode" to "false"))
            )
        )

        assertContains(payload, "\"crashId\":\"123e4567-e89b-12d3-a456-426614174000\"")
        assertContains(payload, "\"errorType\":\"java.lang.IllegalStateException\"")
        assertContains(payload, "\"errorMessage\":\"something broke\"")
        assertContains(payload, "\"stackTrace\":")
        assertContains(payload, "\"gameLog\":\"turn=7\"")
        assertContains(payload, "\"settingsJson\":")
        // Standard client info contributed by appendClientInfo
        assertContains(payload, "\"platform\":")
        assertContains(payload, "\"platformLong\":")
        val osName = de.egril.defender.utils.getPlatform().osName
        if (osName != null) {
            assertContains(payload, "\"osName\":\"${escapeJsonString(osName)}\"")
        } else {
            assertTrue("\"osName\":" !in payload)
        }
        assertContains(payload, "\"versionName\":")
        assertContains(payload, "\"commitHash\":")
    }

    @Test
    fun buildCrashReportUploadJson_allowsNullOptionalFields() {
        val payload = buildCrashReportUploadJson(
            CrashReportSubmitRequest(
                crashId = "123e4567-e89b-12d3-a456-426614174000",
                errorType = "kotlin.IllegalArgumentException",
                errorMessage = null,
                stackTrace = null,
                gameLog = null,
                settingsJson = null
            )
        )

        assertContains(payload, "\"errorMessage\":null")
        assertContains(payload, "\"stackTrace\":null")
        assertContains(payload, "\"gameLog\":null")
        assertContains(payload, "\"settingsJson\":null")
    }

    @Test
    fun buildCrashReportUploadJson_escapesQuotesAndNewlinesInErrorMessage() {
        val payload = buildCrashReportUploadJson(
            CrashReportSubmitRequest(
                crashId = "123e4567-e89b-12d3-a456-426614174000",
                errorType = "RuntimeException",
                errorMessage = "bad \"value\"\nnext line",
                stackTrace = null,
                gameLog = null,
                settingsJson = null
            )
        )

        // Quotes and newlines must be escaped so the body is still valid JSON.
        assertContains(payload, "\\\"value\\\"")
        assertTrue("\n" !in payload.substringAfter("\"errorMessage\""), "raw newline must be escaped")
    }
}

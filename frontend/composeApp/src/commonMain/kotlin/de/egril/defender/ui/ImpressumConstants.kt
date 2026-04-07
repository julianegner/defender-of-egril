package de.egril.defender.ui

/**
 * Constants for the Impressum (legal information page).
 * These strings are not translatable and are only used when the withImpressum build flag is enabled.
 * Using constants instead of hardcoded strings to avoid test failures for fixed text in UI.
 */
object ImpressumConstants {
    const val IMPRESSUM_TITLE = "Impressum"
    const val IMPRESSUM_NAME = "Julian Egner"
    const val IMPRESSUM_STREET = "Weissstrasse 18"
    const val IMPRESSUM_POSTAL_CODE = "53123"
    const val IMPRESSUM_CITY = "Bonn"
    const val IMPRESSUM_COUNTRY = "Germany"
    const val IMPRESSUM_EMAIL_LABEL = "mail: "
    const val IMPRESSUM_EMAIL = "admin@egril.de"
}

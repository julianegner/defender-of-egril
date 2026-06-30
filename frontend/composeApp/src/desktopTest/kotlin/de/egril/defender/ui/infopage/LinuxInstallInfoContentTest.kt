package de.egril.defender.ui.infopage

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class LinuxInstallInfoContentTest {
    @Test
    fun installationAndDownloadPagesReferenceLinuxPackageManagerComposable() {
        val installationInfo =
            File(
                "src/commonMain/kotlin/de/egril/defender/ui/infopage/InstallationInfo.kt",
            ).readText()
        val downloadInfo =
            File(
                "src/commonMain/kotlin/de/egril/defender/ui/infopage/DownloadInfo.kt",
            ).readText()

        assertTrue(installationInfo.contains("LinuxPackageManagerInstallInfo()"))
        assertTrue(downloadInfo.contains("LinuxPackageManagerInstallInfo()"))
    }

    @Test
    fun englishStringsContainSnapAndAptCommands() {
        val stringsXml =
            File(
                "src/commonMain/composeResources/values/strings.xml",
            ).readText()

        assertTrue(stringsXml.contains("installation_linux_snapstore_command"))
        assertTrue(stringsXml.contains("sudo snap install defender-of-egril"))
        assertTrue(stringsXml.contains("installation_linux_apt_setup_command"))
        assertTrue(stringsXml.contains("installation_linux_apt_install_command"))
        assertTrue(stringsXml.contains("sudo apt install defender-of-egril"))
    }
}

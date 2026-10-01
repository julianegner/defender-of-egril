package de.egril.defender.ui.editor

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.license_agpl_freedom_modify
import defender_of_egril.composeapp.generated.resources.license_agpl_freedom_redistribute
import defender_of_egril.composeapp.generated.resources.license_agpl_freedom_run
import defender_of_egril.composeapp.generated.resources.license_agpl_freedom_study
import defender_of_egril.composeapp.generated.resources.license_agpl_freedoms_title
import defender_of_egril.composeapp.generated.resources.license_agpl_network_clause_description
import defender_of_egril.composeapp.generated.resources.license_agpl_network_clause_title
import defender_of_egril.composeapp.generated.resources.upload_community_agpl_summary
import defender_of_egril.composeapp.generated.resources.upload_community_license_notice
import org.junit.Rule
import org.junit.Test

class CommunityUploadLicenseNoticeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun noticeIncludesAllLicenseTerms() {
        val texts = mutableListOf<String>()
        composeTestRule.setContent {
            texts.clear()
            texts += stringResource(Res.string.upload_community_license_notice)
            texts += stringResource(Res.string.upload_community_agpl_summary)
            texts += stringResource(Res.string.license_agpl_freedoms_title)
            texts += "• " + stringResource(Res.string.license_agpl_freedom_run)
            texts += "• " + stringResource(Res.string.license_agpl_freedom_study)
            texts += "• " + stringResource(Res.string.license_agpl_freedom_redistribute)
            texts += "• " + stringResource(Res.string.license_agpl_freedom_modify)
            texts += stringResource(Res.string.license_agpl_network_clause_title)
            texts += stringResource(Res.string.license_agpl_network_clause_description)
            CommunityUploadLicenseNotice()
        }

        texts.forEach { composeTestRule.onNodeWithText(it).assertExists() }
        composeTestRule.onNodeWithText("https://www.gnu.org/licenses/agpl-3.0.txt", substring = true).assertExists()
    }
}

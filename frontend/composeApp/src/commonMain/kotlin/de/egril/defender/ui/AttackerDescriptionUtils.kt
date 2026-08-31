package de.egril.defender.ui

import androidx.compose.runtime.Composable
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.demonling_description
import defender_of_egril.composeapp.generated.resources.blue_demon_description
import defender_of_egril.composeapp.generated.resources.dragon_description
import defender_of_egril.composeapp.generated.resources.evil_wizard_description
import defender_of_egril.composeapp.generated.resources.ghost_description
import defender_of_egril.composeapp.generated.resources.goblin_description
import defender_of_egril.composeapp.generated.resources.green_witch_description
import defender_of_egril.composeapp.generated.resources.ogre_description
import defender_of_egril.composeapp.generated.resources.ork_description
import defender_of_egril.composeapp.generated.resources.pirate_description
import defender_of_egril.composeapp.generated.resources.red_demon_description
import defender_of_egril.composeapp.generated.resources.red_witch_description
import defender_of_egril.composeapp.generated.resources.robotic_goblin_description
import defender_of_egril.composeapp.generated.resources.skeleton_description
import defender_of_egril.composeapp.generated.resources.snotling_description
import defender_of_egril.composeapp.generated.resources.spiderling_description
import defender_of_egril.composeapp.generated.resources.troll_description
import defender_of_egril.composeapp.generated.resources.undead_dragon_description
import defender_of_egril.composeapp.generated.resources.villain_araxxa_description
import defender_of_egril.composeapp.generated.resources.villain_ewhad_description
import defender_of_egril.composeapp.generated.resources.villain_freya_description
import defender_of_egril.composeapp.generated.resources.villain_garokk_description
import defender_of_egril.composeapp.generated.resources.villain_gribnak_description
import defender_of_egril.composeapp.generated.resources.villain_haga_description
import defender_of_egril.composeapp.generated.resources.villain_ignis_va_description
import defender_of_egril.composeapp.generated.resources.villain_kraken_description
import defender_of_egril.composeapp.generated.resources.villain_malakor_description
import defender_of_egril.composeapp.generated.resources.villain_morguk_description
import defender_of_egril.composeapp.generated.resources.villain_morvath_description
import defender_of_egril.composeapp.generated.resources.villain_ratterzahn_description
import defender_of_egril.composeapp.generated.resources.villain_roderich_description
import defender_of_egril.composeapp.generated.resources.villain_silas_description
import defender_of_egril.composeapp.generated.resources.villain_sybilla_description
import defender_of_egril.composeapp.generated.resources.villain_sylvanas_description
import defender_of_egril.composeapp.generated.resources.villain_valerius_description
import defender_of_egril.composeapp.generated.resources.villain_xarithon_description
import defender_of_egril.composeapp.generated.resources.villain_zussa_description
import defender_of_egril.composeapp.generated.resources.villain_zythar_description
import defender_of_egril.composeapp.generated.resources.zombie_description

@Composable
fun AttackerType.getLocalizedDescription(): String {
    val stringRes =
        when (this) {
            AttackerType.GOBLIN -> Res.string.goblin_description
            AttackerType.ORK -> Res.string.ork_description
            AttackerType.OGRE -> Res.string.ogre_description
            AttackerType.TROLL -> Res.string.troll_description
            AttackerType.SKELETON -> Res.string.skeleton_description
            AttackerType.ZOMBIE -> Res.string.zombie_description
            AttackerType.EVIL_WIZARD -> Res.string.evil_wizard_description
            AttackerType.BLUE_DEMON -> Res.string.blue_demon_description
            AttackerType.RED_DEMON -> Res.string.red_demon_description
            AttackerType.GHOST -> Res.string.ghost_description
            AttackerType.PIRATE -> Res.string.pirate_description
            AttackerType.RED_WITCH -> Res.string.red_witch_description
            AttackerType.GREEN_WITCH -> Res.string.green_witch_description
            AttackerType.SNOTLING -> Res.string.snotling_description
            AttackerType.SPIDERLING -> Res.string.spiderling_description
            AttackerType.ROBOTIC_GOBLIN -> Res.string.robotic_goblin_description
            AttackerType.DRAGON -> Res.string.dragon_description
            AttackerType.DRAGON_TERROR -> Res.string.dragon_description
            AttackerType.UNDEAD_DRAGON -> Res.string.undead_dragon_description
            AttackerType.EWHAD -> Res.string.villain_ewhad_description
            AttackerType.GAROKK -> Res.string.villain_garokk_description
            AttackerType.SNOTLING_BOSS -> Res.string.villain_gribnak_description
            AttackerType.MORGUK_BONEWHISPER -> Res.string.villain_morguk_description
            AttackerType.ARAXXA -> Res.string.villain_araxxa_description
            AttackerType.BARON_RATTERZAHN -> Res.string.villain_ratterzahn_description
            AttackerType.FALLEN_SHIELDMAIDEN_FREYA -> Res.string.villain_freya_description
            AttackerType.PRINCE_VALERIUS_THE_SOULREAPER -> Res.string.villain_valerius_description
            AttackerType.SILAS_THE_MASKMASTER,
            AttackerType.SILAS_MIRROR_IMAGE,
            -> Res.string.villain_silas_description
            AttackerType.GRAND_COVEN_MOTHER_SYBILLA -> Res.string.villain_sybilla_description
            AttackerType.HAGA -> Res.string.villain_haga_description
            AttackerType.ZUSSA -> Res.string.villain_zussa_description
            AttackerType.SYLVANAS_THE_MOLDING -> Res.string.villain_sylvanas_description
            AttackerType.ARCHMAGE_MALAKOR_THE_RENEGADE -> Res.string.villain_malakor_description
            AttackerType.IGNIS_VA_THE_DRAGONVOICE -> Res.string.villain_ignis_va_description
            AttackerType.MORVATH_THE_SHADOWMASTER -> Res.string.villain_morvath_description
            AttackerType.XARITHON_THE_SHADOW_DRAGON -> Res.string.villain_xarithon_description
            AttackerType.CAPTAIN_RODERICH -> Res.string.villain_roderich_description
            AttackerType.THE_KRAKEN -> Res.string.villain_kraken_description
            AttackerType.DEMONLING -> Res.string.demonling_description
            AttackerType.ZYTHAR_THE_RIFTCALLER -> Res.string.villain_zythar_description
        }
    return stringResource(stringRes)
}

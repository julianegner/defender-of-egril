package de.egril.defender.ui

import de.egril.defender.model.AttackType
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.SpellType
import com.hyperether.resources.AppLocale
import com.hyperether.resources.LocalizedStrings

/**
 * Get localized name for a DefenderType
 */
fun DefenderType.getLocalizedName(locale: AppLocale = com.hyperether.resources.currentLanguage.value): String {
    val key = when (this) {
        DefenderType.SPIKE_TOWER -> "spike_tower_name"
        DefenderType.SPEAR_TOWER -> "spear_tower_name"
        DefenderType.BOW_TOWER -> "bow_tower_name"
        DefenderType.WIZARD_TOWER -> "wizard_tower_name"
        DefenderType.ALCHEMY_TOWER -> "alchemy_tower_name"
        DefenderType.BALLISTA_TOWER -> "ballista_tower_name"
        DefenderType.DWARVEN_MINE -> "dwarven_mine_name"
        DefenderType.DRAGONS_LAIR -> "dragons_lair_name"
    }
    return LocalizedStrings.get(key, locale)
}

/**
 * Get localized name for a SpellType
 */
fun SpellType.getLocalizedName(locale: AppLocale = com.hyperether.resources.currentLanguage.value): String {
    val key = when (this) {
        SpellType.ATTACK_AREA -> "spell_attack_area_name"
        SpellType.ATTACK_AIMED -> "spell_attack_aimed_name"
        SpellType.HEAL -> "spell_heal_name"
        SpellType.INSTANT_TOWER -> "spell_instant_tower_name"
        SpellType.BOMB -> "spell_bomb_name"
        SpellType.DOUBLE_TOWER_LEVEL -> "spell_double_tower_level_name"
        SpellType.COOLING_SPELL -> "spell_cooling_spell_name"
        SpellType.FREEZE_SPELL -> "spell_freeze_spell_name"
        SpellType.DOUBLE_TOWER_REACH -> "spell_double_tower_reach_name"
    }
    return LocalizedStrings.get(key, locale)
}

/**
 * Get localized description for a SpellType
 */
fun SpellType.getLocalizedDescription(locale: AppLocale = com.hyperether.resources.currentLanguage.value): String {
    val key = when (this) {
        SpellType.ATTACK_AREA -> "spell_attack_area_desc"
        SpellType.ATTACK_AIMED -> "spell_attack_aimed_desc"
        SpellType.HEAL -> "spell_heal_desc"
        SpellType.INSTANT_TOWER -> "spell_instant_tower_desc"
        SpellType.BOMB -> "spell_bomb_desc"
        SpellType.DOUBLE_TOWER_LEVEL -> "spell_double_tower_level_desc"
        SpellType.COOLING_SPELL -> "spell_cooling_spell_desc"
        SpellType.FREEZE_SPELL -> "spell_freeze_spell_desc"
        SpellType.DOUBLE_TOWER_REACH -> "spell_double_tower_reach_desc"
    }
    return LocalizedStrings.get(key, locale)
}

/**
 * Get localized name for an AttackType
 */
fun AttackType.getLocalizedName(locale: AppLocale = com.hyperether.resources.currentLanguage.value): String {
    val key = when (this) {
        AttackType.MELEE -> "attack_type_melee"
        AttackType.RANGED -> "attack_type_ranged"
        AttackType.AREA -> "attack_type_fireball"
        AttackType.LASTING -> "attack_type_acid"
        AttackType.NONE -> "attack_type_special"
    }
    return LocalizedStrings.get(key, locale)
}

/**
 * Get localized name for an AttackerType
 */
fun AttackerType.getLocalizedName(locale: AppLocale = com.hyperether.resources.currentLanguage.value): String {
    val key = when (this) {
        AttackerType.GOBLIN -> "goblin_name"
        AttackerType.ORK -> "ork_name"
        AttackerType.OGRE -> "ogre_name"
        AttackerType.SKELETON -> "skeleton_name"
        AttackerType.EVIL_WIZARD -> "evil_wizard_name"
        AttackerType.BLUE_DEMON -> "blue_demon_name"
        AttackerType.RED_DEMON -> "red_demon_name"
        AttackerType.EVIL_MAGE -> "evil_mage_name"
        AttackerType.RED_WITCH -> "red_witch_name"
        AttackerType.GREEN_WITCH -> "green_witch_name"
        AttackerType.EWHAD -> "ewhad_name"
        AttackerType.DRAGON -> "dragon_name"
    }
    return LocalizedStrings.get(key, locale)
}

/**
 * Get localized short name for a DefenderType (for compact displays)
 */
fun DefenderType.getLocalizedShortName(locale: AppLocale = com.hyperether.resources.currentLanguage.value): String {
    val key = when (this) {
        DefenderType.SPIKE_TOWER -> "spike_tower_short"
        DefenderType.SPEAR_TOWER -> "spear_tower_short"
        DefenderType.BOW_TOWER -> "bow_tower_short"
        DefenderType.WIZARD_TOWER -> "wizard_tower_short"
        DefenderType.ALCHEMY_TOWER -> "alchemy_tower_short"
        DefenderType.BALLISTA_TOWER -> "ballista_tower_short"
        DefenderType.DWARVEN_MINE -> "dwarven_mine_short"
        DefenderType.DRAGONS_LAIR -> "dragons_lair_short"
    }
    return LocalizedStrings.get(key, locale)
}

package de.egril.defender.model

import de.egril.defender.editor.RepositoryLoader

/**
 * List of dragon names that can be loaded from repository or fall back to defaults
 * Used to give each spawned dragon a unique name
 */
object DragonNames {
    // Default names as fallback (200 dragon names inspired by mythopedia.com name generator)
    private val defaultNames = listOf(
        "Flameheart", "Shadowwing", "Frostfang", "Thunderclaw", "Emberstrike",
        "Nightshade", "Stormscale", "Icevein", "Blazefury", "Darkflame",
        "Ironhide", "Silvermoon", "Bloodfang", "Ashenwing", "Skyrender",
        "Earthshaker", "Cloudbreaker", "Sunfire", "Moonshadow", "Starburst",
        "Deathwing", "Lifebringer", "Soulshard", "Voidwalker", "Lightbearer",
        "Crimsonscale", "Goldcrest", "Bronzetail", "Copperhorn", "Steelfang",
        "Windwhisper", "Raincaller", "Snowdrift", "Fogveil", "Mistwalker",
        "Bonecrusher", "Fleshrender", "Bloodhunter", "Skullbreaker", "Doomcaller",
        "Hopebringer", "Faithkeeper", "Dreamweaver", "Visionseeker", "Truthfinder",
        "Fireborn", "Waterborn", "Earthborn", "Airborn", "Stormborn",
        "Nightbane", "Daybringer", "Duskfall", "Dawnriser", "Twilightshade",
        "Dragonclaw", "Wyrmfang", "Serpentscale", "Basiliskgaze", "Cobrahood",
        "Tigerstripe", "Lionmane", "Eagleeye", "Hawkwing", "Ravencall",
        "Wolfhowl", "Bearclaw", "Foxfur", "Deerleap", "Elkhorn",
        "Mountainpeak", "Valleylow", "Riverflow", "Oceandeep", "Lakeshore",
        "Forestshade", "Plainrunner", "Desertwind", "Tundracold", "Jungleheart",
        "Rubyeye", "Sapphirewing", "Emeraldscale", "Diamondfang", "Onyxclaw",
        "Amethysthorn", "Topazglow", "Pearlshine", "Opalhue", "Jadegreen",
        "Blackscale", "Whitewing", "Redclaw", "Bluefang", "Greenhorn",
        "Yelloweye", "Purpleheart", "Orangefire", "Browneearth", "Graystorm",
        "Thunderheart", "Lightningbolt", "Tempestfury", "Hurricanewing", "Tornadoclaw",
        "Earthquakefang", "Avalanchehorn", "Tsunamiscale", "Volcanoeye", "Blizzardwing",
        "Auroralight", "Nebulashade", "Galaxyswirl", "Cometail", "Meteorstrike",
        "Sunbeam", "Moonbeam", "Starlight", "Voidlight", "Shadowlight",
        "Timekeeper", "Spacewalker", "Realmshifter", "Dimensionbreaker", "Realitywarper",
        "Chaosborn", "Orderbringer", "Balancekeeper", "Harmonyseeker", "Discordsower",
        "Wisdomwing", "Knowledgefang", "Intelligencescale", "Cunningclaw", "Crafthorn",
        "Strengthheart", "Powerclaw", "Mightfang", "Forcewing", "Energyscale",
        "Speedrun", "Swiftfly", "Quickstrike", "Flashmove", "Blurmotion",
        "Silentshadow", "Quietwhisper", "Peacefulrest", "Calmwave", "Sereneshade",
        "Wildrage", "Fiercestorm", "Savageclaw", "Brutalfang", "Violentwing",
        "Gentlebreeze", "Kindwhisper", "Mercifulheart", "Compassionclaw", "Gracewing",
        "Proudpeak", "Noblescale", "Royalcrown", "Majesticwing", "Regalclaw",
        "Humblevalley", "Modestheart", "Simplefang", "Plainwing", "Basicscale",
        "Ancientone", "Elderwing", "Wisestone", "Oldguard", "Timeworn",
        "Youngblood", "Newborn", "Freshwing", "Brightfuture", "Hopespring",
        "Eternalsoul", "Immortalspirit", "Endslesslife", "Foreverwing", "Timelessheart",
        "Mortalfear", "Deathrattle", "Endtimes", "Finalbreath", "Lastcall",
        "Mysticseer", "Magicweaver", "Spellcaster", "Enchantedwing", "Arcanescale",
        "Holysanct", "Divinelight", "Blessedone", "Sacredflame", "Purewing"
    )
    
    // Loaded names from repository (cached after first load)
    private var loadedNames: List<String>? = null
    private var loadAttempted = false
    
    /**
     * Get the list of dragon names (from repository or defaults)
     */
    val names: List<String>
        get() {
            // If we've already loaded or attempted to load, use that
            if (loadAttempted) {
                return loadedNames ?: defaultNames
            }
            
            // Try to load from repository
            try {
                loadAttempted = true
                kotlinx.coroutines.runBlocking {
                    loadedNames = RepositoryLoader.loadDragonNames()
                }
                
                if (loadedNames != null) {
                    println("Loaded ${loadedNames!!.size} dragon names from repository")
                } else {
                    println("Using default dragon names (${defaultNames.size} names)")
                }
            } catch (e: Exception) {
                println("Error loading dragon names from repository, using defaults: ${e.message}")
            }
            
            return loadedNames ?: defaultNames
        }
    
    /**
     * Get a random dragon name from the list
     */
    fun getRandomName(): String {
        return names.random()
    }
}

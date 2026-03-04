package de.egril.defender.model

import de.egril.defender.editor.RepositoryLoader
import de.egril.defender.utils.runBlockingCompat
import de.egril.defender.config.LogConfig

/**
 * List of dragon names that can be loaded from repository or fall back to defaults
 * Used to give each spawned dragon a unique name
 */
object DragonNames {
    // Default names as fallback (200 dragon names from mythopedia.com name generator)
    private val defaultNames = listOf(
        "Hakirax",
        "Fangan",
        "Aphard",
        "Kryss",
        "Melin",
        "Phyrus",
        "Lagon",
        "Jakusiarmi",
        "Ororon",
        "Drithustix",
        "Mandax",
        "Aitrax",
        "Malcoatl",
        "Wyvethak",
        "Phirax",
        "Sivern",
        "Ephanastix",
        "Drithusina",
        "Ephirwenna",
        "Lynna",
        "Daene",
        "Zysysys",
        "Sarduinda",
        "Amelinelix",
        "Sarmungana",
        "Sastix",
        "Sardra",
        "Lagonin",
        "Vritrax",
        "Sarmus",
        "Vrizard",
        "Ephix",
        "Sivesia",
        "Fanaldr",
        "Sarithrax",
        "Zystix",
        "Gonienna",
        "Brendrmix",
        "Orona",
        "Vessapep",
        "Fannarmix",
        "Drusinda",
        "Zyssia",
        "Wyvermuna",
        "Melix",
        "Tsung",
        "Aithora",
        "Goninda",
        "Sinda",
        "Amindax",
        "Hardrng",
        "Scyllax",
        "Krystana",
        "Hardanagon",
        "Lumira",
        "Calduinern",
        "Zarmitra",
        "Annax",
        "Calduinda",
        "Dakryss",
        "Nineirax",
        "Nindaene",
        "Linene",
        "Sapephyrus",
        "Aithormung",
        "Smaley",
        "Malduine",
        "Chakirax",
        "Fanahhak",
        "Ancatl",
        "Eirochira",
        "Amitrax",
        "Jorax",
        "Ormitenna",
        "Lievermite",
        "Melieves",
        "Krysys",
        "Tsungang",
        "Tsuna",
        "Andakira",
        "Dritrax",
        "Apepon",
        "Scylladon",
        "Jakirax",
        "Aldrax",
        "Jormien",
        "Laladmusa",
        "Tsunganax",
        "Aitephyrus",
        "Ritrax",
        "Vrizardon",
        "Apephia",
        "Fangandax",
        "Ehecadon",
        "Tsungan",
        "Lynnastix",
        "Hardon",
        "Ridleyss",
        "Maugant",
        "Cadon",
        "Brennastix",
        "Lumitrax",
        "Linermia",
        "Zarizarmin",
        "Lienern",
        "Chakuk",
        "Chirax",
        "Anthrax",
        "Chirwen",
        "Mannahhaku",
        "Aleficen",
        "Rithus",
        "Melus",
        "Salaug",
        "Amelix",
        "Eiralagon",
        "Lineves",
        "Dalcoatl",
        "Zarmien",
        "Phirwenes",
        "Brenepona",
        "Krysstixue",
        "Lievern",
        "Zysstandax",
        "Quethus",
        "Sindrus",
        "Sadmusapep",
        "Quenen",
        "Lardmung",
        "Zystixue",
        "Quenaug",
        "Lozzahhard",
        "Zaleylla",
        "Lumaleysys",
        "Scyllaze",
        "Alduindax",
        "Quetzahhak",
        "Sarithumi",
        "Sivestix",
        "Nindruk",
        "Kryssia",
        "Lienthormi",
        "Anasia",
        "Ehecatlien",
        "Krysystix",
        "Maleysstix",
        "Lumaug",
        "Hakirwen",
        "Ningana",
        "Drysssia",
        "Niney",
        "Lozzenna",
        "Orozz",
        "Cadong",
        "Amelus",
        "Eponepepon",
        "Wyvestasia",
        "Faninda",
        "Lozzzysyss",
        "Aphyruk",
        "Jormuss",
        "Anagon",
        "Orazepon",
        "Tsunabelix",
        "Fanasinern",
        "Quethorax",
        "Amelindaku",
        "Sivernna",
        "Sarmusa",
        "Wyvermi",
        "Zysysss",
        "Eponasiaku",
        "Wyvethaku",
        "Linern",
        "Zelusia",
        "Queveryss",
        "Chumanda",
        "Angandaen",
        "Chieves",
        "Eiroatl",
        "Brene",
        "Sives",
        "Wyves",
        "Ephithrax",
        "Ormusiax",
        "Andrus",
        "Lindaentl",
        "Cadoninda",
        "Vrithus",
        "Fanasives",
        "Ehecatleys",
        "Phirozz",
        "Orminelina",
        "Chira",
        "Lumaldr",
        "Zysung",
        "Ancalazene",
        "Lieveryss",
        "Smaldrite",
        "Smanasives"
    )
    
    // Loaded names from repository (cached after first load)
    private var loadedNames: List<String>? = null
    private var loadAttempted = false
    
    /**
     * Get the list of dragon names (from repository or defaults)
     * Note: First access will block briefly to load from repository.
     * This is acceptable as dragon names are only needed when spawning dragons,
     * which happens after the game is already running.
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
                runBlockingCompat {
                    loadedNames = RepositoryLoader.loadDragonNames()
                }
                
                if (loadedNames != null) {
                    println("Loaded ${loadedNames!!.size} dragon names from repository")
                } else {
                    if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                    println("Using default dragon names (${defaultNames.size} names)")
                    }
                }
            } catch (e: Exception) {
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("Error loading dragon names from repository, using defaults: ${e.message}")
                }
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

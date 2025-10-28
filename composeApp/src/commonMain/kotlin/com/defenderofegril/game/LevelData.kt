package com.defenderofegril.game

import com.defenderofegril.model.*

object LevelData {
    
    fun createLevels(): List<Level> {
        return listOf(
            createLevel1(),
            createLevel2(),
            createLevel3(),
            createLevel4(),
            createLevel5(),
            createLevel6()
        )
    }
    
    private fun createLevel1(): Level {
        val pathAndIslands = Level.generateCurvedPathWithIslands(30, 8)
        return Level(
            id = 1,
            name = "The First Wave",
            pathCells = pathAndIslands.pathCells,
            buildIslands = pathAndIslands.buildIslands,
            attackerWaves = listOf(
                AttackerWave(
                    attackers = List(30) { AttackerType.GOBLIN },
                    spawnDelay = 1  // Spawn every turn
                )
            ),
            initialCoins = 100,
            healthPoints = 10
        )
    }
    
    private fun createLevel2(): Level {
        val pathAndIslands = Level.generateCurvedPathWithIslands(35, 9)
        return Level(
            id = 2,
            name = "Mixed Forces",
            pathCells = pathAndIslands.pathCells,
            buildIslands = pathAndIslands.buildIslands,
            attackerWaves = listOf(
                AttackerWave(
                    attackers = List(30) { AttackerType.GOBLIN } + List(20) { AttackerType.SKELETON },
                    spawnDelay = 1  // Spawn every turn
                ),
                AttackerWave(
                    attackers = List(15) { AttackerType.ORK },
                    spawnDelay = 1
                )
            ),
            initialCoins = 120,
            healthPoints = 10
        )
    }
    
    private fun createLevel3(): Level {
        val pathAndIslands = Level.generateCurvedPathWithIslands(40, 10)
        return Level(
            id = 3,
            name = "The Ork Invasion",
            pathCells = pathAndIslands.pathCells,
            buildIslands = pathAndIslands.buildIslands,
            attackerWaves = listOf(
                AttackerWave(
                    attackers = List(40) { AttackerType.GOBLIN },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(30) { AttackerType.ORK } + List(20) { AttackerType.SKELETON },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(20) { AttackerType.ORK } + List(5) { AttackerType.OGRE },
                    spawnDelay = 2
                )
            ),
            initialCoins = 150,
            healthPoints = 8
        )
    }
    
    private fun createLevel4(): Level {
        val pathAndIslands = Level.generateCurvedPathWithIslands(45, 11)
        return Level(
            id = 4,
            name = "Dark Magic Rises",
            pathCells = pathAndIslands.pathCells,
            buildIslands = pathAndIslands.buildIslands,
            attackerWaves = listOf(
                AttackerWave(
                    attackers = List(30) { AttackerType.GOBLIN } + List(5) { AttackerType.EVIL_WIZARD },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(20) { AttackerType.ORK } + List(5) { AttackerType.WITCH },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(5) { AttackerType.OGRE } + List(20) { AttackerType.SKELETON },
                    spawnDelay = 3
                ),
                AttackerWave(
                    attackers = List(10) { AttackerType.EVIL_WIZARD } + List(10) { AttackerType.WITCH },
                    spawnDelay = 2
                )
            ),
            initialCoins = 180,
            healthPoints = 8
        )
    }
    
    private fun createLevel5(): Level {
        val pathAndIslands = Level.generateCurvedPathWithIslands(50, 12)
        return Level(
            id = 5,
            name = "The Final Stand",
            pathCells = pathAndIslands.pathCells,
            buildIslands = pathAndIslands.buildIslands,
            attackerWaves = listOf(
                AttackerWave(
                    attackers = List(50) { AttackerType.SKELETON } + List(20) { AttackerType.EVIL_WIZARD },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(30) { AttackerType.ORK } + List(5) { AttackerType.WITCH },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(20) { AttackerType.OGRE } + List(30) { AttackerType.GOBLIN },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(20) { AttackerType.OGRE } + List(10) { AttackerType.EVIL_WIZARD } + List(10) { AttackerType.WITCH } + List(1) { AttackerType.EWHAD },
                    spawnDelay = 2
                )
            ),
            initialCoins = 200,
            healthPoints = 6
        )
    }
    
    private fun createLevel6(): Level {
        val pathAndIslands = Level.generateCurvedPathWithIslands(50, 12)
        return Level(
            id = 6,
            name = "Ewhad's Challenge",
            pathCells = pathAndIslands.pathCells,
            buildIslands = pathAndIslands.buildIslands,
            attackerWaves = listOf(
                AttackerWave(
                    attackers = List(1) { AttackerType.EWHAD },
                    spawnDelay = 1
                )
            ),
            initialCoins = 300,
            healthPoints = 10
        )
    }
}

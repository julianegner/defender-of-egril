package com.defenderofegril.game

import com.defenderofegril.model.*

object LevelData {
    
    fun createLevels(): List<Level> {
        return listOf(
            createLevel1(),
            createLevel2(),
            createLevel3(),
            createLevel4(),
            createLevel5()
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
                    attackers = List(5) { AttackerType.GOBLIN },
                    spawnDelay = 3
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
                    attackers = List(3) { AttackerType.GOBLIN } + List(2) { AttackerType.SKELETON },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(2) { AttackerType.ORK },
                    spawnDelay = 3
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
                    attackers = List(4) { AttackerType.GOBLIN },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(3) { AttackerType.ORK } + List(2) { AttackerType.SKELETON },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(2) { AttackerType.ORK } + List(1) { AttackerType.OGRE },
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
                    attackers = List(3) { AttackerType.GOBLIN } + List(2) { AttackerType.EVIL_WIZARD },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(2) { AttackerType.ORK } + List(2) { AttackerType.WITCH },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(1) { AttackerType.OGRE } + List(2) { AttackerType.SKELETON },
                    spawnDelay = 3
                ),
                AttackerWave(
                    attackers = List(1) { AttackerType.EVIL_WIZARD } + List(1) { AttackerType.WITCH },
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
                    attackers = List(5) { AttackerType.SKELETON } + List(2) { AttackerType.EVIL_WIZARD },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(3) { AttackerType.ORK } + List(2) { AttackerType.WITCH },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(2) { AttackerType.OGRE } + List(3) { AttackerType.GOBLIN },
                    spawnDelay = 2
                ),
                AttackerWave(
                    attackers = List(2) { AttackerType.OGRE } + List(2) { AttackerType.EVIL_WIZARD } + List(2) { AttackerType.WITCH },
                    spawnDelay = 2
                )
            ),
            initialCoins = 200,
            healthPoints = 6
        )
    }
}

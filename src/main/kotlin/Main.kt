package me.znotchill

import BaseGenerator
import me.znotchill.blossom.server.BlossomServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerLoadedEvent
import net.minestom.server.instance.block.Block
import opencl.ClimateRange
import opencl.OpenCLGenerator
import opencl.OpenCLWriter

object Server : BlossomServer() {
    val mainInstance = BaseGenerator.new()

    val generator = OpenCLGenerator()

    override fun preLoad() {

        generator.edit {
            biome {
                name = "plains"
                temp = ClimateRange(0.7f, 1f)
                humidity = ClimateRange(0f, 0.4f)
                surfaceBlock = Block.GRASS_BLOCK
            }
            biome {
                name = "jungle"
                temp = ClimateRange(0.7f, 1f)
                humidity = ClimateRange(0.7f, 1f)
                surfaceBlock = Block.PODZOL
            }
            biome {
                name = "taiga"
                temp = ClimateRange(0f, 0.4f)
                humidity = ClimateRange(0.3f, 0.8f)
                surfaceBlock = Block.DIRT
            }
        }

        val writer = OpenCLWriter(generator)
        println(writer.generate())

        mainInstance.timeRate = 0
        listener<AsyncPlayerConfigurationEvent> { event ->
            event.spawningInstance = mainInstance
            event.player.respawnPoint = Pos(0.0, 80.0, 0.0)
            event.player.permissionLevel = 4
        }
        listener<PlayerLoadedEvent> { event ->
            event.player.gameMode = GameMode.SPECTATOR
        }
    }
    override fun postLoad() {
        logger.info("Server started!")
    }
}

fun main() {
    Server.start()
}
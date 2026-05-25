package me.znotchill

import BaseGenerator
import GpuTerrainContext
import me.znotchill.blossom.server.BlossomServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerLoadedEvent
import net.minestom.server.instance.block.Block
import opencl.ClimateRange
import opencl.NoiseType
import opencl.OpenCLGenerator
import opencl.OpenCLWriter

object Server : BlossomServer() {

    val generator = OpenCLGenerator()

    override fun preLoad() {

        generator.edit {
            heightProfile.noiseScale = 0.004f
            heightProfile.biomeStrength = 0.004f
            biome {
                name = "plains"
                temp = ClimateRange(0.7f, 1f)
                humidity = ClimateRange(0f, 0.4f)
                surfaceBlock = Block.GRASS_BLOCK
                underBlock = Block.DIRT

                weight = 1.2f

                noise.type = NoiseType.FBM
                noise.operation = "* 0.4f + 0.3f"
            }
            biome {
                name = "jungle"
                temp = ClimateRange(0.7f, 1f)
                humidity = ClimateRange(0.7f, 1f)
                surfaceBlock = Block.PODZOL
                underBlock = Block.DIRT
                maxHeight = 100

                weight = 1.0f

                noise.type = NoiseType.FBM
                noise.operation = "* 0.6f + 0.2f"
            }
            biome {
                name = "taiga"
                temp = ClimateRange(0f, 0.4f)
                humidity = ClimateRange(0.3f, 0.8f)
                surfaceBlock = Block.DIRT_PATH
                underBlock = Block.DIRT
                maxHeight = 95

                weight = 0.9f

                noise.type = NoiseType.FBM
                noise.operation = "* 0.5f + 0.25f"
            }
            biome {
                name = "ocean"
                temp = ClimateRange(0f, 0.25f)
                humidity = ClimateRange(0.8f, 1f)
                surfaceBlock = Block.GLASS
                underBlock = Block.GRAVEL
                airBlock = Block.WATER
                minHeight = 44

                weight = 1.6f

                noise.type = NoiseType.FBM
                noise.operation = "* 0.3f"
            }
        }

        val writer = OpenCLWriter(generator)
        println(writer.generate())

        val context = GpuTerrainContext()
        val src = writer.generate()

        context.build(src)

        val mainInstance = BaseGenerator(context, generator).new()

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
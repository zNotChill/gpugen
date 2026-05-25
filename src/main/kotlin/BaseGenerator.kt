import me.znotchill.blossom.instances.InstanceTemplate
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.GenerationUnit

object BaseGenerator : InstanceTemplate {
    val gpu = GpuTerrainContext()

    override fun generate(unit: GenerationUnit) {
        val start = unit.absoluteStart()
        val chunkX = start.blockX() shr 4
        val chunkZ = start.blockZ() shr 4
        val startY = start.blockY()

        val data = gpu.generate(chunkX, chunkZ)

        for (x in 0..15)
            for (z in 0..15)
                for (y in 0..383) {
                    val block = when (data.blocks[x + z * 16 + y * 256]) {
                        1 -> Block.STONE
                        2 -> Block.DIRT
                        3 -> Block.GRASS_BLOCK
                        4 -> Block.SAND
                        5 -> Block.SANDSTONE
                        6 -> Block.SNOW_BLOCK
                        7 -> Block.GRAVEL
                        8 -> Block.WATER
                        9 -> Block.DIRT_PATH
                        10 -> Block.PODZOL
                        11 -> Block.MUD
                        else -> continue
                    }
                    unit.modifier().setBlock(start.blockX() + x, startY + y, start.blockZ() + z, block)
                }
    }
}
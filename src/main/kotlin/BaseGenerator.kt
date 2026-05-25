import me.znotchill.blossom.instances.InstanceTemplate
import net.minestom.server.instance.generator.GenerationUnit
import opencl.OpenCLGenerator

class BaseGenerator(
    val gpu: GpuTerrainContext,
    val generator: OpenCLGenerator
) : InstanceTemplate {
    override fun generate(unit: GenerationUnit) {
        val start = unit.absoluteStart()
        val chunkX = start.blockX() shr 4
        val chunkZ = start.blockZ() shr 4
        val startY = start.blockY()

        val data = gpu.generate(chunkX, chunkZ)

        for (x in 0..15)
            for (z in 0..15)
                for (y in 0..383) {
                    val blockId = data.blocks[x + z * 16 + y * 256]
                    val block = generator.getBlockAtIndex(blockId)
                    unit.modifier().setBlock(start.blockX() + x, startY + y, start.blockZ() + z, block)
                }
    }
}
package opencl

import net.minestom.server.instance.block.Block

class OpenCLGenerator {
    val biomes = mutableListOf<BiomeRule>()

    fun biome(block: BiomeRule.() -> Unit) {
        biomes += BiomeRule().apply(block)
    }

    fun getBlocks(): List<Block> {
        val blocks = biomes.map { it.surfaceBlock }
        return (listOf(Block.AIR, Block.WATER) + blocks).distinct()
    }

    fun edit(block: OpenCLGenerator.() -> Unit = {}) {
        this.block()
    }
}
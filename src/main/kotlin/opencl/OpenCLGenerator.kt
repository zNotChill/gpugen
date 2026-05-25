package opencl

import net.minestom.server.instance.block.Block

class OpenCLGenerator {
    val biomes = mutableListOf<BiomeRule>()
    val heightProfile = HeightProfile()

    private val cachedBlocks = mutableMapOf<Int, Block>()

    fun biome(block: BiomeRule.() -> Unit) {
        biomes += BiomeRule().apply(block)
    }

    fun getBlocks(): List<Block> {
        val surfaceBlocks = biomes.map { it.surfaceBlock }
        val underBlocks = biomes.map { it.underBlock }
        val base = listOf(Block.AIR, Block.WATER, Block.STONE)
        val total = (base + surfaceBlocks + underBlocks).distinct()

        cachedBlocks.clear()
        total.forEachIndexed { i, block -> cachedBlocks[i] = block }

        return total
    }

    fun getBlockAtIndex(index: Int): Block? {
        return cachedBlocks[index]
    }

    fun edit(block: OpenCLGenerator.() -> Unit = {}) {
        this.block()
    }
}
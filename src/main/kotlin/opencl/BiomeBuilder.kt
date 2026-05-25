package opencl

import net.minestom.server.instance.block.Block

class BiomeRule {
    var name: String = "unknown"

    var temp: ClimateRange = ClimateRange.DEFAULT
    var humidity: ClimateRange = ClimateRange.DEFAULT

    var surfaceBlock: Block = Block.GRASS_BLOCK

    var computedIndex: Int = 0
}

data class ClimateRange(
    val min: Float,
    val max: Float
) {
    operator fun contains(value: Float): Boolean =
        value in min..max

    fun center(): Float =
        (min + max) / 2f

    companion object {
        val DEFAULT = ClimateRange(0f, 1.0f)
    }
}
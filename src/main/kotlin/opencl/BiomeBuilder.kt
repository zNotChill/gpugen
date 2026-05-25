package opencl

import net.minestom.server.instance.block.Block

class BiomeRule {
    var name: String = "unknown"

    var temp: ClimateRange = ClimateRange.DEFAULT
    var humidity: ClimateRange = ClimateRange.DEFAULT

    var surfaceBlock: Block = Block.GRASS_BLOCK
    var underBlock: Block = Block.DIRT
    var airBlock: Block = Block.AIR

    var computedIndex: Int = 0

    var minHeight: Int = 62
    var maxHeight: Int = 85
    var weight: Float = 1.0f

    var noise: NoiseProfile = NoiseProfile()
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

enum class NoiseType(
    val functionName: String
) {
    FBM("fbm"),
    RIDGED("ridged"),
    SIMPLEX("simplex")
}

class NoiseProfile {
    var type: NoiseType = NoiseType.FBM
    var octaves: Int = 4
    var frequency: Float = 1f
    var amplitude: Float = 1f
    var operation: String = ""
}
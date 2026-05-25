package opencl

import net.minestom.server.instance.block.Block

class OpenCLWriter(
    val generator: OpenCLGenerator
) {
    val indent = "\t"
    val hashFunction = """
        float hash2(float x, float y) {
            float v = x * 127.1f + y * 311.7f;
            v = sin(v) * 43758.5453f;
            return v - floor(v);
        }
    """.trimIndent()

    val generateChunkHeader = """
        __kernel void generateChunk(
            __global int* blocks,
            __global int* heightmap,
            __global int* biomemap,
            int chunkX,
            int chunkZ,
            float scale
        )
    """.trimIndent()

    val getBiomeHeader = """
        int getBiome(float temp, float humidity)
    """.trimIndent()

    val getSurfaceHeader = """
        int getSurface(int biome, int y, int height)
    """.trimIndent()

    val definitionTag = "#define"

    fun getBiomeDefinition(biome: BiomeRule, index: Int): String {
        return "$definitionTag ${getBiomeVariable(biome)} $index"
    }

    fun getBlockDefinition(block: Block, index: Int): String {
        return "$definitionTag ${getBlockVariable(block)} $index"
    }

    fun getBlockVariable(block: Block): String =
        "BLOCK_${block.name().replace("minecraft:", "").uppercase()}"
    fun getBiomeVariable(biome: BiomeRule): String =
        "BIOME_${biome.name.uppercase()}"

    fun getBiomeFunction(): List<String> {
        val final = mutableListOf<String>()

        final += "$getBiomeHeader {"

        generator.biomes.forEachIndexed { i, biome ->
            val codeBlock = mutableListOf<String>()
            codeBlock += "if (temp > ${biome.temp.min}f && temp < ${biome.temp.max}f && humidity > ${biome.humidity.min}f && humidity < ${biome.humidity.max}f) {"
            codeBlock += "${indent}return ${getBiomeVariable(biome)};"
            codeBlock += ")"
            final += codeBlock.map { "$indent$it" }
        }

        final += "}"

        return final
    }

    fun getSurfaceFunction(): List<String> {
        val final = mutableListOf<String>()

        final += "$getSurfaceHeader {"

        final += "${indent}if (y > height) return (biome == BIOME_OCEAN && y <= SEA_LEVEL) ? BLOCK_WATER : BLOCK_AIR;"

        final += "${indent}if (y == height) {"
        generator.biomes.forEachIndexed { i, biome ->
            val codeBlock = mutableListOf<String>()
            val block = biome.surfaceBlock
            codeBlock += "if (biome == ${getBiomeVariable(biome)}) {"
            codeBlock += "${indent}return ${getBlockVariable(block)};"
            codeBlock += ")"
            final += codeBlock.map { "$indent$indent$it" }
        }

        final += "$indent}"
        final += "}"

        return final
    }

    fun generate(): String {
        val final = mutableListOf<String>()

        val blocks = generator.getBlocks()

        blocks.forEachIndexed { i, block ->
            final += getBlockDefinition(block, i)
        }

        generator.biomes.forEachIndexed { i, biome ->
            biome.computedIndex = i
            final += getBiomeDefinition(biome, i)
        }

        final += "$definitionTag SEA_LEVEL 64;"
        final += getBiomeFunction()
        final += getSurfaceFunction()

        return final.joinToString("\n")
    }
}
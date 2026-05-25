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

    val smoothNoiseFunction = """
        float smoothNoise(float px, float py) {
            float ix = floor(px);
            float iy = floor(py);
            float fx = px - ix;
            float fy = py - iy;
            float ux = fx * fx * (3.0f - 2.0f * fx);
            float uy = fy * fy * (3.0f - 2.0f * fy);

            float a = hash2(ix, iy);
            float b = hash2(ix + 1.0f, iy);
            float c = hash2(ix, iy + 1.0f);
            float d = hash2(ix + 1.0f, iy + 1.0f);

            float ab = a + (b - a) * ux;
            float cd = c + (d - c) * ux;
            return ab + (cd - ab) * uy;
        }
    """.trimIndent()

    val fbmFunction = """
        float fbm(float px, float py, int octaves) {
            float value = 0.0f;
            float amplitude = 0.5f;
            float frequency = 1.0f;
            for (int i = 0; i < octaves; i++) {
                value += amplitude * smoothNoise(px * frequency, py * frequency);
                frequency *= 2.0f;
                amplitude *= 0.5f;
            }
            return value;
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

    fun getBiomeWeight(biome: BiomeRule): Float {
        return biome.weight
    }

    fun getBiomeFunction(): List<String> {
        val final = mutableListOf<String>()

        final += "$getBiomeHeader {"

        final += "${indent}int value = 0;"
        final += "${indent}float bestScore = -9999.0f;"
        generator.biomes.forEach { biome ->
            final += "${indent}{"
            final += "${indent}${indent}float tempCenter = ${(biome.temp.min + biome.temp.max) * 0.5f}f;"
            final += "${indent}${indent}float humCenter = ${(biome.humidity.min + biome.humidity.max) * 0.5f}f;"

            final += "${indent}${indent}float dx = temp - tempCenter;"
            final += "${indent}${indent}float dy = humidity - humCenter;"

            final += "${indent}${indent}float dist = dx*dx + dy*dy;"
            final += "${indent}${indent}float score = exp(-dist * 2.5f);"
            final += "${indent}${indent}score *= ${getBiomeWeight(biome)}f;"

            final += "${indent}${indent}if (score > bestScore) {"
            final += "${indent}${indent}${indent}bestScore = score;"
            final += "${indent}${indent}${indent}value = ${getBiomeVariable(biome)};"
            final += "${indent}${indent}}"
            final += "${indent}}"
        }

        final += "${indent}return value;"
        final += "}"

        return final
    }

    fun getSurfaceFunction(): List<String> {
        val final = mutableListOf<String>()

        final += "$getSurfaceHeader {"

        generator.biomes.forEachIndexed { i, biome ->
            val codeBlock = mutableListOf<String>()
            val block = biome.surfaceBlock
            codeBlock += "if (biome == ${getBiomeVariable(biome)}) {"
            codeBlock += "${indent}if (y > height) {"
            codeBlock += "${indent}${indent}if (y <= SEA_LEVEL && height < SEA_LEVEL)"
            codeBlock += "${indent}${indent}${indent}return ${getBlockVariable(biome.airBlock)};"
            codeBlock += "${indent}${indent}return ${getBlockVariable(Block.AIR)};"
            codeBlock += "${indent}}"
            codeBlock += "${indent}if (y == height) return ${getBlockVariable(block)};"
            codeBlock += "${indent}if (y >= height - 4) return ${getBlockVariable(biome.underBlock)};"
            codeBlock += "}"
            final += codeBlock.map { "$indent$it" }
        }

        final += "${indent}return ${getBlockVariable(Block.STONE)};"
        final += "}"

        return final
    }

    fun generateChunkFunction(): List<String> {
        val final = mutableListOf<String>()

        final += "$generateChunkHeader {"

        final += """
            int x = get_global_id(0);
            int z = get_global_id(1);
            int y = get_global_id(2);
        
            float bx = (chunkX * 16 + x) * ${generator.heightProfile.noiseScale};
            float bz = (chunkZ * 16 + z) * ${generator.heightProfile.noiseScale};

            float temp = fbm(bx, bz, 6);
            float humidity = fbm(bx + 100.0f, bz + 100.0f, 6);
            int biome = getBiome(temp, humidity);
        
            float wx = (chunkX * 16 + x) * scale;
            float wz = (chunkZ * 16 + z) * scale;
            
            float heightNoise;
            int minHeight;
            int maxHeight;
        """.trimIndent().split("\n").map { "$indent$it "}

        generator.biomes.forEachIndexed { i, biome ->
            val codeBlock = mutableListOf<String>()
            val func =
                "${biome.noise.type.functionName}(wx, wz, 3)${biome.noise.operation}"

            codeBlock += "if (biome == ${getBiomeVariable(biome)}) {"
            codeBlock += "${indent}heightNoise = $func;"
            codeBlock += "${indent}minHeight = ${biome.minHeight};"
            codeBlock += "${indent}maxHeight = ${biome.maxHeight};"
            codeBlock += "}"
            final += codeBlock.map { "$indent$it" }
        }


        final += """
            int height = minHeight + (int)(heightNoise * (float)(maxHeight - minHeight));
        
            int idx = x + z * 16 + y * 256;
            blocks[idx] = getSurface(biome, y, height);
        
            if (y == 0) {
                heightmap[x + z * 16] = height;
                biomemap[x + z * 16] = biome;
            }
        """.trimIndent().split("\n").map { "$indent$it "}

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

        final += hashFunction
        final += smoothNoiseFunction
        final += fbmFunction
        final += "$definitionTag SEA_LEVEL 64"
        final += getBiomeFunction()
        final += getSurfaceFunction()
        final += generateChunkFunction()

        return final.joinToString("\n")
    }
}
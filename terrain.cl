float hash2(float x, float y) {
    float v = x * 127.1f + y * 311.7f;
    v = sin(v) * 43758.5453f;
    return v - floor(v);
}

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

#define BIOME_PLAINS    0
#define BIOME_DESERT    1
#define BIOME_MOUNTAINS 2
#define BIOME_OCEAN     3
#define BIOME_FOREST    4
#define BIOME_JUNGLE    5
#define BIOME_TAIGA     6

#define AIR        0
#define STONE      1
#define DIRT       2
#define GRASS      3
#define SAND       4
#define SANDSTONE  5
#define SNOW       6
#define GRAVEL     7
#define WATER      8
#define SNOW_DIRT  9
#define PODZOL     10
#define MUD        11

int getBiome(float temp, float humidity) {
    if (temp < 0.3f) return BIOME_OCEAN;

    if (temp < 0.5f) {
        if (humidity < 0.5f) return BIOME_TAIGA;
        else return BIOME_MOUNTAINS;
    } else if (temp < 0.7f) {
        if (humidity < 0.35f) return BIOME_PLAINS;
        else if (humidity < 0.65f) return BIOME_FOREST;
        else return BIOME_MOUNTAINS;
    } else {
        if (humidity < 0.4f) return BIOME_DESERT;
        else if (humidity < 0.7f) return BIOME_PLAINS;
        else return BIOME_JUNGLE;
    }
}

int getSurface(int biome, int y, int height) {
    if (y > height) return (biome == BIOME_OCEAN && y <= 64) ? WATER : AIR;

    if (y == height) {
        if (biome == BIOME_DESERT)
            return SAND;
        if (biome == BIOME_MOUNTAINS && height > 140)
            return SNOW;
        if (biome == BIOME_OCEAN)
            return GRAVEL;
        if (biome == BIOME_TAIGA)
            return SNOW_DIRT;
        if (biome == BIOME_JUNGLE)
            return PODZOL;
        return GRASS;
    }

    if (y >= height - 4) {
        if (biome == BIOME_DESERT) return SANDSTONE;
        if (biome == BIOME_OCEAN) return GRAVEL;
        if (biome == BIOME_JUNGLE) return MUD;
        return DIRT;
    }

    return STONE;
}

__kernel void generateChunk(
    __global int* blocks,
    __global int* heightmap,
    __global int* biomemap,
    int chunkX,
    int chunkZ,
    float scale
) {
    int x = get_global_id(0);
    int z = get_global_id(1);
    int y = get_global_id(2);

    float bx = (chunkX * 16 + x) * 0.004f;
    float bz = (chunkZ * 16 + z) * 0.004f;

    float temp = fbm(bx, bz, 6);
    float humidity = fbm(bx + 100.0f, bz + 100.0f, 6);
    int biome = getBiome(temp, humidity);

    float wx = (chunkX * 16 + x) * scale;
    float wz = (chunkZ * 16 + z) * scale;

    float heightNoise;
    if (biome == BIOME_MOUNTAINS) {
        float ridged = 1.0f - fabs(fbm(wx, wz, 6) * 2.0f - 1.0f);
        heightNoise = ridged * ridged;
    } else if (biome == BIOME_OCEAN) {
        heightNoise = fbm(wx, wz, 3) * 0.3f;
    } else if (biome == BIOME_PLAINS) {
        heightNoise = fbm(wx, wz, 4) * 0.4f + 0.3f;
    } else if (biome == BIOME_JUNGLE) {
        heightNoise = fbm(wx, wz, 5) * 0.6f + 0.2f;
    } else if (biome == BIOME_TAIGA) {
        heightNoise = fbm(wx, wz, 4) * 0.5f + 0.25f;
    } else if (biome == BIOME_FOREST) {
        heightNoise = fbm(wx, wz, 4) * 0.5f + 0.2f;
    } else {
        heightNoise = fbm(wx, wz, 4);
    }

    int minHeight = (biome == BIOME_OCEAN) ? 44 : 62;
    int maxHeight;
    if (biome == BIOME_MOUNTAINS) maxHeight = 200;
    else if (biome == BIOME_JUNGLE) maxHeight = 100;
    else if (biome == BIOME_TAIGA) maxHeight = 95;
    else if (biome == BIOME_FOREST) maxHeight = 90;
    else maxHeight = 85;

    int height = minHeight + (int)(heightNoise * (float)(maxHeight - minHeight));

    int idx = x + z * 16 + y * 256;
    blocks[idx] = getSurface(biome, y, height);

    if (y == 0) {
        heightmap[x + z * 16] = height;
        biomemap[x + z * 16]  = biome;
    }
}
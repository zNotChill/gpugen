import org.lwjgl.BufferUtils
import org.lwjgl.opencl.CL10
import org.lwjgl.opencl.CL21
import org.lwjgl.opencl.CLContextCallback
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.IntBuffer
import java.util.concurrent.ArrayBlockingQueue

//todo: document this filth
class GpuTerrainContext : AutoCloseable {
    private var context: Long = 0
    private var program: Long = 0
    private var kernel: Long = 0

    private val queuePool = ArrayBlockingQueue<Long>(8)

    companion object {
        const val CHUNK_VOLUME = 16 * 16 * 384
        const val POOL_SIZE = 8
    }

    fun build(src: String) {
        val err = BufferUtils.createIntBuffer(1)

        val platformCount = BufferUtils.createIntBuffer(1)
        CL10.clGetPlatformIDs(null, platformCount)
        val platforms = BufferUtils.createPointerBuffer(platformCount[0])
        CL10.clGetPlatformIDs(platforms, null as IntBuffer?)
        val platform = platforms[0]

        val deviceCount = BufferUtils.createIntBuffer(1)
        CL10.clGetDeviceIDs(platform, CL10.CL_DEVICE_TYPE_GPU.toLong(), null, deviceCount)
        val devices = BufferUtils.createPointerBuffer(deviceCount[0])
        CL10.clGetDeviceIDs(platform, CL10.CL_DEVICE_TYPE_GPU.toLong(), devices, null as IntBuffer?)
        val device = devices[0]

        val ctxProps = BufferUtils.createPointerBuffer(3)
        ctxProps.put(0, CL10.CL_CONTEXT_PLATFORM.toLong()).put(1, platform).put(2, 0).rewind()

        val cb = CLContextCallback.create { errinfo, _, _, _ ->
            System.err.println("CL context error: $errinfo")
        }

        context = CL10.clCreateContext(ctxProps, device, cb, MemoryUtil.NULL, err)
        check(err[0] == 0) { "clCreateContext failed: ${err[0]}" }

        repeat(POOL_SIZE) {
            val q = CL10.clCreateCommandQueue(context, device, 0L, err)
            check(err[0] == 0) { "clCreateCommandQueue failed: ${err[0]}" }
            queuePool.add(q)
        }

        program = CL10.clCreateProgramWithSource(context, src, err)
        val buildResult = CL10.clBuildProgram(program, device, "", null, 0)
        if (buildResult != 0) {
            val logSize = BufferUtils.createPointerBuffer(1)
            CL10.clGetProgramBuildInfo(program, device, CL10.CL_PROGRAM_BUILD_LOG, null as IntBuffer?, logSize)
            val log = BufferUtils.createByteBuffer(logSize[0].toInt())
            CL10.clGetProgramBuildInfo(program, device, CL10.CL_PROGRAM_BUILD_LOG, log, null)
            val bytes = ByteArray(log.remaining()).also { log.get(it) }
            error("Kernel build failed:\n${String(bytes)}")
        }

        kernel = CL10.clCreateKernel(program, "generateChunk", err)
        check(err[0] == 0) { "clCreateKernel failed: ${err[0]}" }
    }


    fun generate(chunkX: Int, chunkZ: Int): ChunkData {
        val err = BufferUtils.createIntBuffer(1)
        val localKernel = CL21.clCloneKernel(kernel, err)
        check(err[0] == 0) { "clCloneKernel failed: ${err[0]}" }

        val queue = if (queuePool.isEmpty()) {
            println("queue pool exhausted")
            queuePool.take()
        } else {
            queuePool.take()
        }

        val blocksBuf = MemoryUtil.memAllocInt(CHUNK_VOLUME)
        val heightmapBuf = MemoryUtil.memAllocInt(16 * 16)
        val biomemapBuf = MemoryUtil.memAllocInt(16 * 16)

        val blocksMem =
            CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY.toLong(), CHUNK_VOLUME.toLong() * Int.SIZE_BYTES, err)
        val heightmapMem =
            CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY.toLong(), (16 * 16).toLong() * Int.SIZE_BYTES, err)
        val biomemapMem =
            CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY.toLong(), (16 * 16).toLong() * Int.SIZE_BYTES, err)

        check(blocksMem != 0L) { "clCreateBuffer blocks failed: ${err[0]}" }
        check(heightmapMem != 0L) { "clCreateBuffer heightmap failed: ${err[0]}" }
        check(biomemapMem != 0L) { "clCreateBuffer biomemap failed: ${err[0]}" }

        try {
            CL10.clSetKernelArg1p(localKernel, 0, blocksMem)
            CL10.clSetKernelArg1p(localKernel, 1, heightmapMem)
            CL10.clSetKernelArg1p(localKernel, 2, biomemapMem)
            CL10.clSetKernelArg1i(localKernel, 3, chunkX)
            CL10.clSetKernelArg1i(localKernel, 4, chunkZ)
            CL10.clSetKernelArg1f(localKernel, 5, 0.015f)

            MemoryStack.stackPush().use { stack ->
                val global = stack.pointers(16, 16, 384)
                CL10.clEnqueueNDRangeKernel(queue, localKernel, 3, null, global, null, null, null)
            }

            CL10.clEnqueueReadBuffer(queue, blocksMem, true, 0, blocksBuf, null, null)
            CL10.clEnqueueReadBuffer(queue, heightmapMem, true, 0, heightmapBuf, null, null)
            CL10.clEnqueueReadBuffer(queue, biomemapMem, true, 0, biomemapBuf, null, null)

            CL10.clFinish(queue)

            val blocks = IntArray(CHUNK_VOLUME).also {
                blocksBuf.get(it)
                blocksBuf.rewind()
            }
            val heightmap = IntArray(16 * 16).also {
                heightmapBuf.get(it)
                heightmapBuf.rewind()
            }
            val biomemap = IntArray(16 * 16).also {
                biomemapBuf.get(it)
                biomemapBuf.rewind()
            }

            return ChunkData(blocks, heightmap, biomemap)
        } finally {
            CL10.clReleaseMemObject(blocksMem)
            CL10.clReleaseMemObject(heightmapMem)
            CL10.clReleaseMemObject(biomemapMem)
            CL10.clReleaseKernel(localKernel)

            // free native memory
            MemoryUtil.memFree(blocksBuf)
            MemoryUtil.memFree(heightmapBuf)
            MemoryUtil.memFree(biomemapBuf)
            queuePool.put(queue)
        }
    }

    override fun close() {
        queuePool.forEach {
            CL10.clReleaseCommandQueue(it)
        }
        CL10.clReleaseKernel(kernel)
        CL10.clReleaseProgram(program)
        CL10.clReleaseContext(context)
    }
}
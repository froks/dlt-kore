package library

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.fileSize
import kotlin.math.min

private const val BUFFER_SIZE = 100_000_000
private const val OVERLAP = 10_000_000

public class LargeFileWindowedByteBufferInputStream(path: Path) : BinaryInputStream {
    private lateinit var currentInputStream: ByteBufferBinaryInputStream
    private val fileSize = path.fileSize()
    private var fileChannel: FileChannel = FileChannel.open(path, StandardOpenOption.READ)
    private var absolutePosition = -1L
    private var bufferIndex = 0

    private val buffer: BinaryInputStream
        get() {
            if (absolutePosition == -1L) {
                absolutePosition = 0
                val buffer = ByteBuffer.allocate(min(BUFFER_SIZE.toLong(), fileSize).toInt())
                fileChannel.read(buffer, absolutePosition)
                buffer.position(0)
                currentInputStream = ByteBufferBinaryInputStream(buffer)
                bufferIndex = 0
                return currentInputStream
            }
            val relativePosition = currentInputStream.position()
            if (relativePosition >= (BUFFER_SIZE - OVERLAP)) {
                absolutePosition += relativePosition
                val buffer = ByteBuffer.allocate(min(BUFFER_SIZE.toLong(), fileSize - absolutePosition).toInt())
                fileChannel.read(buffer, absolutePosition)
                buffer.position(0)
                currentInputStream.close()
                currentInputStream = ByteBufferBinaryInputStream(buffer)
            }
            return currentInputStream
        }

    override fun order(order: ByteOrder) {
        buffer.order(order)
    }

    override fun hasRemaining(): Boolean {
        val remaining = buffer.hasRemaining()
        if (!remaining) {
            fileChannel.close()
        }
        return remaining
    }

    override fun position(): Long =
        absolutePosition + currentInputStream.position()

    override fun readByte(): Byte =
        buffer.readByte()

    override fun readShort(): Short =
        buffer.readShort()

    override fun readInt(): Int =
        buffer.readInt()

    override fun readLong(): Long =
        buffer.readLong()

    override fun readArray(len: Int): ByteArray =
        buffer.readArray(len)

    override fun close() {
        buffer.close()
        fileChannel.close()
    }
}

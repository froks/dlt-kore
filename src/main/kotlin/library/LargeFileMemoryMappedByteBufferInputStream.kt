package library

import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.fileSize
import kotlin.math.min

private const val OVERLAP = 10_000_000

public class LargeFileMemoryMappedByteBufferInputStream(path: Path) : BinaryInputStream {
    private lateinit var currentInputStream: ByteBufferBinaryInputStream

    private val fileSize = path.fileSize()
    private var fileChannel: FileChannel = FileChannel.open(path, StandardOpenOption.READ)
    private var absolutePosition = -1L
    private var bufferIndex = 0
    private var markedPosition = -1L

    private val buffer: BinaryInputStream
        get() {
            if (absolutePosition == -1L) {
                absolutePosition = 0
                val buffer = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    absolutePosition,
                    min(fileSize, Integer.MAX_VALUE.toLong())
                )
                currentInputStream = ByteBufferBinaryInputStream(buffer)
                bufferIndex = 0
                return currentInputStream
            }
            val relativePosition = currentInputStream.position()
            if (relativePosition >= (Integer.MAX_VALUE - OVERLAP)) {
                absolutePosition += relativePosition
                val buffer = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    absolutePosition,
                    min(fileSize - absolutePosition, Integer.MAX_VALUE.toLong())
                )
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

    override fun mark() {
        markedPosition = absolutePosition + currentInputStream.position()
        buffer.mark()
    }

    override fun reset() {
        if (markedPosition == -1L) {
            return
        }
        if (markedPosition < absolutePosition) {
            // If we've read another chunk between the last mark and reset, remap a chunk starting
            // from the marked position. This should almost never happen, since reset's only get called
            // when a message parsing fails
            absolutePosition = markedPosition
            val buffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                absolutePosition,
                min(fileSize - absolutePosition, Integer.MAX_VALUE.toLong())
            )
            currentInputStream = ByteBufferBinaryInputStream(buffer)
        } else {
            buffer.reset()
        }
        markedPosition = -1
    }

    override fun close() {
        markedPosition = -1
        buffer.close()
        fileChannel.close()
    }
}

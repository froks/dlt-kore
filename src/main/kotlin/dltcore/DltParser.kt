package dltcore

import library.BinaryInputStream
import library.ByteOrder
import library.LargeFileByteBufferInputStream
import library.ParseException
import java.nio.file.Path
import kotlin.io.path.fileSize

public data class DltReadStatus(
    val index: Long,
    val filePosition: Long?,
    val fileSize: Long?,
    val progress: Float?,
    val progressText: String?,
    val errorCount: Long,
    val successCount: Long,
    val dltMessage: DltMessage?,
    val error: Exception?,
)

private class DltMessageIterator(
    private val buffer: BinaryInputStream,
    private val totalSize: Long?
) : Iterator<DltReadStatus> {
    private var index: Long = 0
    private var successCount: Long = 0
    private var errorCount: Long = 0

    private fun findFirstValidMagic(): Int {
        buffer.order(ByteOrder.BIG_ENDIAN)
        var magic = 0
        while (!DltStorageVersion.isValidMagic(magic, ByteOrder.BIG_ENDIAN)) {
            magic = magic shl 8
            magic = magic or (buffer.readByte().toInt() and 0xFF)
        }
        return magic
    }

    private fun parseDltMessage(buffer: BinaryInputStream, version: DltStorageVersion): DltMessage =
        when (version) {
            DltStorageVersion.V1 -> DltMessageV1.read(buffer)
            DltStorageVersion.V2 -> throw UnsupportedOperationException("not supported yet")
        }

    override fun hasNext(): Boolean =
        buffer.hasRemaining()

    override fun next(): DltReadStatus {
        buffer.order(ByteOrder.BIG_ENDIAN)
        if (buffer.hasRemaining()) {
            val result = try {
                val magic = findFirstValidMagic()
                val version = DltStorageVersion.getByMagic(magic)
                val msg = parseDltMessage(buffer, version)
                successCount++
                msg
            } catch (e: Exception) {
                errorCount++

                ParseException(
                    buffer.position(),
                    "Error while parsing message at file position ${buffer.position()}: ${e.message}",
                    e
                )
            }
            val progress = if (totalSize != null) {
                buffer.position().toFloat() / totalSize.toFloat()
            } else {
                null
            }
            return DltReadStatus(
                index = index++,
                filePosition = buffer.position(),
                fileSize = totalSize,
                progress = progress,
                progressText = "Parsing file",
                errorCount = errorCount,
                successCount = successCount,
                dltMessage = result as? DltMessage,
                error = result as? Exception,
            )
        }
        throw IllegalStateException("No more data available, but next() was called on iterator")
    }
}

public class DltMessageParser private constructor() {

    public companion object {
        public fun parseBuffer(buffer: BinaryInputStream, totalSize: Long?): Sequence<DltReadStatus> =
            DltMessageIterator(buffer, totalSize).asSequence()
        public fun parseFile(path: Path): Sequence<DltReadStatus> {
            val bis = LargeFileByteBufferInputStream(path)
            return parseBuffer(bis, path.fileSize())
        }

    }
}

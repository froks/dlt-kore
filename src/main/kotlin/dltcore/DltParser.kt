package dltcore

import library.BinaryInputStream
import library.ByteOrder
import library.LargeFileByteBufferInputStream
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
)

private class DltMessageIterator(val buffer: BinaryInputStream, val totalSize: Long?) : Iterator<DltReadStatus> {
    private var index: Long = 0
    private var successCount: Long = 0
    private var errorCount: Long = 0

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
            val message = try {
                val magic = buffer.readInt()
                val version = DltStorageVersion.getByMagic(magic)
                parseDltMessage(buffer, version)
            } catch (e: RuntimeException) {
                errorCount++
                throw RuntimeException(
                    "Error while parsing message at file position ${buffer.position()}: ${e.message}",
                    e
                )
            }
            successCount++
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
                dltMessage = message
            )
        }
        throw RuntimeException("No more data available, but next() was called on iterator")
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

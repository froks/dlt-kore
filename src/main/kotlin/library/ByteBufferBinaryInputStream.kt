package library

import java.nio.ByteBuffer

public class ByteBufferBinaryInputStream(private val buffer: ByteBuffer) : BinaryInputStream {
    override fun order(order: ByteOrder) {
        buffer.order(order.asByteBufferByteOrder())
    }

    override fun hasRemaining(): Boolean =
        buffer.hasRemaining()

    override fun position(): Long =
        buffer.position().toLong()

    override fun readByte(): Byte =
        buffer.get()

    override fun readShort(): Short =
        buffer.getShort()

    override fun readInt(): Int =
        buffer.getInt()

    override fun readLong(): Long =
        buffer.getLong()

    override fun readArray(len: Int): ByteArray {
        val data = ByteArray(len)
        buffer.get(data)
        return data
    }

    override fun close() {
    }
}

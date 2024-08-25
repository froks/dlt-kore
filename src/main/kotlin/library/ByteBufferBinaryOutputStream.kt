package library

import java.nio.ByteBuffer

public class ByteBufferBinaryOutputStream(private val buffer: ByteBuffer) : BinaryOutputStream {
    private var position: Long = 0

    override fun order(order: ByteOrder) {
        buffer.order(order.asByteBufferByteOrder())
    }

    override fun writeByte(value: Byte) {
        buffer.put(value)
        position += 1
    }

    override fun writeShort(value: Short) {
        buffer.putShort(value)
        position += 2
    }

    override fun writeInt(value: Int) {
        buffer.putInt(value)
        position += 4
    }

    override fun writeLong(value: Long) {
        buffer.putLong(value)
        position += 8
    }

    override fun writeArray(data: ByteArray) {
        buffer.put(data)
        position += data.size
    }

    override fun position(): Long =
        position
}

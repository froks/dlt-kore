package library

import java.nio.ByteBuffer

public class ByteBufferBinaryOutputStream(private val buffer: ByteBuffer) : BinaryOutputStream {
    override fun order(order: ByteOrder) {
        buffer.order(order.asByteBufferByteOrder())
    }

    override fun put(data: ByteArray) {
        buffer.put(data)
    }

    override fun writeByte(value: Byte) {
        buffer.put(value)
    }

    override fun writeShort(value: Short) {
        buffer.putShort(value)
    }

    override fun writeInt(value: Int) {
        buffer.putInt(value)
    }

    override fun writeLong(value: Long) {
        buffer.putLong(value)
    }
}

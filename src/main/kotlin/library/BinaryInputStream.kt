package library

import java.nio.ByteBuffer

public interface BinaryInputStream : AutoCloseable {
    public fun order(order: ByteOrder)
    public fun hasRemaining(): Boolean
    public fun position(): Long

    public fun readByte(): Byte
    public fun readShort(): Short
    public fun readInt(): Int
    public fun readLong(): Long

    public fun readArray(len: Int): ByteArray

    public fun mark()
    public fun reset()

    public companion object {
        public fun wrap(array: ByteArray): BinaryInputStream =
            ByteBufferBinaryInputStream(ByteBuffer.wrap(array))
    }
}

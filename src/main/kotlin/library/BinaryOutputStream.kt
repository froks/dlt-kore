package library

public interface BinaryOutputStream {
    public fun order(order: ByteOrder)

    public fun writeByte(value: Byte)
    public fun writeShort(value: Short)
    public fun writeInt(value: Int)
    public fun writeLong(value: Long)
    public fun writeArray(data: ByteArray)
    public fun position(): Long
}

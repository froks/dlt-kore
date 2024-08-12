package library

public interface BinaryOutputStream {
    public fun order(order: ByteOrder)
    public fun put(data: ByteArray)

    public fun writeByte(value: Byte)
    public fun writeShort(value: Short)
    public fun writeInt(value: Int)
    public fun writeLong(value: Long)
}

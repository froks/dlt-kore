package library

public fun ByteOrder.asByteBufferByteOrder(): java.nio.ByteOrder = when (this) {
    ByteOrder.LITTLE_ENDIAN -> java.nio.ByteOrder.LITTLE_ENDIAN
    ByteOrder.BIG_ENDIAN -> java.nio.ByteOrder.BIG_ENDIAN
}

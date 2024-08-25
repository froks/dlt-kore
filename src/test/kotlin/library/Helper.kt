package library

import java.nio.ByteBuffer

fun createSequencedByteBuffer(length: Int): ByteBuffer =
    ByteBuffer.wrap(createSequencedByteArray(length))

fun createSequencedByteArray(length: Int, valueOffset: Byte = 0): ByteArray {
    val array = ByteArray(length)
    for (i in 0 until length) {
        array[i] = (valueOffset + i.toByte()).toByte()
    }
    return array
}

fun String.decodeHex(): ByteArray {
    val s = this.replace(" ", "")
    check(s.length % 2 == 0) { "String must have an even length" }
    return s.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

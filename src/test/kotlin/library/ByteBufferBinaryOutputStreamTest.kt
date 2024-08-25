package library

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import java.nio.ByteBuffer

class ByteBufferBinaryOutputStreamTest {
    @Test
    fun `test big endian`() {
        val bb = ByteBuffer.allocate(10000)
        val length = ByteBufferBinaryOutputStream(bb).let {
            it.order(ByteOrder.BIG_ENDIAN)
            it.writeByte(0x00)
            it.writeShort(0x0001)
            it.writeInt(0x00010203)
            it.writeLong(0x0001020304050607)
            assertThat(it.position()).isEqualTo(1+2+4+8)
            it.writeArray(createSequencedByteArray(32))
            assertThat(it.position()).isEqualTo(1+2+4+8+32)
            it.position().toInt()
        }
        val data = ByteArray(length)
        bb.position(0)
        bb.get(data, 0, data.size)
        assertThat(data).isEqualTo("000001000102030001020304050607".decodeHex() + createSequencedByteArray(32))
    }

    @Test
    fun `test little endian`() {
        val bb = ByteBuffer.allocate(10000)
        val length = ByteBufferBinaryOutputStream(bb).let {
            it.order(ByteOrder.LITTLE_ENDIAN)
            it.writeByte(0x00)
            it.writeShort(0x0001)
            it.writeInt(0x00010203)
            it.writeLong(0x0001020304050607)
            assertThat(it.position()).isEqualTo(1+2+4+8)
            it.writeArray(createSequencedByteArray(32))
            assertThat(it.position()).isEqualTo(1+2+4+8+32)
            it.position().toInt()
        }
        val data = ByteArray(length)
        bb.position(0)
        bb.get(data, 0, data.size)
        assertThat(data).isEqualTo("000100030201000706050403020100".decodeHex() + createSequencedByteArray(32))
    }
}
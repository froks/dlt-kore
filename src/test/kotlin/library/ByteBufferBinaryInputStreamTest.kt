package library

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test
import java.nio.BufferUnderflowException

class ByteBufferBinaryInputStreamTest {
    @Test
    fun `test byte`() {
        ByteBufferBinaryInputStream(createSequencedByteBuffer(4)).let {
            it.order(ByteOrder.BIG_ENDIAN)
            assertThat(it.hasRemaining()).isTrue()
            assertThat(it.position()).isEqualTo(0)
            assertThat(it.readByte()).isEqualTo(0x00)
            assertThat(it.readByte()).isEqualTo(0x01)
            assertThat(it.readByte()).isEqualTo(0x02)
            assertThat(it.readByte()).isEqualTo(0x03)
            assertThat(it.hasRemaining()).isFalse()
            assertFailure {
                it.readByte()
            }.hasClass(BufferUnderflowException::class)
        }

        ByteBufferBinaryInputStream(createSequencedByteBuffer(4)).let {
            it.order(ByteOrder.LITTLE_ENDIAN)
            assertThat(it.hasRemaining()).isTrue()
            assertThat(it.position()).isEqualTo(0)
            assertThat(it.readByte()).isEqualTo(0x00)
            assertThat(it.readByte()).isEqualTo(0x01)
            assertThat(it.readByte()).isEqualTo(0x02)
            assertThat(it.readByte()).isEqualTo(0x03)
            assertThat(it.hasRemaining()).isFalse()
            assertFailure {
                it.readByte()
            }.hasClass(BufferUnderflowException::class)
        }
    }

    @Test
    fun `test short`() {
        ByteBufferBinaryInputStream(createSequencedByteBuffer(4)).let {
            it.order(ByteOrder.BIG_ENDIAN)
            assertThat(it.hasRemaining()).isTrue()
            assertThat(it.position()).isEqualTo(0)
            assertThat(it.readShort()).isEqualTo(0x0001)
            assertThat(it.hasRemaining()).isTrue()
            assertThat(it.position()).isEqualTo(2)
            assertThat(it.readShort()).isEqualTo(0x0203)
            assertThat(it.hasRemaining()).isFalse()
            assertFailure {
                it.readShort()
            }.hasClass(BufferUnderflowException::class)
        }

        ByteBufferBinaryInputStream(createSequencedByteBuffer(4)).let {
            it.order(ByteOrder.LITTLE_ENDIAN)
            assertThat(it.hasRemaining()).isTrue()
            assertThat(it.position()).isEqualTo(0)
            assertThat(it.readShort()).isEqualTo(0x0100)
            assertThat(it.hasRemaining()).isTrue()
            assertThat(it.position()).isEqualTo(2)
            assertThat(it.readShort()).isEqualTo(0x0302)
            assertThat(it.hasRemaining()).isFalse()
            assertFailure {
                it.readShort()
            }.hasClass(BufferUnderflowException::class)
        }
    }

    @Test
    fun `test int`() {
        ByteBufferBinaryInputStream(createSequencedByteBuffer(8)).let {
            it.order(ByteOrder.BIG_ENDIAN)
            assertThat(it.hasRemaining()).isTrue()
            assertThat(it.position()).isEqualTo(0)
            assertThat(it.readInt()).isEqualTo(0x00010203)
            assertThat(it.hasRemaining()).isTrue()
            assertThat(it.position()).isEqualTo(4)
            assertThat(it.readInt()).isEqualTo(0x04050607)
            assertThat(it.hasRemaining()).isFalse()
            assertFailure {
                it.readInt()
            }.hasClass(BufferUnderflowException::class)
        }

        ByteBufferBinaryInputStream(createSequencedByteBuffer(8)).let {
            it.order(ByteOrder.LITTLE_ENDIAN)
            assertThat(it.hasRemaining()).isTrue()
            assertThat(it.position()).isEqualTo(0)
            assertThat(it.readInt()).isEqualTo(0x03020100)
            assertThat(it.hasRemaining()).isTrue()
            assertThat(it.position()).isEqualTo(4)
            assertThat(it.readInt()).isEqualTo(0x07060504)
            assertThat(it.hasRemaining()).isFalse()
            assertFailure {
                it.readInt()
            }.hasClass(BufferUnderflowException::class)
        }
    }

    @Test
    fun `test long`() {
        ByteBufferBinaryInputStream(createSequencedByteBuffer(16)).let {
            it.order(ByteOrder.BIG_ENDIAN)
            assertThat(it.hasRemaining()).isTrue()
            assertThat(it.position()).isEqualTo(0)
            assertThat(it.readLong()).isEqualTo(0x0001020304050607)
            assertThat(it.hasRemaining()).isTrue()
            assertThat(it.position()).isEqualTo(8)
            assertThat(it.readLong()).isEqualTo(0x08090a0b0c0d0e0f)
            assertThat(it.hasRemaining()).isFalse()
            assertFailure {
                it.readLong()
            }.hasClass(BufferUnderflowException::class)
        }

        ByteBufferBinaryInputStream(createSequencedByteBuffer(16)).let {
            it.order(ByteOrder.LITTLE_ENDIAN)
            assertThat(it.hasRemaining()).isTrue()
            assertThat(it.position()).isEqualTo(0)
            assertThat(it.readLong()).isEqualTo(0x0706050403020100)
            assertThat(it.hasRemaining()).isTrue()
            assertThat(it.position()).isEqualTo(8)
            assertThat(it.readLong()).isEqualTo(0x0f0e0d0c0b0a0908)
            assertThat(it.hasRemaining()).isFalse()
            assertFailure {
                it.readLong()
            }.hasClass(BufferUnderflowException::class)
        }
    }

    @Test
    fun `test array`() {
        ByteBufferBinaryInputStream(createSequencedByteBuffer(64)).let {
            assertThat(it.readArray(32)).isEqualTo(createSequencedByteArray(32))
            assertThat(it.position()).isEqualTo(32)
            assertThat(it.readArray(16)).isEqualTo(createSequencedByteArray(16, 32))
            assertThat(it.position()).isEqualTo(48)
            assertThat(it.readArray(16)).isEqualTo(createSequencedByteArray(16, 48))
            assertThat(it.position()).isEqualTo(64)
            assertFailure {
                it.readArray(1)
            }.hasClass(BufferUnderflowException::class)
        }
    }
}

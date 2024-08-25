package dltcore

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class DltExtensionsTest {
    @Test
    fun `test int to string`() {
        assertThat(0x30313233.asStringValue()).isEqualTo("0123")
        assertThat(0x30313200.asStringValue()).isEqualTo("012")
        assertThat(0x30310000.asStringValue()).isEqualTo("01")
        assertThat(0x30000000.asStringValue()).isEqualTo("0")
        assertThat(0x00000000.asStringValue()).isEqualTo("")
    }

    @Test
    fun `test string to int`() {
        assertThat("1234".asIntValue()).isEqualTo(0x31323334)
        assertThat("123".asIntValue()).isEqualTo(0x31323300)
        assertThat("12".asIntValue()).isEqualTo(0x31320000)
        assertThat("1".asIntValue()).isEqualTo(0x31000000)
    }
}
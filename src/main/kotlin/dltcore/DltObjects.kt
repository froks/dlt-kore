package dltcore

import library.BinaryInputStream
import library.BinaryOutputStream
import library.ByteOrder
import java.nio.charset.Charset
import java.time.Instant
import java.util.logging.Logger
import kotlin.experimental.and
import kotlin.time.Duration.Companion.microseconds

private val logger = Logger.getLogger("Parsing")
private var strictMode = false

public enum class DltStorageVersion(public val magicValue: Int) {
    V1(0x444c5401),  // "DLT+0x01"
    V2(0x444c5402); // "DLT+0x02"

    private val littleEndian = Integer.reverseBytes(magicValue)

    public companion object {
        public fun getByMagic(value: Int): DltStorageVersion =
            DltStorageVersion.entries.firstOrNull { it.magicValue == value }
                ?: throw IllegalArgumentException("Unknown dlt-header magic ${value.toString(16)}")

        public fun isValidMagic(value: Int, endian: ByteOrder): Boolean =
            when (endian) {
                ByteOrder.BIG_ENDIAN -> DltStorageVersion.entries.any { it.magicValue == value  }
                ByteOrder.LITTLE_ENDIAN -> DltStorageVersion.entries.any { it.littleEndian == value }
            }
    }
}

public interface DltStorageHeader {
    public val utcTimestamp: Instant
    public val ecuIdText: String
}

public interface DltStandardHeader {

}

public interface DltExtendedHeader {
    public val apIdText: String
    public val ctIdText: String
}

public interface DltPayload {
    public val logMessage: String
}

public interface DltMessage {
    public fun getVersion(): DltStorageVersion
    public fun write(bb: BinaryOutputStream)
    public val storageHeader: DltStorageHeader
    public val standardHeader: DltStandardHeader?
    public val extendedHeader: DltExtendedHeader?
    public val payload: DltPayload
}

public class DltMessageV1(
    public override val storageHeader: DltStorageHeaderV1,
    public override val standardHeader: DltStandardHeaderV1,
    public override val extendedHeader: DltExtendedHeaderV1?,
    public override val payload: DltPayloadV1,
) : DltMessage {
    override fun getVersion(): DltStorageVersion =
        DltStorageVersion.V1

    public val messageTypeInfo: MessageTypeInfo?
        get() =
            extendedHeader?.messageTypeInfo

    override fun write(bb: BinaryOutputStream) {
        storageHeader.write(bb)
        standardHeader.write(bb)
        extendedHeader?.write(bb)
        payload.write(bb)
    }

    public companion object {
        public fun read(bis: BinaryInputStream): DltMessageV1 {
            val storageHeader = DltStorageHeaderV1.read(bis)
            val standardHeader = DltStandardHeaderV1.read(bis)
            val extendedHeader =
                if (standardHeader.useExtendedHeader)
                    DltExtendedHeaderV1.read(bis)
                else
                    null
            val payloadLength =
                standardHeader.len.toInt() - standardHeader.totalLength - (extendedHeader?.totalLength ?: 0)
            val payload = DltPayloadV1.read(
                bis,
                payloadLength,
                standardHeader.mostSignificantByteFirst,
                extendedHeader
            )
            return DltMessageV1(storageHeader, standardHeader, extendedHeader, payload)
        }
    }
}

public class DltStorageHeaderV1(
    public val timestampEpochSeconds: Long, // 4 unsigned byte in V1
    public val timestampMicroseconds: Int, // microseconds in V1, Nanoseconds in V2
    public val ecuId: IdInt, // 4 chars
) : DltStorageHeader {
    public override val ecuIdText: String
        get() = ecuId.asStringValue()

    public fun write(bb: BinaryOutputStream) {
        bb.order(ByteOrder.BIG_ENDIAN)
        bb.writeInt(DltStorageVersion.V1.magicValue)

        bb.order(ByteOrder.LITTLE_ENDIAN)
        bb.writeInt((timestampEpochSeconds and 0xFFFFFFFF).toInt())
        bb.writeInt(timestampMicroseconds)
        bb.writeInt(ecuId)
    }

    public override val utcTimestamp: Instant by lazy {
        Instant.ofEpochSecond(timestampEpochSeconds, timestampMicroseconds.microseconds.inWholeNanoseconds)
    }

    public companion object {
        public fun read(buffer: BinaryInputStream): DltStorageHeaderV1 {
            // why is this little endian, when all other headers are big?
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            val timestampEpochSeconds = buffer.readInt().toUInt().toLong()
            val timestampMicroseconds = buffer.readInt()
            val ecuId = buffer.readInt()
            return DltStorageHeaderV1(timestampEpochSeconds, timestampMicroseconds, ecuId)
        }
    }
}

public class DltStandardHeaderV1(
    public val htyp: Byte, // header type
    public val mcnt: UByte, // message counter
    public val len: UShort,
    public val ecuId: Int?,
    public val sessionId: Int?,
    public val timestamp: UInt?,
) : DltStandardHeader {
    public fun write(bb: BinaryOutputStream) {
        // The Standard Header and the Extended Header shall be in big endian format (MSB first).
        bb.order(ByteOrder.BIG_ENDIAN)
        bb.writeByte(htyp)
        bb.writeByte(mcnt.toByte())
        bb.writeShort(len.toShort())
        if (ecuId != null) {
            bb.writeInt(ecuId)
        }
        if (sessionId != null) {
            bb.writeInt(sessionId)
        }
        if (timestamp != null) {
            bb.writeInt(timestamp.toInt())
        }
    }

    public inline val useExtendedHeader: Boolean
        get() =
            htyp.and(HEADER_TYPE_UEH) > 0

    public inline val mostSignificantByteFirst: Boolean
        get() =
            htyp.and(HEADER_TYPE_MSBF) > 0

    public val totalLength: Int
        get() {
            var length = 4
            if (ecuId != null) {
                length += 4
            }
            if (sessionId != null) {
                length += 4
            }
            if (timestamp != null) {
                length += 4
            }
            return length
        }

    public val version: Byte
        get() =
            (htyp.toUByte().toInt() shr 5).toByte()

    public companion object {
        public const val HEADER_TYPE_UEH: Byte = (1 shl 0).toByte() // use extended header
        public const val HEADER_TYPE_MSBF: Byte = (1 shl 1).toByte() // most significant byte first
        public const val HEADER_TYPE_WEID: Byte = (1 shl 2).toByte() // with ECU-ID
        public const val HEADER_TYPE_WSID: Byte = (1 shl 3).toByte() // with Session ID
        public const val HEADER_TYPE_WTMS: Byte = (1 shl 4).toByte() // with timestamp

        public fun read(buffer: BinaryInputStream): DltStandardHeaderV1 {
            // The Standard Header and the Extended Header shall be in big endian format (MSB first).
            buffer.order(ByteOrder.BIG_ENDIAN)

            val htyp = buffer.readByte()
            val mcnt = buffer.readByte().toUByte()
            val len = buffer.readShort().toUShort()

            val ecuId = if (htyp.and(HEADER_TYPE_WEID) > 0) buffer.readInt() else null
            val sid = if (htyp.and(HEADER_TYPE_WSID) > 0) buffer.readInt() else null
            val timestamp = if (htyp.and(HEADER_TYPE_WTMS) > 0) buffer.readInt().toUInt() else null

            return DltStandardHeaderV1(htyp, mcnt, len, ecuId, sid, timestamp)
        }
    }
}

public enum class MessageType(public val value: Int) {
    DLT_TYPE_LOG(0x0),
    DLT_TYPE_APP_TRACE(0x1),
    DLT_TYPE_NW_TRACE(0x2),
    DLT_TYPE_CONTROL(0x3);

    public companion object {
        public fun getByValue(mstp: Int): MessageType =
            MessageType.entries.first { it.value == mstp }
    }
}

public enum class MessageTypeInfo(public val type: MessageType, public val value: Int) {
    DLT_LOG_FATAL(MessageType.DLT_TYPE_LOG, 0x1),
    DLT_LOG_ERROR(MessageType.DLT_TYPE_LOG, 0x2),
    DLT_LOG_WARN(MessageType.DLT_TYPE_LOG, 0x3),
    DLT_LOG_INFO(MessageType.DLT_TYPE_LOG, 0x4),
    DLT_LOG_DEBUG(MessageType.DLT_TYPE_LOG, 0x5),
    DLT_LOG_VERBOSE(MessageType.DLT_TYPE_LOG, 0x6),

    DLT_TRACE_VARIABLE(MessageType.DLT_TYPE_APP_TRACE, 0x1),
    DLT_TRACE_FUNCTION_IN(MessageType.DLT_TYPE_APP_TRACE, 0x2),
    DLT_TRACE_FUNCTION_OUT(MessageType.DLT_TYPE_APP_TRACE, 0x3),
    DLT_TRACE_STATE(MessageType.DLT_TYPE_APP_TRACE, 0x4),
    DLT_TRACE_VFB(MessageType.DLT_TYPE_APP_TRACE, 0x5),

    DLT_NW_TRACE_IPC(MessageType.DLT_TYPE_NW_TRACE, 0x1),
    DLT_NW_TRACE_CAN(MessageType.DLT_TYPE_NW_TRACE, 0x2),
    DLT_NW_TRACE_FLEXRAY(MessageType.DLT_TYPE_NW_TRACE, 0x3),
    DLT_NW_TRACE_MOST(MessageType.DLT_TYPE_NW_TRACE, 0x4),
    DLT_NW_TRACE_ETHERNET(MessageType.DLT_TYPE_NW_TRACE, 0x5),
    DLT_NW_TRACE_SOMEIP(MessageType.DLT_TYPE_NW_TRACE, 0x6),

    DLT_CONTROL_REQUEST(MessageType.DLT_TYPE_CONTROL, 0x5),
    DLT_CONTROL_RESPONSE(MessageType.DLT_TYPE_CONTROL, 0x6),

    UNKNOWN(MessageType.DLT_TYPE_LOG, -1);

    public val shortText: String =
        name.replaceFirst("DLT_LOG_", "").replaceFirst("DLT_", "")

    public companion object {
        public fun getByMessageType(mstp: Int, mtin: Int): MessageTypeInfo =
            when (mstp) {
                MessageType.DLT_TYPE_LOG.value -> entries[mtin - 1]
                MessageType.DLT_TYPE_APP_TRACE.value -> entries[DLT_TRACE_VARIABLE.ordinal + mtin - 1]
                MessageType.DLT_TYPE_NW_TRACE.value -> entries[DLT_NW_TRACE_IPC.ordinal + mtin - 1]
                MessageType.DLT_TYPE_CONTROL.value -> entries[DLT_CONTROL_REQUEST.ordinal + mtin - 1]
                else -> throw IllegalArgumentException("Unknown mstp $mstp")
            }

        public fun getByShort(short: String): MessageTypeInfo? =
            entries.firstOrNull { it.shortText == short }
    }

}

public class DltExtendedHeaderV1(
    public val msin: Byte, // message info
    public val noar: Byte, // number of arguments
    public val apid: IdInt, // application id
    public val ctid: IdInt, // context id
) : DltExtendedHeader {
    public fun write(bb: BinaryOutputStream) {
        bb.order(ByteOrder.BIG_ENDIAN)

        bb.writeByte(msin)
        bb.writeByte(noar)
        bb.writeInt(apid)
        bb.writeInt(ctid)
    }

    public val isVerbose: Boolean
        get() = (msin and MESSAGEINFO_VERB) > 0

    public val messageType: MessageType
        get() = MessageType.getByValue((msin and MESSAGEINFO_MSTP).toInt() shr 1)

    public val messageTypeInfo: MessageTypeInfo
        get() = MessageTypeInfo.getByMessageType(
            (msin and MESSAGEINFO_MSTP).toInt() shr 1,
            (msin and MESSAGEINFO_MTIN).toInt() shr 4
        )

    public override val apIdText: String
        get() = this.apid.asStringValue()

    public override val ctIdText: String
        get() = this.ctid.asStringValue()

    public val totalLength: Int =
        10

    public companion object {
        public const val MESSAGEINFO_VERB: Byte = 0b0001.toByte() // verbsoe
        public const val MESSAGEINFO_MSTP: Byte = 0b1110.toByte() // message type
        public const val MESSAGEINFO_MTIN: Byte = 0xF0.toByte() // message type info

        public fun read(buffer: BinaryInputStream): DltExtendedHeaderV1 {
            // The Standard Header and the Extended Header shall be in big endian format (MSB first).
            buffer.order(ByteOrder.BIG_ENDIAN)

            val msin = buffer.readByte()
            val noar = buffer.readByte()
            val apid = buffer.readInt()
            val ctid = buffer.readInt()
            return DltExtendedHeaderV1(msin, noar, apid, ctid)
        }
    }
}

public class DltPayloadV1(
    public val data: ByteArray,
    private val mostSignificantByteFirst: Boolean,
    private val extendedHeader: DltExtendedHeaderV1?
) : DltPayload {
    private val byteOrder = if (mostSignificantByteFirst) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN

    public override val logMessage: String by lazy {
        val bb = BinaryInputStream.wrap(data)
        bb.order(byteOrder)

        if (extendedHeader?.isVerbose != true) {
            bb.readInt()
            val msg = bb.readArray(data.size - 4)
            return@lazy String(msg)
        } else {
            val sb = StringBuilder()
            var len = data.size
            for (i in 0 until extendedHeader.noar) {
                val arg = DltPayloadArgument.read(bb, byteOrder)
                len -= arg.totalLength
                if (arg.variableName != null) {
                    sb.append(arg.variableName)
                    sb.append(" = ")
                }
                sb.append(arg)
                sb.append(" ")

            }
            return@lazy sb.toString()
        }
    }

    public fun write(bb: BinaryOutputStream) {
        bb.order(byteOrder)

        bb.writeArray(data)
    }


    public companion object {
        public fun read(
            bis: BinaryInputStream,
            len: Int,
            mostSignificantByteFirst: Boolean,
            extendedHeader: DltExtendedHeaderV1?
        ): DltPayloadV1 {
            val data = bis.readArray(len)
            return DltPayloadV1(data, mostSignificantByteFirst, extendedHeader)
        }
    }
}

public enum class DltPayloadArgumentType(
    public val value: Int
) {
    BOOL(0x10),
    SINT(0x20),
    UINT(0x40),
    FLOA(0x80),
    ARAY(0x100),
    STRG(0x200),
    RAWD(0x400),
    VARI(0x800),
    FIXP(0x1000),
    TRAI(0x2000),
    STRU(0x4000),
    SCOD(0x8000);

    public companion object {
        public fun getByTypeInfo(typeInfo: Int): DltPayloadArgumentType =
            entries.first { typeInfo and it.value > 0 }
    }
}

public abstract class DltPayloadArgument(
    public val argumentType: DltPayloadArgumentType,
    public val variableName: String?
) {
    public abstract val dataSize: Int

    public val totalLength: Int
        get() = 4 + dataSize + (variableName?.toByteArray(Charsets.US_ASCII)?.size?.plus(2 + 1)
            ?: 0) // size + string + null-termination

    public companion object {
        public const val TYPEINFO_VARI: Int = 0x0800
        public const val TYPEINFO_FIXP: Int = 0x1000
        public const val TYPEINFO_MASK_TYLE: Int = 0x7
        public const val TYPEINFO_MASK_STRING_ENCODING: Int = 0x38000

        public fun getVariableName(typeInfo: Int, bb: BinaryInputStream): String? {
            if (typeInfo and TYPEINFO_VARI > 0) {
                val len = bb.readShort().toUShort().toInt()
                if (len == 0) {
                    // Weird?
                    return ""
                }
                val nameArray = bb.readArray(len)
                return String(nameArray, 0, len - 1, Charsets.US_ASCII)
            }
            return null
        }

        public fun read(bb: BinaryInputStream, byteOrder: ByteOrder): DltPayloadArgument {
            bb.order(ByteOrder.LITTLE_ENDIAN)
            val arTypeInfo = bb.readInt()
            val argType = DltPayloadArgumentType.getByTypeInfo(arTypeInfo)
            return when (argType) {
                DltPayloadArgumentType.STRG -> DltPayloadArgumentString.read(arTypeInfo, bb, byteOrder)
                DltPayloadArgumentType.RAWD -> DltPayloadArgumentRawData.read(arTypeInfo, bb, byteOrder)
                DltPayloadArgumentType.UINT -> DltPayloadArgumentNumber.read(
                    argType,
                    arTypeInfo,
                    bb,
                    byteOrder
                )

                DltPayloadArgumentType.SINT -> DltPayloadArgumentNumber.read(
                    argType,
                    arTypeInfo,
                    bb,
                    byteOrder
                )

                DltPayloadArgumentType.FLOA -> DltPayloadArgumentNumber.read(
                    argType,
                    arTypeInfo,
                    bb,
                    byteOrder
                )

                DltPayloadArgumentType.BOOL -> DltPayloadArgumentBool.read(
                    arTypeInfo,
                    bb,
                    byteOrder
                )

                else -> throw IllegalArgumentException("arg type $argType is not supported (yet)")
            }
        }
    }
}

public class DltPayloadArgumentBool(
    public val data: Boolean,
    variableName: String?,
) : DltPayloadArgument(DltPayloadArgumentType.BOOL, variableName) {
    public companion object {
        public fun read(typeInfo: Int, bb: BinaryInputStream, byteOrder: ByteOrder): DltPayloadArgumentBool {
            bb.order(byteOrder)

            val variableName = getVariableName(typeInfo, bb)
            val lenInfo = typeInfo and TYPEINFO_MASK_TYLE
            if (lenInfo != 1) {
                logger.warning("Unsupported length info $lenInfo for Bool argument")
                if (strictMode) {
                    throw UnsupportedOperationException("Unsupported length info $lenInfo for Bool argument")
                }
            }
            return DltPayloadArgumentBool(bb.readByte() != 0x0.toByte(), variableName)
        }
    }

    override val dataSize: Int
        get() = 1

    override fun toString(): String =
        data.toString()
}

public class DltPayloadArgumentString(
    public val data: String,
    public val charset: Charset,
    variableName: String?
) : DltPayloadArgument(DltPayloadArgumentType.STRG, variableName) {
    override val dataSize: Int
        get() = data.toByteArray(charset).size + 1 // null termination

    override fun toString(): String =
        data

    public companion object {
        public fun read(typeInfo: Int, bb: BinaryInputStream, byteOrder: ByteOrder): DltPayloadArgumentString {
            bb.order(byteOrder)

            val variableName = getVariableName(typeInfo, bb)
            val len = bb.readShort().toUShort().toInt()
            val data = bb.readArray(len)
            val charset = when (val encoding = (typeInfo and TYPEINFO_MASK_STRING_ENCODING) shr 15) {
                0 -> Charsets.US_ASCII
                1 -> Charsets.UTF_8
                else -> throw IllegalStateException("Unknown string encoding $encoding")
            }

            val s = String(data, 0, len - 1, charset)
            return DltPayloadArgumentString(s, charset, variableName)
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
private val DltRawHexFormat = HexFormat {
    upperCase = false
    bytes {
        byteSeparator = " "
    }
}


public class DltPayloadArgumentRawData(
    public val data: String,
    variableName: String?
) : DltPayloadArgument(DltPayloadArgumentType.STRG, variableName) {
    override val dataSize: Int
        get() = data.length

    override fun toString(): String =
        data

    public companion object {
        @OptIn(ExperimentalStdlibApi::class)
        public fun read(typeInfo: Int, bb: BinaryInputStream, byteOrder: ByteOrder): DltPayloadArgumentRawData {
            bb.order(byteOrder)

            val variableName = getVariableName(typeInfo, bb)
            val len = bb.readShort().toUShort().toInt()
            val data = bb.readArray(len)

            val s = data.toHexString(DltRawHexFormat)
            return DltPayloadArgumentRawData(s, variableName)
        }
    }
}

public class DltPayloadArgumentNumber(
    type: DltPayloadArgumentType,
    public val number: Number,
    public val isUnsigned: Boolean,
    private val len: Int,
    variableName: String?
) :
    DltPayloadArgument(type, variableName) {
    override val dataSize: Int
        get() = len

    public companion object {
        public fun read(
            type: DltPayloadArgumentType,
            typeInfo: Int,
            bb: BinaryInputStream,
            byteOrder: ByteOrder
        ): DltPayloadArgumentNumber {
            bb.order(byteOrder)

            val variableName = getVariableName(typeInfo, bb)
            val lenInfo = typeInfo and TYPEINFO_MASK_TYLE
            val lenInBytes = when (lenInfo) {
                1 -> 1
                2 -> 2
                3 -> 4
                4 -> 8
                5 -> 16
                else -> throw UnsupportedOperationException("Unsupported length info $lenInfo")
            }
            val fixedPoint = (typeInfo and TYPEINFO_FIXP) > 0
            if (fixedPoint) {
                throw UnsupportedOperationException("Fixed point for numbers not supported")
            }
            val number = when (type) {
                DltPayloadArgumentType.FLOA -> when (lenInBytes) {
                    2 -> throw UnsupportedOperationException("Half precision floats aren't supported")
                    4 -> Float.fromBits(bb.readInt())
                    8 -> Double.fromBits(bb.readLong())
                    else -> throw UnsupportedOperationException("Unsupported float len $lenInfo")
                }

                DltPayloadArgumentType.SINT -> when (lenInBytes) {
                    2 -> bb.readShort()
                    4 -> bb.readInt()
                    8 -> bb.readLong()
                    else -> throw UnsupportedOperationException("Unsupported sint len $lenInBytes")
                }

                DltPayloadArgumentType.UINT -> when (lenInBytes) {
                    2 -> bb.readShort()
                    4 -> bb.readInt()
                    8 -> bb.readLong()
                    else -> throw UnsupportedOperationException("Unsupported uint len $lenInBytes")
                }

                else -> throw UnsupportedOperationException("Unsupported number type $type")
            }
            return DltPayloadArgumentNumber(type, number, type == DltPayloadArgumentType.UINT, lenInBytes, variableName)
        }
    }

    override fun toString(): String =
        number.toString()
}

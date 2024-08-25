package library

public class ParseException(public val position: Long, msg: String, e: Exception) : RuntimeException(msg, e)

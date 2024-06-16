# dlt-core

dlt-core is a kotlin library to parse autosar dlt files.

It currently only supports dlt version 1.

Usage:
```kotlin
DltMessageParser.parseFile(Path.of("file.dlt")).forEach { 
    val msg = it.dltMessage as DltMessageV1
    if (msg.extendedHeader?.apIdText != "MYAP") {
        return@forEach
    }
    // do stuff with msg
}
```

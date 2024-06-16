[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.froks/dlt-core/badge.svg)](https://central.sonatype.com/artifact/io.github.froks/dlt-core)


# dlt-core

dlt-core is a kotlin library to parse autosar dlt files.

It currently only supports dlt version 1.

Usage:

Add `io.github.froks:dlt-core:<version>` to your dependencies.

Code:
```kotlin
DltMessageParser.parseFile(Path.of("file.dlt")).forEach { 
    val msg = it.dltMessage as DltMessageV1
    if (msg.extendedHeader?.apIdText != "MYAP") {
        return@forEach
    }
    // do stuff with msg
}
```

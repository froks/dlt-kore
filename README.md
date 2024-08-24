[![Maven Central](https://img.shields.io/maven-central/v/io.github.froks/dlt-core)](https://central.sonatype.com/artifact/io.github.froks/dlt-core)


# dlt-core

dlt-core is a kotlin library to parse autosar dlt files.

It currently only supports dlt version 1.

Usage:

Add `io.github.froks:dlt-core:<version>` to your dependencies - see [mavencentral](https://central.sonatype.com/artifact/io.github.froks/dlt-core) for snippets.

Code:
```kotlin
DltMessageParser.parseFile(Path.of("file.dlt")).forEach { 
    val msg = it.dltMessage as? DltMessageV1
    if (msg?.extendedHeader?.apIdText != "MYAP") {
        return@forEach
    }
    // do stuff with msg
}
```

---
title: Building
permalink: technical-docs/building-pharmcat
parent: Technical Documentation
nav_order: 4
---
# Building PharmCAT

## Build Requirements

### Java

The PharmCAT application is written in Java and was developed and tested with [OpenJDK 14](https://adoptium.net/index.html?variant=openjdk17&jvmVariant=hotspot). 

You will need to install [Java 14 or newer](https://adoptium.net/index.html?variant=openjdk17&jvmVariant=hotspot) to compile and run PharmCAT. 

### Gradle

We use [Gradle](https://gradle.org) to compile PharmCAT. The repo comes with gradle wrapper scripts (i.e. gradlew and 
gradlew.bat), you are not required to install it separately.


## Build Commands

To build the PharmCAT jar that contains all dependencies:

```shell
> ./gradlew shadowJar
```

This will put a new JAR file in the `build/libs` directory with the current version number in the filename.


## Development

This section is only applicable to the maintainers/contributors of this repo. If you are just looking to run PharmCAT, 
you can safely ignore this. 

### Updating data

To update the data PharmCAT relies on:

```shell
> make updateData
```

Non-developers can download the latest version of the data on the [Releases page](https://github.com/PharmGKB/PharmCAT/releases).

### Releasing

To build the PharmCAT distribution packages:

```shell
> ./gradlew assemble
```

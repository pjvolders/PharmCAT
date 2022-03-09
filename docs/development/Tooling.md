---
title: Tooling for Development
permalink: technical-docs/tooling
parent: Development
nav_order: 1
---
# Tooling for Development

Here are some tips if you'd like to contribute to the development of PharmCAT. We don't require particular tools but 
below are tips for tools we've used.


## Eclipse

After checking out the project, on the command line, run:

```commandline
> ./gradlew eclipse
```

This will generate the Eclipse project files for you.

You should now be able to open up the project in Eclipse.

Please make sure you install and use the [EditorConfig plugin](http://editorconfig.org/#download).


## IntelliJ IDEA

When you open the project in [IntelliJ IDEA](https://www.jetbrains.com/idea/), it should detect that there is an unlinked Gradle project.  Click on the option to _Import Gradle project_.

Make sure you enable the _Use auto-import_ setting.

All the other IDEA config files are already configured so that everyone should be able to start with the same baseline.

If IDEA asks you about using EditorConfig, just say no.  IDEA has better code style support and should already be configured to use the project default.

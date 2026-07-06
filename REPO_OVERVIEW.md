# Savs-Common-Economy: Contributor Overview

## What this repository is
`EM-ED/Savs-Common-Economy` is a Java-first Gradle project for shared economy functionality in the Minecraft/Fabric ecosystem. As a contributor, you will usually work in `src/` and validate changes through Gradle tasks.

## Project structure
```text
.github/           GitHub automation and repository configuration
gradle/            Gradle wrapper support files
src/               Java source code (main implementation)
build.gradle       Build logic, plugins, dependencies, and tasks
settings.gradle    Project identity and Gradle module configuration
gradle.properties  Build and version properties
README.md          User-facing features and configuration documentation
```

## Build and test
Use the Gradle wrapper from the repository root:

```bash
./gradlew tasks
./gradlew build
./gradlew test
```

## Where to start contributing
1. Read `README.md` to understand supported features and configuration.
2. Inspect `build.gradle` and `gradle.properties` to confirm build/runtime expectations.
3. Explore `src/` by feature area, then pick a small improvement (bug fix, docs sync, or focused refactor).
4. Run wrapper tasks locally before opening or updating a PR.

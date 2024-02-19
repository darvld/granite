# Build Kit

This project acts as an embedded Gradle plugin, providing utility functions and extensions used in the build. All
subprojects automatically have this plugin applied (it's applied at the root `build.gradle.kts`), so there is no need
to explicitly enable it.

Note that this plugin follows opinionated approaches to certain problems such as build configuration.

## Options

The Options API provides a better way to configure projects using Gradle properties. When an option is requested, the
specified key is prefixed with `granite.` to ensure it's scoped to the project, avoiding clashes with other Gradle
builds in the same machine that use similar property names.

Options are treated as strings by default, but can also be automatically converted to boolean values with
the `optionEnabled` function, removing the need to use explicit casts and conversion methods.

## Platform configurations

Projects using platform-specific dependencies can opt-in for additional configurations using
the `granite.platform-configurations` property. This will generate dependency configurations for every supported
platform (`linux`, `windows`, `macos`), architecture (`amd64`, `arm64`) and "
type" (`implementation`, `compileOnly`, `runtimeOnly`). These combinations can then be used to declare dependencies
scoped to a particular target, even for JVM projects (e.g. platform-specific binaries used via JNI).
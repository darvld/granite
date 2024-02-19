# Granite Intellij Plugin

A plugin for the Intellij platform, providing IDE support for the Granite Kotlin plugin.

## Building

As a general rule, this project is excluded from most builds to avoid unnecessary downloads of the sandboxed IDE used
for development. Set the `granite.build.granite-plugin-intellij.disable` property in your local `gradle.properties`
to ignore the project and exclude from the build.

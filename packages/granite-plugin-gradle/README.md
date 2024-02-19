# Granite Gradle Plugin

This project defines the Gradle support plugin for the Granite Kotlin compiler plugin. It integrates the plugin with
the Kotlin Gradle Plugin and provides a few configuration options using a DSL.

## Building

This project is included in the build pre-pass since it is used by others such as the plugin sample project. The plugin
can be published locally using the `publishToMavenLocal` task, or automatically as part of the related Make targets.

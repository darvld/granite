# Engine

This directory contains the Granite ECS engine and supporting tools. See each project's `README.md` for details on their
usage and instructions for contributors:

- [`granite-core`](./granite-core): this is the core of the engine, providing the ECS library and runtime, as well
  as annotations and other symbols used by other packages.

- [`granite-plugin-kotlin`](./granite-plugin-kotlin): a Kotlin compiler plugin, used to generate synthetic declarations
  for classes annotated with `@ComponentData`.

- [`granite-plugin-gradle`](./granite-plugin-gradle): a Gradle plugin supporting the Kotlin compiler plugin, it
  registers the Granite plugin in Kotlin builds and provides a DSL for configuration.

- [`granite-plugin-intellij`](./granite-plugin-intellij): a plugin for the Intellij platform, integrating the Granite
  compiler plugin into the IDE for a better development experience.

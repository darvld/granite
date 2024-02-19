# Granite Core

Granite is a custom ECS framework implemented in pure Kotlin/JVM, with built-in support for coroutines and parallel
processing of entity data. It is heavily inspired by [Flecs](https://github.com/SanderMertens/flecs), a fast,
lightweight ECS implemented in C, and most of the indexing and storage methods are based on a
[series of articles](https://ajmmertens.medium.com/building-an-ecs-1-where-are-my-entities-and-components-63d07c7da742)
written by the author of Flecs.

### Goals and non-goals

Granite is specifically meant to power small-scale personal game projects, not as a general-purpose engine, and as
such, some features are tailored for use in said projects. This does not mean, however, that it can't be used on you
own projects. The engine is reasonably flexible in its implementation, and tools like the compiler plugin are meant to
ease its adoption outside this repository.

These are the explicit goals of the engine:

- Provide a reasonably fast ECS implementation in pure Kotlin.
- Minimize third-party dependencies.
- Support simple use cases required by personal projects (not yet published).

The following are non-goals of the project, and will _not_ be considered for implementation:

- Competing performance with existing ECS implementations in the JVM.
- Built-in "systems" API, as provided by other JVM libraries.
- High-level tools for dependency injection and other patterns or principles.

As the project is developed, these goals and non-goals may be updated (some might even change categories).

## Usage

All ECS libraries and frameworks are built around *entities*, typically a numeric identifier that can be used to
retrieve component data. Granite defines an entity as a value class wrapping an `Int` value:

```kotlin
@JvmInline value class Entity(val id: Int)
```

Components are also wrapped `Int` values, representing a component's "type", rather than holding the data:

```kotlin
@JvmInline value class Component(val id: Int)
```

Using these previous definitions alone, it is impossible to manage component data or entities in any meaningful way.
This is made possible by the `Engine`, which keeps track of entities, components, and their internal data storage
structures.

### Engine and processing steps

To access the API used to manage entities and components, an Engine step must be requested. Each step consists of
a `CoroutineScope` and several functions to add, remove, and update components and entites. Note that steps need not
map 1:1 to, say, physics processing steps in a game, or graphic frames. Instead, an engine step should be considered
as a transaction, during which all component values and entity states are consistent, and writes are deferred atomic
operations.

```kotlin
suspend fun useEngine(engine: Engine) {
  // component identifier (arbitrary id, kept simple for the sample)
  val Age = Component(123)

  // request a processing step
  engine.step {
    // we can draft entities and use them, they will be fully commited once the processing step scope is complete
    val entity = newEntity()

    // components can be added to new entities (and existing ones)
    entity[Age] = 1

    // don't do this, the entity does not actually exist yet, so we can't query its component values
    // val age = entity[Age]
  }

  // construct a query to find all entities with a specific combination of components
  val aging = EntityQuery.selectEntities { with(Age) }

  // request a new step, the entity we created in the last one will be available now
  engine.step {
    // here we do something new: iterate over every entity
    // with an Age component
    forEach(aging) {
      // increase the value of the component by one
      val age = entity[Age] as Int
      entity[Age] = age + 1

      // changes are deferred, this will still return 1
      val oldAge = entity[Age]
    }
  }
}
```

### Concurrency and parallel processing

Since engine steps are transactional, reads are always consistent, and changes will not be visible until the next step
is requested. This allows for easy decomposition of parallel work, as multiple systems can iterate over the same
components concurrently within the same step, as long as they don't both modify the component's value (at most one of
the processors should modify the value, since normal race conditions still apply even if deferred).

```kotlin
suspend fun useEngine(engine: Engine) {
  // we assume for this example that all three components use Float values
  val Position = Component(123)
  val Velocity = Component(124)
  val Drag = Component(125)

  val Moving = EntityQuery.selectEntities {
    with(Position)
    with(Velocity)
  }

  val Slowing = EntityQuery.selectEntities {
    with(Velocity)
    with(Drag)
  }

  // launch two processors in parallel: they modify different components, so no interference is possible
  engine.step {
    // move all entities with velocity
    launch {
      forEach(Moving) { entity ->
        val position = entity[Position] as Float
        val velocity = entity[Velocity] as Float

        entity[Position] = position + velocity
      }
    }

    // reduce velocity of entities with drag
    launch {
      forEach(Slowing) { entity ->
        val velocity = entity[Velocity] as Float
        val drag = entity[Drag] as Float

        entity[Velocity] = velocity * (1 - drag)
      }
    }
  }
}
```

### Authoring components

The previous examples show how to manage component data, but they use untyped APIs, requiring casts on every read. To
avoid this issue, the `ComponentType<T>` interface can be used. It allows defining typed component markers, usually
defined as companion objects to the classes representing the data:

```kotlin
// component data storage class
class Position(val x: Float, val y: Float) {
  // type-safe marker
  companion object : ComponentType<Position> {
    // actual component ID
    override val type = Component(123)
  }
}

suspend fun useEngine(engine: Engine) = engine.step {
  val entity = newEntity()

  // now we can use type-safe position components
  entity[Position] = Position(4f, 2f)
}
```

Another problem is introduced by this type safety though: component types must be manually defined in companion objects,
and additonally, we are still manually assigining component ID values. Writing this boilerplate for dozens of components
can be tiresome, which is why Granite provides a Kotlin compiler plugin to automatically generate these declarations at
compile-time:

```kotlin
// build.gradle.kts
plugins {
  id("io.github.darvld.granite") version "0.1.0-SNAPSHOT"
}
```

Using the compiler plugin, the previous sample can be rewritten in one line:

```kotlin
// simply mark types that represent components, and a synthetic  companion will be generated
@ComponentData class Position(val x: Float, val y: Float)

suspend fun useEngine(engine: Engine) = engine.step {
  val entity = newEntity()

  // just like in the previous example, we can use type-safe APIs, without the boilerplate!
  entity[Position] = Position(4f, 2f)
}
```

As a bonus, component IDs will be automatically distributed, making sure no duplicates exist within a compilation unit.
See [the plugin's README](../granite-plugin-kotlin/README.md) for more information on how to apply and configure it
using the command line or Gradle.

> Synthetic declarations generated by the compiler plugin will not be automatically recognized by your IDE. In order to
> support this, install the [Intellij platform plugin](../granite-plugin-intellij/README.md).

#### Mutable vs immutable components

While atomic deferred mutations are convenient for parallel processing, they must be paired with careful design of the
component data structures in order to be truly concurrency-safe.

A simple way to ensure thread safety is to designate all component data classes as immutable, meaning that the only way
of updating the value of a component is to replace the data. However, this can impact performance when using structures
with multiple fields due to allocation overhead, total memory usage, and garbage collection costs.

Mutable components (i.e. defining mutable properties with `var`) can reduce allocation overhead, but risk concurrency
issues like race conditions and inconsistent reads.

Ultimately, the decision on the approach depends on the specific use case. Immutable components should be preferred
whenever possible if the performance cost is not significant, but mutable structures can be used as long as proper
caution is applied.

## Behind the scenes

The engine sorts entities into "tables", according to unique component combinations. For example, an entity with
a `Position` component will be placed in a different table than an entity with both `Position` _and_ `Velocity`.
These tables hold the actual component data and are very fast to access thanks to indexing techniques, not unlike how
databases work.

Each table has a "signature", which is made of the sorted IDs of the components it contains. Signatures allow entity
queries to work _really_ fast by skipping entire tables (maybe hundreds of entities) if they don't contain a component
specified in the query. This is a very obvious improvement over other ECS implementations on the JVM, which test
_each entity individually_ during iterations.

## Building

The `granite-engine` project has no external dependencies, excepting the `kotlinx.coroutines` library, and is entirely
independent from other projects in the repository. It is included in the build pre-pass since it is referenced by the
compiler plugin project.

If changes made during development result in expected updates to the public API, run the `apiDump` task to re-generate
the pinned metadata. Note that if the API updates are not intentional, changes should be revised to avoid breaking user
code without warning.

## Resources

The documentation comments in the code provide an excellent starting point for understanding the way in which the
engine works internally; see the following sources for details:

- [`Engine`](./src/main/kotlin/io/github/darvld/granite/Engine.kt): manages internal state and coordinates indexing.
- [`EngineScope`](./src/main/kotlin/io/github/darvld/granite/EngineScope.kt): provides the public entity API.
- [`EntityIndex`](./src/main/kotlin/io/github/darvld/granite/EntityIndex.kt): tracks the internal state of entites.
- [`Table`](./src/main/kotlin/io/github/darvld/granite/Table.kt): holds data for a unique combination of components.
- [`TableIndex`](./src/main/kotlin/io/github/darvld/granite/TableIndex.kt): locates tables by signature and ID.
- [`EntityQuery`](./src/main/kotlin/io/github/darvld/granite/EntityQuery.kt): matches entity signatures for fast iterations.

Additionally, these projects are closely related to the engine and provide better support for its features:

- [Granite Kotlin plugin](../granite-plugin-kotlin): generates synthetic component types in compile-time.
- [Granite Gradle plugin](../granite-plugin-kotlin): applies the compiler plugin to a Kotlin Gradle build.
- [Granite Intellij plugin](../granite-plugin-kotlin): adds support for synthetic declarations in the IDE.

See also the following projects for examples on how to use the engine API and related support features:

- [Sample project](../../samples/granite-basic): simple demo project showing the use of the engine API and Kotlin plugin.

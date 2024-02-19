# Granite sample project

This project is a very basic sample showing the intended use of the Granite ECS framework, including the Kotlin, Gradle,
and Intellij plugins. A demo application can be built and tested using the `run` task. See the instructions below on how
to build and experiment with the code.

## Building

Before the sample can be built, the root project must be properly initialized by publishing the Granite plugins to the
local Maven repository:

```shell
make prepare
```

After the plugins are ready, build and run the sample using:

```shell
./gradlew run
```

## Exploring the sample code

> Make sure to install the Granite support plugin in your IDE, or you will see errors caused by missing synthetic
> declarations. These errors will not prevent the code from compiling, but they will affect the development experience.

The sample defines three [components](./src/main/kotlin/io/github/darvld/granite/sample/components): `Position`,
`Velocity`, and `RandomForce`, and a custom interface, `Processor`, used to provide a uniform definition for classes
that iterate over a fixed entity query every frame:

- [`LinearMotion`](./src/main/kotlin/io/github/darvld/granite/sample/processors/LinearMotion.kt) iterates over
  entities with `Position` and `Velocity`, and adds the velocity vector to the position vector.
- [`RandomLinearForce`](./src/main/kotlin/io/github/darvld/granite/sample/processors/RandomLinearForce.kt) iterates
  over entities with `Velocity` and `RandomForce`, adding a small randomized value to the velocity vector.

Note that processors in this sample run sequentially, to avoid concurrency issues caused by the mutable component data
structures.
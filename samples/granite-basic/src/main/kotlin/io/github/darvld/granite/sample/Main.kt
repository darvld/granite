package io.github.darvld.granite.sample

import io.github.darvld.granite.Engine
import io.github.darvld.granite.sample.processors.LinearMotion
import io.github.darvld.granite.sample.processors.RandomLinearForce
import kotlinx.coroutines.runBlocking

// keep a list of all processors being used
val processors = listOf(
  LinearMotion,
  RandomLinearForce,
)

fun main() = runBlocking {
  // create and configure the engine
  val engine = Engine()

  // run for 10_000 frames
  for (i in 0 until 10_000) engine.step {
    // run all processors sequentially
    processors.forEach { it.step(this) }
  }
}
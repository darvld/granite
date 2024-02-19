package io.github.darvld.granite.sample.processors

import io.github.darvld.granite.EngineScope
import io.github.darvld.granite.Entity
import io.github.darvld.granite.EntityQuery

/** An abstract processing system invoked during an engine step. */
fun interface Processor {
  /** Process the current step in a given [scope]. */
  suspend fun step(scope: EngineScope)
}

/** Construct a new [Processor] that iterates over all entities matched by a [query]. */
inline fun process(query: EntityQuery, crossinline block: suspend EngineScope.(Entity) -> Unit): Processor {
  return Processor { scope -> scope.forEach(query) { scope.block(it) } }
}
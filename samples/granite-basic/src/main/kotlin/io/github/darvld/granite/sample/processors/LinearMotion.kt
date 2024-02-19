package io.github.darvld.granite.sample.processors

import io.github.darvld.granite.EntityQuery.Companion.selectEntities
import io.github.darvld.granite.sample.components.Position
import io.github.darvld.granite.sample.components.Velocity
import io.github.darvld.granite.with

/** Entities that have both [Position] and [Velocity], i.e. are moving. */
private val Moving = selectEntities {
  with(Position)
  with(Velocity)
}

/** Apply an entity's [Velocity] to its [Position]. */
val LinearMotion = process(Moving) { entity ->
  val position = entity[Position]
  val velocity = entity[Velocity]
  
  position.x += velocity.x
  position.y += velocity.y
}
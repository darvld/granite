package io.github.darvld.granite.sample.processors

import io.github.darvld.granite.EntityQuery.Companion.selectEntities
import io.github.darvld.granite.sample.components.RandomForce
import io.github.darvld.granite.sample.components.Velocity
import io.github.darvld.granite.with
import kotlin.random.Random

/** Entities with [Velocity] to which a [RandomForce] is applied every frame. */
private val Randomized = selectEntities {
  with(Velocity)
  with(RandomForce)
}

/** Randomize the [Velocity] of an entity by a random amount. */
val RandomLinearForce = process(Randomized) { entity ->
  val velocity = entity[Velocity]

  velocity.x += Random.nextFloat()
  velocity.y += Random.nextFloat()
}
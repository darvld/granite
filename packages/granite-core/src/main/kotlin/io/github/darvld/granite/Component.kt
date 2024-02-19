package io.github.darvld.granite

/**
 * Components can be used to attach data to an [Entity].
 *
 * Note that the component itself holds no data, but instead represents a stable ID that the engine can use to manage
 * the value for a given entity.
 *
 * @see Entity
 */
@JvmInline public value class Component internal constructor(internal val id: Int)

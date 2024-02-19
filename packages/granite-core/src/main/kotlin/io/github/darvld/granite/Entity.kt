package io.github.darvld.granite

/**
 * A unique identifier representing an entity in the engine.
 *
 * Entities are defined by their "signature", which is formed from the combination of components added to them.
 * Component values are stored in optimized database table-like structures that allow quick iterations.
 *
 * @see Engine
 */
@JvmInline public value class Entity internal constructor(internal val id: Int)
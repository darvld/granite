package io.github.darvld.granite

import java.nio.ByteBuffer

/**
 * Represents an entity signature formed by a sorted array of component IDs. The component IDs are stored using an
 * [IntArray] to reduce overhead. New derivative signatures [with] and [without] specific components will always be
 * sorted on creation.
 *
 * Signatures can be used to uniquely identify entity archetypes and perform fast component queries during iterations,
 * see the overloaded operators in this wrapper for details.
 *
 * Note that signatures by default are compared using the identity of their component arrays, not by their contents.
 * If a content equality check is required the [hash] of the two signatures should be compared instead.
 *
 * @see contains
 */
@JvmInline internal value class Signature internal constructor(internal val types: IntArray) {
  /** The number of component types forming this signature. */
  internal val size: Int get() = types.size

  /** Returns a [Component] of this signature at the given [index]. */
  internal operator fun get(index: Int): Component {
    return runCatching { Component(types[index]) }.getOrElse {
      error("Signature has no component at index $index")
    }
  }

  /** Returns whether the given [component] is part of this signature, using a binary search. */
  internal operator fun contains(component: Component): Boolean {
    // the search function will only return a positive index if the value is found
    return types.binarySearch(component.id) >= 0
  }

  /**
   * Returns the index of a [component] of this signature using a binary search, or a negative value if the
   * component is not present.
   */
  internal fun indexOf(component: Component): Int {
    return types.binarySearch(component.id)
  }

  /**
   * Returns a new [Signature] which is a copy of this one with an additional [component]. If this signature already
   * contains the given component, an exception will be thrown.
   *
   * @see without
   */
  internal infix fun with(component: Component): Signature {
    val insertIndex = types.binarySearch(component.id).also {
      require(it < 0) { "Signature already contains component ${component.id}" }
    }.inv()

    return Signature(IntArray(types.size + 1) { i ->
      if (i < insertIndex) types[i]
      else if (i == insertIndex) component.id
      else types[i - 1]
    })
  }

  /**
   * Returns a new [Signature] which is a copy of this one without a [component]. If this signature does not contain
   * the given component, an exception will be thrown.
   *
   * @see with
   */
  internal infix fun without(component: Component): Signature {
    val removeIndex = types.binarySearch(component.id).also {
      require(it >= 0) { "Signature does not contain component ${component.id}" }
    }

    return Signature(IntArray(types.size - 1) {
      if (it < removeIndex) types[it]
      else types[it + 1]
    })
  }

  /**
   * Returns a unique, stable string representation of this signature's types. This is a relatively slow operation and
   * should be avoided if possible.
   */
  internal fun hash(): String {
    // dump the bytes of the types array into a buffer
    val buffer = ByteBuffer.allocate(types.size * 4)
    types.forEach(buffer::putInt)
    buffer.flip()

    // return the buffer contents as a hex string
    return buildString { repeat(buffer.limit()) { append(buffer.get().toString(16)) } }
  }

  override fun toString(): String {
    return hash().chunked(8).joinToString(":")
  }

  internal companion object {
    /** The special "empty" signature is used for entities without components. */
    internal val EMPTY: Signature = Signature(intArrayOf())
  }
}
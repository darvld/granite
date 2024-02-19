package io.github.darvld.granite

import java.util.concurrent.atomic.AtomicInteger

/**
 * An index used to resolve tables by ID, or by [Signature].
 *
 * The index does not allow explicitly creating tables, instead, a table will be created as a last resort if it cannot
 * be located on any of the caching levels available.
 *
 * Tables form a graph where each edge is a [Component]. A table with an additional component can be determined by
 * moving along that edge of the corresponding origin table.
 *
 * If a table's edges have not cached the target table, a second caching mechanism is used: the target [Signature] is
 * constructed and used to resolve an existing table. If all previous methods fail, a new table is created, added to
 * the index by signature, and recorded in the appropriate edge of the original table.
 *
 * @see Table
 */
internal class TableIndex(initialSize: Int = 10) {
  /** Array containing all existing tables, used for locating a table by its ID. */
  private var tables: Array<Table?> = arrayOfNulls(initialSize)

  /** Internal index binding tables to their signature hash. */
  private val index: MutableMap<String, Table> = mutableMapOf()

  /**
   * The last ID assigned to a table, representing the last index in the [tables] array that holds a value. If it
   * exceeds the size of [tables], the array will be resized to accomodate new values.
   */
  private var lastId: Int = -1

  /** A table used for new entities, holding no component data. The value is cached to avoid redundant lookups. */
  internal val emptyTable = newTable(Signature.EMPTY)

  /** Returns a [Table] by its [id], or null if no such table exists. */
  internal operator fun get(id: Int): Table? {
    return tables.getOrNull(id)
  }

  /** Returns a [Table] by its [id], throwing an exception if no such table exists. */
  internal fun getOrFail(id: Int): Table {
    return runCatching { tables[id] }.getOrNull() ?: error("Table with id $id does not exist.")
  }

  /** Locates a table by its [signature], returning null if no such table exists. */
  internal operator fun get(signature: Signature): Table? {
    return index[signature.hash()]
  }

  /** Locates a table by its [signature], throwing an exception if no such table exists. */
  internal fun getOrFail(signature: Signature): Table {
    return index[signature.hash()] ?: error("Table with signature $signature does not exist.")
  }

  /**
   * Resolves or creates a new entry based on the provided [table], plus one extra [component].
   *
   * @param table The table to be used as starting point for the resolution.
   * @param component A component not present in [table], required in the target value.
   * @param createWithSize Optional size to be used if creating a new table is necessary.
   * @return A table with the requested
   */
  internal fun resolveWithComponent(table: Table, component: Component, createWithSize: Int = 10): Table {
    // attempt to locate the table using the edges of the current one
    table.withComponent(component)?.let { return it }

    // slower path, construct the new signature and try to locate using the index
    val signature = table.signature with component
    index[signature.hash()]?.let { return it }

    // slowest path, create a new table and add it to the index
    return newTable(signature, createWithSize)
  }

  /**
   * Resolves or creates a new entry based on the provided [table], excluding one [component].
   *
   * @param table The table to be used as starting point for the resolution.
   * @param component A component present in [table], required to not be part of the target value.
   * @param createWithSize Optional size to be used if creating a new table is necessary.
   * @return A table with the requested
   */
  internal fun resolveWithoutComponent(table: Table, component: Component, createWithSize: Int = 10): Table {
    // attempt to locate the table using the edges of the current one
    table.withoutComponent(component)?.let { return it }

    // slower path, construct the new signature and try to locate using the index
    val signature = table.signature without component
    index[signature.hash()]?.let { return it }

    // slowest path, create a new table and add it to the index
    return newTable(signature, createWithSize)
  }

  /** Returns a thread-safe iterator that yields the values for every [Table] in the index. */
  internal operator fun iterator(): Iterator<Table> = object : Iterator<Table> {
    private var next = AtomicInteger(0)

    override fun hasNext(): Boolean {
      return next.get() <= lastId
    }

    override fun next(): Table {
      return tables[next.getAndIncrement()]!!
    }
  }

  /** Create a new [Table] for a given [signature], resizing the [tables] array if out of space. */
  private fun newTable(signature: Signature, initialSize: Int = 10): Table {
    if (lastId + 1 >= tables.size) resize()
    lastId += 1

    val table = Table(lastId, signature, initialSize)
    tables[lastId] = table
    index[signature.hash()] = table

    return table
  }

  /**
   * Resize the [tables] array, increasing its length by [GROW_FACTOR]. This is a costly operation and should be
   * avoided when possible by adjusting the initial size of the index.
   */
  private fun resize() {
    tables = tables.copyOf(tables.size * GROW_FACTOR)
  }

  private companion object {
    /** Factor by which the length of the [tables] array will grow when out of space. */
    private const val GROW_FACTOR: Int = 2
  }
}
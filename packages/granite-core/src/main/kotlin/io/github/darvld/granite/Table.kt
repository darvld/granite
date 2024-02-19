package io.github.darvld.granite

import java.util.concurrent.atomic.AtomicInteger

/**
 * Tables are the main storage component in the engine. Each table holds the values for a unique combination of
 * components, known as the [signature], or archetype. Additionaly, each table has a unique numeric [id] that can be
 * used to retrieve it from the index.
 *
 * Values are stored in columns (one per component), each column being an array where elements represent the entities
 * in the table. Removing an entity from the table does not cause other rows to be updated, and instead the row is
 * flagged as available for storing new added entities.
 *
 * Each table provides two maps of tables with similar signature, known as the "edges" of the table (it can be useful
 * to think of tables as nodes in a graph), which can be used to resolve the table to which an entity will be moved
 * when a component is added or removed from it. See [withComponent] and [withoutComponent] for more details.
 *
 * @see TableIndex
 */
internal class Table internal constructor(
  internal val id: Int,
  internal val signature: Signature,
  initialSize: Int = 10
) {
  init {
    // simple assertion to avoid obscure out-of-bounds errors: if size is 0, lastUsedRow++ will be 0 on the first add,
    // causing an exception when the entity is added to the table (also, an empty table is pointless in any case)
    require(initialSize > 0) { "Initial table size must be greater than zero." }
  }

  /**
   * Represents an edge of the table graph, where a [Component] can be used to move between tables. This is a fully
   * inline construct and is meant to improve readability over the plain [cache] map.
   *
   * Tables can use edges to locate other tables when a component is added or removed from an entity. Cached
   * transitions are stored to avoid the cost of allocating a new [Signature] in order to perform an index lookup.
   *
   * @see withoutComponent
   * @see withComponent
   */
  @JvmInline internal value class Edge(val cache: MutableMap<Component, Table> = mutableMapOf()) {
    /** Returns the cached value for this edge at the specified [component]. */
    @Suppress("nothing_to_inline") inline operator fun invoke(component: Component): Table? {
      return cache[component]
    }

    /** Set the cached [value] for this edge at the specified [component]. */
    @Suppress("nothing_to_inline") inline fun register(component: Component, value: Table) {
      cache[component] = value
    }
  }

  /** A small, immutable index linking a [Component] ID with the column holding its values. */
  private val componentIndex: Map<Int, Int> = HashMap<Int, Int>().apply {
    signature.types.forEachIndexed { index, i -> put(i, index) }
  }

  /**
   * A special column holding the ids for every entity in the table. If a row is not occupied by an entity, its value
   * in this column is used as a pointer to another available row as described by [lastAvailableRow].
   */
  private var idColumn: IntArray = IntArray(initialSize)

  /**
   * Data columns in the table, each corresponding to a [Component] type in the table's [signature]. Empty rows are
   * represented by null values.
   */
  private val columns: Array<Array<Any?>> = Array(signature.size) { arrayOfNulls(initialSize) }

  /**
   * The current size of the table, matching the size of the arrays stared in each of the [columns].
   */
  private var size: Int = initialSize

  /**
   * The index of the last row holding an entity's component values, or [ROW_NONE] if no rows are used.
   *
   * This value is used to determine whether the arrays for each column must be resized to accomodate new entities.
   * When a new entity is added, if [lastAvailableRow] is [ROW_NONE], [lastUsedRow] + 1 will be used to store it,
   * unless it exceeds the current [size].
   */
  private var lastUsedRow: Int = ROW_NONE

  /**
   * The index of the last row that can accomodate an entity being inserted, or [ROW_NONE] if no rows are free.
   *
   * When an entity is removed from the table, this value is set to its corresponding row. If this value points to a
   * valid row at that time, the [idColumn] will be updated at the released row to point to the previous value.
   *
   * When an entity is added to the table, this value is used to determine if allocating a new row is available. If the
   * value points to a row, this property will be set to the value held at that row in the [idColumn].
   *
   * Free rows thus form a "stack" using the [idColumn], where newly freed rows point to the last free row at the time,
   * and newly occupied rows "pop" the last value by reading this value and then updating it to the next lower entry
   * in the stack.
   */
  private var lastAvailableRow: Int = ROW_NONE

  /**
   * Graph edge linking to other tables with one component in addition to the ones in this table.
   *
   * This edge is used when resolving the destination [Table] when a component is added to an entity from this table,
   * avoiding the need to create a new [Signature] for an index lookup.
   *
   * @see withoutComponent
   */
  internal val withComponent: Edge = Edge()

  /**
   * Graph edge linking to other tables with all the components in this table except one.
   *
   * This edge is used when resolving the destination [Table] when a component is removed from an entity from this
   * table, avoiding the need to create a new [Signature] for an index lookup.
   *
   * @see withComponent
   */
  internal val withoutComponent: Edge = Edge()

  /** Returns the value at a [row] and [column], throwing an exception if no value is set. */
  internal operator fun get(row: Int, column: Int): Any {
    return columns[column][row] ?: error("No value at row $row for component ${signature[column]} (column $column)")
  }

  /** Sets the value at a [row] and [column]. */
  internal operator fun set(row: Int, column: Int, value: Any) {
    columns[column][row] = value
  }

  /**
   * Add an entity to the table, returning the row number where it was stored.
   *
   * This method will attempt to reuse available rows from removed entities; if no freed rows are available, it will
   * try to use a previously unused row; as a last resort, the value arrays for each column will be resized.
   *
   * @param entity The entity being added to the table.
   * @return The row index at which the new entity is stored.
   */
  internal fun add(entity: Entity): Int {
    val row = when {
      // reuse rows if possible, by "popping" the stack of available rows
      lastAvailableRow != ROW_NONE -> lastAvailableRow.also {
        // reassign the pointer to the row referenced by the one being reused; if this is the last entry in the stack,
        // the value will be ROW_NONE
        lastAvailableRow = idColumn[lastAvailableRow]
      }

      // there is still space to add entities without resizing data arrays
      lastUsedRow + 1 < size -> ++lastUsedRow

      // all rows are in use, we need to resize the arrays; this operation is costly and should be avoided by tuning
      // the initial size of the table to match the expected number of entities with this signature
      else -> {
        // `lastUsedRow` + 1 will point to the first empty element
        resizeArrays(GROW_FACTOR)
        ++lastUsedRow
      }
    }

    idColumn[row] = entity.id
    return row
  }

  /**
   * Remove all data associated with the entity at [row]. The row may be reused in future [add] calls.
   *
   * Component values at the specified [row] will be cleared, and the row will be marked for reuse in future [add]
   * calls. An exception will be thrown if the row index is out of bounds.
   *
   * @param row The row to be removed from the table.
   * @return The [Entity] removed from the table.
   */
  internal fun remove(row: Int): Entity {
    val removed = idColumn[row]

    // "push" the entry to the available stack
    idColumn[row] = lastAvailableRow
    lastAvailableRow = row

    // clear component values
    repeat(columns.size) { columns[it][row] = null }

    return Entity(removed)
  }

  /**
   * Returns the column of this table holding values for a given [component], throwing an exception if the component is
   * not part of the [signature].
   */
  internal fun columnFor(component: Component): Int {
    return componentIndex[component.id] ?: error("Component ${component.id} is not present in the table.")
  }

  /**
   * Returns the column of this table holding values for a given [component], or null if the component is not part of
   * the [signature].
   */
  internal fun columnOrNull(component: Component): Int? {
    return componentIndex[component.id]
  }

  /**
   * Returns an [Iterator] over all entries in this table.
   *
   * There are no guarantees about the order in which the rows will be visited, and most likely consecutive iterations
   * will not yield consecutive row numbers.
   *
   * The returned iterator is thread-safe and can be used concurrently with no external synchronization.
   */
  internal operator fun iterator(): Iterator<Entity> = object : Iterator<Entity> {
    private var next = AtomicInteger(lastUsedRow)
    private var nextSkip = AtomicInteger(lastAvailableRow)

    override fun hasNext(): Boolean {
      return next.get() >= 0
    }

    override fun next(): Entity {
      val nextValue = next.getAndDecrement()

      // if the next row (not the one on this iteration) is marked as "released", skip it,
      // and pop the available rows stack; we decrement the value of next so that hasNext()
      // returns correctly after this, otherwise it would need to check for availability
      if (nextSkip.compareAndSet(nextValue - 1, idColumn[nextValue - 1])) {
        next.decrementAndGet()
      }

      return Entity(nextValue)
    }
  }

  /**
   * Resize the value arrays for each of the [columns] (and the special [idColumn]). This is a costly operation and
   * should be avoided when possible.
   */
  private fun resizeArrays(factor: Int) {
    val newSize = size * factor

    // use the fastest copy function available (native copy)
    repeat(columns.size) { columns[it] = columns[it].copyOf(newSize) }
    idColumn = idColumn.copyOf(newSize)

    size = newSize
  }

  private companion object {
    /** Value used with [lastAvailableRow] to indicate that no rows are available and a new one must be added. */
    private const val ROW_NONE: Int = -1

    /** Factor by which the length of the value arrays in the table's column grows when [resizeArrays] is called. */
    private const val GROW_FACTOR: Int = 2
  }
}

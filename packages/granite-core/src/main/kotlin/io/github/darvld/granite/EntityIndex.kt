package io.github.darvld.granite

/**
 * An index used to resolve the current table and row for a given [Entity] efficiently.
 *
 * The [new] method returns an [Entity] without recording into the index. This allows calling code to initialize
 * external state, such as table rows, before calling [record] to commit the entity to the index. Between the calls to
 * [new] and [record], the entity is considered to be in a special "uninitialized" state, during which it will be
 * treated as inexistent for [get] calls.
 *
 * Entities can be deleted from the index using [remove]. This causes the entry to be invalidated, and future calls to
 * [get] and [record] will throw an exception.
 *``
 * Index entries are never reused even after being cleared. The `initialSize` parameter in the constructor determines
 * the length of the internal index array holding entity records, and `growFactor` can be used to adjust how the array
 * will be resized when more space is needed to accomodate new entities.
 *
 * @see Entity
 */
internal class EntityIndex(initialSize: Int = 10, private val growFactor: Int = GROW_FACTOR) {
  /**
   * An entry in the entity index, providing the table and row in which this entity's component values are stored.
   *
   * Entity records have three states: "valid" (the default), in which they represent a [table] and [row] for a given
   * entity; "unassigned", which is set after a [new] entity is created but before [record] is called, and is
   * considered an invalid state for the purpose of [get] queries; and "removed", which is set after an entity is
   * deleted from the index and represents a non-existent entity.
   *
   * To check whether an entity record is valid, use [isActive] and [isAlive]. Specificially, [isActive] will return
   * true as long as the entity is not in the "unassigned" state (including if it has been removed); and [isAlive]
   * will return true if the entity is not in the "removed" state _and_ [isActive] is true.
   *
   * @see EntityIndex
   */
  @JvmInline internal value class Record(private val packedValue: Int) {
    /** Construct a new [Record] holding the given [table] and [row]. */
    constructor(table: Int, row: Int) : this(valueOf(table, row))

    /** The ID of the table holding the data for this entity. */
    internal val table: Int get() = unpackLowInt(packedValue)

    /** The row of the [table] holding the data for this entity. */
    internal val row: Int get() = unpackHighInt(packedValue)

    /** Whether this record represents a live entity. */
    internal val isAlive: Boolean get() = packedValue != REMOVED_ENTITY && isActive

    /** Whether this record's entity has an assigned entry, even if that entry is [REMOVED_ENTITY]. */
    internal val isActive: Boolean get() = packedValue != UNASSIGNED_ENTITY

    /** Returns the [table] component of this record. */
    internal operator fun component1(): Int = table

    /** Returns the [row] component of this record. */
    internal operator fun component2(): Int = row

    internal companion object {
      /** The value of a [Record] for an entity that has been removed from the index. */
      internal val REMOVED_ENTITY: Int = valueOf(-1, -1)

      /** The value of a [Record] for an entity that has been created, before setting the record values. */
      internal val UNASSIGNED_ENTITY: Int = valueOf(-1, -2)

      /** Unpack the lower 16 bits of the [packed] value as an [Int]. */
      private fun unpackLowInt(packed: Int): Int {
        return packed ushr 16
      }

      /** Unpack the upper 16 bits of the [packed] value as an [Int]. */
      private fun unpackHighInt(packed: Int): Int {
        return packed and 0xFFFF
      }

      /** Returns the packed value that would be used to construct a [Record] with [table] and [row]. */
      internal fun valueOf(table: Int, row: Int): Int {
        return (table shl 16) or (row and 0xFFFF)
      }
    }
  }

  /** A primitive array holding all the packed values for index entries. */
  private var entries: IntArray = IntArray(initialSize)

  /**
   * The last ID assigned to an entity, representing the last index in the [entries] array that holds a valid entity.
   * If it exceeds the bounds of [entries], the array will be resized to accomodate new values.
   */
  private var lastId: Int = -1

  /**
   * Resolve the current table and row for a given [entity], returning `null` if the entity does not exist, is
   * [inactive][Record.isActive], or has been [removed][Record.isAlive].
   *
   * @see getOrFail
   */
  internal operator fun get(entity: Entity): Record? {
    if (entity.id < 0 || entity.id > lastId) return null
    return Record(entries[entity.id]).takeIf { it.isActive && it.isAlive }
  }

  /**
   * Resolve the current table and row for a given [entity], throwing an exception if the entity does not exist, is
   * [inactive][Record.isActive], or has been [removed][Record.isAlive].
   *
   * @see get
   */
  internal fun getOrFail(entity: Entity): Record {
    return get(entity) ?: error("Entity ${entity.id} does not exist")
  }

  /**
   * Returns a new [Entity] after adding it to the index. The returned entity is uninitialized, use [record] to add
   * the corresponding index entry after a table and row are selected for it.
   *
   * This operation may cause the index to be resized if the new entity does not fit the current storage; this scenario
   * should be minimized by adjusting the initial index size and the [GROW_FACTOR].
   *
   * @return A new [Entity] without an associated table and row.
   */
  internal fun new(): Entity {
    if (lastId + 1 >= entries.size) resizeEntries()
    lastId += 1

    entries[lastId] = Record.UNASSIGNED_ENTITY
    return Entity(lastId)
  }

  /**
   * Add a batch of entities of the specified [size].
   *
   * All created entities are uninitialized, so a [record] or [recordUnsafe] call is required before they can be used.
   * The batch is guaranteed to span all the values in the returned range.
   *
   * @return The range of entity IDs created in the batch.
   */
  internal fun newBatch(size: Int): IntRange {
    val batchRange = (lastId + 1) until (lastId + 1 + size)
    while (batchRange.last >= entries.size) resizeEntries()

    // batch insert entries in the index and update the id counter
    entries.fill(Record.UNASSIGNED_ENTITY, batchRange.first, batchRange.last + 1)
    lastId += size

    return batchRange
  }

  /**
   * Returns the [Entity] that will be created on the next call to [new] as a "draft", meaning no entry will be added
   * to the index and consecutive [draft] calls will return the same value until [new] is called.
   *
   * @see new
   */
  internal fun draft(): Entity {
    return Entity(lastId + 1)
  }

  /**
   * Record the [table] and [row] for this entity, typically after being created. If the entity has already been
   * marked as "removed", an exception will be thrown.
   *
   * This operation is necessary to allow new entities to be created using [new], without the requirement of a
   * pre-determined table or row (which might need the entity ID to obtain, causing a circular dependency).
   *
   * @param entity The entity being recorded into the index.
   * @param table The table storing the entity.
   * @param row The row of the [table] storing the entity.
   * @return The recorded [entity].
   */
  internal fun record(entity: Entity, table: Int, row: Int): Entity {
    check(entries[entity.id] != Record.REMOVED_ENTITY) { "Entity ${entity.id} does not exist" }
    entries[entity.id] = Record.valueOf(table, row)
    return entity
  }

  /**
   * Record the [table] and [row] for this entity, typically after being created, without checking if it has been
   * removed already.
   *
   * This operation is necessary to allow new entities to be created using [new], without the requirement of a
   * pre-determined table or row (which might need the entity ID to obtain, causing a circular dependency).
   *
   * @param entity The entity being recorded into the index.
   * @param table The table storing the entity.
   * @param row The row of the [table] storing the entity.
   * @return The recorded [entity].
   */
  internal fun recordUnsafe(entity: Entity, table: Int, row: Int) {
    entries[entity.id] = Record.valueOf(table, row)
  }

  /**
   * Remove an [entity] from the index, invalidating the entry. This method does not check if the entity is alive.
   * The returned value represents the index record before being invalidated.
   *
   * @see new
   */
  internal fun remove(entity: Entity): Record {
    val record = Record(entries[entity.id])
    entries[entity.id] = Record.REMOVED_ENTITY

    return record
  }

  /** Increase the size of the [entries] array by the [growFactor]. */
  private fun resizeEntries() {
    entries = entries.copyOf(entries.size * growFactor)
  }

  private companion object {
    /** Factor by which the length of the [entries] array will grow when out of space. */
    private const val GROW_FACTOR: Int = 2
  }
}
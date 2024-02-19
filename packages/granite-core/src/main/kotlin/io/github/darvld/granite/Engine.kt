package io.github.darvld.granite

import kotlinx.coroutines.supervisorScope
import io.github.darvld.granite.EntityIndex.Record as EntityRecord

/**
 * The [Engine] manages entities and coordinates processing.
 *
 * Interaction with entities takes place in the scope of a processing [step]. During a step, read-only operations are
 * executed immediately and are guaranteed to be consistent even for concurrent calls. This is because write operations
 * are deferred until the end of the step. Entities created during the step are considered "drafts" until the end of the
 * step, at which point they are commited to the index.
 *
 * While operations in a given step are thread-safe, only one step may be active across all threads. Concurrent calls
 * to [step] (calling the method before a previous call has returned) will throw an exception.
 *
 * @see EngineScope
 * @see Entity
 */
public class Engine {
  /** Index holding all the entities in the engine. */
  private val entities: EntityIndex = EntityIndex()

  /** Index holding all the component storage tables in the engine. */
  private val tables: TableIndex = TableIndex()

  /** Reusable engine scope used during processing steps. */
  private val scope: EngineScope = EngineScope(this)

  /**
   * Perform a processing step by running a [block] in the scope of the engine.
   *
   * All read operations (read/check components) are guaranteed to be consistent during the step; write operations
   * such as creating/destroying entities, adding/removing components, and updating component values, will be deferred
   * until the end of the step.
   *
   * The [EngineScope] used as receiver implements is a [supervisorScope], meaning coroutines launched within the step
   * can fail without cancelling each other or the parent scope.
   *
   * This method suspends until all child coroutines launched in the scope of the step have finished executing, and all
   * deferred operations are reconciled.
   *
   * @see EngineScope
   */
  public suspend fun step(block: suspend EngineScope.() -> Unit) {
    scope.acquire()
    supervisorScope {
      scope.prepare(nextEntityId = entities.draft().id, context = coroutineContext)
      scope.block()
      scope.collect()
    }
    scope.release()
  }

  /**
   * Run the specified [block] for every entity in the engine matching a [query].
   *
   * This method does not guarantee the order in which entities are accessed. Entire tables will be discarded from the
   * operation if their signature does not satisfy the [query], which allows very fast execution at the cost of
   * inconsistent order.
   */
  internal suspend fun forEach(query: EntityQuery, block: suspend (Entity) -> Unit) {
    for (table in tables) {
      // skip non-matching tables
      if (!(query matches table.signature)) continue

      // now iterate over every row in the table
      for (entity in table) block(entity)
    }
  }

  /**
   * Create a new empty [Entity] and return it. The entity is fully initialized and can immediately be
   * [destroyed][destroyEntity] or modified by [adding][addComponent] and [removing][removeComponent] components or
   * setting their values.
   *
   * @see destroyEntity
   */
  internal fun newEntity(): Entity {
    // add the entity index and table records 
    val entity = entities.new()
    val row = tables.emptyTable.add(entity)

    return entities.record(entity, tables.emptyTable.id, row)
  }

  /**
   * Create a new batch of empty entities and return the range of their IDs. The entities are fully initialized and can
   * immediately be [destroyed][destroyEntity] or modified by [adding][addComponent] and [removing][removeComponent]
   * components or setting their values.
   *
   * @see newEntity
   * @see destroyEntity
   */
  internal fun newEntityBatch(size: Int): IntRange {
    // create the entity batch
    val ids = entities.newBatch(size)
    val table = tables.emptyTable

    // add the entities to the "empty" table
    for (i in ids) {
      val entity = Entity(i)
      entities.recordUnsafe(entity, table.id, table.add(entity))
    }

    return ids
  }

  /** Returns whether the given [entity] is part of the index, is alive, and is currently valid. */
  internal fun exists(entity: Entity): Boolean {
    return entities.get(entity) != null
  }

  /**
   * Invalidate a target [entity], removing all related data from the component storage. This method has no effect on
   * entities that have already been destroyed.
   *
   * After this method is called, [hasComponent], [getComponent], [setComponent], [addComponent], and [removeComponent]
   * will throw an exception.K
   *
   * @see newEntity
   */
  internal fun destroyEntity(entity: Entity) {
    // invalidate the entity index record, ignore already invalidated entries
    val record = entities.remove(entity)
    if (!record.isAlive) return

    // remove the row from the table
    tables.getOrFail(record.table).remove(record.row)
  }

  /** Returns whether an [entity] has a certain [component], throwing an exception if the entity does not exist. */
  internal fun hasComponent(entity: Entity, component: Component): Boolean {
    val entityRecord = entities.getOrFail(entity)
    val table = tables.getOrFail(entityRecord.table)

    return component in table.signature
  }

  /**
   * Returns the value of a [component] for a given [entity] has a certain [component], throwing an exception if the
   * entity does not exist or does not have that component.
   */
  internal fun getComponent(entity: Entity, component: Component): Any {
    val entityRecord = entities.getOrFail(entity)
    val table = tables.getOrFail(entityRecord.table)

    return table[entityRecord.row, table.columnFor(component)]
  }

  /**
   * Returns the value of a [component] for a given [entity] has a certain [component], or null if the entity does not
   * have that component. If the entity does not exist, and exception will be thrown
   */
  internal fun getComponentOrNull(entity: Entity, component: Component): Any? {
    val entityRecord = entities.getOrFail(entity)
    val table = tables.getOrFail(entityRecord.table)

    val column = table.columnOrNull(component) ?: return null
    return table[entityRecord.row, column]
  }

  /**
   * Updates the [value] of a [component] for a given [entity] has a certain [component], adding the component if it
   * is not present in the entity. If the entity does not exist, an exception will be thrown
   */
  internal fun setComponent(entity: Entity, component: Component, value: Any) {
    val entityRecord = entities.getOrFail(entity)
    val table = tables.getOrFail(entityRecord.table)
    val column = table.columnOrNull(component)

    if (column != null) table[entityRecord.row, column] = value
    else addComponent(entity, component, value, entityRecord, table)
  }

  /**
   * Add a [component] with the specified [value] to an [entity], failing if the entity does not exist, is invalid, or
   * already contains the component.
   *
   * For convenience, if the record and table for the target entity are known when this method is called, they may be
   * specified in the arguments to avoid additional lookups.
   */
  internal fun addComponent(
    entity: Entity,
    component: Component,
    value: Any,
    record: EntityRecord? = null,
    table: Table? = null
  ) {
    val entityRecord = record ?: entities.getOrFail(entity)
    val currentTable = table ?: tables.getOrFail(entityRecord.table)

    // find the new table for the entity, and the column of the component
    val newTable = tables.resolveWithComponent(currentTable, component)
    val newColumn = newTable.columnFor(component)

    // move values to the new table
    val newRow = newTable.add(entity)
    for (column in 0 until newTable.signature.size) when {
      column == newColumn -> newTable.set(newRow, column, value)
      column > newColumn -> newTable.set(newRow, column, currentTable[entityRecord.row, column - 1])
      else -> newTable.set(newRow, column, currentTable[entityRecord.row, column])
    }

    // delete the record from the old table
    currentTable.remove(entityRecord.row)

    // update the entity index
    entities.record(entity, newTable.id, newRow)
  }

  /**
   * Removes a [component] from an [entity], failing if the entity does not exist, is invalid, or does not contain
   * the specified component.
   */
  internal fun removeComponent(entity: Entity, component: Component) {
    val entityRecord = entities.getOrFail(entity)
    val currentTable = tables.getOrFail(entityRecord.table)
    val currentColumn = currentTable.columnFor(component)

    // find the new table for the entity, and the column of the component
    val newTable = tables.resolveWithoutComponent(currentTable, component)

    // move values to the new table
    val newRow = newTable.add(entity)
    for (column in 0 until newTable.signature.size) when {
      column >= currentColumn -> newTable.set(newRow, column, currentTable[entityRecord.row, column + 1])
      else -> newTable.set(newRow, column, currentTable[entityRecord.row, column])
    }

    // delete the record from the old table
    currentTable.remove(entityRecord.row)

    // update the entity index
    entities.record(entity, newTable.id, newRow)
  }
}
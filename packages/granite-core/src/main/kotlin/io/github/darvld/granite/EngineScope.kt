package io.github.darvld.granite

import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A scope providing access to entities, components, and the operations to manage them.
 *
 * All read operations in a scope are guaranteed to be consistent, that is, concurrent calls will return the same
 * value, even if a mutating counterpart is called in between.
 *
 * Write operations are deferred until the end of the processing step, such that they will not affect the values of
 * components during the step.
 *
 * @see forEach
 */
public class EngineScope internal constructor(private val engine: Engine) : CoroutineScope {
  /**
   * Defines a deferred operation that should be executed at the end of the processing step.
   *
   * All mutating operations (add/remove entity, add/remove/update component) must be deferred to ensure reading
   * consistency across concurrent processors in a single step.
   *
   * @see commands
   * @see collect
   */
  private sealed interface Command {
    /** Schedule an [entity] to be removed from the engine. */
    @JvmInline value class DestroyEntity(val entity: Entity) : Command

    /** Schedule a [component] to be removed from an [entity]. */
    data class RemoveComponent(val entity: Entity, val component: Component) : Command

    /** Schedule a [component] to be added to an [entity]. */
    data class AddComponent(val entity: Entity, val component: Component, val value: Any) : Command

    /** Schedule a [component] to be updated for an [entity]. */
    data class SetComponent(val entity: Entity, val component: Component, val value: Any) : Command
  }

  /**
   * The ID to be assigned to the next entity to be scheduled for creation. Once the step is over, every ID not
   * present in the index will be added.
   */
  private val nextEntity: AtomicInteger = AtomicInteger(0)

  /**
   * The ID assigned to the first entity to be scheduled for creation in a step, used to calculate the size of the
   * batch to be created at the end of the step.
   */
  private val firstEntity: AtomicInteger = AtomicInteger(0)

  /** Whether the scope is currently busy, i.e. a step is currently being processed. */
  private val busy: AtomicBoolean = AtomicBoolean(false)

  /**
   * A thread-safe queue for commands that modify the state of the engine.
   *
   * Commands are consumed by the [collect] method, which applies all changes queued during the step. The queue will
   * be cleared by [prepare] to ensure a clean state at the start of the step.
   */
  private val commands: ConcurrentLinkedQueue<Command> = ConcurrentLinkedQueue()

  /** A mutable context to be replaced by the value of the scope used on each step. */
  override var coroutineContext: CoroutineContext = EmptyCoroutineContext

  /**
   * Draft a new [Entity], deferring its creation until the end of the step.
   *
   * Note that reading components for an entity created in this step is not allowed, as the entity will not have
   * components until the end of the step, even if [add] is called.
   *
   * @see destroy
   */
  public fun newEntity(): Entity {
    // draft an entity and increase the global counter
    return Entity(nextEntity.getAndIncrement())
  }

  /**
   * Schedule an [entity]'s destruction. The entity will be invalidated at the end of the step.
   *
   * Note that reading components for the entity is still possible during the rest of the step until the change is
   * commited to the index.
   *
   * @see newEntity
   */
  public fun destroy(entity: Entity) {
    // schedule for collection at the end of the step
    commands.offer(Command.DestroyEntity(entity))
  }

  /**
   * Add a [component] with the provided [value] to this [Entity].
   *
   * The update will not be commited until the end of the step, meaning until then reading the value of the
   * component for this entity will still throw an exception.
   *
   * @see remove
   */
  public fun <T : Any> Entity.add(component: ComponentType<T>, value: T) {
    // schedule for collection at the end of the step
    commands.offer(Command.AddComponent(this, component.type, value))
  }

  /**
   * Remove a [component] from this [Entity].
   *
   * The update will not be commited until the end of the step, meaning until then reading the value of the
   * component for this entity will still return normally.
   *
   * @see add
   */
  public fun Entity.remove(component: ComponentType<*>) {
    // schedule for collection at the end of the step
    commands.offer(Command.RemoveComponent(this, component.type))
  }

  /**
   * Set the [value] for a [component] of this [Entity].
   *
   * The update will not be commited until the end of the step, meaning until then reading the value of the
   * component for this entity will return the previous value.
   *
   * @see get
   */
  public operator fun <T : Any> Entity.set(component: ComponentType<T>, value: T) {
    // schedule for collection at the end of the step
    commands.offer(Command.SetComponent(this, component.type, value))
  }

  /**
   * Reads the value of a [component] of this [Entity]. This operation is consistent across threads, and will return
   * the same value until the end of the step even if [set] is called.
   *
   * @see set
   */
  public operator fun <T : Any> Entity.get(component: ComponentType<T>): T {
    // this operation is read-only, so it's thread safe
    @Suppress("unchecked_cast") return engine.getComponent(this, component.type) as T
  }

  /**
   * Reads the value of a [component] of this [Entity], or `null` if the component is not part of the entity.
   *
   * This operation is consistent across threads, and will return the same value until the end of the step even
   * if [set] is called.
   *
   * @see set
   */
  public fun <T : Any> Entity.getOrNull(component: ComponentType<T>): T? {
    // this operation is read-only, so it's thread safe
    @Suppress("unchecked_cast") return engine.getComponentOrNull(this, component.type) as? T
  }

  /**
   * Returns whether this [Entity] contains a specified [component]. This operation is consistent across threads, and
   * will return the same value until the end of the step even if [add] or [remove] are called.
   */
  public operator fun Entity.contains(component: ComponentType<*>): Boolean {
    // this check is read-only, so it's thread safe
    return engine.hasComponent(this, component.type)
  }

  /**
   * Iterate over entities matching the specified [query] and call a function on each one. It is safe to concurrently
   * invoke this method from different threads.
   *
   * Calling [newEntity] and [remove] is allowed during this operation since changes will be deferred until the end of
   * the step. This means new entities will not be included in the iteration even if they "match" the query, and
   * removed entities will still be included if their signature matches the request.
   *
   * @see EntityQuery
   */
  public suspend fun forEach(query: EntityQuery, block: suspend (Entity) -> Unit) {
    engine.forEach(query, block)
  }

  /**
   * Ensure that the scope is ready to begin the next step, throwing an exception if the previous step is still
   * executing.
   *
   * Calling [prepare] without calling this method may cause concurrency issues and undefined behavior. This method
   * marks the scope as "busy" until [release] is called.
   *
   * @see release
   */
  internal fun acquire() = check(!busy.getAndSet(true)) {
    "Multiple concurrent calls to Engine.step detected, please ensure a single step is running at a time"
  }

  /**
   * Ensure that the scope is ready to begin the next step, throwing an exception if the previous step is still
   * executing.
   *
   * Calling [prepare] without calling this method may cause concurrency issues and undefined behavior. This method
   * marks the scope as "busy" until [release] is called.
   *
   * @see release
   */
  internal fun release() = check(busy.getAndSet(false)) {
    "Multiple concurrent calls to Engine.step detected, please ensure a single step is running at a time"
  }

  /**
   * Prepare the scope for a new step; [acquire] should be called before this method to ensure the previous step
   * finished executing correctly, otherwise behavior during this step will be undefined.
   *
   * This will set the [nextEntityId] counter to match the specified value and clear internal command queues in order to
   * prepare the next iteration.
   *
   * After this method has finished, [release] should be called to ensure the "busy" flag is cleared correctly.
   *
   * @see collect
   */
  internal fun prepare(nextEntityId: Int, context: CoroutineContext) {
    coroutineContext = context
    firstEntity.set(nextEntityId)
    nextEntity.set(nextEntityId)
    commands.clear()
  }

  /**
   * Collect and apply all changes queued during the last step (since [prepare] was called). This will create and
   * remove queued entities, and perform any requested component additions/removals/updates.
   *
   * @see prependIndent
   */
  internal fun collect() {
    // create all drafted entities (difference in drafted ID since the step started)
    engine.newEntityBatch(nextEntity.get() - firstEntity.get())

    // process all pending changes
    while (true) when (val command = commands.poll()) {
      is Command.DestroyEntity -> engine.destroyEntity(command.entity)
      is Command.AddComponent -> engine.addComponent(command.entity, command.component, command.value)
      is Command.SetComponent -> engine.setComponent(command.entity, command.component, command.value)
      is Command.RemoveComponent -> engine.removeComponent(command.entity, command.component)
      null -> break
    }
  }
}
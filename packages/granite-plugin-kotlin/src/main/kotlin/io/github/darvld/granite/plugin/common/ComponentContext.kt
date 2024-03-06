package io.github.darvld.granite.plugin.common

import org.jetbrains.kotlin.config.CompilerConfiguration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Context for component type generation. Use the [issueComponentId] method to claim an ID value during generation.
 */
class ComponentContext(
  private val minComponentValue: Int,
  private val maxComponentValue: Int
) {
  /** Thread-safe handle for the next component ID. */
  private val nextId: AtomicInteger = AtomicInteger(minComponentValue)

  /**
   * Issue a new Component ID value, ensuring that it is unique and within the parameters specified by plugin options.
   * This method should be used during IR generation of synthetic property accessors.
   *
   * If all allowed values have been issued, an exception will be thrown.
   */
  fun issueComponentId(): Int = nextId.getAndIncrement().takeUnless { it > maxComponentValue } ?: error(
    "Maximum component ID value exceeded. Consider increasing the number of allocated IDs for this module."
  )

  companion object {
    /** Default minimum value for component IDs. */
    private const val DEFAULT_MIN_ID = 0

    /** Default maximum value for component IDs. */
    private const val DEFAULT_MAX_ID = Int.MAX_VALUE

    /**
     * Create a new [ComponentContext] using the provided [configuration] options. If the [PluginConfiguration] keys
     * are not present, default values will be used.
     *
     * @see PluginConfiguration
     */
    fun from(configuration: CompilerConfiguration): ComponentContext {
      return ComponentContext(
        minComponentValue = configuration[PluginConfiguration.MinComponentValue] ?: DEFAULT_MIN_ID,
        maxComponentValue = configuration[PluginConfiguration.MaxComponentValue] ?: DEFAULT_MAX_ID,
      )
    }
  }
}
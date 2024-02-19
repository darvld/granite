package io.github.darvld.granite.gradle

import org.gradle.api.provider.Property

/**
 * Extension used to configure the Granite compiler plugin. The extension is registered under the name `granite`, and
 * it allows setting the ID range for generated components among other options.
 */
abstract class GraniteExtension {
  /**
   * Sets the minimum value assigned as ID to a component. This is useful for segmenting components defined in a given
   * package/module, to avoid collisions when used as a library.
   *
   * @see maxComponentId
   */
  abstract val minComponentId: Property<Int>

  /**
   * Sets the maximum value assigned as ID to a component. This is useful for segmenting components defined in a given
   * package/module, to avoid collisions when used as a library.
   *
   * If the number of components in the compilation exceeds the unique ID count, a compilation error will be raised.
   *
   * @see minComponentId
   */
  abstract val maxComponentId: Property<Int>

  internal companion object {
    /** Name of the Granite extension. */
    internal const val EXTENSION_NAME = "granite"
  }
}
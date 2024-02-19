package io.github.darvld.granite.plugin.common

import org.jetbrains.kotlin.config.CompilerConfigurationKey

/** Configuration keys used by the plugin. */
object PluginConfiguration {
  /** Minimum ID value used for components. */
  val MinComponentValue = CompilerConfigurationKey<Int>("granite-min-component-id")

  /** Maximum ID value used for components. */
  val MaxComponentValue = CompilerConfigurationKey<Int>("granite-max-component-id")
}
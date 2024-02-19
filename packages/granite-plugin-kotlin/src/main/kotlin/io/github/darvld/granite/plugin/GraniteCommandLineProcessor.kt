package io.github.darvld.granite.plugin

import io.github.darvld.granite.plugin.BuildConfig
import io.github.darvld.granite.plugin.GraniteCommandLineProcessor.Companion.OPTION_MAX_ID
import io.github.darvld.granite.plugin.GraniteCommandLineProcessor.Companion.OPTION_MIN_ID
import io.github.darvld.granite.plugin.common.PluginConfiguration
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * Command line processor for the Granite plugin.
 *
 * The plugin accepts the following options:
 * - [min-component-id][OPTION_MIN_ID]: the minimum (integer) value used for generated component identifiers. Used to
 *   populate the [PluginConfiguration.MinComponentValue] key.
 * - [max-component-id][OPTION_MAX_ID]: the maximum (integer) value used for generated component identifiers. Used to
 *   populate the [PluginConfiguration.MaxComponentValue] key.
 *
 * @see GraniteComponentPluginRegistrar
 */
class GraniteCommandLineProcessor : CommandLineProcessor {
  override val pluginId: String = BuildConfig.KOTLIN_PLUGIN_ID

  override val pluginOptions: Collection<CliOption> = listOf(
    CliOption(
      optionName = OPTION_MIN_ID,
      valueDescription = "minimum component ID value",
      description = "minimum ID to be issued for generated components.",
      required = false,
      allowMultipleOccurrences = false,
    ),
    CliOption(
      optionName = OPTION_MAX_ID,
      valueDescription = "maximum component ID value",
      description = "maximum ID to be issued for generated components.",
      required = false,
      allowMultipleOccurrences = false,
    )
  )

  override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
    fun parseIntOrFail(value: String): Int = value.toIntOrNull() ?: error(
      "Invalid value for option ${option.optionName}: expected an integer, got $value"
    )

    when (option.optionName) {
      OPTION_MIN_ID -> configuration.put(PluginConfiguration.MinComponentValue, parseIntOrFail(value))
      OPTION_MAX_ID -> configuration.put(PluginConfiguration.MaxComponentValue, parseIntOrFail(value))
      else -> error("Unexpected plugin option ${option.optionName}")
    }
  }

  private companion object {
    /** Name of the CLI option used to configure the min component ID value. */
    private const val OPTION_MIN_ID = "min-component-id"

    /** Name of the CLI option used to configure the max component ID value. */
    private const val OPTION_MAX_ID = "max-component-id"
  }
}

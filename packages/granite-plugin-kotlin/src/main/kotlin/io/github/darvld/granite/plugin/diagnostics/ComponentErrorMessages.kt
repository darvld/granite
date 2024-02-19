package io.github.darvld.granite.plugin.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap

/**
 * Extension registering diagnostic messages for known errors.
 *
 * @see ComponentErrorDiagnostics
 */
object ComponentErrorMessages : DefaultErrorMessages.Extension {
  /** Static map instance configured at init-time, see [configureMessages] for entries. */
  private val rendererMap = DiagnosticFactoryToRendererMap("GranitePlugin").also(::configureMessages)

  /** Add entries to the [rendererMap], configuring messages for known diagnostics. */
  private fun configureMessages(map: DiagnosticFactoryToRendererMap) = with(map) {
    put(
      /* factory = */ ComponentErrorDiagnostics.INVALID_COMPONENT_TYPE_PARAMETER,
      /* message = */"Invalid component type marker: the companion object for a class " +
      "annotated with `@ComponentData` should have `ComponentType<T>` with the containing " +
      "class as type argument, or should otherwise not declare the interface."
    )
  }

  override fun getMap(): DiagnosticFactoryToRendererMap = rendererMap
}
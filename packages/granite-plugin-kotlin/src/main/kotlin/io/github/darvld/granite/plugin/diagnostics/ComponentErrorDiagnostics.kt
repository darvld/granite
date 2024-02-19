package io.github.darvld.granite.plugin.diagnostics

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity

/**
 * Diagnostic identifiers for errors related to component type generation. Messages for each diagnostic are registered
 * by the [ComponentErrorMessages] extension.
 *
 * @see ComponentErrorMessages
 */
object ComponentErrorDiagnostics {
  /**
   * Diagnostic raised when a companion object declares the `ComponentType` with a type other than the containing
   * `@ComponentData` class. In this situation, an additional compiler error will be raised due to inconsistent types.
   */
  @JvmField val INVALID_COMPONENT_TYPE_PARAMETER = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)

  init {
    Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages(
      ComponentErrorDiagnostics::class.java,
      ComponentErrorMessages
    )
  }
}
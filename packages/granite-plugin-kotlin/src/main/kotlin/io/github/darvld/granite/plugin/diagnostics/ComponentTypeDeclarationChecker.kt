package io.github.darvld.granite.plugin.diagnostics

import io.github.darvld.granite.plugin.resolve.ComponentTypeResolver
import io.github.darvld.granite.plugin.resolve.isComponentDataCompanion
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

/**
 * A declaration checker that detects incorrect `ComponentType` implementations and reports them.
 *
 * @see ComponentStorageContributor
 */
class ComponentTypeDeclarationChecker : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    if (descriptor !is ClassDescriptor || !descriptor.isComponentDataCompanion) return

    // report mismatching type arguments for ComponentType in companion objects
    if (!ComponentTypeResolver.validateComponentType(descriptor)) {
      context.trace.report(ComponentErrorDiagnostics.INVALID_COMPONENT_TYPE_PARAMETER.on(declaration))
    }
  }
}
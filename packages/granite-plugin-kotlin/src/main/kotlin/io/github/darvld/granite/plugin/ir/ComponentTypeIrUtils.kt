package io.github.darvld.granite.plugin.ir

import io.github.darvld.granite.plugin.common.ClassIds
import io.github.darvld.granite.plugin.common.QualifiedNames
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentAsClass

/**
 * Returns whether this [IrClass] is a companion object of a class annotated with `@ComponentData`.
 *
 * @see ComponentTypeIrVisitor
 */
val IrClass.isComponentDataCompanion: Boolean
  get() = isCompanion && parentAsClass.hasAnnotation(QualifiedNames.componentDataAnnotation)

/** Resolve a reference to the constructor of the `Component` value class. */
fun IrPluginContext.resolveComponentConstructor(): IrConstructorSymbol {
  return referenceClass(ClassIds.componentValueClass)?.constructors?.single() ?: error(
    "Failed to resolve constructor for the Component class"
  )
}

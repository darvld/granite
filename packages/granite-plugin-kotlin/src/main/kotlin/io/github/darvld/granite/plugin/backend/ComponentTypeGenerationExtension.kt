package io.github.darvld.granite.plugin.backend

import io.github.darvld.granite.plugin.common.ComponentContext
import io.github.darvld.granite.plugin.frontend.k1.ComponentTypeResolveExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * An extension used to generate a body for the synthetic `type` property added by the [ComponentTypeResolveExtension].
 *
 * The generated property returns a `Component` with an ID generated by the [ComponentContext], which ensures no
 * duplicate values are issued.
 *
 * The [ComponentTypeIrVisitor] is used to visit every declaration in a module and locate class declarations
 * corresponding to companion objects of `@ComponentData` classes.
 *
 * @see ComponentTypeResolveExtension
 * @see ComponentTypeIrVisitor
 */
open class ComponentTypeGenerationExtension(private val componentContext: ComponentContext) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    moduleFragment.accept(ComponentTypeIrVisitor(pluginContext), componentContext)
  }
}
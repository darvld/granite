package io.github.darvld.granite.plugin.backend

import io.github.darvld.granite.plugin.common.ComponentContext
import io.github.darvld.granite.plugin.common.Names
import io.github.darvld.granite.plugin.frontend.k1.ComponentTypeResolveExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * Element visitor that selects companion objects of classes annotated with `@ComponentData`, generating the synthetic
 * `type` property getter added by the resolve extension.
 *
 * Component IDs are assigned using a [ComponentContext] passed as data to this visitor. This ensures no duplicate IDs
 * will be issued and lifts the generation logic to a common backend.
 *
 * @see ComponentTypeResolveExtension
 * @see generateTypeProperty
 */
class ComponentTypeIrVisitor(private val pluginContext: IrPluginContext) : IrElementVisitor<Unit, ComponentContext> {
  override fun visitElement(element: IrElement, data: ComponentContext) {
    element.acceptChildren(this, data)
  }

  override fun visitClass(declaration: IrClass, data: ComponentContext) {
    if (declaration.isComponentDataCompanion) generateTypeProperty(declaration, data)
    visitElement(declaration, data)
  }

  /**
   * Locate the `type` synthetic property in the target companion object [declaration] and generate the getter. The
   * generated body will be the equivalent of:
   *
   * ```kotlin
   * val type: Component get() {
   *  return Component(<id>)
   * }
   * ```
   *
   * A component ID will be selected using the [context] to ensure unique values are used.
   */
  @OptIn(UnsafeDuringIrConstructionAPI::class)
  private fun generateTypeProperty(declaration: IrClass, context: ComponentContext) {
    // locate the synthetic property and its getter (both generated by the resolve extension)
    val typeProperty = declaration.properties.single { it.name == Names.componentTypeProperty }
    val propertyGetter = typeProperty.getter ?: error("expected type property to have a getter")

    // add a body in the form `{ return Component(<id>) }`
    propertyGetter.body = DeclarationIrBuilder(pluginContext, propertyGetter.symbol).irBlockBody {
      val constructorCall = irCallConstructor(pluginContext.resolveComponentConstructor(), emptyList())
      constructorCall.putValueArgument(0, irInt(context.issueComponentId()))

      +irReturn(constructorCall)
    }
  }
}
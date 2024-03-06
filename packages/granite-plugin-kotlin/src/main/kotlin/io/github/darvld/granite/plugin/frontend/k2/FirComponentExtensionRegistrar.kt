package io.github.darvld.granite.plugin.frontend.k2

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * A component used to register FIR extensions for the plugin, specifically:
 * - the [FirComponentGenerationExtension], which generates synthetic companion objects for component classes and
 *   and adds synthetic overrides for the `type` property, and
 * - the [FirComponentSupertypeExtension], which adds the synthetic `ComponentType<T>` superinterface to existing
 *   companion objects of a component class.
 *
 * See the main plugin extension registrar for an example on how to use this component with an adapter.
 */
class FirComponentExtensionRegistrar : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +::FirComponentSupertypeExtension
    +::FirComponentGenerationExtension
  }
}
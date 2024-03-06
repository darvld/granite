package io.github.darvld.granite.intellij

import io.github.darvld.granite.plugin.frontend.k2.FirComponentExtensionRegistrar

/**
 * Extension used to install the Granite FIR extension registrar into the Kotlin compiler used by the IDE. See the
 * documentation in the original component for details.
 *
 * @See FirComponentExtensionRegistrar
 */
class ComponentTypeIntellijFirExtensionRegistrar : FirComponentExtensionRegistrar()
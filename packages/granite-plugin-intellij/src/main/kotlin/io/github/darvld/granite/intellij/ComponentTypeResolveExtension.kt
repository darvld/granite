package io.github.darvld.granite.intellij

import io.github.darvld.granite.plugin.frontend.k1.ComponentTypeResolveExtension

/**
 * Extension used to install the Granite Synthetic Resolve extension into the Kotlin compiler used by the IDE. See the
 * documentation in the original extension for details.
 *
 * @See ComponentTypeResolveExtension
 */
open class ComponentTypeIntellijResolveExtension : ComponentTypeResolveExtension()

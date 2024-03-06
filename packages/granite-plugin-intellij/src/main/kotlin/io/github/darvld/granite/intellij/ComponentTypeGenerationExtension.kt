package io.github.darvld.granite.intellij

import io.github.darvld.granite.plugin.common.ComponentContext
import io.github.darvld.granite.plugin.backend.ComponentTypeGenerationExtension

/**
 * Extension used to install the Granite IR Generation extension into the Kotlin compiler used by the IDE. See the
 * documentation in the original extension for details.
 *
 * Instead of configurable component ID ranges, this extension uses the maximum available range, since generated
 * synthetic declarations are only used in the IDE.
 *
 * @See ComponentTypeGenerationExtension
 */
class ComponentTypeIntellijGenerationExtension : ComponentTypeGenerationExtension(ComponentContext(
  minComponentValue = Int.MIN_VALUE,
  maxComponentValue = Int.MAX_VALUE,
))

package io.github.darvld.granite.plugin.diagnostics

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform

/** An extension that registers the [ComponentTypeDeclarationChecker] as a module component. */
class ComponentStorageContributor : StorageComponentContainerContributor {
  override fun registerModuleComponents(
    container: StorageComponentContainer,
    platform: TargetPlatform,
    moduleDescriptor: ModuleDescriptor,
  ) {
    container.useInstance(ComponentTypeDeclarationChecker())
  }
}
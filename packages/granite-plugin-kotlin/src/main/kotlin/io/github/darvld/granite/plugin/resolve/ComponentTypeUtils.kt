package io.github.darvld.granite.plugin.resolve

import io.github.darvld.granite.plugin.common.QualifiedNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

/**
 * Returns whether this declaration corresponds to a class annotated with `@ComponentData`. If this value is `true`,
 * then the descriptor is guaranteed to be a [ClassDescriptor].
 */
val DeclarationDescriptor.isComponentDataClass: Boolean
  get() = this is ClassDescriptor && annotations.any { it.fqName == QualifiedNames.componentDataAnnotation }

/**
 * Returns whether this class declaration corresponds to a companion object of a class annotated with `@ComponentData`.
 * If this value is `true` then the descriptor is guaranteed to be a companion object.
 */
val ClassDescriptor.isComponentDataCompanion: Boolean
  get() = isCompanionObject && containingDeclaration.isComponentDataClass

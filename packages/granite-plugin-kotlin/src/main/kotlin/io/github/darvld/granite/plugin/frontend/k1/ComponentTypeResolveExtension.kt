package io.github.darvld.granite.plugin.frontend.k1

import io.github.darvld.granite.plugin.common.Names
import io.github.darvld.granite.plugin.backend.ComponentTypeGenerationExtension
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.types.KotlinType

/**
 * An extension tasked with adding synthetic companion objects to classes marked with `@ComponentData`.
 *
 * ### Generated synthetic declarations
 *
 * All companion objects of marked classes will also have `ComponentType<T>` (where T is the marked class) as a
 * synthetic superinterface, and will implement the `type` property with a generated component ID.
 *
 * The synthezised symbols can be merged in with user code: if an annotated class already has a companion object, only
 * the interface and property implementation will be added. If the companion already implemensts the interface, the
 * property will be implemented.
 *
 * Note that if a companion object of an annotated class already implements `ComponentType` with a different type
 * parameter, this will cause a compilation error as the synthetic superinterface with the correct type parameter will
 * still be added, creating an inconsistent declaration.
 *
 * ### Integration with other extensions
 *
 * The [ComponentTypeGenerationExtension] is responsible for generating the body of the synthetic `type` property in the
 * companions of annotated classes.
 *
 * ### Implementation details
 *
 * The [getSyntheticCompanionObjectNameIfNeeded] method returns a non-null name only for classes annotated with
 * the `@ComponentData` marker, causing the compiler to generate a synthetic companion object if none is found.
 *
 * For companion objects, [addSyntheticSupertypes] and [getSyntheticPropertiesNames] add the `ComponentType`
 * interface (with the correct type parameter), and the `type` property, respectively.
 *
 * The [generateSyntheticProperties] method generates a [PropertyDescriptor] for the synthetic `type` property.
 *
 * @see ComponentTypeGenerationExtension
 * @see ComponentTypeResolver
 */
open class ComponentTypeResolveExtension : SyntheticResolveExtension {
  override fun addSyntheticSupertypes(thisDescriptor: ClassDescriptor, supertypes: MutableList<KotlinType>) {
    // only process companion objects of @ComponentData classes
    if (!thisDescriptor.isComponentDataCompanion) return

    // if the object already implements ComponentType but with an incorrect type, abort;
    // the declaration checker will report the issue
    if (!ComponentTypeResolver.validateComponentType(thisDescriptor, supertypes)) return

    // add ComponentType<T> as super interface, where T is the class this is a companion of
    supertypes += ComponentTypeResolver.componentTypeInterfaceFor(thisDescriptor)
  }

  override fun generateSyntheticProperties(
    thisDescriptor: ClassDescriptor,
    name: Name,
    bindingContext: BindingContext,
    fromSupertypes: ArrayList<PropertyDescriptor>,
    result: MutableSet<PropertyDescriptor>
  ) {
    // only process the 'type' property for companion objects of @ComponentData classes
    if (name != Names.componentTypeProperty) return
    if (!thisDescriptor.isComponentDataCompanion) return

    result += ComponentTypeResolver.createComponentPropertyDescriptorFor(thisDescriptor)
  }

  override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? = when {
    // if this is a class marked with @ComponentData, generate a synthetic companion
    thisDescriptor.isComponentDataClass -> SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
    else -> null
  }

  override fun getSyntheticPropertiesNames(thisDescriptor: ClassDescriptor): List<Name> = when {
    // Companion objects for @ComponentData get a synthetic override for the 'type' property
    thisDescriptor.isComponentDataCompanion -> listOf(Names.componentTypeProperty)
    else -> emptyList()
  }
}

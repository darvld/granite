package io.github.darvld.granite.plugin.resolve

import io.github.darvld.granite.plugin.common.ClassIds
import io.github.darvld.granite.plugin.common.Names
import io.github.darvld.granite.plugin.common.QualifiedNames
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

/**
 * A helper class providing all the synthetic resolution logic.
 *
 * The [ComponentTypeResolveExtension] uses methods in this class to add custom property descriptors, resolve
 * specific types for synthetic companions, etc.
 *
 * @see ComponentTypeResolveExtension
 */
object ComponentTypeResolver {
  /**
   * Create and initialize a new [PropertyDescriptor] for the synthetic `type` property implementation of the
   * `ComponentType` interface.
   *
   * The [classDescriptor] should refer to the companion object of a class annotated with
   * `@ComponentData`. The getter of the returned property is added later by the IR generation extension.
   *
   * @see isComponentDataCompanion
   */
  fun createComponentPropertyDescriptorFor(classDescriptor: ClassDescriptor): PropertyDescriptor {
    return createPropertyDescriptor(
      name = Names.componentTypeProperty,
      type = resolveComponentClassIn(classDescriptor.module),
      target = classDescriptor,
    )
  }

  /**
   * Resolve and return a [KotlinType] representing the parameterized form of the `ComponentType` interface, where the
   * type parameter is set to the class containing the target [descriptor].
   *
   * The [descriptor] should refer to the companion object of a class annotated with `@ComponentData`, not to the class
   * declaration itself.
   *
   * @see isComponentDataCompanion
   */
  fun componentTypeInterfaceFor(descriptor: ClassDescriptor): KotlinType {
    val baseType = descriptor.module.findClassAcrossModuleDependencies(ClassIds.componentTypeInterface)?.defaultType
      ?: error("Unable to locate the ComponentType interface, check your module dependencies")


    val dataDescriptor = descriptor.containingDeclaration as ClassDescriptor
    val targetTypeProjection = dataDescriptor.defaultType.asTypeProjection()
    val typeWithArguments = KotlinTypeFactory.simpleType(baseType, arguments = listOf(targetTypeProjection))

    return typeWithArguments
  }

  /**
   * Validate a companion object [descriptor] as a ComponentType declaration. If the descriptor declares the
   * `ComponentType` interface with the containing class as type argument, or if it does not declare the interface,
   * this method returns `true`, otherwise it will return `false`.
   */
  fun validateComponentType(descriptor: ClassDescriptor): Boolean {
    return validateComponentType(descriptor, descriptor.defaultType.constructor.supertypes)
  }

  /**
   * Validate a companion object [descriptor] as a ComponentType declaration. If the descriptor's [superinterfaces]
   * contain the `ComponentType` interface with the containing class as type argument, or if it does not declare the
   * interface, this method returns `true`, otherwise it will return `false`.
   */
  fun validateComponentType(descriptor: ClassDescriptor, superinterfaces: Collection<KotlinType>): Boolean {
    val containingClass = descriptor.containingDeclaration as ClassDescriptor

    val superinterface = superinterfaces.find {
      it.constructor.declarationDescriptor?.fqNameSafe == QualifiedNames.componentTypeInterface
    } ?: return true

    // if an inconsistent type is detected, abort, the diagnostics will report it
    return superinterface.arguments.single().type == containingClass.defaultType
  }

  /**
   * Resolve the [KotlinType] corresponding to the `Component` value class, in a given [module]. This helper function
   * is used during synthesis of the `type` property for component type implementations.
   *
   * @see createComponentPropertyDescriptorFor
   */
  private fun resolveComponentClassIn(module: ModuleDescriptor): KotlinType {
    return module.findClassAcrossModuleDependencies(ClassIds.componentValueClass)?.defaultType
      ?: error("Unable to locate the Component class, check your module dependencies")
  }

  /**
   * Create and initialize a new [PropertyDescriptor] with the given [name] and return [type] for a specified [target]
   * class declaration.
   *
   * Sensible defaults are provided for all other parameters required by the property descriptor, according to the
   * requirements for synthetic `type` properties generated by this class.
   *
   * @see createComponentPropertyDescriptorFor
   */
  private fun createPropertyDescriptor(
    name: Name,
    type: KotlinType,
    target: ClassDescriptor,
    modality: Modality = Modality.FINAL,
    annotations: Annotations = Annotations.EMPTY,
    visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
    kind: CallableMemberDescriptor.Kind = CallableMemberDescriptor.Kind.SYNTHESIZED,
  ): PropertyDescriptor {
    // create a descriptor for the synthetic property
    val propertyDescriptor = PropertyDescriptorImpl.create(
      /* containingDeclaration = */ target,
      /* annotations = */ annotations,
      /* modality = */ modality,
      /* visibility = */ visibility,
      /* isVar = */ false,
      /* name = */ name,
      /* kind = */ kind,
      /* source = */ target.source,
      /* lateInit = */ false,  /* isConst = */ false, /* isExpect = */ false,
      /* isActual = */ false, /* isExternal = */ false, /* isDelegated = */ false,
    )

    propertyDescriptor.setType(
      /* outType = */ type,
      /* typeParameters = */ emptyList(),
      /* dispatchReceiverParameter = */ target.thisAsReceiverParameter,
      /* extensionReceiverParameter = */ null,
      /* contextReceiverParameters = */ emptyList(),
    )

    val getterDescriptor = PropertyGetterDescriptorImpl(
      /* correspondingProperty = */ propertyDescriptor,
      /* annotations = */ Annotations.EMPTY,
      /* modality = */ Modality.FINAL,
      /* visibility = */ DescriptorVisibilities.PUBLIC,
      /* isDefault = */ false,
      /* isExternal = */ false,
      /* isInline = */ false,
      /* kind = */ CallableMemberDescriptor.Kind.SYNTHESIZED,
      /* original = */ null,
      /* source = */ target.source,
    ).apply {
      initialize(type)
    }

    propertyDescriptor.initialize(
      /* getter = */ getterDescriptor,
      /* setter = */ null,
    )

    return propertyDescriptor
  }
}
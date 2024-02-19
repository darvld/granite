package io.github.darvld.granite.plugin.common

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Name constants used for type resolution. If a [Name] or [FqName] is needed, don't use these values to create them,
 * use [Names] and [QualifiedNames] instead.
 *
 * @see Names
 * @see QualifiedNames
 * @see ClassIds
 */
object Constants {
  /**
   * Name for the root package of the Granite API.
   *
   * @see QualifiedNames.rootPackage
   */
  const val PACKAGE_NAME: String = "io.github.darvld.granite"

  /**
   * Name of the annotation used to mark classes for transformation.
   *
   * @see Names.componentDataAnnotation
   * @see QualifiedNames.componentDataAnnotation
   */
  const val COMPONENT_DATA_ANNOTATION: String = "ComponentData"

  /**
   * Name of the marker interface used for companion objects of component classes.
   *
   * @see Names.componentTypeInterface
   * @see QualifiedNames.componentTypeInterface
   * @see ClassIds.componentTypeInterface
   */
  const val COMPONENT_TYPE_INTERFACE: String = "ComponentType"

  /**
   * Name of the Component value class.
   *
   * @see Names.componentValueClass
   * @see QualifiedNames.componentValueClass
   * @see ClassIds.componentValueClass
   */
  const val COMPONENT_VALUE_CLASS: String = "Component"

  /**
   * Name of the property to override in the [COMPONENT_TYPE_INTERFACE].
   *
   * @see Names.componentTypeProperty
   */
  const val COMPONENT_TYPE_PROPERTY: String = "type"
}

/**
 * Identifiers for common types and symbols used by the plugin. Every value is constructed from an entry in the
 * [Constants]. For [FqName] variants, see [QualifiedNames].
 *
 * @see Constants
 * @see QualifiedNames
 * @see ClassIds
 */
object Names {
  /**
   * Identifier [Name] for the Component value class, built from [Constants.COMPONENT_VALUE_CLASS].
   *
   * @see Constants.COMPONENT_VALUE_CLASS
   * @see QualifiedNames.componentValueClass
   * @see ClassIds.componentValueClass
   */
  val componentValueClass: Name = Name.identifier(Constants.COMPONENT_VALUE_CLASS)

  /**
   * Identifier [Name] for the @ComponentData annotation, built from [Constants.COMPONENT_DATA_ANNOTATION].
   *
   * @see Constants.COMPONENT_DATA_ANNOTATION
   * @see QualifiedNames.componentDataAnnotation
   */
  val componentDataAnnotation: Name = Name.identifier(Constants.COMPONENT_DATA_ANNOTATION)

  /**
   * Identifier [Name] for the component type marker interface, built from [Constants.COMPONENT_TYPE_INTERFACE].
   *
   * @see Constants.COMPONENT_TYPE_INTERFACE
   * @see QualifiedNames.componentTypeInterface
   * @see ClassIds.componentTypeInterface
   */
  val componentTypeInterface: Name = Name.identifier(Constants.COMPONENT_TYPE_INTERFACE)

  /**
   * Identifier [Name] for the synthetic component type property, built from [Constants.COMPONENT_TYPE_PROPERTY].
   *
   * @see Constants.COMPONENT_TYPE_PROPERTY
   */
  val componentTypeProperty: Name = Name.identifier(Constants.COMPONENT_TYPE_PROPERTY)
}

/**
 * [Fully qualified][FqName] names for common types. See [Names] for simple variants.
 *
 * @see Constants
 * @see ClassIds
 * @see Names
 */
object QualifiedNames {
  /**
   * Fully-qualified name for the root package containing the Granite API.
   *
   * @see Constants.PACKAGE_NAME
   */
  val rootPackage: FqName = FqName(Constants.PACKAGE_NAME)

  /**
   * Fully-qualified name for the Component value class.
   *
   * @see Constants.COMPONENT_VALUE_CLASS
   * @see Names.componentValueClass
   * @see ClassIds.componentValueClass
   */
  val componentValueClass: FqName = rootPackage.child(Names.componentValueClass)

  /**
   * Fully-qualified name for the @ComponentData annotation.
   *
   * @see Constants.COMPONENT_DATA_ANNOTATION
   * @see Names.componentDataAnnotation
   */
  val componentDataAnnotation: FqName = rootPackage.child(Names.componentDataAnnotation)

  /**
   * Fully-qualified name for the component type marker interface.
   *
   * @see Constants.COMPONENT_TYPE_INTERFACE
   * @see Names.componentTypeInterface
   * @see ClassIds.componentTypeInterface
   */
  val componentTypeInterface: FqName = rootPackage.child(Names.componentTypeInterface)
}

/**
 * Well-known [ClassId]s for public API types used by the plugin.
 *
 * @see Names
 * @see QualifiedNames
 * @see Constants
 */
object ClassIds {
  /**
   * Class ID for the component value class.
   *
   * @see Constants.COMPONENT_VALUE_CLASS
   * @see Names.componentValueClass
   * @see QualifiedNames.componentValueClass
   */
  val componentValueClass = ClassId(QualifiedNames.rootPackage, Names.componentValueClass)

  /**
   * Class ID for the component type marker interface.
   *
   * @see Constants.COMPONENT_TYPE_INTERFACE
   * @see Names.componentTypeInterface
   * @see QualifiedNames.componentTypeInterface
   */
  val componentTypeInterface = ClassId(QualifiedNames.rootPackage, Names.componentTypeInterface)
}
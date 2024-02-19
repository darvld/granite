package io.github.darvld.granite

/**
 * Designates a class as a component data holder.
 *
 * The Granite compiler plugin will detect classes with this annotation and generate a synthetic companion object
 * implementing [ComponentType]. This allows using any class annotated with [ComponentData] in functions that
 * manage components without writing the boilerplate code manually.
 *
 * If the annotated class already has a companion object, the compiler plugin will add [ComponentType] as
 * superinterface and implement the component field. If the existing object already implements [ComponentType], the
 * property accessor will be replaced by a synthetic getter.
 *
 * @see ComponentType
 */
@Target(AnnotationTarget.CLASS) public annotation class ComponentData
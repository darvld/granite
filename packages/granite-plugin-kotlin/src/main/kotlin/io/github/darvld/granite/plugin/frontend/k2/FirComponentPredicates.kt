package io.github.darvld.granite.plugin.frontend.k2

import io.github.darvld.granite.plugin.common.QualifiedNames
import io.github.darvld.granite.plugin.frontend.k2.FirComponentPredicates.annotatedWithComponentData
import io.github.darvld.granite.plugin.frontend.k2.FirComponentPredicates.registerWith
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate

/**
 * A collection of FIR predicates used to match types based on their annotations. Use [registerWith] inside a FIR
 * extension's registrar method to enable the predicates for that extension.
 *
 * The [annotatedWithComponentData] predicate can be used to find classes annotated with `@ComponentData`.
 */
object FirComponentPredicates {
  /**
   * A predicate matching types annotated with `@ComponentData`. The predicate must be registered with an extension
   * before it can be used.
   *
   * @see registerWith
   */
  val annotatedWithComponentData = LookupPredicate.create { annotated(QualifiedNames.componentDataAnnotation) }

  fun registerWith(registrar: FirDeclarationPredicateRegistrar) {
    registrar.register(annotatedWithComponentData)
  }
}
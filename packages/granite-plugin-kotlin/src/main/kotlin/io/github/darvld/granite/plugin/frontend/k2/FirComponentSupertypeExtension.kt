package io.github.darvld.granite.plugin.frontend.k2

import io.github.darvld.granite.plugin.common.ClassIds
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId

/**
 * An extension which generates synthetic supertypes for existing companion objects of `@ComponentData` classes.
 *
 * The object's supertypes are requested to be transformed by [needTransformSupertypes], and updated by
 * [computeAdditionalSupertypes]. The general procedure for supertype generation is equal to that of fully synthetic
 * component type companions, as performed by the [FirComponentGenerationExtension].
 */
class FirComponentSupertypeExtension(session: FirSession) : FirSupertypeGenerationExtension(session) {
  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    FirComponentPredicates.registerWith(this)
  }

  context(TypeResolveServiceContainer) override fun computeAdditionalSupertypes(
    classLikeDeclaration: FirClassLikeDeclaration,
    resolvedSupertypes: List<FirResolvedTypeRef>
  ): List<FirResolvedTypeRef> {
    // ignore the declaration if it already has the ComponentType interface as supertype
    if (resolvedSupertypes.any { it.type.classId == ClassIds.componentTypeInterface }) return emptyList()

    if (session.isComponentDataCompanion(classLikeDeclaration.symbol)) {
      val container = classLikeDeclaration.getContainingClassSymbol(session)
      check(container is FirClassSymbol<*>) { "Companion object must be contained by a regular class" }

      // build the superinterface using the companion's container class as type argument
      return listOf(buildResolvedTypeRef { type = firComponentInterfaceType(container) })
    }

    return emptyList()
  }

  override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
    return session.isComponentDataCompanion(declaration.symbol)
  }
}
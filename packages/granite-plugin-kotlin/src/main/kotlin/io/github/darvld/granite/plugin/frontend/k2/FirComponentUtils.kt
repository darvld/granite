package io.github.darvld.granite.plugin.frontend.k2

import io.github.darvld.granite.plugin.common.ClassIds
import io.github.darvld.granite.plugin.frontend.k2.FirComponentPredicates.annotatedWithComponentData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.toTypeProjection
import org.jetbrains.kotlin.types.Variance

/**
 * Checks whether the provided [symbol] corresponds to a regular class annotated with `@ComponentData`.
 *
 * **Important:** For this check to work properly, the [annotatedWithComponentData] predicate must be registered in
 * the extension, otherwise the result will always be `false`.
 */
fun FirSession.isComponentDataClass(symbol: FirClassLikeSymbol<*>): Boolean {
  return symbol is FirRegularClassSymbol && predicateBasedProvider.matches(annotatedWithComponentData, symbol)
}

/**
 * Checks whether the provided [symbol] corresponds to a companion object of a class annotated with `@ComponentData`.
 *
 * **Important:** For this check to work properly, the [annotatedWithComponentData] predicate must be registered in
 * the extension, otherwise the result will always be `false`.
 */
fun FirSession.isComponentDataCompanion(symbol: FirClassLikeSymbol<*>): Boolean {
  return symbol.isCompanion && symbol.getContainingClassSymbol(this)?.let { isComponentDataClass(it) } == true
}

/**
 * Constructs a cone type for the `ComponentType<T>` interface, using the specified [owner] symbol as type parameter.
 *
 * For example, if the [owner] corresponds to a declaration like `class Foo` the returned cone type will be equivalent
 * to `ComponentType<Foo>`.
 */
fun firComponentInterfaceType(owner: FirClassSymbol<*>): ConeClassLikeType {
  return ClassIds.componentTypeInterface.constructClassLikeType(
    typeArguments = arrayOf(owner.defaultType().toTypeProjection(Variance.INVARIANT)),
    isNullable = false,
  )
}
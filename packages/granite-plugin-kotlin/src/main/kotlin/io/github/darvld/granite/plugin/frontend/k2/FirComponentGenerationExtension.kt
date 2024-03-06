package io.github.darvld.granite.plugin.frontend.k2

import io.github.darvld.granite.plugin.common.ClassIds
import io.github.darvld.granite.plugin.common.Names
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/**
 * An extension which generates synthetic companion objects for classes annotated with `@ComponentData`, and adds
 * synthetic overrides for the `type` property in elegible companion objects.
 *
 * ### Component class matching
 *
 * To detect which class declarations are annotated with `@ComponentData`, the [FirComponentPredicates] helpers are
 * used. A FIR predicate can be used with the compiler frontend to detect classes with certain annotations, and must
 * be registered manually in the [registerPredicates] method.
 *
 * Note that using predicates is the only way to determine whether a class has the target annotation, unlike in K1
 * synthetic resolution extensions (in which the class descriptor provides the annnotation info).
 *
 * ### Synthetic generation
 *
 * There are two phases to synthetic generation in the frontend:
 *
 * In the first phase, the extension uses [getNestedClassifiersNames] and [getCallableNamesForClass] to provide the
 * compiler with a list of [names][Name] for synthetic declarations. During this step the extension will select which
 * classes are candidates for synthetic companion generation by looking for the `@ComponentData` annotation.
 *
 * Then, the compiler will ask the extension to generate the descriptors for the names specified in the first phase.
 * See [generateConstructors], [generateProperties], and [generateNestedClassLikeDeclaration] for details, these
 * methods construct the information (the equivalent of K1 descriptors) about the synthetic declarations.
 */
class FirComponentGenerationExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {
  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    FirComponentPredicates.registerWith(this)
  }

  override fun getNestedClassifiersNames(
    classSymbol: FirClassSymbol<*>,
    context: NestedClassGenerationContext
  ): Set<Name> {
    // generate a synthetic companion object for @ComponentData classes
    if (session.isComponentDataClass(classSymbol)) return setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
    return emptySet()
  }

  override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext
  ): Set<Name> {
    if (!session.isComponentDataCompanion(classSymbol)) return emptySet()

    // the <init> block must also be added for synthetic companion objects, otherwise no constructor
    // can be generated and compilation will fail (companion objects must have a private constructor)
    return setOf(SpecialNames.INIT, Names.componentTypeProperty)
  }

  override fun generateProperties(
    callableId: CallableId,
    context: MemberGenerationContext?
  ): List<FirPropertySymbol> {
    val owner = context?.owner ?: return emptyList()

    if (!session.isComponentDataCompanion(owner)) return emptyList()
    if (callableId.callableName != Names.componentTypeProperty) return emptyList()

    // generate the descriptor for the 'type' property, the backend will detect this property
    // and add an IR body to the property getter
    val prop = createMemberProperty(
      owner = owner,
      key = ComponentPluginKey,
      name = callableId.callableName,
      returnTypeProvider = { ClassIds.componentValueClass.constructClassLikeType(emptyArray(), false) },
    )

    return listOf(prop.symbol)
  }

  override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
    if (!session.isComponentDataCompanion(context.owner)) return emptyList()

    // generate a default constructor for the synthetic companion object, otherwise compilation will fail
    return listOf(createDefaultPrivateConstructor(context.owner, ComponentPluginKey).symbol)
  }

  override fun generateNestedClassLikeDeclaration(
    owner: FirClassSymbol<*>,
    name: Name,
    context: NestedClassGenerationContext
  ): FirClassLikeSymbol<*>? {
    // only generate a companion object for a regular class annotated with @ComponentData
    // that doesn't already have a companion
    if (name != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) return null
    if (owner !is FirRegularClassSymbol || owner.companionObjectSymbol != null) return null
    if (!session.isComponentDataClass(owner)) return null

    return createCompanionObject(owner, ComponentPluginKey) {
      superType(firComponentInterfaceType(owner))
    }.symbol
  }
}

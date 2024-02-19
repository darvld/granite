package io.github.darvld.granite

import io.github.darvld.granite.EntityQuery.Clause.Exclude
import io.github.darvld.granite.EntityQuery.Clause.Include
import io.github.darvld.granite.EntityQuery.Companion.selectEntities

/**
 * Entity queries are used to quickly select entities that match a specific criteria.
 *
 * Clauses in a query are sorted according to their subject component, allowing every check to be performed in a single
 * iteration over the target signature.
 *
 * Queries can be stored after creation and reused as many times as necessary. See [EngineScope.forEach] for a
 * practical use case of entity queries.
 *
 * @see selectEntities
 */
@JvmInline public value class EntityQuery private constructor(private val clauses: Array<Clause>) {
  /**
   * Represents a component selection clause, used during entity queries.
   *
   * When deciding whether a query [matches] a target signature, [clauses] may [Include] or [Exclude] a specific
   * component, serving as criteria for the query result.
   *
   * Note that the [clauses] array is ordered by component ID to match the types in a [Signature], meaning a single
   * iteration over the target signature will be enough to detect a match.
   *
   * @see Builder
   */
  private sealed interface Clause {
    /** The [Component] referenced by this clause. */
    val subject: Component

    /** Clause matching signatures that include a specific [subject]. */
    @JvmInline value class Include(override val subject: Component) : Clause

    /** Clause matching signatures that do not include a specific [subject]. */
    @JvmInline value class Exclude(override val subject: Component) : Clause
  }

  /**
   * A builder DSL used to form [entity queries][EntityQuery]. Using this class, queries can be configured to match
   * signatures [with] and [without] specific components.
   *
   * @see EntityQuery
   */
  @JvmInline public value class Builder internal constructor(
    /** Clauses for the query, indicating whether certain components should be present or absent. */
    private val clauses: MutableMap<Component, Boolean> = mutableMapOf()
  ) {

    /** Select entities only if they include a specific [component]. */
    public fun with(component: Component): Builder = apply {
      clauses[component] = true
    }

    /** Select entities only if they don't include a specific [component]. */
    public fun without(component: Component): Builder = apply {
      clauses[component] = false
    }

    /**
     * Compile a query builder and obtain a prepared [EntityQuery]. Component clauses are sorted and translated to
     * internal clause wrappers.
     *
     * @see selectEntities
     */
    internal fun build(): EntityQuery {
      // compile the map into an array
      val compiledClauses = clauses.map { (component, include) ->
        if (include) Include(component) else Exclude(component)
      }.toTypedArray()

      // clauses must be sorted to ensure optimal performance during queries
      compiledClauses.sortBy { it.subject.id }
      return EntityQuery(compiledClauses)
    }
  }

  /**
   * Returns whether this query matches the specified [signature], meaning it satisfies every one of its clauses.
   *
   * This check is always performed in a single iteration over the signature since the query's clauses have the same
   * order as the signature types.
   *
   * @see Clause
   */
  internal infix fun matches(signature: Signature): Boolean {
    // index for the query and the signature match at the start, they may differ
    // if a clause subject does not match the current type, in which case the type
    // index will increase but the clause index will remain the same
    var currentClause = 0
    var currentType = 0

    // matching sample
    // -5 +7 +10 -16 +21 -25 -68
    //  2  7  10  17  21

    // non-matching sample
    // -5 +7 +10 [-16] +21
    //  2  7  10 [ 16]  21

    // alternative non-matching sample
    // -5 +7 +10 -16 [+21]
    //  2  7  10  13 [ 25]

    while (currentClause < clauses.size) {
      val clause = clauses[currentClause]

      // we have exhausted the types in the signature
      if (currentType >= signature.size) {
        // any remaining inclusive clauses will fail now
        if (clause is Include) return false

        // but if all we have are excluding clauses, they may yet match
        currentClause++
        continue
      }

      // compare the current value in the signature with the subject of the current clause
      when (signature[currentType].id.compareTo(clause.subject.id)) {
        // subject is not present in the signature; since both arrays are sorted, and the
        // target type is larger than the clause subject, the subject will never be found
        // in this array (and neither will any other clause subjects)
        1 -> {
          // if we needed the component to be present, the check fails
          if (clause is Include) return false

          // but if the component did not need to be present, we can move on to the next
          // clause, *without* increasing the type index (otherwise we might skip a match
          // for the next clause)
          currentClause++
        }

        // a direct match, this will always increase the clause index; no other clause may
        // have the same subject, so it is also safe to increase the type index
        0 -> {
          // since this is a match, fail if we did not want the type to be present
          if (clause is Exclude) return false

          currentClause++
          currentType++
        }

        // otherwise, we may yet find the subject for this clause in another entry, and since
        // clauses are sorted by subject, we can guarantee that no other clause will match
        // until this one does; increase the type index and try again
        -1 -> currentType++
      }
    }

    return true
  }

  public companion object {
    /**
     * Construct a new [EntityQuery] using a [builder] function.
     *
     * Use the [with][Builder.with] and [without][Builder.without] methods in the builder to include and exclude
     * components from the query, respectively.
     *
     * @see EntityQuery
     */
    public fun selectEntities(builder: Builder.() -> Unit): EntityQuery {
      return Builder().apply(builder).build()
    }
  }
}

/** Select entities only if they include a specific [component]. */
public fun EntityQuery.Builder.with(component: ComponentType<*>) {
  with(component.type)
}

/** Select entities only if they don't include a specific [component]. */
public fun EntityQuery.Builder.without(component: ComponentType<*>) {
  without(component.type)
}
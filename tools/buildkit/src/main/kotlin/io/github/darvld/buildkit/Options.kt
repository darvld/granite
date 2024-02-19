package io.github.darvld.buildkit

import org.gradle.api.Project

/**
 * Resolve a project option by [name], throwing an exception if not found. The [name] will be automatically
 * prefixed with the project namespace (`granite.<name>`).
 *
 * @see optionEnabled
 */
public fun Project.option(name: String): String {
  return findProperty("granite.$name") as? String ?: error("Option granite.$name is not set")
}

/**
 * Resolve a project option by [name], returning null if not found. The [name] will be automatically
 * prefixed with the project namespace (`granite.<name>`).
 *
 * @see optionEnabled
 */
public fun Project.optionOrNull(name: String): String? {
  return findProperty("granite.$name") as? String
}

/**
 * Resolve a project option by [name], returning a [default] value if not found. The [name] will be automatically
 * prefixed with the project namespace (`granite.<name>`).
 *
 * @see optionEnabled
 */
public fun Project.option(name: String, default: String): String {
  return findProperty("granite.$name") as? String ?: default
}

/**
 * Resolve a project option by [name] and parse it as a boolean, or returning a [default] value if not found. The
 * [name] will be automatically prefixed with the project namespace (`granite.<name>`).
 *
 * Boolean parsing is performed using [String.toBooleanStrictOrNull].
 */
public fun Project.optionEnabled(name: String, default: Boolean = false): Boolean {
  return option(name, default.toString()).toBooleanStrictOrNull() ?: default
}

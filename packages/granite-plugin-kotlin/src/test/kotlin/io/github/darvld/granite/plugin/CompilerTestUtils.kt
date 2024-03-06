package io.github.darvld.granite.plugin

import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import kotlin.test.assertEquals

/** Returns a [ClassLoader] capable of loading the compiled classes for JVM compilations. */
val CompilationResult.classLoader: ClassLoader
  get() = (this as JvmCompilationResult).classLoader

/**
 * Prepare a [KotlinCompilation] with the [GraniteComponentPluginRegistrar], using a string [source] as part of a
 * virtual "main.kt" file. The compilation will use sensible defaults for options like useIR.
 *
 * @see compile
 */
fun prepareCompilation(@Language("kotlin") source: String): KotlinCompilation {
  return KotlinCompilation().apply {
    sources = listOf(SourceFile.kotlin("main.kt", source))
    compilerPluginRegistrars = listOf(GraniteComponentPluginRegistrar())
    inheritClassPath = true
  }
}

/**
 * Prepare and compile the given [source], returning the compilation result. The source will be prepared using
 * [prepareCompilation].
 *
 * @see assertCompiles
 */
fun compile(@Language("kotlin") source: String): CompilationResult {
  return prepareCompilation(source).compile()
}

/**
 * [Prepare][prepareCompilation] and [compile] the given [source], asserting that the result is
 * [KotlinCompilation.ExitCode.OK].
 *
 * @see assertValidComponent
 */
fun assertCompiles(
  @Language("kotlin") source: String,
  message: String = "expected compilation to succeed",
): CompilationResult {
  val result = compile(source)
  assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, message)

  return result
}

/**
 * [Prepare][prepareCompilation] and [compile] the given [source], asserting that the result is
 * [KotlinCompilation.ExitCode.COMPILATION_ERROR].
 *
 * @see assertValidComponent
 */
fun assertDoesNotCompile(
  @Language("kotlin") source: String,
  message: String = "expected compilation to succeed",
): CompilationResult {
  val result = compile(source)
  assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, message)

  return result
}


/**
 * Asserts that the given [declaration] is an invalid `@ComponentData`. The [symbol] indicates the name of the
 * class to be used as component data.
 *
 * @see assertCompiles
 */
fun assertInvalidComponent(symbol: String, @Language("kotlin") declaration: String): CompilationResult {
  return assertDoesNotCompile(
    source = prepareComponentDeclarationSource(symbol, declaration),
    message = "expected declaration to be an invalid component."
  )
}

/**
 * Asserts that the given [declaration] works as a valid `@ComponentData`. The [symbol] indicates the name of the
 * class to be used as component data.
 *
 * @see assertCompiles
 */
fun assertValidComponent(symbol: String, @Language("kotlin") declaration: String): CompilationResult {
  // verify compile-time correctness of the generated declarations
  val result = assertCompiles(
    source = prepareComponentDeclarationSource(symbol, declaration),
    message = "expected declaration to be a valid component."
  )

  // verify that the component() function works as intended at runtime
  result.classLoader.loadClass("MainKt").getMethod("component").invoke(null)

  return result
}

private fun prepareComponentDeclarationSource(symbol: String, @Language("kotlin") declaration: String): String {
  return """
    import io.github.darvld.granite.ComponentData
    import io.github.darvld.granite.ComponentType
    import io.github.darvld.granite.Component

    // symbol under test begins
    $declaration
    // symbol under test ends

    // entrypoint for assertions
    fun component(): Component {
      return getComponentType($symbol)
    }

    // superinterface validation
    fun getComponentType(component: ComponentType<$symbol>): Component {
      return component.type
    }
  """
}
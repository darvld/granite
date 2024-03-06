package io.github.darvld.granite.plugin

import org.junit.Test

internal class PluginIntegrationTest {
  @Test fun `should add synthetic companion`() {
    assertValidComponent(
      symbol = "Position",
      declaration = """
      @ComponentData class Position(val x: Float, val y: Float)
      """
    )
  }

  @Test fun `should add synthetic interface and property to existing companion`() {
    assertValidComponent(
      symbol = "Position",
      declaration = """
      @ComponentData class Position(val x: Float, val y: Float) {
        companion object
      }
      """
    )
  }

  @Test fun `should add synthetic property to existing ComponentType companion`() {
    assertValidComponent(
      symbol = "Position",
      declaration = """
      @ComponentData class Position(val x: Float, val y: Float) {
        companion object : ComponentType<Position>
      }
      """
    )
  }

  @Test fun `should fail for inconsistent component type argument`() {
    assertInvalidComponent(
      symbol = "Position",
      declaration = """
      @ComponentData class Position(val x: Float, val y: Float) {
        companion object : ComponentType<Int>
      }
      """
    )
  }
}
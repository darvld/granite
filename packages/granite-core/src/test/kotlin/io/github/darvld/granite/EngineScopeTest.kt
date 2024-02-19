package io.github.darvld.granite

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class EngineScopeTest {
  /** Stub component type used in tests. */
  private object Position : ComponentType<Int> {
    override val type = Component(0)
  }

  /** Instance under test. */
  private val engine = Engine()

  @Test fun `should defer entity creation`() = runTest {
    engine.step {
      for (i in 0 until 50) assertFalse(
        actual = engine.exists(newEntity()),
        message = "expected entity not to exist yet"
      )
    }

    for (i in 0 until 50) assertTrue(
      actual = engine.exists(Entity(i)),
      message = "expected entity to exist"
    )
  }

  @Test fun `should defer entity removal`() = runTest {
    for (i in 0 until 50) assertTrue(
      actual = engine.exists(engine.newEntity()),
      message = "expected entity to exist"
    )

    engine.step {
      for (i in 0 until 50) assertTrue(
        actual = engine.exists(Entity(i).also { destroy(it) }),
        message = "expected entity to still exist"
      )
    }

    for (i in 0 until 50) assertFalse(
      actual = engine.exists(Entity(i)),
      message = "expected entity not to exist anymore"
    )
  }

  @Test fun `should defer component insertion`() = runTest {
    for (i in 0 until 50) engine.newEntity()

    engine.step {
      for (i in 0 until 50) {
        val entity = Entity(i)
        entity.add(Position, 0)

        assertFalse(
          actual = entity.contains(Position),
          message = "expected entity to not have component yet"
        )
      }
    }

    for (i in 0 until 50) assertTrue(
      actual = engine.hasComponent(Entity(i), Position.type),
      message = "expected entity to have component"
    )
  }

  @Test fun `should defer component removal`() = runTest {
    for (i in 0 until 50) engine.newEntity().also { engine.addComponent(it, Position.type, 0) }

    engine.step {
      for (i in 0 until 50) {
        val entity = Entity(i)
        entity.remove(Position)

        assertTrue(
          actual = entity.contains(Position),
          message = "expected entity to still have component"
        )
      }
    }

    for (i in 0 until 50) assertFalse(
      actual = engine.hasComponent(Entity(i), Position.type),
      message = "expected entity not to have component anymore"
    )
  }

  @Test fun `should defer component update`() = runTest {
    for (i in 0 until 50) engine.newEntity().also { engine.addComponent(it, Position.type, 0) }

    engine.step {
      for (i in 0 until 50) {
        val entity = Entity(i)
        entity.set(Position, 1)

        assertEquals(
          expected = 0,
          actual = entity.get(Position),
          message = "expected component to still have initial value"
        )
      }
    }

    for (i in 0 until 50) assertEquals(
      expected = 1,
      actual = engine.getComponent(Entity(i), Position.type),
      message = "expected component to have updated value"
    )
  }
}
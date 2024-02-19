package io.github.darvld.granite

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.fail

internal class EngineTest {
  /** Sample component used in ECS test cases. */
  private data class Position(val value: Int = Random.nextInt()) {
    companion object {
      /** A stable ID for this component. */
      val component = Component(0)
    }
  }

  /** Engine instance to be used in tests. */
  private val engine = Engine()

  @Test fun `should create entity`() {
    assertDoesNotThrow("should not throw when creating entity") {
      engine.newEntity()
    }
  }

  @Test fun `should destroy entity`() {
    val entity = engine.newEntity()

    val position = Position()
    engine.addComponent(entity, Position.component, position)

    assertEquals(
      expected = position,
      actual = engine.getComponent(entity, Position.component),
      message = "expected component value to be set"
    )

    assertDoesNotThrow("should not throw when destroying entity") {
      engine.destroyEntity(entity)
    }

    assertDoesNotThrow("should ignore redundant 'destroy' call") {
      engine.destroyEntity(entity)
    }

    assertThrows<IllegalStateException>("should reject destroyed entity") {
      engine.getComponent(entity, Position.component)
    }
  }

  @Test fun `should add component to entity`() {
    val entity = engine.newEntity()

    val position = Position()
    engine.addComponent(entity, Position.component, position)

    assertEquals(
      expected = position,
      actual = engine.getComponent(entity, Position.component),
      message = "expected component value to be set"
    )
  }

  @Test fun `should remove component from entity`() {
    val entity = engine.newEntity()

    val position = Position()
    engine.addComponent(entity, Position.component, position)

    assertEquals(
      expected = position,
      actual = engine.getComponent(entity, Position.component),
      message = "expected component value to be set"
    )

    engine.removeComponent(entity, Position.component)

    assertThrows<IllegalStateException>("should reject component not present in entity") {
      engine.getComponent(entity, Position.component)
    }
  }

  @Test fun `should set component value for entity`() {
    val entity = engine.newEntity()

    val position = Position()
    engine.addComponent(entity, Position.component, position)

    assertEquals(
      expected = position,
      actual = engine.getComponent(entity, Position.component),
      message = "expected component value to be set"
    )

    val newPosition = Position()
    engine.setComponent(entity, Position.component, newPosition)

    assertEquals(
      expected = newPosition,
      actual = engine.getComponent(entity, Position.component),
      message = "expected component value to be updated"
    )
  }

  @Test fun `should add component to entity if not present`() {
    val entity = engine.newEntity()

    val position = Position()
    engine.setComponent(entity, Position.component, position)

    assertEquals(
      expected = position,
      actual = engine.getComponent(entity, Position.component),
      message = "expected component value to be set"
    )
  }

  @Test fun `should get component value for entity`() {
    val entity = engine.newEntity()

    val position = Position()
    engine.addComponent(entity, Position.component, position)

    assertEquals(
      expected = position,
      actual = engine.getComponent(entity, Position.component),
      message = "expected component value to be set"
    )
  }

  @Test fun `should reject concurrent steps`() = runTest {
    val firstBarrier = CompletableDeferred<Unit>()
    val secondBarrier = CompletableDeferred<Unit>()

    launch {
      engine.step {
        firstBarrier.complete(Unit)
        secondBarrier.await()
      }
    }

    launch {
      firstBarrier.await()
      assertFails("expected concurrent steps to be forbidden") {
        engine.step { fail("should not allow concurrent steps") }
      }
      secondBarrier.complete(Unit)
    }

    secondBarrier.await()
  }
}

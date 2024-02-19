package io.github.darvld.granite

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TableIndexTest {
  /** Index instance to be used in tests. */
  private val index = TableIndex()

  @Test fun `should resolve table with component`() {
    val component = Component(1)

    val table = assertDoesNotThrow("expected table to be created") {
      index.resolveWithComponent(index.emptyTable, component)
    }

    assertTrue(component in table.signature, "expected new table signature to include component")
  }

  @Test fun `should resolve table without component`() {
    val componentA = Component(1)
    val componentB = Component(2)

    val tableWithA = assertDoesNotThrow("expected table to be created") {
      index.resolveWithComponent(index.emptyTable, componentA)
    }

    val tableWithAB = assertDoesNotThrow("expected table to be created") {
      index.resolveWithComponent(tableWithA, componentB)
    }

    val tableWithB = assertDoesNotThrow("expected table to be created") {
      index.resolveWithoutComponent(tableWithAB, componentA)
    }

    assertFalse(componentA in tableWithB.signature, "expected new table signature to exclude component")
    assertTrue(componentB in tableWithB.signature, "expected new table signature to include component")
  }

  @Test fun `should resolve table by id`() {
    val component = Component(1)

    val table = assertDoesNotThrow("expected table to be created") {
      index.resolveWithComponent(index.emptyTable, component)
    }

    assertEquals(
      expected = table,
      actual = index.get(table.id),
      message = "expected table to be resolved by id"
    )
  }

  @Test fun `should resolve table by signature`() {
    val component = Component(1)

    val table = assertDoesNotThrow("expected table to be created") {
      index.resolveWithComponent(index.emptyTable, component)
    }

    assertEquals(
      expected = table,
      actual = index.get(Signature.EMPTY with component),
      message = "expected table to be resolved by signature"
    )
  }

  @Test fun `should iterate over every table`() {
    val tables = List(10, ::Component).runningFold(index.emptyTable, index::resolveWithComponent)
    val iteratedTables = index.iterator().asSequence().toList()

    for (table in tables) assertTrue(table in iteratedTables, "expected iterator to include ${table.id}")
  }
}
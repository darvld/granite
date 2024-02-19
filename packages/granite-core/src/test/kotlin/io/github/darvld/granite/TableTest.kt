package io.github.darvld.granite

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TableTest {
  private val component = Component(0)
  private val table = Table(0, Signature.EMPTY with component)

  @Test fun `should add empty row`() {
    val row = table.add(Entity(0))
    val column = table.columnFor(component)

    // new rows are empty until the entity's component values are set
    assertTrue(row >= 0, "expected a valid row value to be returned")
    assertThrows<IllegalStateException>("expected an error for empty cell") { table[row, column] }
  }

  @Test fun `should clear deleted row`() {
    val row = table.add(Entity(0))
    val column = table.columnFor(component)

    // set the value and verify result
    table[row, column] = 5
    assertEquals(5, table[row, column], "expected value to be set")

    // now remove the row and check that it's empty
    table.remove(row)
    assertThrows<IllegalStateException>("expected an empty cell") { table[row, column] }
  }

  @Test fun `should reuse available row`() {
    val row = table.add(Entity(0))
    val column = table.columnFor(component)

    // set the value and verify result
    table[row, column] = 5
    assertEquals(5, table[row, column], "expected value to be set")

    // remove the row and check that it's empty
    table.remove(row)
    assertThrows<IllegalStateException>("expected an empty cell") { table[row, column] }

    // add a new row and verify reuse
    assertEquals(
      expected = row,
      actual = table.add(Entity(1)),
      message = "expected row to be reused"
    )
  }

  @Test fun `should return value at row and column`() {
    val row = table.add(Entity(0))
    val column = table.columnFor(component)

    // set the value and verify result
    table[row, column] = 5
    assertEquals(5, table[row, column], "expected value to be set")

  }

  @Test fun `should set value at row and column`() {
    val row = table.add(Entity(0))
    val column = table.columnFor(component)

    // set the value and verify result
    table[row, column] = 5
    assertEquals(5, table[row, column], "expected value to be set")

    table[row, column] = 7
    assertEquals(7, table[row, column], "expected value to be updated")
  }

  @Test fun `should return column for component`() {
    assertEquals(
      expected = table.signature.indexOf(component),
      actual = table.columnFor(component),
      message = "expected component column indext to match signature"
    )
  }

  @Test fun `should iterate over all rows`() {
    // populate the table, then remove every other entity
    repeat(100) { table.add(Entity(it)) }
    repeat(50) { table.remove(it * 2) }

    // collect every element
    val iterated = table.iterator().asSequence().sortedBy { it.id }.toList()

    assertEquals(
      expected = 50,
      actual = iterated.size,
      message = "expected iterator to skip empty rows and use the rest"
    )

    for (i in 1 until 100 step 2) assertEquals(
      expected = i,
      actual = iterated[(i - 1) / 2].id,
      message = "expected all non-removed entities to be iterated"
    )
  }
}
package io.github.darvld.granite

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class SignatureTest {
  /** Sample signature used for tests. */
  private val signature = Signature(intArrayOf(2, 6, 12))

  @Test fun `should return component`() {
    assertEquals(expected = Component(6), actual = signature[1], message = "expected component to be returned")
    assertThrows<IllegalStateException>("expected out-of-bounds error") { signature[5] }
  }

  @Test fun `should hadle 'contains' checks on components`() {
    for (id in arrayOf(2, 6, 12)) assertTrue(
      actual = Component(id) in signature,
      message = "expected signature to contain component"
    )

    assertFalse(
      actual = Component(id = 1) in signature,
      message = "expected signature to not contain component"
    )
  }

  @Test fun `should return index of component`() {
    for ((index, id) in arrayOf(2, 6, 12).withIndex()) assertEquals(
      expected = index,
      actual = signature.indexOf(Component(id)),
      message = "expected correct index for component",
    )

    for (id in arrayOf(-2, 5, 7, 43)) assertTrue(
      actual = signature.indexOf(Component(id)) < 0,
      message = "expected negative (invalid) index for component",
    )
  }

  @Test fun `should return new signature with added component`() {
    assertContentEquals(
      expected = Signature(intArrayOf(2, 3, 6, 12)).types,
      actual = signature.with(Component(3)).types,
      message = "expected signature to contain new component"
    )
  }

  @Test fun `should return new signature without removed component`() {
    assertContentEquals(
      expected = Signature(intArrayOf(6, 12)).types,
      actual = signature.without(Component(2)).types,
      message = "expected signature not to contain removed component"
    )
  }

  @Test fun `should return stable signature hash`() {
    assertEquals(
      expected = signature.hash(),
      actual = signature.hash(),
      message = "expected the same hash value to be returned multiple times"
    )
  }
}
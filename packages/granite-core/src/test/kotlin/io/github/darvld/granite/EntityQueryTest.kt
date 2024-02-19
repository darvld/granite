package io.github.darvld.granite

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class EntityQueryTest {
  @Test fun `should match signature`() {
    val query = EntityQuery.selectEntities {
      with(Component(1))
      without(Component(4))
      with(Component(12))
    }

    val matches = listOf(
      Signature(intArrayOf(1, 12)),
      Signature(intArrayOf(1, 2, 3, 5, 12, 43)),
    )

    for (case in matches) assertTrue(
      actual = query matches case,
      message = "expected query to match signature ${case.types}"
    )
  }

  @Test fun `should not match signature`() {
    val query = EntityQuery.selectEntities {
      with(Component(1))
      without(Component(4))
      with(Component(12))
    }

    val matches = listOf(
      Signature(intArrayOf(1, 4, 12)),
      Signature(intArrayOf(2, 4, 5, 12)),
      Signature(intArrayOf(2, 4, 5)),
      Signature(intArrayOf(13)),
      Signature(intArrayOf()),
    )

    for (case in matches) assertFalse(
      actual = query matches case,
      message = "expected query to not match signature ${case.types}"
    )
  }
}
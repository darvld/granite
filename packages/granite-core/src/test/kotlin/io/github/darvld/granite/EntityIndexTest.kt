package io.github.darvld.granite

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.*

internal class EntityIndexTest {
  /** Index instance to be used in tests. */
  private val index = EntityIndex()

  @Test fun `should create entity without recording it`() {
    val entity = index.new()
    assertNull(index[entity], "expected entity not to be recorded on creation")
  }

  @Test fun `should draft entity without reserving ID`() {
    assertEquals(
      expected = index.draft(),
      actual = index.draft(),
      message = "expected consecutive drafts to have the same id."
    )

    val draft = index.draft()
    assertEquals(
      expected = draft,
      actual = index.new(),
      message = "expected new entity to match the last draft."
    )

    assertNotEquals(
      illegal = draft,
      actual = index.draft(),
      message = "expected new draft not to match after creating entity."
    )
  }

  @Test fun `should create in batch`() {
    val batch = index.newBatch(50)

    assertEquals(
      expected = batch.last + 1,
      actual = index.draft().id,
      message = "expected batch to reserve ids."
    )

    for (id in batch) {
      assertNull(
        actual = index.get(Entity(id)),
        message = "expected batch not to initialize reserved entities",
      )

      assertDoesNotThrow("expected batch entities to be recordable") {
        index.record(Entity(id), 0, 0)
      }
    }
  }

  @Test fun `should allow unsafe recording`() {
    val entity = index.new()
    index.remove(entity)

    assertFails("expected normal recording to fail on deleted entity") { index.record(entity, 0, 0) }
    assertDoesNotThrow("expected normal recording to fail on deleted entity") { index.recordUnsafe(entity, 0, 0) }
  }

  @Test fun `should remove entity`() {
    val entity = index.new()

    // use stubbed values for table id and row
    index.record(entity, 1, 1)
    index.remove(entity)

    assertNull(index[entity], "expected entity record not to exist")
  }

  @Test fun `should return record for valid entity`() {
    val entity = index.new()
    index.record(entity, 1, 2)

    val record = assertDoesNotThrow("expected a valid record to be returned") {
      index.getOrFail(entity)
    }

    assertEquals(
      expected = 1,
      actual = record.table,
      message = "unexpected value for record.table"
    )

    assertEquals(
      expected = 2,
      actual = record.row,
      message = "unexpected value for record.row"
    )
  }

  @Test fun `should fail to return record for non-existent entity`() {
    assertNull(index[Entity(1)], "expected null record for invalid entity")
    assertNull(index[Entity(100)], "expected null record for out-of-bounds entity")
  }

  @Test fun `should fail to return record for invalidated entity`() {
    val entity = index.new()
    index.record(entity, 0, 1)

    assertNotNull(index[entity], "expected valid record for live entity")
    index.remove(entity)
    assertNull(index.get(entity), "expected invalid record for invalidated entity")
  }

  @Test fun `should update record for entity`() {
    val entity = index.new()
    index.record(entity, 1, 2)

    val record = index.getOrFail(entity)
    assertEquals(expected = 1, actual = record.table, message = "unexpected value for record.table")
    assertEquals(expected = 2, actual = record.row, message = "unexpected value for record.row")

    index.record(entity, 5, 6)

    val updated = index.getOrFail(entity)
    assertEquals(expected = 5, actual = updated.table, message = "unexpected value for record.table after update")
    assertEquals(expected = 6, actual = updated.row, message = "unexpected value for record.row after update")
  }
}
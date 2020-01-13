package photo.backup.kt.data

import arrow.core.None
import org.junit.jupiter.api.Test
import photo.backup.kt.SessionId
import java.util.*
import kotlin.test.assertEquals

class FileEntityTest {
    @Test
    fun changesId() {
        val expected = UUID.randomUUID()
        val entity = GoogleFileEntity("", None, 0, 0, SessionId(UUID.randomUUID()), Media.IMAGE)

        val result = entity.changeId(expected)
        val result2 = entity.changeId(GooglePhotoId(expected))

        assertEquals(expected, result.id.value)
        assertEquals(expected, result2.id.value)
    }


}
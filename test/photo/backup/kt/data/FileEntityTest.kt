package photo.backup.kt.data

import arrow.core.None
import arrow.core.Some
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import photo.backup.kt.SessionId
import java.util.*
import kotlin.test.assertEquals

class FileEntityTest {
    private lateinit var backupEntity: GoogleFileEntity
    private lateinit var sourceEntity: SourceFileEntity

    private lateinit var entity: FileEntity

    @BeforeEach
    fun setup() {
        backupEntity = GoogleFileEntity("", None, 0, 0, SessionId(UUID.randomUUID()), Media.IMAGE)
        sourceEntity = SourceFileEntity("", None, 0, 0, SessionId(UUID.randomUUID()), Media.IMAGE)

    }

    @Test
    fun changesId() {
        //Backup
        entity = backupEntity
        val expected = UUID.randomUUID()

        val result = entity.changeId(expected)
        assertEquals(expected, result.id.value)

        val result2 = entity.changeId(GooglePhotoId(expected))
        assertEquals(expected, result2.id.value)

        // Source
        entity = sourceEntity

        val result3 = entity.changeId(expected)
        assertEquals(expected, result3.id.value)

        val result4 = entity.changeId(SourcePhotoId(expected))
        assertEquals(expected, result4.id.value)
    }

    @Test
    fun changesSessionId() {
        val newSession = SessionId(UUID.randomUUID())

        //Backup
        entity = backupEntity
        val result = entity.copy(newSession)
        assertEquals(newSession, result.sessionId)

        //Source
        entity = sourceEntity
        val result2 = entity.copy(newSession)
        assertEquals(newSession, result2.sessionId)
    }

    @Test
    fun changesHashId() {
        val newHashId = Some(HashId(UUID.randomUUID()))

        //Backup
        entity = backupEntity
        val result = entity.changeHashId(newHashId)
        assertEquals(newHashId, result.hash)

        //Source
        entity = sourceEntity
        val result2 = entity.changeHashId(newHashId)
        assertEquals(newHashId, result2.hash)
    }


}
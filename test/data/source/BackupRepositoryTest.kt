package data.source

import arrow.core.*
import arrow.core.extensions.either.applicativeError.handleError
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
import photo.backup.kt.data.source.BackupRepository
import photo.backup.kt.data.source.IBackupRepository
import photo.backup.kt.data.source.RepoType
import photo.backup.kt.domain.backup
import photo.backup.kt.domain.source
import java.io.File
import java.util.*

internal class BackupRepositoryTest {

    private lateinit var repo: IBackupRepository
    private lateinit var backupEntity: GoogleFileEntity
    private lateinit var sourceEntity: SourceFileEntity
    private val sessionId = SessionId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        repo = BackupRepository.newInstance(RepoType.TEST)
        backupEntity = GoogleFileEntity(
            absolutePath = "/path/to",
            hash=None,
            size=0,
            dateModified=0,
            sessionId=sessionId,
            type=Media.IMAGE
        )
        sourceEntity = SourceFileEntity(
            absolutePath = "/path/to",
            hash=None,
            size=0,
            dateModified=0,
            sessionId=sessionId,
            type=Media.IMAGE
        )
    }

    @AfterEach
    fun tearDown() {
        repo.disconnect()
    }

    @Test
    @DisplayName("backedUp returns None when there's no file")
    fun backedUpReturnsNone() {
        val result = runBlocking { repo.backedUp(backupEntity) }
        assertTrue(result is None)
    }

    @Test
    @DisplayName("backedUp returns Some(MediaId) when file has been inserted via backUp")
    fun backedUpReturnsSome() {
        val id = runBlocking { repo.backUp(backupEntity) }
        val result = runBlocking { repo.backedUp(backupEntity)}
        assertEquals(Some(id), result)
    }

    @Test
    @DisplayName("backedUp returns Some(Source ID) when entity searched for is Source")
    fun backedUpReturnsSourceId() {
        runBlocking { repo.backUp(sourceEntity)}
        val result = runBlocking { repo.backedUp(sourceEntity)}
        assertTrue(result is Some && result.t is SourcePhotoId)
    }

    @Test
    @DisplayName("backedUp returns Some(Backup Id) when entity searched for is Backup")
    fun backedUpReturnsBackupId() {
        runBlocking { repo.backUp(backupEntity)}
        val result = runBlocking { repo.backedUp(backupEntity)}
        assertTrue(result is Some && result.t is GooglePhotoId)
    }

    @Test
    @DisplayName("backUp stores backup entity and returns backup id")
    fun backUp() {
        val resultId = runBlocking { repo.backUp(backupEntity) }
        assertTrue(resultId is GooglePhotoId)
    }

    @Test
    @DisplayName("backUp stores source entity and returns source id")
    fun backUpSource() {
        val id = runBlocking { repo.backUp(sourceEntity)}
        assertTrue(id is SourcePhotoId)
    }

    @Test
    @DisplayName("absolute path is preserved when inserting via backUp")
    fun backUpCorrectPath() {
        runBlocking { repo.backUp(backupEntity)}
        val result = runBlocking { repo.getBackup(backupEntity.absolutePath, backup)}
        assertTrue(result is Some<*>)
        assertEquals(backupEntity.absolutePath, (result as Some).t.absolutePath)
    }

    @Test
    @DisplayName("Unknown file can be inserted and retrieved")
    fun unknownFileCRUD() = runBlocking {
        val file = File("/path/to/thing.ext")
        assertEquals(0, repo.getUnknownFiles(sessionId).size)

        repo.createUnknownFile(file, sessionId)
        val unknownList = repo.getUnknownFiles(sessionId)

        assertEquals(1, unknownList.size)
        assertEquals(file.absolutePath, unknownList[0].absolutePath)
        assertEquals(sessionId.value, unknownList[0].session)
    }

    @Test
    @DisplayName("Returns Left if extension too long")
    fun extensionTooLong() = runBlocking {
        val file = File((1..1000).fold(".") { acc, item -> "$acc$item"}) // Generate 1000-char file extension
        val result = repo.createUnknownFile(file, sessionId)
        assertTrue(result.isLeft())
    }

    @Test
    @DisplayName("Upsert Hash adds new hash if hash does not exist yet")
    fun upsertHashAdds() = runBlocking {
        assertEquals(None, repo.getHashId(Hash("")))
        val entity = HashEntity(hash=Hash(""), sessionId=sessionId)
        val result = repo.upsertHash(entity)
        assertTrue(repo.getHashId(Hash("")) is Some)
    }

    @Test
    @DisplayName("Upsert Hash updates hash with new sessionId if it already exists")
    fun upsertHashUpdates() = runBlocking {
        val entity = HashEntity(hash=Hash(""), sessionId=SessionId(UUID.randomUUID()))
        val updatedEntity = entity.copy(sessionId=sessionId)
        val id: HashId = repo.addHash(entity).getOrHandle {
            HashId(UUID.randomUUID())
        }
        val secondId: HashId = repo.upsertHash(updatedEntity)
        assertEquals(id, secondId)
        val entityOpt: Option<HashEntity> = repo.getHash(secondId)
        assertTrue(entityOpt is Some)
        assertEquals(sessionId, (entityOpt as Some).t.sessionId)
    }

    @Test
    @DisplayName("when no files of same size are in db, findSourceByFileSize returns empty list")
    fun comparables() {
        val result = runBlocking { repo.findSourceByFileSize(0, sessionId) }
        assertTrue(result.isEmpty())
    }

    @Test
    @DisplayName("when a file of different size is in db, fSBFS returns empty list")
    fun emptyListReturned() = runBlocking {
        repo.backUp(sourceEntity)
        val result = repo.findSourceByFileSize(sourceEntity.size+1, sessionId)
        assertTrue(result.isEmpty())
    }

    @Test
    @DisplayName("when a file of same size is in db, returns list of 1")
    fun listOfOne() = runBlocking {
        repo.backUp(sourceEntity)
        val result = repo.findSourceByFileSize(sourceEntity.size, sessionId)
        assertEquals(1, result.size)
    }

    @Test
    @DisplayName("when a file of same size is in backup, returns empty list")
    fun doesNotReturnAny() = runBlocking {
        repo.backUp(backupEntity)
        val result = repo.findSourceByFileSize(backupEntity.size, sessionId)
        assertTrue(result.isEmpty())
    }

    @Test
    @DisplayName("When no files, upsertFile inserts")
    fun upsertFileInserts() = runBlocking {
        val opt = repo.backedUp(sourceEntity)
        assertTrue(opt is None)
        repo.upsertFile(sourceEntity)
        val result = repo.backedUp(sourceEntity)
        assertTrue(result is Some)
        val ent = repo.getBackup(sourceEntity.absolutePath, source)
        assertTrue(ent is Some)
        assertEquals(sourceEntity.sessionId, (ent as Some).t.sessionId)
    }

    @Test
    @DisplayName("When there is a file, upsertFile updates")
    fun upsertFileUpdates() = runBlocking {
        repo.upsertFile(sourceEntity)
        repo.upsertFile(sourceEntity.copy(sessionId=SessionId(UUID.randomUUID())))
        val entity = repo.getBackup(sourceEntity.absolutePath, source)
        assertTrue(entity is Some)
        assertNotEquals(sourceEntity.sessionId, (entity as Some).t.sessionId)
    }

    @Test
    @DisplayName("Renew hash")
    fun renewHash() {
        val entity = HashEntity(Hash(""), sessionId)
        val either = runBlocking { repo.addHash(entity) }
        either.fold({
            assertTrue(false, ("Insertion failed"))
        }, {
            val updated = entity.copy(id=Some(it), sessionId=SessionId(UUID.randomUUID()))
            runBlocking { repo.renewHash(it, updated.sessionId) }

            val storedEntity = runBlocking { repo.getHash(it) }
            storedEntity.fold({
                assertTrue(false, "Failed to retrieve row")
            }, {
                assertEquals(it.sessionId, updated.sessionId)
            })
        })
    }

    @Test
    @DisplayName("When three rows are created with different session Ids, updateSessionIds will update them all to a given one")
    fun updatesSessionIds() = runBlocking {
        repo.backUp(sourceEntity)
        repo.backUp(sourceEntity.copy(absolutePath="/foo"))
        repo.backUp(sourceEntity.copy(absolutePath="/bar"))
        val newSession = SessionId(UUID.randomUUID())

        val updated = repo.updateSessionIds(source, listOf(sourceEntity.absolutePath, "/foo", "/bar").map { File(it)}, newSession)
        val checks = listOf(repo.getBackup(sourceEntity.absolutePath, source), repo.getBackup("/foo", source), repo.getBackup("/bar", source)).map {
            it.fold({
                SessionId(UUID.randomUUID())
            },{
                it.sessionId
            })
        }

        assertEquals(3, updated)
        assertTrue(checks.all { it == newSession })
    }

    /**
     * Regression test against a bug where it was erroring out for any batch update of backup because the code referred to SourceTable at one point
     */
    @Test
    @DisplayName("Batch updates session IDs for backup table")
    fun updatesSessionIdsBackupTable() {
        runBlocking { repo.updateSessionIds(backup, emptyList(), sessionId) }
    }

    @Test
    @DisplayName("getSourceImagesWithBackups does not have multiple instances of a path in its results")
    fun notRedundant() = runBlocking {
        val hash = Hash("")
        val hashId = repo.upsertHash(HashEntity(hash, sessionId))
        repo.upsertFile(sourceEntity.copy(hash=Some(hashId)))
        repo.upsertFile(backupEntity.copy(hash=Some(hashId)))

        val items = repo.getSourceImagesWithBackups(sessionId)

        items.fold({
            assertTrue(false)
        }, {
            assertEquals(1, it.size)
        })

    }
}
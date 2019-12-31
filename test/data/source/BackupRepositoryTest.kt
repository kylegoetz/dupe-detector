package data.source

import arrow.core.None
import arrow.core.Some
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
    @DisplayName("when no files of same size are in db, findSourceByFileSize returns empty list")
    fun comparables() {
        val result = runBlocking { repo.findSourceByFileSize(0, sessionId) }
        assertTrue(result.isEmpty())
    }

    @Test
    @DisplayName("when a file of different size is in db, fSBFS returns empty list")
    fun emptyList() = runBlocking {
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
}
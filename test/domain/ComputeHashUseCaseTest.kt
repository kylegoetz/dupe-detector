package photo.backup.kt.domain

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import arrow.fx.IO
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import photo.backup.kt.SessionId
import java.util.*
import photo.backup.kt.data.*
import photo.backup.kt.data.source.IBackupRepository
import java.io.File

class ComputeHashUseCaseTest {

    @MockK lateinit var repo: IBackupRepository
    private val sessionId = SessionId(UUID.randomUUID())
    private lateinit var case: (ByteArray, String, Long, StageType)-> IO<HashId>
    private val sourceEntity = EntityFactory.build(Source, File(""), Some(HashId(UUID.randomUUID())), Media.IMAGE, sessionId)

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        case = generateComputeHashUseCase(repo, { Hash("") }, sessionId)
    }

    @Test
    @DisplayName("[Re-hash case] If file has changed")
    fun reHash() {
        coEvery { repo.getBackup(sourceEntity.absolutePath, Source) } returns Some(sourceEntity)
        coEvery { repo.upsertHash(any()) } returns HashId(UUID.randomUUID())

        case(ByteArray(5), sourceEntity.absolutePath, sourceEntity.dateModified+1, Source).unsafeRunSync()

        coVerify { repo.upsertHash(HashEntity(Hash(""), sessionId, None)) }
    }

    @Test
    @DisplayName("[Update Hash case] If file has not changed, updates session ID for hash entry")
    fun unchangedFileUpdatesSessionId() {
        coEvery { repo.getBackup(any(), Source)} returns Some(sourceEntity)
        coEvery { repo.renewHash(HashId(any()), SessionId(any()))} just runs

        val hashId = case(ByteArray(5), sourceEntity.absolutePath, sourceEntity.dateModified, Source).unsafeRunSync()

        coVerify { repo.renewHash((sourceEntity.hash as Some).t, sessionId) }
    }

    @Test
    @DisplayName("[Entity Has Expired case] If file has changed and hash is in DB, hash file and update hash session Id")
    fun entityHasExpired() {
        val file = File("")
        val path = ""
        coEvery { repo.getBackup(path, Source) } returns Some(SourceFileEntity(
                path,
                None,
                0,
                file.lastModified(),
                sessionId,
                Media.IMAGE
        ))
        val hashId = HashId(UUID.randomUUID())
        coEvery { repo.getHashId(Hash(any())) } returns Some(hashId)
        coEvery { repo.upsertHash(any()) } returns hashId

        case(ByteArray(5), path, file.lastModified()+1, Source).unsafeRunSync()

//        coVerify { repo.renewHash(hashId, sessionId) }
        coVerify { repo.upsertHash(HashEntity(hash=Hash(""), sessionId=sessionId, id=None)) }
    }

    @Test
    @DisplayName("[No Hash case] If file has changed and the hash is not in the DB, it upserts hash coming from hash fn")
    fun noHashCase() {
        val path = ""
        coEvery { repo.getBackup(path, Source) } returns Some(SourceFileEntity(
                path,
                None,
                0,
                File(path).lastModified(),
                sessionId,
                Media.IMAGE
        ))
        coEvery { repo.getHashId(Hash(any())) } returns None
        coEvery { repo.upsertHash(any()) } returns HashId(UUID.randomUUID())

        case(ByteArray(5), path, File("").lastModified()+1, Source).unsafeRunSync()

//        coVerify { repo.addHash(any()) }
        coVerify { repo.upsertHash(HashEntity(Hash(""), sessionId, None)) }
    }

    @Test
    @DisplayName("[No Entity case] If file is new, upsert hash with session ID")
    fun noEntityCase() {
        val path = ""
        coEvery { repo.getBackup(path, Source) } returns None
        coEvery { repo.getHashId(Hash(any())) } returns None
        coEvery { repo.upsertHash(any()) } returns HashId(UUID.randomUUID())

        case(ByteArray(5), path, 0, Source).unsafeRunSync()

        coVerify { repo.upsertHash(HashEntity(Hash(""), sessionId, None)) }
    }

    @Test
    @DisplayName("[Renew case] If file has not changed, return stored Hash and do not hash the content")
    fun unchangedFileGetOldHash() {
        val path = "/foo/bar"
        val expectedHashId = HashId(UUID.randomUUID())
        val lastModified = 123L
        coEvery { repo.getBackup(path, Source) } returns Some(SourceFileEntity(
                path,
                Some(expectedHashId),
                0,
                lastModified,
                sessionId,
                Media.IMAGE))
        coEvery { repo.renewHash(expectedHashId, sessionId) } just runs
//        val case = ComputeHashUseCase(repo, { Hash("") }, sessionId)

        val hashId: HashId = case(ByteArray(5), path, lastModified, Source).unsafeRunSync()

        coVerify(exactly=0) { repo.getHashId(Hash(any())) }
        assertEquals(expectedHashId, hashId)
    }
}
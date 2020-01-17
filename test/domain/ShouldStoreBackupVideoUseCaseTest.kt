package photo.backup.kt.domain

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import arrow.fx.IO
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
import java.io.File
import java.util.*

class ShouldStoreBackupVideoUseCaseTest : BaseUseCaseTest<ShouldStoreBackupVideoUseCase>() {
    override lateinit var SUT: ShouldStoreBackupVideoUseCase
    private lateinit var spy: File
    private lateinit var hasherSpy: (File)->IO<Either<Throwable, Hash>>

    override fun configureSUT() {
        val hasher = { _:File -> IO.just(Either.right(Hash(""))) }
        hasherSpy = spyk(hasher)
        spy = spyk(File(""))

        SUT = generateShouldStoreBackupVideoUseCase(repo, sessionId, hasherSpy)
    }

    @Test
    @DisplayName("If repo finds no source files that are same size as backup, return false")
    fun noSourceFiles() {
        coEvery { repo.findSourceByFileSize(any(), SessionId(any())) } returns emptyList()

        val result = SUT(spy).unsafeRunSync()

        assertFalse(result)
    }

    @Test
    @DisplayName("Calls repo with correct file size and session ID")
    fun correctParams() {
        coEvery { repo.findSourceByFileSize(any(), SessionId(any())) } returns emptyList()
        coEvery { spy.length() } returns 123

        SUT(spy).unsafeRunSync()

        coVerify { repo.findSourceByFileSize(123, sessionId) }
    }

    @Test
    @DisplayName("For every found file from repo call, hashes it")
    fun hashFoundSourceFiles() {
        val otherPath = "/some/other/path"
        coEvery { repo.findSourceByFileSize(any(), SessionId(any()))} returns listOf(
            sourceEntity,
            sourceEntity.copy(absolutePath=otherPath)
        )
        coEvery { repo.upsertHash(any()) } returns HashId(UUID.randomUUID())
        coEvery { repo.upsertFile(any()) } returns SourcePhotoId(UUID.randomUUID())

        SUT(spy).unsafeRunSync()

        coVerifyAll {
            hasherSpy(File(sourceEntity.absolutePath))
            hasherSpy(File(otherPath))
        }
    }

    @Test
    @DisplayName("For every found file, upserts the hash that's generated")
    fun upsertAllHashes() {
        coEvery { repo.findSourceByFileSize(any(), SessionId(any()))} returns listOf(sourceEntity)
        coEvery { repo.upsertHash(any()) } returns HashId(UUID.randomUUID())
        coEvery { repo.upsertFile(any()) } returns SourcePhotoId(UUID.randomUUID())

        SUT(spy).unsafeRunSync()

        coVerify {
            repo.upsertHash(
                HashEntity(
                    Hash(""),
                    sessionId,
                    None
                ))
        }
    }

    // TODO probably doesn't need to update the session Id, should already be most recent
    @Test
    @DisplayName("for every found file, upserts the file with updated hash and session ID")
    fun upsertAllFoundEntities() {
        val newHashId = HashId(UUID.randomUUID())
        coEvery { repo.findSourceByFileSize(any(), SessionId(any())) } returns listOf(sourceEntity.copy(SessionId(UUID.randomUUID())))
        coEvery {repo.upsertHash(any())} returns newHashId
        coEvery { repo.upsertFile(any()) } returns SourcePhotoId(UUID.randomUUID())

        SUT(spy).unsafeRunSync()

        coVerify {
            repo.upsertFile(SourceFileEntity(
                absolutePath=sourceEntity.absolutePath,
                hash=Some(newHashId),
                size=sourceEntity.size,
                dateModified=sourceEntity.dateModified,
                sessionId=sessionId,
                type=sourceEntity.type,
                id=sourceEntity.id
            ))
        }

    }

}
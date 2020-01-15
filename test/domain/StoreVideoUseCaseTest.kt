package photo.backup.kt.domain

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import arrow.fx.IO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
import java.io.File
import java.util.*

class StoreVideoUseCaseTest : BaseUseCaseTest<StoreVideoUseCase>() {
    override lateinit var SUT: StoreVideoUseCase
    private lateinit var hashFnSpy: (File)->IO<Either<Throwable, Hash>>

    override fun configureSUT() {
        val hash = { _: File -> IO.just(Either.right(Hash("")))}
        hashFnSpy = spyk(hash)
        SUT = generateStoreVideoUseCase(repo, sessionId, hashFnSpy)
    }

    @Test
    @DisplayName("When hash fails")
    fun hashFails() {
        coEvery { hashFnSpy(any()) } returns IO.just(Either.left(IllegalStateException("")))
        coEvery { repo.upsertFile(any()) } returns GooglePhotoId(UUID.randomUUID())
        val slot = slot<GoogleFileEntity>()

        SUT(backupEntity).unsafeRunSync()

        coVerify { repo.upsertFile(capture(slot)) }
        assertEquals(None, slot.captured.hash)
    }

    @Test
    @DisplayName("When entity is source, do not hash, upsert file with new session ID")
    fun sourceEntity() {
        coEvery { repo.upsertFile(any()) } returns SourcePhotoId(UUID.randomUUID())

        SUT(sourceEntity.copy(sessionId= SessionId(UUID.randomUUID()))).unsafeRunSync()

        coVerify { repo.upsertFile(sourceEntity.copy(sessionId)) }
        coVerify(exactly=0) { hashFnSpy(any()) }
    }

    @Test
    @DisplayName("When entity is backup, hash, upsert hash, and upsert file")
    fun backupEntityTest() {
        val hashId = HashId(UUID.randomUUID())
        coEvery { repo.upsertFile(any())} returns GooglePhotoId(UUID.randomUUID())
        coEvery { repo.upsertHash(any()) } returns hashId

        SUT(backupEntity).unsafeRunSync()

        coVerify { hashFnSpy(File(backupEntity.absolutePath)) }
        coVerify { repo.upsertHash(HashEntity(Hash(""), sessionId, id=None)) }
        coVerify { repo.upsertFile(backupEntity.copy(sessionId=sessionId, hash= Some(hashId))) }
    }

}
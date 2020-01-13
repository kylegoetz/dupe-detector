package photo.backup.kt.domain

import arrow.core.None
import arrow.core.Some
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
import java.util.UUID


class SaveSourcePhotoBackupUseCaseTest: BaseUseCaseTest<SavePhotoUseCase>() {
    private val sessionId = SessionId(UUID.randomUUID())
    private val id = SourcePhotoId(UUID.randomUUID())

    override lateinit var SUT: SavePhotoUseCase

    @BeforeEach
    override fun configureSUT() {
        SUT = generateSavePhotoUseCase(repo, sessionId)
    }

    @Test
    @DisplayName("Should upsertFile with session id")
    fun insertNewSourceFile() {
        /* Given */
        coEvery { repo.upsertFile(any()) } returns id

        /* When */
        SUT(sourceEntity).unsafeRunSync()

        /* Then */
        coVerify { repo.upsertFile(sourceEntity.copy(sessionId)) }
    }
}
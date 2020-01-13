package photo.backup.kt.domain

import arrow.core.Either
import arrow.core.Some
import arrow.fx.IO
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
import photo.backup.kt.data.source.IBackupRepository
import java.util.*

class ListDupesUseCaseTest() {
    @MockK lateinit var repo: IBackupRepository
    private lateinit var case: () -> IO<List<SourceFileEntity>>
    private val sessionId = SessionId(UUID.randomUUID())

    @BeforeEach
    fun configureSystemUnderTest() {
        MockKAnnotations.init(this)
        case = generateListDupesUseCase(repo, sessionId)
    }

    @Test
    @DisplayName("When repo errors out, returns empty list")
    fun emptyList() {
        coEvery { repo.getSourceImagesWithBackups(SessionId(any()))} returns Either.left(IllegalStateException(""))

        val result = case().unsafeRunSync()

        assertTrue(result.isEmpty())
    }

    @Test
    @DisplayName("When repo does not error out, the use case returns the list repo gives you, unwrapped")
    fun list() {
        val list = listOf(SourceFileEntity(
            "",
            Some(HashId(UUID.randomUUID())),
            0,
            0,
            sessionId,
            Media.IMAGE))
        coEvery { repo.getSourceImagesWithBackups(SessionId(any()))} returns Either.right(list)

        val result = case().unsafeRunSync()

        assertEquals(list, result)
    }

}
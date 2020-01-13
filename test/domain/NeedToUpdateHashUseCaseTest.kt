package photo.backup.kt.domain

import arrow.fx.IO
import io.mockk.coEvery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

class NeedToUpdateHashUseCaseTest: BaseUseCaseTest<NeedToUpdateHashUseCase>() {
    override lateinit var case: NeedToUpdateHashUseCase

    @BeforeEach
    override fun configureSystemUnderTest() {
        case = NeedToUpdateHashUseCase(repo)
    }

    @Test
    @DisplayName("Returns false when repo returns false")
    fun returnsFalse() {
        coEvery { repo.isFileChanged(any(), any()) } returns false
        val result = case(File("")).unsafeRunSync()
        assertEquals(false, result)
    }

    @Test
    @DisplayName("Returns true when repo returns true")
    fun returnsTrue() {
        coEvery { repo.isFileChanged(any(), any())} returns true
        val result = case(File("")).unsafeRunSync()
        assertEquals(true, result)
    }

}
package photo.backup.kt.domain

import arrow.core.None
import arrow.core.Some
import io.mockk.coEvery
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

class NeedToUpdateHashUseCaseTest: BaseUseCaseTest<NeedToUpdateHashUseCase>() {
    override lateinit var SUT: NeedToUpdateHashUseCase
    lateinit var file: File

    @BeforeEach
    override fun configureSUT() {
        SUT = generateNeedToUpdateHashUseCase(repo)
        file = spyk(File(""))
    }

    @Test
    @DisplayName("Returns false when getFileModificationDate returns None")
    fun noFileInDb() {
        coEvery { repo.getFileModificationDate(any(), any())} returns None

        val result = SUT(file, source).unsafeRunSync()

        assertEquals(true, result)
    }

    @Test
    @DisplayName("Returns true when getFileModificationDate returns a # smaller than file's modification date")
    fun returnsFalse() {
        coEvery { repo.getFileModificationDate(any(), any()) } returns Some(0)
        coEvery { file.lastModified() } returns 1

        val result = SUT(file, source).unsafeRunSync()

        assertEquals(true, result)
    }

    @Test
    @DisplayName("Returns false when getFileModificationDate returns a # equal to file's modification date")
    fun returnsTrue() {
        coEvery { repo.getFileModificationDate(any(), any())} returns Some(0)
        coEvery { file.lastModified() } returns 0

        val result = SUT(file, source).unsafeRunSync()

        assertEquals(false, result)
    }

}
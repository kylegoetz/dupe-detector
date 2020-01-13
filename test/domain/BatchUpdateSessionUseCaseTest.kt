package photo.backup.kt.domain

import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import photo.backup.kt.SessionId
import java.io.File

class BatchUpdateSessionUseCaseTest: BaseUseCaseTest<BatchUpdateSessionUseCase>() {
    override lateinit var SUT: BatchUpdateSessionUseCase

    override fun configureSUT() {
        SUT = generateBatchUpdateSession(repo, sessionId)
    }

    @Test
    @DisplayName("does not bother with repo call if list empty")
    fun emptyListNoCall() {
        coEvery { repo.updateSessionIds(any(), any(), sessionId) } returns 0

        SUT(source, emptyList()).unsafeRunSync()

        coVerify(exactly=0) { repo.updateSessionIds(source, any(), sessionId) }
    }

    @Test
    @DisplayName("if list non-empty, calls repo")
    fun nonEmptyListCalls() {
        coEvery { repo.updateSessionIds(any(), any(), sessionId) } returns 0
        val fileList = listOf(File(""))

        SUT(source, fileList).unsafeRunSync()

        coVerify { repo.updateSessionIds(source, fileList, sessionId) }
    }
}
package photo.backup.kt.domain

import arrow.core.Either
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import photo.backup.kt.SessionId
import java.io.File
import java.util.*

class StoreUnknownFileRefUseCaseTest: BaseUseCaseTest<StoreUnknownFileRefUseCase>() {
    override lateinit var SUT: StoreUnknownFileRefUseCase

    @BeforeEach
    override fun configureSUT() {
        SUT = generateStoreUnknownFileRefUseCase(repo, sessionId)
    }

    @Test
    @DisplayName("calls repo.createUnknownFile with file and session id")
    fun call() {
        coEvery { repo.createUnknownFile(any(), SessionId(any())) } returns Either.right(UUID.randomUUID())

        SUT(File("/")).unsafeRunSync()

        coVerify { repo.createUnknownFile(File("/"), sessionId)}
    }

}
package photo.backup.kt.domain

import org.junit.jupiter.api.BeforeEach
import photo.backup.kt.SessionId
import java.util.*

class StoreUnknownFileRefUseCaseTest: BaseUseCaseTest<StoreUnknownFileRefUseCase>() {
    override lateinit var case: StoreUnknownFileRefUseCase

    @BeforeEach
    override fun configureSystemUnderTest() {
        case = StoreUnknownFileRefUseCase(repo, SessionId(UUID.randomUUID()))
    }

}
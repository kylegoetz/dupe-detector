package photo.backup.kt.domain

import arrow.core.Some
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import photo.backup.kt.SessionId
import photo.backup.kt.data.HashId
import photo.backup.kt.data.Media
import photo.backup.kt.data.SourceFileEntity
import photo.backup.kt.data.SourcePhotoId
import photo.backup.kt.data.source.IBackupRepository
import java.util.*

abstract class BaseUseCaseTest<T> {
    abstract var SUT: T
    @MockK protected lateinit var repo: IBackupRepository

    protected lateinit var sourceEntity: SourceFileEntity
    protected val sessionId = SessionId(UUID.randomUUID())

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        sourceEntity = SourceFileEntity(
            absolutePath = "",
            hash = Some(HashId(UUID.randomUUID())),
            size=0L,
            dateModified = 0L,
            type = Media.IMAGE,
            id = SourcePhotoId(UUID.randomUUID()),
            sessionId = SessionId(UUID.randomUUID()))
        configureSUT()
    }

    @BeforeEach
    abstract fun configureSUT()
}
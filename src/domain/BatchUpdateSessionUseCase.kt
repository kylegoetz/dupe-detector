package photo.backup.kt.domain

import arrow.fx.IO
import arrow.fx.IO.Companion.effect
import arrow.fx.extensions.fx
import photo.backup.kt.SessionId
import photo.backup.kt.data.FileEntity
import photo.backup.kt.data.source.IBackupRepository
import java.io.File

class BatchUpdateSessionUseCase(private val repository: IBackupRepository, private val sessionId: SessionId) {
    operator fun invoke(stage: StageType, entities: List<File>): IO<Int> =
        IO { repository.updateSessionIds(stage, entities, sessionId) }
}
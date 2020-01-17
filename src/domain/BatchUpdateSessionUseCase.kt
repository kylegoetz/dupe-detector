package photo.backup.kt.domain

import arrow.fx.IO
import photo.backup.kt.SessionId
import photo.backup.kt.data.StageType
import photo.backup.kt.data.source.IBackupRepository
import java.io.File

//class BatchUpdateSessionUseCase(private val repository: IBackupRepository, private val sessionId: SessionId) {
//    operator fun invoke(stage: StageType, entities: List<File>): IO<Int> =
//        IO { repository.updateSessionIds(stage, entities, sessionId) }
//}

fun generateBatchUpdateSession(repository: IBackupRepository, sessionId: SessionId): (StageType, List<File>)->IO<Int> {
    return { stage, entities ->
        when(entities.isEmpty()) {
            true -> IO.just(0)
            false -> IO.effect { repository.updateSessionIds(stage, entities, sessionId)}
        }
    }
}

typealias BatchUpdateSessionUseCase = (StageType, List<File>) -> IO<Int>
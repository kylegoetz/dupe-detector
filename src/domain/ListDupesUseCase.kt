package photo.backup.kt.domain

import arrow.core.getOrElse
import arrow.fx.IO
import arrow.fx.extensions.fx
import photo.backup.kt.SessionId
import photo.backup.kt.data.SourceFileEntity
import photo.backup.kt.data.source.IBackupRepository

class ListDupesUseCase(private val repository: IBackupRepository, private val sessionId: SessionId) {
    operator fun invoke(): IO<List<SourceFileEntity>> = IO.fx {
        effect { repository.getSourceImagesWithBackups(sessionId) }.bind().getOrElse {
            emptyList()
        }
    }
}

sealed class ListDupesExceptions {
    object NoDupesFound: ListDupesExceptions()
}
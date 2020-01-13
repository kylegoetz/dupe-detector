package photo.backup.kt.domain

import arrow.core.getOrElse
import arrow.fx.IO
import arrow.fx.extensions.fx
import photo.backup.kt.SessionId
import photo.backup.kt.data.SourceFileEntity
import photo.backup.kt.data.source.IBackupRepository

fun generateListDupesUseCase(repository: IBackupRepository, sessionId: SessionId): ()->IO<List<SourceFileEntity>> = {
    IO.effect { repository.getSourceImagesWithBackups(sessionId) }.map {
        it.getOrElse { emptyList() }
    }
}
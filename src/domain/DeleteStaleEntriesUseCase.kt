package photo.backup.kt.domain

import arrow.fx.IO
import arrow.fx.extensions.fx
import photo.backup.kt.SessionId
import photo.backup.kt.data.source.IBackupRepository

class DeleteStaleEntriesUseCase(private val repository: IBackupRepository) {
    operator fun invoke(sessionId: SessionId) = IO.fx {
        !effect { repository.deleteStaleBackupEntries(sessionId) }
        !effect { repository.deleteStaleOriginalEntries(sessionId) }
        !effect { repository.deleteStaleHashEntries(sessionId) }
    }
}
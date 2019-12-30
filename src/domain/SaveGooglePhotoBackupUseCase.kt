package photo.backup.kt.domain

import arrow.core.None
import photo.backup.kt.data.source.IBackupRepository
import arrow.core.Option
import arrow.core.Some
import arrow.fx.IO
import arrow.fx.extensions.fx
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
import java.util.*

class SavePhotoUseCase(private val repository: IBackupRepository, private val sessionId: SessionId = SessionId(UUID.randomUUID())) {
    operator fun invoke(entity: FileEntity): IO<MediaId> = IO.fx {
        val id: Option<MediaId> = !effect { repository.backedUp(entity) }
        id.fold(
                { !effect { repository.backUp(entity.copy(sessionId=sessionId))} },
                { !effect { repository.update(entity.changeId(it).copy(sessionId=sessionId)) } ; it}
        )
    }
}
package photo.backup.kt.domain

import arrow.core.None
import photo.backup.kt.data.source.IBackupRepository
import arrow.core.Option
import arrow.core.Some
import arrow.fx.IO
import arrow.fx.extensions.fx
import ch.frankel.slf4k.error
import org.slf4j.LoggerFactory
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
import java.util.*

private val logger = LoggerFactory.getLogger("test")

class SavePhotoUseCase(private val repository: IBackupRepository, private val sessionId: SessionId = SessionId(UUID.randomUUID())) {
    operator fun invoke(entity: FileEntity): IO<MediaId> = IO.fx {
        if(entity.sessionId != sessionId) logger.error { "Session IDs do not match" }
        !effect { repository.upsertFile(entity) }
//        val id: Option<MediaId> = !effect { repository.backedUp(entity) }
//        id.fold(
//                { !effect { repository.backUp(entity.copy(sessionId=sessionId))} },
//                { !effect { repository.update(entity.changeId(it).copy(sessionId=sessionId)) } ; it}
//        )
    }
}
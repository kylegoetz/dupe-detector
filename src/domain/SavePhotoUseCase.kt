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

typealias SavePhotoUseCase = (FileEntity)->IO<MediaId>

fun generateSavePhotoUseCase(repository: IBackupRepository, sessionId: SessionId): (FileEntity)->IO<MediaId> =
    { IO.effect { repository.upsertFile(it.copy(sessionId)) } }


@Suppress("UNUSED_PARAMETER")
private val logger = LoggerFactory.getLogger("test")
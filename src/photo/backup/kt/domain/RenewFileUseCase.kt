package photo.backup.kt.photo.backup.kt.domain

import arrow.core.Option
import arrow.fx.IO
import arrow.fx.extensions.fx
import ch.frankel.slf4k.error
import ch.frankel.slf4k.trace
import org.slf4j.LoggerFactory
import photo.backup.kt.SessionId
import photo.backup.kt.data.FileEntity
import photo.backup.kt.data.source.IBackupRepository
import photo.backup.kt.domain.StageType
import java.io.File

private val logger = LoggerFactory.getLogger("test")
class RenewFileUseCase(private val repository: IBackupRepository, private val sessionId: SessionId) {
    operator fun invoke(file: File, stage: StageType): IO<Unit> = IO.fx {
        logger.trace { "Renewing hash and file for ${file.canonicalPath}"}
        val entityOpt: Option<FileEntity> = effect { repository.getBackup(file.canonicalPath, stage) }.bind()
        logger.trace { "Result of getBackup on ${file.canonicalPath}: $entityOpt"}
        entityOpt.fold({
            logger.error { "Renew Error: ${file.canonicalPath} was expected in DB $stage but not found" }
        },{ entity->
            entity.hash.fold({
                logger.error { "${entity.absolutePath} is missing a hash ID but program logic expects it to have one."}
            },{
                logger.trace { "Renewing hash and file with session Id ${sessionId.value}" }
                !effect { repository.renewHash(it, sessionId) }
                !effect { repository.upsertFile(entity.copy(sessionId)) }
            })
        })
    }
}

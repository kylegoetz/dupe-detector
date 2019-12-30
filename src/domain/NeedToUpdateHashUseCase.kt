package photo.backup.kt.domain

import arrow.fx.IO
import ch.frankel.slf4k.trace
import org.slf4j.LoggerFactory
import photo.backup.kt.data.source.IBackupRepository
import java.io.File

private val logger = LoggerFactory.getLogger("test")
class NeedToUpdateHashUseCase(private val repository: IBackupRepository) {
    operator fun invoke(file: File, stage: StageType): IO<Boolean> = IO {
        logger.trace { "Must we re-hash this file?"}
        repository.getFileModificationDate(file.canonicalPath, stage).fold({
            logger.trace{"Yes"}
            true
        }, {
            (file.lastModified() > it).also {
                if(it) logger.trace{"Yes"} else logger.trace{"No"}
            }
        })
    }
}
package photo.backup.kt.domain

import arrow.fx.IO
import ch.frankel.slf4k.trace
import org.slf4j.LoggerFactory
import photo.backup.kt.data.source.IBackupRepository
import java.io.File


fun generateNeedToUpdateHashUseCase(repository: IBackupRepository): (File, StageType)->IO<Boolean> {
    return { file, stage ->
        IO.effect {
            repository
                .getFileModificationDate(file.canonicalPath, stage)
                .fold({ true },{ file.lastModified() > it })
        }
    }
}
typealias NeedToUpdateHashUseCase = (File, StageType) -> IO<Boolean>
private val logger = LoggerFactory.getLogger("test")
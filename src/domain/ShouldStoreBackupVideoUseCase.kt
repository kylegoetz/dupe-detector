package photo.backup.kt.domain

import arrow.core.Either
import arrow.core.Some
import arrow.fx.IO
import arrow.fx.extensions.fx
import ch.frankel.slf4k.debug
import ch.frankel.slf4k.error
import ch.frankel.slf4k.trace
import org.slf4j.LoggerFactory
import photo.backup.kt.SessionId
import photo.backup.kt.data.Hash
import photo.backup.kt.data.HashEntity
import photo.backup.kt.data.HashId
import photo.backup.kt.data.source.BackupRepository
import photo.backup.kt.data.source.IBackupRepository
import java.io.File

private val logger = LoggerFactory.getLogger("test")

/**
 * Determines if a backup video needs to be stored. It needs to be stored only if there is a video of the same
 * size in the source directory being scanned.
 */
typealias ShouldStoreBackupVideoUseCase = (File)->IO<Boolean>
fun generateShouldStoreBackupVideoUseCase(
    repository: IBackupRepository,
    sessionId: SessionId,
    hashFn: (File)->IO<Either<Throwable, Hash>>): (File)->IO<Boolean> {

    return { file -> IO.fx {
        val targetSize = file.length()
        logger.trace { "${file.canonicalPath} is of size $targetSize. Looking for same-sized in source."}
        val (files) = effect { repository.findSourceByFileSize(targetSize, sessionId) }
        when(files.isEmpty()) {
            true -> false
            false -> {
                files.forEach {sourceEntity ->
                    logger.debug { "${sourceEntity.absolutePath} might match ${file.canonicalPath}"}
                    val either: Either<Throwable, Hash> = hashFn(File(sourceEntity.absolutePath)).bind()
                    either.fold({
                        logger.error { "Hasher failed for ${sourceEntity.absolutePath} : $it"}
                    }, {
                        val hashId: HashId = !effect { repository.upsertHash(HashEntity(it, sessionId))}
                        val entity = sourceEntity.copy(hash= Some(hashId), sessionId=sessionId)
                        !effect { repository.upsertFile(entity) }
                    })
                }
                true
            }
        }
    }}
}

//class ShouldStoreBackupVideoUseCase(
//    private val repository: IBackupRepository,
//    private val sessionId: SessionId,
//    private val getHash: (File)->IO<Either<Throwable, Hash>>) {
//
//    operator fun invoke(file: File): IO<Boolean> = IO.fx {
//        val targetSize = file.length()
//        logger.trace { "${file.canonicalPath} is of size $targetSize. Looking for same-sized in source."}
//        val (files) = effect { repository.findSourceByFileSize(targetSize, sessionId) }
//        when(files.isEmpty()) {
//            true -> false
//            false -> {
//                files.forEach {sourceEntity ->
//                    logger.debug { "${sourceEntity.absolutePath} might match ${file.canonicalPath}"}
//                    val either: Either<Throwable, Hash> = getHash(File(sourceEntity.absolutePath)).bind()
//                    either.fold({
//                        logger.error { "Hasher failed for ${sourceEntity.absolutePath} : $it"}
//                    }, {
//                        val hashId: HashId = !effect { repository.upsertHash(HashEntity(it, sessionId))}
//                        val entity = sourceEntity.copy(hash= Some(hashId), sessionId=sessionId)
//                        !effect { repository.upsertFile(entity) }
//                    })
//                }
//                true
//            }
//        }
//    }
//}
package photo.backup.kt.domain

import arrow.core.Either
import arrow.core.Some
import arrow.fx.IO
import arrow.fx.extensions.fx
import ch.frankel.slf4k.error
import org.slf4j.LoggerFactory
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
import photo.backup.kt.data.source.IBackupRepository
import java.io.File

//private val logger = LoggerFactory.getLogger("test")
////
////class StoreBackupVideoUseCase(private val repository: IBackupRepository, private val sessionId: SessionId, private val hasher: (File)-> IO<Either<Throwable, Hash>>) {
////    operator fun invoke(file: File): IO<Either<Throwable, GooglePhotoId>> = IO.fx {
////        val hashEither: Either<Throwable, Hash> = hasher(file).bind()
////        when(hashEither is Either.Right) {
////            false -> {
////                logger.error { "Hasher failed for ${file.canonicalPath}"}
////                hashEither as Either.Left
////            }
////            true -> {
////                val hash: Hash = hashEither.b
////                val hashId: HashId = !effect { repository.upsertHash(HashEntity(hash, sessionId))}
////                val entity = GoogleFileEntity(
////                    absolutePath=file.canonicalPath,
////                    size=file.length(),
////                    dateModified = file.lastModified(),
////                    sessionId=sessionId,
////                    type= Media.VIDEO,
////                    hash= Some(hashId)
////                )
////                val id = !effect { repository.upsertFile(entity) as GooglePhotoId }
////                Either.right(id)
////            }
////        }
////    }
////}
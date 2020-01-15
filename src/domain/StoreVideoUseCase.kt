package photo.backup.kt.domain

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.fx.IO
import arrow.fx.extensions.fx
import ch.frankel.slf4k.debug
import ch.frankel.slf4k.error
import org.slf4j.LoggerFactory
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
import photo.backup.kt.data.source.IBackupRepository
import java.io.File

private val logger = LoggerFactory.getLogger("test")

typealias StoreVideoUseCase = (FileEntity) -> IO<Either<Throwable, MediaId>>
fun generateStoreVideoUseCase(
    repository: IBackupRepository,
    sessionId: SessionId,
    hashFn: (File)->IO<Either<Throwable, Hash>>): (FileEntity)->IO<Either<Throwable, MediaId>> {

    return { entity ->
        IO.fx {
            val hashId: Option<HashId> = when (entity is GoogleFileEntity) {
                true -> {
                    val maybeHash = hashFn(File(entity.absolutePath)).bind()
                    maybeHash.fold({
                        logger.error { "Failed to hash ${entity.absolutePath} : $it" }
                        IO.just(None)
                    }, {
                        val hashEntity = HashEntity(it, sessionId)
                        IO.effect { Some(repository.upsertHash(hashEntity)) }
                    }).bind()
                }
                false -> entity.hash
            }
            IO.effect { repository.upsertFile(entity.copy(sessionId).changeHashId(hashId)) }.attempt().bind()
        }
    }
}
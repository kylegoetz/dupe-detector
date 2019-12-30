package photo.backup.kt.domain

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.fx.IO
import arrow.fx.extensions.fx
import ch.frankel.slf4k.trace
import org.slf4j.LoggerFactory
import photo.backup.kt.SessionId
import photo.backup.kt.data.FileEntity
import photo.backup.kt.data.Hash
import photo.backup.kt.data.HashEntity
import photo.backup.kt.data.HashId
import photo.backup.kt.data.source.IBackupRepository
import java.util.*
import kotlin.system.measureTimeMillis

private val logger = LoggerFactory.getLogger("test")
class ComputeHashUseCase(private val repository: IBackupRepository, val hasher: suspend (ByteArray)->Hash, val sessionId: SessionId = SessionId(UUID.randomUUID())) {
    operator fun invoke(data: ByteArray, path: String, lastModified: Long, stage: StageType): IO<HashId> = IO.fx {
        logger.trace { "Computing hash for $path" }
        val (entity: Option<FileEntity>) = effect { repository.getBackup(path, stage) }
        if(hashIsRenewable(entity, lastModified)) {
            with(entity as Some) {
                logger.trace { "Renewing hash for $path" }
                !effect { repository.renewHash((entity.t.hash as Some).t, sessionId) }
                (entity.t.hash as Some).t
            }
        } else {
            logger.trace { "Computing hash for $path"}
            val (hash: Hash) = effect { hasher(data) }
            logger.trace { "Trace computed for $path"}
            val (result: Option<HashId>) = effect { repository.getHashId(hash) }
            when (result) {
                is Some -> {
                    logger.trace { "Renewing hash for $path"}
                    !effect { repository.renewHash(result.t, sessionId) }
                    result.t
                }
                is None -> {
                    logger.trace { "Adding hash for $path" }
                    val hashEntity = HashEntity(hash, sessionId)
                    !effect { repository.addHash(hashEntity) }
                }
            }
        }
    }

    private fun hashIsRenewable(entity: Option<FileEntity>, lastModified: Long): Boolean {
        return entity is Some
                && entity.t.dateModified == lastModified
                && entity.t.hash is Some
    }
}
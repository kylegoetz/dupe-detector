package photo.backup.kt.domain

import arrow.core.Option
import arrow.core.Some
import arrow.fx.IO
import arrow.fx.extensions.io.monad.flatten
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
import photo.backup.kt.data.source.IBackupRepository

typealias ComputeHashUseCase = (ByteArray, String, Long, StageType) -> IO<HashId>
fun generateComputeHashUseCase(repository: IBackupRepository,
                               hashFn: (ByteArray)->Hash,
                               sessionId: SessionId): (ByteArray, String, Long, StageType)->IO<HashId> {

    return { data, pathStr, lastModified, stage ->
        IO.effect { repository.getBackup(pathStr, stage) }.map {
            when(determineBranch(it, lastModified)) {
                is RedoBranch -> hashSaveAndGetId(data, hashFn, repository, sessionId)
                is RenewableBranch -> {
                    val entity = (it as Some).t
                    val hashId = (entity.hash as Some).t
                    updateCase(hashId, sessionId, repository)
                }
            }
        }.flatten()
    }
}

private fun isRenewable(entity: FileEntity, lastModified: Long): Boolean =
    (entity.dateModified == lastModified && entity.hash is Some)

private val hashSaveAndGetId: (data: ByteArray, hashFn: (ByteArray) -> Hash, repository: IBackupRepository, sessionId: SessionId) -> IO<HashId> = { data, hashFn, repository, sessionId ->
    IO.effect { repository.upsertHash(HashEntity(hashFn(data), sessionId)) }
}

private fun updateCase(id: HashId, sessionId: SessionId, repository: IBackupRepository): IO<HashId> =
    IO.effect { repository.renewHash(id, sessionId) }.map { id }

private sealed class HashBranch {
    sealed class MustRedo: HashBranch() {
        object NoEntity: MustRedo()
        object ExpiredHash: MustRedo()
        object NoHash: MustRedo()
    }
    object Renewable: HashBranch()
}
private typealias NoEntityBranch=HashBranch.MustRedo.NoEntity
private typealias ExpiredBranch=HashBranch.MustRedo.ExpiredHash
private typealias NoHashBranch=HashBranch.MustRedo.NoHash
private typealias RenewableBranch=HashBranch.Renewable
private typealias RedoBranch=HashBranch.MustRedo

private fun determineBranch(maybeEntity: Option<FileEntity>, lastModified: Long): HashBranch {
    return maybeEntity.fold({NoEntityBranch}, { entity ->
        entity.hash.fold({
            NoHashBranch
        }, { // very odd error
            val rv: HashBranch = when(isRenewable(entity, lastModified)) {
                true -> RenewableBranch
                false -> ExpiredBranch
            }
            rv
        })
    })
}

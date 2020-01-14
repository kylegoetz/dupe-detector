package photo.backup.kt.data.source

import arrow.core.Either
import arrow.core.Option
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
import java.io.File
import java.util.*

interface IBackupRepository {
    fun disconnect()
    suspend fun backedUp(entity: FileEntity): Option<MediaId>
    suspend fun backUp(entity: FileEntity): MediaId
//    suspend fun update(entity: FileEntity): Boolean
    suspend fun getBackup(path: String, stage: StageType): Option<FileEntity>
    suspend fun upsertFile(entity: FileEntity): MediaId

    // Hash
    suspend fun getHashId(hash: Hash): Option<HashId>
    suspend fun getHash(id: HashId): Option<HashEntity>
    suspend fun upsertHash(hash: HashEntity): HashId

    // Backups
//    suspend fun photoBackedUp(checksum: Hash): Boolean

    // All
    suspend fun renewHash(hashId: HashId, sessionId: SessionId)
    suspend fun getSourceImagesWithBackups(sessionId: SessionId): Either<Throwable, List<SourceFileEntity>>

    suspend fun getFileModificationDate(canonicalPath: String, stage: StageType): Option<Long>

    /**
     * This function should persist information about a file that was not backup-able. It's for future
     * work on increasing backed-up types.
     */
    suspend fun createUnknownFile(file: File, sessionId: SessionId): Either<RepositoryException, UUID>

    suspend fun getUnknownFiles(sessionId: SessionId): List<UnknownFileRow>

    /**
     * Search source video files for anything with given size
     */
    suspend fun findSourceByFileSize(size: Long, sessionId: SessionId): List<SourceFileEntity>

    // return value is number of rows updated
    suspend fun updateSessionIds(table: StageType, files: List<File>, session: SessionId): Int
}
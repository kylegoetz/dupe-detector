package photo.backup.kt.data

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.jetbrains.exposed.dao.UUIDTable
import org.jetbrains.exposed.sql.Column
import photo.backup.kt.SessionId
import java.io.File
import java.util.UUID


sealed class FileEntity {
    abstract val id: MediaId
    abstract val absolutePath: String
    abstract val hash: Option<HashId>
    abstract val size: Long
    abstract val dateModified: Long
    abstract val sessionId: SessionId
    abstract val type: Media

    abstract fun changeId(id: UUID): FileEntity
    abstract fun changeId(id: MediaId): FileEntity
    abstract fun copy(sessionId: SessionId): FileEntity
    abstract fun changeHashId(id: Option<HashId>): FileEntity
}

object EntityFactory {
    fun build(row: MediaRow): FileEntity {
        val stage = when(row) {
            is SourceRow -> Source
            is BackupRow -> Backup
        }

        return build(stage, File(row.absolutePath), row.hash?.run { Some(HashId(this)) } ?: None, row.type, SessionId(row.sessionId))
    }

    /**
     * Try to use this as little as possible since it doesn't have a session ID
     */
    fun build(stage: StageType, file: File, hashId: Option<HashId>, type: Media) = build(stage, file, hashId, type, SessionId(UUID.randomUUID()))

    fun build(stage: StageType, file: File, hashId: Option<HashId>, type: Media, sessionId: SessionId): FileEntity {
        return when(stage) {
            is Source -> SourceFileEntity(
                    absolutePath =file.canonicalPath,
                    hash =hashId,
                    size =file.length(),
                    dateModified =file.lastModified(),
                    sessionId = sessionId,
                    type = type
            )
            is Backup -> GoogleFileEntity(
                    absolutePath =file.canonicalPath,
                    hash =hashId,
                    size =file.length(),
                    dateModified =file.lastModified(),
                    sessionId = sessionId,
                    type = type
            )
        }
    }
}

data class GoogleFileEntity(
        override val absolutePath: String,
        override val hash: Option<HashId>,
        override val size: Long,
        override val dateModified: Long,
        override val sessionId: SessionId,
        override val type: Media,
        override val id: GooglePhotoId = GooglePhotoId(UUID.randomUUID())
): FileEntity() {
    override fun changeId(id: MediaId) = copy(id=id as GooglePhotoId)
    override fun changeId(id: UUID) = copy(id=GooglePhotoId(id))
    override fun copy(sessionId: SessionId): GoogleFileEntity = copy(sessionId=sessionId, id=id)
    override fun changeHashId(id: Option<HashId>): GoogleFileEntity = copy(hash=id)
}

data class SourceFileEntity(
    override val absolutePath: String,
    override val hash: Option<HashId>,
    override val size: Long,
    override val dateModified: Long,
    override val sessionId: SessionId,
    override val type: Media,
    override val id: SourcePhotoId = SourcePhotoId(UUID.randomUUID())
): FileEntity() {
    override fun changeId(id: UUID) = copy(id=SourcePhotoId(id))
    override fun changeId(id: MediaId) = copy(id=id as SourcePhotoId)
    override fun copy(sessionId: SessionId): SourceFileEntity = copy(sessionId=sessionId, id=id)
    override fun changeHashId(id: Option<HashId>): SourceFileEntity = copy(hash=id)
}

enum class Media {
    IMAGE, VIDEO
}

sealed class MediaTable(name: String): UUIDTable(name) {
    val absolutePath = text("absolute_path").index(isUnique=true)
    val hash = uuid("hash").nullable()
    val size: Column<Long> = long("size")
    val dateModified: Column<Long> = long("date_modified")
    val sessionId: Column<UUID> = uuid("session_id")
    val type: Column<Media> = enumeration("media_type", Media::class)

    object Source: MediaTable("source_files")
    object Backup: MediaTable("backup_files")
}
typealias SourceTable=MediaTable.Source
typealias BackupTable=MediaTable.Backup


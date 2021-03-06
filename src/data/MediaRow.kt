package photo.backup.kt.data

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import java.util.*

sealed class MediaRow(id: EntityID<UUID>): UUIDEntity(id) {
    abstract var absolutePath: String
    abstract var hash: UUID? // this points to Hash.id, which is a UUID, it is NOT a UUID generated by a hash
    abstract var size: Long
    abstract var dateModified: Long
    abstract var sessionId: UUID
    abstract var type: Media

    class Source(id: EntityID<UUID>): MediaRow(id) {
        companion object : UUIDEntityClass<Source>(SourceTable)
        override var absolutePath: String by SourceTable.absolutePath
        override var hash: UUID? by SourceTable.hash
        override var size: Long by SourceTable.size
        override var dateModified: Long by SourceTable.dateModified
        override var sessionId: UUID by SourceTable.sessionId
        override var type: Media by SourceTable.type
    }

    class Backup(id: EntityID<UUID>): MediaRow(id) {
        companion object: UUIDEntityClass<Backup>(BackupTable)
        override var absolutePath: String by BackupTable.absolutePath
        override var hash: UUID? by BackupTable.hash
        override var size: Long by BackupTable.size
        override var dateModified: Long by BackupTable.dateModified
        override var sessionId: UUID by BackupTable.sessionId
        override var type: Media by BackupTable.type
    }
}
typealias SourceRow=MediaRow.Source
typealias BackupRow=MediaRow.Backup
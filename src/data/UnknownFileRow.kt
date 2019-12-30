package photo.backup.kt.data

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import photo.backup.kt.SessionId
import java.io.File
import java.util.*

class UnknownFileRow(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<UnknownFileRow>(UnknownFileTable)
    var absolutePath: String by UnknownFileTable.absolutePath
    var extension: String by UnknownFileTable.extension
    var session: UUID by UnknownFileTable.session
}

object UnknownFileTable: UUIDTable() {
    val absolutePath = text("absolute_path")
    val extension = varchar("file_extension", 50)
    val session = uuid("session_id")
}

data class UnknownFile(val file: File, val session: SessionId) {
    constructor(row: UnknownFileRow) : this(File(row.absolutePath), SessionId(row.session))

    val extension = file.extension
}
package photo.backup.kt.data

import org.jetbrains.exposed.dao.*
import photo.backup.kt.SessionId
import java.util.UUID

inline class HashId(val value: UUID)
inline class Hash(val value: String)


data class HashEntity(
    val hash: Hash,
    val sessionId: SessionId,
    val id: HashId = HashId(UUID.randomUUID())
) {
    constructor(row: HashRow) : this(hash=Hash(row.hash), sessionId=SessionId(row.sessionId), id=HashId(row.id.value))
}

object HashTable : UUIDTable(name="hashes") {
    val hash = varchar("hash", 50)
    val sessionId = uuid("session_id")
    val hashIndex = index(true, hash)
}

class HashRow(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<HashRow>(HashTable)

    var hash by HashTable.hash
    var sessionId by HashTable.sessionId
}
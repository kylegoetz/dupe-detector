package photo.backup.kt.data.source

import arrow.core.*
import arrow.core.extensions.fx
import ch.frankel.slf4k.trace
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
import photo.backup.kt.data.SourceFileTable.absolutePath
import photo.backup.kt.domain.StageType
import photo.backup.kt.domain.backup
import photo.backup.kt.domain.source
import java.io.File
import java.sql.Connection
import java.util.*
import javax.sql.DataSource

enum class RepoType {
    PROD, TEST
}

private val logger = LoggerFactory.getLogger("test")

object BackupRepository : IBackupRepository {

    private var database: Database? = null

    fun newInstance(type: RepoType, path: String = "backup_test.db"): IBackupRepository {
        database =  database ?: when(type) {
            RepoType.TEST -> Database.connect("jdbc:sqlite::memory:", "org.sqlite.JDBC")
            RepoType.PROD -> Database.connect("jdbc:sqlite:$path", "org.sqlite.JDBC")
        }
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction {
            SchemaUtils.create(HashTable, SourceTable, BackupTable, UnknownFileTable)
            if(type == RepoType.TEST) { // To keep in-memory DB alive "between" transactions" to forcing the initial transaction to never close
                GlobalScope.launch {
                    while(true) {
                        delay(10000)
                    }
                }
            }
        }
        return this
    }

    override fun disconnect() {
        database = null
    }

    override suspend fun backedUp(entity: FileEntity): Option<MediaId> = transaction(database) {
        logger.trace { "Is ${entity.absolutePath} backed up?"}
        val adjust: (FileEntity, UUID) -> MediaId = { entity, id ->
            when(entity) {
                is SourceFileEntity -> SourcePhotoId(id)
                is GoogleFileEntity -> GooglePhotoId(id)
            }
        }
        val (row, table) = when(entity) {
            is SourceFileEntity -> Pair(SourceRow, SourceTable)
            is GoogleFileEntity -> Pair(BackupRow, BackupTable)
        }
        val result = row.find {
            table.absolutePath eq entity.absolutePath
        }.toList()
        when(result.isEmpty()) {
            true -> None
            false -> Some(adjust(entity, result.first().id.value))
        }
    }

    override suspend fun backUp(entity: FileEntity): MediaId {
        logger.trace { "Back up ${entity.absolutePath}"}
        val (row: UUIDEntityClass<MediaRow>, table: MediaTable) = when(entity) {
            is GoogleFileEntity -> Pair(BackupRow, BackupTable)
            is SourceFileEntity -> Pair(SourceRow, SourceTable)
        }
        val id: UUID = transaction(database) {
            val id = table.insert {
                it[absolutePath] = entity.absolutePath
                it[hash] = entity.hash.map { it.value }.orNull()
                it[size] = entity.size
                it[dateModified] = entity.dateModified
                it[sessionId] = entity.sessionId.value
                it[type] = entity.type
            } get table.id
            id
        }.value
        return when(entity) {
            is GoogleFileEntity -> GooglePhotoId(id)
            is SourceFileEntity -> SourcePhotoId(id)
        }
    }

    override suspend fun update(entity: FileEntity) = transaction {
//        addLogger(StdOutSqlLogger)
    }

    override suspend fun getFileModificationDate(canonicalPath: String, stage: StageType): Option<Long> = transaction {
        val (row, table) = when(stage) {
            is source -> Pair(SourceRow, SourceTable)
            is backup -> Pair(BackupRow, BackupTable)
        }
        row.find {
            table.absolutePath eq canonicalPath
        }.firstOrNone().map {
            it.dateModified
        }
    }

    override suspend fun getBackup(path: String, stageType: StageType): Option<FileEntity> = transaction {
        logger.trace { "Get backup $stageType for $path"}
        val (row, table) = when(stageType) {
            is source -> Pair(SourceRow, SourceTable)
            is backup -> Pair(BackupRow, BackupTable)
        }
        row.find {
            table.absolutePath eq path
        }.firstOrNone().map {
            EntityFactory.build(it)
        }
    }

    override suspend fun addHash(hash: HashEntity): HashId {
        logger.trace { "Adding a hash" }
        val id = transaction(database) {
            HashTable.insert {
                it[this.hash] = hash.hash.value
                it[sessionId] = hash.sessionId.value
            } get HashTable.id
        }

        return HashId(id.value)
    }

    override suspend fun getHashId(hash: Hash): Option<HashId> = transaction(database) {
        logger.trace { "Getting a hash ID"}
        val rows = HashRow.find {
            HashTable.hash eq hash.value
        }.limit(1)
        when(rows.count()) {
            0 -> None
            else -> Some(HashId(rows.first().id.value))
        }
    }

    override suspend fun getHash(id: HashId): Option<HashEntity> = transaction(database) {
        logger.trace { "Getting a hash"}
        val result: HashRow? = HashRow.findById(id.value)
        when(result == null) {
            true -> None
            false -> Some(HashEntity(result))
        }
    }

    override suspend fun renewHash(hashId: HashId, sessionId: SessionId) = transaction {
        logger.trace { "renewing a hash"}
        HashRow.findById(hashId.value)?.sessionId = sessionId.value
    }


    override suspend fun pathExists(path: File, isSource: Boolean): Option<UUID> = transaction {
        with(when(isSource) {
            true -> SourceRow
            false -> BackupRow
        }) {
            find {
                absolutePath eq path.canonicalPath
            }.run {
                when(count()) {
                    0 -> None
                    else ->  Some(first().id.value)
                }
            }
        }
    }

    override suspend fun deleteStaleBackupEntries(sessionId: SessionId) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun deleteStaleOriginalEntries(sessionId: SessionId) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun deleteStaleHashEntries(sessionId: SessionId) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun getSourceImagesWithBackups(sessionId: SessionId): Either<Throwable, List<SourceFileEntity>> = transaction {
        logger.trace { "Getting source image with backups" }
        Either.fx {

            val join = SourceTable.join(BackupTable, JoinType.INNER, SourceTable.hash, BackupTable.hash)
            val query = join.select { SourceTable.sessionId eq sessionId.value }
            query.toList().map {
                EntityFactory.build(
                        source,
                        File(it[SourceTable.absolutePath]),
                        it[SourceTable.hash]?.run { Some(HashId(this)) } ?: None,
                        it[SourceTable.type]
                ) as SourceFileEntity
            }
        }
    }

    override suspend fun getCurrentEntities(sessionId: SessionId): List<SourceFileEntity> = transaction {
        SourceRow.find {
            SourceTable.sessionId eq sessionId.value
        }.map { EntityFactory.build(it) as SourceFileEntity }
    }

    override suspend fun isFileChanged(canonicalPath: String, lastModified: Long): Boolean = transaction {
        !SourceRow.find {
            SourceTable.absolutePath eq canonicalPath
            SourceTable.dateModified eq lastModified
        }.empty()
    }

    override suspend fun createUnknownFile(file: File, sessionId: SessionId): Either<RepositoryException, UUID> = transaction {
        logger.trace { "Creating unknown for ${file.extension}"}
        try {
            val result = UnknownFileTable.insert {
                it[absolutePath] = file.absolutePath
                it[extension] = file.extension
                it[session] = sessionId.value
            } get UnknownFileTable.id
            Either.Right(result.value)
        } catch(e: Throwable) {
            if(e is IllegalStateException && e.message?.contains("because exceeds length")==true) {
                Either.Left(RepositoryException.ValueExceedsFieldLength(e.message!!))
            }
            Either.Left(RepositoryException.UnknownError(e))
        }
    }

    override suspend fun getUnknownFiles(sessionId: SessionId): List<UnknownFileRow> = transaction {
        logger.trace { "Getting unknown files"}
        UnknownFileRow.find {
            UnknownFileTable.session eq sessionId.value
        }.toList()
    }

    override suspend fun findSourceByFileSize(size: Long, sessionId: SessionId): List<SourceFileEntity> = transaction {
        logger.trace { "Find source by file size"}
        SourceRow.find {
            SourceTable.size eq size
            SourceTable.sessionId eq sessionId.value
        }.toList().map {
            EntityFactory.build(it) as SourceFileEntity
        }
    }

}

sealed class RepositoryException {
    data class ValueExceedsFieldLength(val message: String): RepositoryException()
    data class UnknownError(val error: Throwable) : RepositoryException()
}
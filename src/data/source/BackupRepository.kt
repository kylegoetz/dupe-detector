package photo.backup.kt.data.source

import arrow.core.*
import arrow.core.extensions.fx
import ch.frankel.slf4k.trace
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.sqlite.SQLiteException
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
//import photo.backup.kt.data.SourceFileTable.absolutePath
import java.io.File
import java.sql.Connection
import java.util.*

enum class RepoType {
    PROD, TEST
}

private val logger = LoggerFactory.getLogger("test")

object BackupRepository : IBackupRepository {

    private var database: Database? = null

    fun newInstance(type: RepoType, path: String = "backup_test.db"): IBackupRepository {
        val shard = when(type) {
            RepoType.TEST -> ":memory:"
            RepoType.PROD -> path
        }
        val cfg: HikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:$shard"
            maximumPoolSize = 6
        }

        val dataSource = HikariDataSource(cfg)
        database = Database.connect(dataSource)
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction(database) {
            SchemaUtils.create(HashTable, SourceTable, BackupTable, UnknownFileTable)
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

    /**
     * Returns true if updated, false if nothing to update
     */
    private suspend fun update(entity: FileEntity): Boolean = transaction {
        val (row, table) = when(entity) {
            is GoogleFileEntity -> Pair(BackupRow, BackupTable)
            is SourceFileEntity -> Pair(SourceRow, SourceTable)
        }
        row.findById(entity.id.value)?.run {
            absolutePath = entity.absolutePath
            hash = entity.hash.orNull()?.value
            size = entity.size
            dateModified = entity.dateModified
            sessionId = entity.sessionId.value
            type = entity.type
            true
        } ?: false
    }

    override suspend fun getFileModificationDate(canonicalPath: String, stage: StageType): Option<Long> = transaction {
        val (row, table) = when(stage) {
            is Source -> Pair(SourceRow, SourceTable)
            is Backup -> Pair(BackupRow, BackupTable)
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
            is Source -> Pair(SourceRow, SourceTable)
            is Backup -> Pair(BackupRow, BackupTable)
        }
        row.find {
            table.absolutePath eq path
        }.firstOrNone().map {
            EntityFactory.build(it)
        }
    }

    override suspend fun upsertFile(entity: FileEntity): MediaId = transaction {
        logger.trace { "upserting ${entity.absolutePath}"}
        val (row, table) = when(entity) {
            is SourceFileEntity -> Pair(SourceRow, SourceTable)
            is GoogleFileEntity -> Pair(BackupRow, BackupTable)
        }
        row.find {
            table.absolutePath eq entity.absolutePath
        }.firstOrNone().fold({
            logger.trace { "upsert - inserting ${entity.absolutePath} for session ${entity.sessionId}"}
            val id = runBlocking { backUp(entity) }
            id
        },{
            val id = it.id.value
            val updatedEntity = entity.changeId(id)
            logger.trace { "upsert - updating ${updatedEntity.absolutePath} for session ${updatedEntity.sessionId}"}
            runBlocking { update(updatedEntity) }
            updatedEntity.id
        })
    }

    override suspend fun upsertHash(entity: HashEntity): HashId = transaction(database) {
        logger.trace { "Upserting a hash"}
        val hashes = HashRow.find { HashTable.hash eq entity.hash.value }
        when(hashes.count()) {
            0 -> addHash(entity)
            else -> hashes.forEach { it.sessionId = entity.sessionId.value }.run {
                HashId(hashes.first().id.value)
            }
        }
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

    /**
     * Returns either an error or a list of source files that have the current sessionId and have backups corresponding with same session ID
     */
    override suspend fun getSourceImagesWithBackups(sessionId: SessionId): Either<Throwable, List<SourceFileEntity>> = transaction {
        logger.trace { "Getting source image with backups" }
        Either.fx {
            val join = SourceTable.join(BackupTable, JoinType.INNER, SourceTable.hash, BackupTable.hash)
            val query = join.select { (SourceTable.sessionId eq sessionId.value) and (BackupTable.sessionId eq sessionId.value) }
            query.toList().map {
                EntityFactory.build(
                    Source,
                    File(it[SourceTable.absolutePath]),
                    it[SourceTable.hash]?.run { Some(HashId(this)) } ?: None,
                    it[SourceTable.type],
                    sessionId
                ) as SourceFileEntity
            }
        }
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
            SourceTable.size eq size and (SourceTable.sessionId eq sessionId.value)
        }.toList().map {
            EntityFactory.build(it) as SourceFileEntity
        }
    }

    /**
     * entries is a list of File
     */
    override suspend fun updateSessionIds(stage: StageType, entries: List<File>, session: SessionId): Int = transaction(database) {
        val table = when(stage) {
            Source -> SourceTable
            Backup -> BackupTable
        }
        table.update({ table.absolutePath inList(entries.map {it.canonicalPath})}) {
            it[sessionId] = session.value
        }
    }


    private fun addHash(hash: HashEntity): HashId {
        logger.trace { "Adding a hash" }
        return transaction(database) {
            HashTable.insert {
                it[this.hash] = hash.hash.value
                it[sessionId] = hash.sessionId.value
            } get HashTable.id
        }.run { HashId(this.value) }
    }
}

sealed class RepositoryException {
    data class ValueExceedsFieldLength(val message: String): RepositoryException()
    data class UnknownError(val error: Throwable) : RepositoryException()
}
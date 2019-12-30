package photo.backup.kt.domain

import arrow.core.*
import arrow.fx.IO
import arrow.fx.extensions.fx
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
import photo.backup.kt.data.source.IBackupRepository
import java.io.File
import java.util.*

sealed class StageType
object source: StageType()
object backup: StageType()

class SavePhotoInfoUseCase(
        private val repository: IBackupRepository,
        private val stageType: StageType,
        private val existsFn: (Hash)-> IO<Option<MediaId>>,
        private val repoSaveFn: suspend (FileEntity) -> MediaId,
        private val repoUpdateFn: suspend (FileEntity)->Unit,
        private val sessionId: SessionId = SessionId(UUID.randomUUID())) {

    operator fun invoke(file: File, hashIdOpt: Option<HashId>, mediaType: Media) = IO.fx {
        val entity: FileEntity = when(stageType) {
            is backup -> {
                GoogleFileEntity(absolutePath=file.canonicalPath,
                        hash=hashIdOpt,
                        size=file.length(),
                        sessionId=sessionId,
                        type=mediaType,
                        dateModified=file.lastModified())
            }
            is source -> {
                SourceFileEntity(absolutePath=file.canonicalPath, hash=hashIdOpt, size=file.length(),
                        dateModified=file.lastModified(), sessionId=sessionId, type=mediaType)
            }
        }

//        val (hashEntity: Option<HashEntity>) = effect { repository.getHash(hashId) }
//        when(hashEntity) {
//            is None -> { TODO("WE HAVE A PROBLEM") }
//            is Some -> {
//                val hashId = hashEntity.t.id
//            }
//        }

//        when(existsFn(hashEntity.hash).bind()) {
//            is None -> {
//                !effect { repoSaveFn(entity) }
//            }
//            is Some -> {
//                !effect { repoUpdateFn(entity) }
//            }
//        }
    }
}
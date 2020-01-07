@file:JvmName("VideoKt")

package photo.backup.kt

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.fx.IO
import arrow.fx.extensions.fx
import org.slf4j.LoggerFactory
import photo.backup.kt.data.*
import photo.backup.kt.data.source.IBackupRepository
import java.io.File
import photo.backup.kt.util.hasher
import ch.frankel.slf4k.*
import photo.backup.kt.data.source.HashResult

private val logger = LoggerFactory.getLogger("test")

data class Video(private val sessionId: SessionId, private val repository: IBackupRepository, private val walker: (File)->Sequence<File>, private val hasher: suspend (File)->Hash) {
    fun storeSourceVideo(file: File): IO<SourcePhotoId> = IO.fx {
        logger.debug { "Storing video ${file.canonicalPath} in repository" }
        val sourceFileEntity = SourceFileEntity(
                absolutePath = file.canonicalPath,
                size=file.length(),
                dateModified = file.lastModified(),
                hash= None,
                sessionId=sessionId,
                type= Media.VIDEO
        )
        !effect { repository.upsertFile(sourceFileEntity) } as SourcePhotoId
    }

    /**
     * Determines if a backup video needs to be stored. It needs to be stored only if there is a video of the same
     * size in the source directory being scanned.
     */
    fun shouldStoreBackupVideo(file: File): IO<Boolean> = IO.fx {
        val targetSize = file.length()
        val (files) = effect { repository.findSourceByFileSize(targetSize, sessionId) }
        when(files.isEmpty()) {
            true -> false
            false -> {
                files.forEach {
                    logger.debug { "${it.absolutePath} might match ${file.canonicalPath}"}
                    val hash: Hash = !effect { hasher(File(it.absolutePath)) }
                    val hashId: HashId = !effect { repository.upsertHash(HashEntity(hash, sessionId)) }
                    val entity = it.copy(hash=Some(hashId), sessionId=sessionId)
                    !effect { repository.update(entity) }
//                    val hashIdResult: Either<HashResult, HashId> = !effect { repository.addHash(HashEntity(hash, sessionId))}
//                    hashIdResult.fold({
//                        logger.error { ""}
//                    },{ hashId ->
//                        val entity = it.copy(hash= Some(hashId), sessionId=sessionId)
//                        !effect { repository.update(entity) }
//                    })
                }
                true
            }
        }
    }

//    fun hashBackupVideo(file: File): IO<Either<Throwable, Hash>> = IO.fx {
//        logger.debug("Hashing ${file.canonicalPath}")
//        !effect { hasher(file) }.attempt()
//    }

//    fun storeBackup(file: File, hash: Boolean): IO<Either, Throwable, GooglePhotoId> = IO.fx {
//        val hash: Either<Throwable, Option<Hash>> = when(shouldStoreBackupVideo(file).bind()) {
//            true -> hashBackupVideo(file).bind().map { Some(it) }
//            false -> Either.Right(None)
//        }
//    }

    fun storeBackupVideo(file: File): IO<Either<Throwable, GooglePhotoId>> = IO.fx {
        logger.debug("Hashing and storing video ${file.canonicalPath}")
        val hash = !effect { hasher(file) }
        val hashId: HashId = !effect {repository.upsertHash(HashEntity(hash, sessionId))}
//        val hashOpt = !effect { repository.getHashId(hash) }
//        val hashId: HashId = when(hashOpt) {
//            is None -> (!effect { repository.addHash(HashEntity(hash, sessionId)) }).fold({
//                (!effect { repository.getHashId(hash) } as Some).t
//            },{
//                it
//            })
//            is Some -> {
//                !effect { repository.renewHash(hashOpt.t, sessionId) }
//                hashOpt.t
//            }
//        }
        val entity = GoogleFileEntity(
                absolutePath=file.canonicalPath,
                size=file.length(),
                dateModified = file.lastModified(),
                sessionId=sessionId,
                type= Media.VIDEO,
                hash= Some(hashId)
        )
        with(!effect { repository.backedUp(entity) }) {
            when(this) {
                is None -> !effect { repository.backUp(entity) as GooglePhotoId }
                is Some -> {
                    !effect { repository.update(entity) }
                    t as GooglePhotoId
                }
            }
        }
    }.attempt()

    fun runVideoPortion(
            sourcePath: String,
            backupPath: String,
            videoScan: (String) -> IO<Sequence<File>> = ::videoScan,
            storeSource: (File)->IO<SourcePhotoId> = ::storeSourceVideo,
            storeBackupVideo: (File)->IO<Either<Throwable,GooglePhotoId>> = ::storeBackupVideo,
            shouldStoreBackupVideo: (File) -> IO<Boolean> = ::shouldStoreBackupVideo): IO<Sequence<Option<GooglePhotoId>>> = IO.fx {

        val (files: Sequence<File>) = videoScan(sourcePath)
        files.forEach { file -> storeSource(file).unsafeRunSync() }
        val (backupFiles: Sequence<File>) = videoScan(backupPath)
        backupFiles.map { file ->
            IO.fx {
                val (should: Boolean) = shouldStoreBackupVideo(file)
                when(should) {
                    true -> {
                        val result = storeBackupVideo(file).bind()
                        result.fold({
                            logger.error { "${file.canonicalPath} : $it"}
                            None
                        },{ Some(it) })
                    }
                    false -> None
                }
            }.unsafeRunSync()
        }
    }

    fun videoScan(root: String): IO<Sequence<File>> = IO {
        walker(File(root))
                .filter { it.extension.toLowerCase() in VIDEO_EXTENSIONS }
    }
}


fun videoMain(args: Array<String>, sessionId: SessionId, repository: IBackupRepository): IO<Sequence<Option<GooglePhotoId>>> {
    logger.info("Beginning video scanning")
    with(Video(sessionId, repository, ::walker) { hasher(videoReader(it))}) {
        return runVideoPortion(args[0], args[1])
    }
}

fun walker(root: File): Sequence<File> =
    root.walk().onEnter { dir ->
        logger.debug { "Scanning for videos in $dir" }
        FORBIDDEN_PATHS.all { !dir.canonicalPath.endsWith(it) }.also {
            when(it) {
                true -> logger.trace { "Scanning $dir"}
                false -> logger.trace { "Skipping $dir"}
            }
        }
    }.onLeave {
        logger.trace { "Done scanning for videos in $it"}
    }.onFail { dir, exception ->
        logger.error { "Failed to scan $dir: $exception" }
    }.filter { it.isFile.also { bool ->
        logger.trace { "Filter is${if(!bool)" not" else ""} allowing $it to be processed"}
    } }
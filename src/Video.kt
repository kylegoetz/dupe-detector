@file:JvmName("VideoKt")

package photo.backup.kt

import arrow.Kind
import arrow.core.*
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.applicative.applicative
import org.slf4j.LoggerFactory
import photo.backup.kt.data.*
import photo.backup.kt.data.source.IBackupRepository
import java.io.File
import arrow.core.extensions.option.traverse.traverse
import arrow.fx.fix

//private val logger = LoggerFactory.getLogger("test")

//data class Video(private val sessionId: SessionId, private val repository: IBackupRepository, private val walker: (File)->Sequence<File>, private val hasher: (File)->IO<Either<Throwable,Hash>>) {

//    fun storeSourceVideo(file: File): IO<SourcePhotoId> {
//        val entity = EntityFactory.build(source, file, Media.VIDEO)
//        return StoreVideoUseCase(repository, sessionId, hasher)(entity).map {
//            it.getOrHandle { SourcePhotoId(UUID.randomUUID()) } as SourcePhotoId // We know the parent will not throw
//        }
//    }

//    fun storeSourceVideo2(file: File): IO<SourcePhotoId> = IO.fx {
//        logger.debug { "Storing video ${file.canonicalPath} in repository" }
//        val sourceFileEntity = SourceFileEntity(
//                absolutePath = file.canonicalPath,
//                size=file.length(),
//                dateModified = file.lastModified(),
//                hash= None,
//                sessionId=sessionId,
//                type= Media.VIDEO
//        )
//        !effect { repository.upsertFile(sourceFileEntity) } as SourcePhotoId
//    }

//    fun shouldStoreBackupVideo(file: File): IO<Boolean>
//            = ShouldStoreBackupVideoUseCase(repository, sessionId, hasher)(file)

    /**
     * Determines if a backup video needs to be stored. It needs to be stored only if there is a video of the same
     * size in the source directory being scanned.
     */
//    fun shouldStoreBackupVideo(file: File): IO<Boolean> = IO.fx {
//        val targetSize = file.length()
//        logger.trace { "${file.canonicalPath} is of size $targetSize. Looking for same-sized in source."}
//        val (files) = effect { repository.findSourceByFileSize(targetSize, sessionId) }
//        when(files.isEmpty()) {
//            true -> false
//            false -> {
//                files.forEach {sourceEntity ->
//                    logger.debug { "${sourceEntity.absolutePath} might match ${file.canonicalPath}"}
//                    val either: Either<Throwable, Hash> = hasher(File(sourceEntity.absolutePath)).bind()
//                    either.fold({
//                        logger.error { "Hasher failed for ${sourceEntity.absolutePath} : $it"}
//                    }, {
//                        val hashId: HashId = !effect { repository.upsertHash(HashEntity(it, sessionId))}
//                        val entity = sourceEntity.copy(hash=Some(hashId), sessionId=sessionId)
//                        !effect { repository.upsertFile(entity) }
//                    })
//
////                        .map { hash ->
////                        val hashId: HashId = !effect { repository.upsertHash(HashEntity(hash, sessionId))}
////                    }
////                    val hash: Hash = !effect { hasher(File(it.absolutePath)) }
////                    val hashId: HashId = !effect { repository.upsertHash(HashEntity(hash, sessionId)) }
////                    val entity = it.copy(hash=Some(hashId), sessionId=sessionId)
////                    !effect { repository.upsertFile(entity) }
//                }
//                true
//            }
//        }
//    }

//    fun storeBackupVideo(file: File): IO<Either<Throwable, GooglePhotoId>> {
//        val entity = EntityFactory.build(backup, file, Media.VIDEO)
//        return StoreVideoUseCase(repository, sessionId, hasher)(entity).map { it.map { it as GooglePhotoId } }
//    }

//    fun storeBackupVideo(file: File): IO<Either<Throwable, GooglePhotoId>> = IO.fx {
//        val hashEither: Either<Throwable, Hash> = hasher(file).bind()
//        when(hashEither is Either.Right) {
//            false -> {
//                logger.error { "Hasher failed for ${file.canonicalPath}"}
//               hashEither as Either.Left
//            }
//            true -> {
//                val hash: Hash = hashEither.b
//                val hashId: HashId = !effect { repository.upsertHash(HashEntity(hash, sessionId))}
//                val entity = GoogleFileEntity(
//                    absolutePath=file.canonicalPath,
//                    size=file.length(),
//                    dateModified = file.lastModified(),
//                    sessionId=sessionId,
//                    type= Media.VIDEO,
//                    hash= Some(hashId)
//                )
//                val id = !effect { repository.upsertFile(entity) as GooglePhotoId }
//                Either.right(id)
//            }
//        }
//    }

//    fun runVideoPortion(
//            sourcePath: String,
//            backupPath: String,
//            videoScan: (String) -> IO<Sequence<File>> = ::videoScan,
//            storeSource: (File)->IO<SourcePhotoId> = ::storeSourceVideo,
//            storeBackupVideo: (File)->IO<Either<Throwable,GooglePhotoId>> = ::storeBackupVideo,
//            shouldStoreBackupVideo: (File) -> IO<Boolean> = ::shouldStoreBackupVideo): IO<Sequence<Option<GooglePhotoId>>> = IO.fx {
//
//        val (files: Sequence<File>) = videoScan(sourcePath)
//        files.forEach { file -> storeSource(file).unsafeRunSync() }
//        val (backupFiles: Sequence<File>) = videoScan(backupPath)
//        backupFiles.map { file ->
//            IO.fx {
//                val (should: Boolean) = shouldStoreBackupVideo(file)
//                when(should) {
//                    true -> {
//                        val result: Either<Throwable, GooglePhotoId> = storeBackupVideo(file).bind()
//                        result.fold({
//                            logger.error { "${file.canonicalPath} : ${it.message}"}
//                            None
//                        },{
//                            logger.debug { "${file.canonicalPath} has backup ID ${it.value}"}
//                            Some(it)
//                        })
//                    }
//                    false -> {
//                        logger.trace { "${file.canonicalPath} has no same-sized files in source. Skipping."}
//                        None
//                    }
//                }
//            }.unsafeRunSync()
//        }
//    }

//    fun videoScan(root: String): IO<Sequence<File>> = IO {
//        walker(File(root))
//                .filter { it.extension.toLowerCase() in VIDEO_EXTENSIONS }
//    }
//    fun videoScan(root: String): IO<Sequence<File>> = ScanVideoUseCase(walker)(root)
//}

fun createRunner(
    sessionId: SessionId,
    scanVideoUseCase: (String)->IO<Sequence<File>>,
    storeFileUseCase: (FileEntity)->IO<Either<Throwable, MediaId>>,
    shouldStoreBackupVideo: (File)->IO<Boolean>): (PathArgs)->IO<Kind<ForOption, Sequence<Option<GooglePhotoId>>>> {

    return { pathArgs ->
        val sourceFileCountProgram: IO<Kind<ForOption, Int>> = pathArgs.source.traverse(IO.applicative()) {
            IO.fx {
                val files = scanVideoUseCase(it.canonicalPath).bind()
                files.k().traverse(IO.applicative()) {
                    val entity = EntityFactory.build(Source, it, None, Media.VIDEO, sessionId)
                    storeFileUseCase(entity)
                }.fix().bind().count()
            }
        }.fix()
        sourceFileCountProgram.unsafeRunSync()
//        pathArgs.source.map {
//            scanVideoUseCase(it.canonicalPath).map {
//                it.map {
//                    val entity = EntityFactory.build(Source, it, None, Media.VIDEO, sessionId)
//                    storeFileUseCase(entity).unsafeRunSync()
//                }.count()
//            }
//        }
        pathArgs.backup.traverse(IO.applicative()) {
            IO.fx {
                val seq = scanVideoUseCase(it.canonicalPath).bind()
                seq.map { file ->
                    IO.fx {
                        val y: Option<GooglePhotoId> = when(shouldStoreBackupVideo(file).bind()) {
                            true -> {
                                val entity = EntityFactory.build(Backup, file, None, Media.VIDEO, sessionId)
                                val maybeId = storeFileUseCase(entity).bind().toOption().map { it as GooglePhotoId }
                                maybeId
                            }
                            false -> { None }
                        }
                        y
                    }.unsafeRunSync()
                }
            }.map {
                it.count()
                it
            }
        }.fix()
    }

}

//fun videoMain(args: PathArgs, sessionId: SessionId, repository: IBackupRepository): IO<Kind<ForOption, Sequence<Option<GooglePhotoId>>>> {
//    logger.info("Beginning video scanning")
//
//    val scanVideoUseCase = ScanVideoUseCase(::walker)
//    val storeFileUseCase = StoreVideoUseCase(repository, sessionId, ::generateMD5)
//    val shouldStoreBackupVideo = ShouldStoreBackupVideoUseCase(repository, sessionId, ::generateMD5)
//
//    return args.backup.traverse(IO.applicative()) {
//        IO.fx {
//            val seq = scanVideoUseCase(it.canonicalPath).bind()
//            seq.map { file ->
//                IO.fx {
//                    val should = shouldStoreBackupVideo(file).bind()
//                    val y: Option<GooglePhotoId> = when(should) {
//                        true -> {
//                            val entity = EntityFactory.build(backup, file, None, Media.VIDEO, sessionId)
//                            val maybeId = storeFileUseCase(entity).bind().toOption().map { it as GooglePhotoId }
//                            maybeId
//                        }
//                        false -> { None }
//                    }
//                    y
//                }.unsafeRunSync()
//            }
//        }
//    }.fix()
//}

//fun walker(root: File): Sequence<File> =
//    root.walk().onEnter { dir ->
//        FORBIDDEN_PATHS.all { !dir.canonicalPath.endsWith(it) }.also {
//            when(it) {
//                true -> logger.trace { "Scanning $dir"}
//                false -> logger.trace { "Skipping $dir"}
//            }
//        }
//    }.onLeave {
//        logger.trace { "Done scanning for videos in $it"}
//    }.onFail { dir, exception ->
//        logger.error { "Failed to scan $dir: $exception" } // kotlin.io.AccessDeniedException
//    }.filter { it.isFile }
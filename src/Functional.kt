package photo.backup.kt

import arrow.Kind
import arrow.core.*
import arrow.core.extensions.option.traverse.traverse
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.applicative.applicative
import arrow.fx.fix
import photo.backup.kt.data.*
import photo.backup.kt.data.source.BackupRepository
import photo.backup.kt.data.source.RepoType
import photo.backup.kt.domain.*
import photo.backup.kt.util.*
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.system.measureTimeMillis

data class UseCases(
    val batchUpdateSessions: (StageType, List<File>) -> IO<Int>,
    val getDupes: ()->IO<List<SourceFileEntity>>,
    val moveFile: MoveFileUseCase,
//    val storeVideo: StoreVideoUseCase,
//    val shouldStoreVideo: ShouldStoreBackupVideoUseCase,
    val videoRunner: VideoRunnerUseCase,
    val scanDirectory: ScanDirectoryUseCase,
    val storeUnknownFile: StoreUnknownFileRefUseCase,
    val needToUpdateHash: NeedToUpdateHashUseCase,
    val getImageData: GetImageDataUseCase,
    val hashingStep: ComputeHashUseCase,
    val savePhoto: SavePhotoUseCase)

typealias VideoRunnerUseCase = (PathArgs) -> IO<Kind<ForOption, Sequence<Option<GooglePhotoId>>>>

fun main(
    paths: PathArgs,
    useCases: UseCases) {

    with(useCases) {
        val program = IO.fx {
            paths.source.traverse(IO.applicative()) { processPath(useCases, batchUpdateSessions, it.canonicalPath, Source) }.bind()
            paths.backup.traverse(IO.applicative()) { processPath(useCases, batchUpdateSessions, it.canonicalPath, Backup) }.bind()
            videoRunner(paths).bind()
            paths.destination.traverse(IO.applicative()) { dest ->
                moveDupeFiles(dest, getDupes, moveFile)
            }.bind()
        }

        measureTimeMillis { program.unsafeRunSync() }.run { println("Execution time took ${this/1000} seconds")}
    }
}

private fun scanPath(
    useCases: UseCases,
    path: String,
    stageType: StageType): IO<Sequence<Option<File>>> { with(useCases) {

    return IO.fx {
        val files = scanDirectory(path).bind()
        files.map { file ->
            determineMediaType(file).fold({
                storeUnknownFile(file)
                None
            }, { mediaType: Media ->
                IO.fx {
                    when(needToUpdateHash(file, stageType).bind()) {
                        false -> Some(file)
                        true -> {
                            getImageData(file, Media.IMAGE).bind().fold({
                                None
                            },{
                                val hashId = hashingStep(it, file.canonicalPath, file.lastModified(), stageType).bind()
                                val myFile = EntityFactory.build(stageType, file, Some(hashId), Media.IMAGE)
                                savePhoto(myFile).bind()
                                None
                            })
                        }
                    }
                }.unsafeRunSync()
            })
        }
    }
}}

private fun processPath(useCases: UseCases, batchUpdateSessions: (StageType, List<File>)->IO<Int>, path: String, stageType: StageType): IO<Int> = IO.fx {
    scanPath(useCases, path, stageType).bind().toList().mapNotNull { it.orNull() }.run { batchUpdateSessions(stageType, this) }.bind()
}

private fun moveDupeFiles(destPath: File, getter: ()->IO<List<SourceFileEntity>>, mover: (String, SourceFileEntity)->IO<Either<Throwable, Path>>): IO<List<Either<Throwable, Path>>> = IO.fx {
    val files = getter().bind()
    files.k().traverse(IO.applicative()) { mover(destPath.canonicalPath, it) }.fix().bind()
}
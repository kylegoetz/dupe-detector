package photo.backup.kt.domain

import arrow.fx.IO
import org.slf4j.LoggerFactory
import photo.backup.kt.VIDEO_EXTENSIONS
import java.io.File

private val logger = LoggerFactory.getLogger("test")

//class ScanPathUseCase(private val params: App.Params) {
//    operator fun invoke(path: File, stage: StageType): IO<Sequence<Option<MediaId>>> = with(params){
//        IO.fx {
//            val (filesSeq: Sequence<File>) = scanDirectoryUseCase(path.canonicalPath)
//            val programs: Sequence<IO<Option<MediaId>>> = filesSeq.map { file ->
//                logger.debug { "Processing ${file.canonicalPath}"}
//                logger.trace { "Media type?" }
//                val mediaTypeEither = getMediaTypeUseCase(file)
//                mediaTypeEither.fold({
//                    logger.trace { "It was unknown type"}
//                    storeUnknownFileRefUseCase(file).map { None }
//                },{ mediaType ->
//                    IO.fx {
//                        val (data: Either<Throwable, ByteArray>) = getDataUseCase(GetDataUseCase.Params(file, mediaType))
//                        data.fold({
//                            None
//                        },{
//                            val (hashId: HashId) = hashUseCase(it, file.canonicalPath, file.lastModified(), stage)
//                            val myFile = EntityFactory.build(stage, file, Some(hashId), mediaType).copy(sessionId)
//                            Some(savePhotoUseCase(myFile).bind())
//                        })
//                    }
//                })
//            }
//            programs.map { it.unsafeRunSync() }
//        } }
//}

//class ScanVideoUseCase(private val walker: (File)->Sequence<File>) {
//    operator fun invoke(root: String): IO<Sequence<File>> = IO {
//        walker(File(root))
//            .filter { it.extension.toLowerCase() in VIDEO_EXTENSIONS }
//    }
//}

fun scanVideo(root: String, walker: (File)->Sequence<File>): IO<Sequence<File>> {
    return IO.effect { walker(File(root)).filter{it.extension.toLowerCase() in VIDEO_EXTENSIONS} }
}

typealias ScanVideoUseCase = (String, (File)->Sequence<File>) -> IO<Sequence<File>>
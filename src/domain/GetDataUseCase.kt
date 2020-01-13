package photo.backup.kt.domain

import arrow.core.Either
import arrow.core.Right
import arrow.fx.IO
import arrow.fx.extensions.fx
import ch.frankel.slf4k.trace
import org.slf4j.LoggerFactory
import photo.backup.kt.data.Media
import java.io.File

fun generateGetImageDataUseCase(imageReader: (File)->IO<Either<Throwable, ByteArray>>): (File, Media)->IO<Either<Throwable, ByteArray>> =
    { file, mediaType ->
        when(mediaType) {
            Media.IMAGE -> imageReader(file)
            else -> IO.effect { Either.left(IllegalStateException("Can only call this on an image") ) }
        }
    }

typealias GetImageDataUseCase = (File, Media) -> IO<Either<Throwable, ByteArray>>

private val logger = LoggerFactory.getLogger("test")
//class GetDataUseCase(val imageReader: suspend (File)-> IO<Either<Throwable, ByteArray>>, val videoReader: (File)->IO<Either<Throwable, ByteArray>>) {
//    operator fun invoke(params: Params): IO<Either<Throwable, ByteArray>> = IO.fx {
//        logger.trace { "Reading in file ${params.file.canonicalPath}" }
//        when(params.type) {
//            Media.IMAGE -> !effect { imageReader(params.file) }.bind()
//            Media.VIDEO -> videoReader(params.file).bind()
//        }
//    }
//
//    data class Params(val file: File, val type: Media)
//}
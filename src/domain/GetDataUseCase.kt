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
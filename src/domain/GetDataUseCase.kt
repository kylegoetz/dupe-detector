package photo.backup.kt.domain

import arrow.core.Either
import arrow.core.Right
import arrow.fx.IO
import arrow.fx.extensions.fx
import ch.frankel.slf4k.trace
import org.slf4j.LoggerFactory
import photo.backup.kt.data.Media
import java.io.File

private val logger = LoggerFactory.getLogger("test")
class GetDataUseCase(val imageReader: suspend (File)-> IO<Either<Throwable, ByteArray>>, val videoReader: suspend (File)->ByteArray) {
    operator fun invoke(params: Params): IO<Either<Throwable, ByteArray>> = IO.fx {
        logger.trace { "Reading in file ${params.file.canonicalPath}" }
        when(params.type) {
            Media.IMAGE -> !effect { imageReader(params.file) }.bind()
            Media.VIDEO -> Right(!effect { videoReader(params.file) })
        }
    }

    data class Params(val file: File, val type: Media)
}
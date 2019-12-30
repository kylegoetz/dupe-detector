package photo.backup.kt.domain

import arrow.core.None
import arrow.core.Option
import arrow.core.extensions.fx
import arrow.fx.IO
import arrow.fx.IO.Companion.effect
import arrow.fx.extensions.fx
import photo.backup.kt.data.FileEntity
import photo.backup.kt.data.Media
import java.io.File

class ReadFileUseCase<T: FileEntity>(private val reader: (File, Media) -> ByteArray) {
//    suspend operator fun invoke(file: File, mediaType: Option<Media>): IO<Pair<File, ByteArray>> = IO.fx {
//        Pair(file, !effect { reader(file, mediaType) })
//    }
    operator fun invoke(params: Params): IO<Pair<File, Option<ByteArray>>> = IO.fx {
        val data: Option<ByteArray> = Option.fx {
            val (mediaType: Media) = params.mediaType
            reader(params.file, mediaType)
        }
        Pair(params.file, data)
    }

    data class Params(val file: File, val mediaType: Option<Media>)
}
package photo.backup.kt.domain

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import photo.backup.kt.IMAGE_EXTENSIONS
import photo.backup.kt.VIDEO_EXTENSIONS
import photo.backup.kt.data.Media
import java.awt.PageAttributes
import java.io.File

interface MediaType

sealed class Image: MediaType
object JPG: Image()
object GIF: Image()
object RAW: Image()
object PNG: Image()

infix fun String.`in`(list: List<String>): Boolean = list.contains(this)

class DetermineMediaTypeUseCase {
    operator fun invoke(file: File): Option<Media> =
        when {
            file.extension.toLowerCase() `in` IMAGE_EXTENSIONS -> Some(Media.IMAGE)
            file.extension.toLowerCase() `in` VIDEO_EXTENSIONS -> Some(Media.VIDEO)
            else -> None
        }
}
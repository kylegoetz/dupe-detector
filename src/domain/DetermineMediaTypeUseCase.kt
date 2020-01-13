package photo.backup.kt.domain

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import photo.backup.kt.IMAGE_EXTENSIONS
import photo.backup.kt.VIDEO_EXTENSIONS
import photo.backup.kt.data.Media
import java.awt.PageAttributes
import java.io.File

//class DetermineMediaTypeUseCase {
//    operator fun invoke(file: File): Option<Media> =
//        when {
//            file.extension.toLowerCase() `in` IMAGE_EXTENSIONS -> Some(Media.IMAGE)
//            file.extension.toLowerCase() `in` VIDEO_EXTENSIONS -> Some(Media.VIDEO)
//            else -> None
//        }
//}

fun determineMediaType(file: File): Option<Media> = when {
    file.extension.toLowerCase() `in` IMAGE_EXTENSIONS -> Some(Media.IMAGE)
    file.extension.toLowerCase() `in` VIDEO_EXTENSIONS -> Some(Media.VIDEO)
    else -> None
}

typealias DetermineMediaTypeUseCase = (File) -> Option<Media>

infix fun String.`in`(list: List<String>): Boolean = list.contains(this)
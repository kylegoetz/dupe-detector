package photo.backup.kt.util
import ch.frankel.slf4k.debug
import org.slf4j.LoggerFactory
import photo.backup.kt.FORBIDDEN_PATHS
import photo.backup.kt.IMAGE_EXTENSIONS
import photo.backup.kt.domain.`in`
import java.io.File

val imageWalker: (File) -> Sequence<File> = {
    it.walk().onEnter { dir ->
        logger.debug { "Scanning for files in $dir" }
        FORBIDDEN_PATHS.all { !dir.canonicalPath.endsWith(it) }
    }.filter {
        (it.isFile && it.extension.toLowerCase() `in` IMAGE_EXTENSIONS).also { allow ->
            when(allow) {
                true -> logger.debug { "Image walker is allowing $it"}
                false -> logger.debug { "Image walker is filtering out $it"}
            }
        }
    }
}

val videoWalker: (File) -> Sequence<File> = {
    it.walk().onEnter { dir ->
        FORBIDDEN_PATHS.all { !dir.canonicalPath.endsWith(it) }
    }
}
private val logger = LoggerFactory.getLogger("test")
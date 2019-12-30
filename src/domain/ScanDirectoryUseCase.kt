package photo.backup.kt.domain

import arrow.core.Option
import arrow.fx.IO
import arrow.fx.extensions.fx
import photo.backup.kt.FORBIDDEN_PATHS
import photo.backup.kt.SessionId
import java.io.File
import java.util.*
import ch.frankel.slf4k.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ScanDirectoryUseCase(private val walker: (String) -> Sequence<File>) {
    operator fun invoke(absolutePath: String): IO<Sequence<File>> = IO.fx {
        !effect { walker(absolutePath).filter { file ->
            FORBIDDEN_PATHS.all {
                !file.canonicalPath.contains(it)
            }
        } }
    }
}
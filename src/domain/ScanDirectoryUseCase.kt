package photo.backup.kt.domain

import arrow.fx.IO
import photo.backup.kt.FORBIDDEN_PATHS
import java.io.File

fun generateScanDirectoryUseCase(walker: (String)->Sequence<File>): (String)->IO<Sequence<File>> {
    return { IO.effect { walker(it).filter(::fileAllowed) } }
}

typealias ScanDirectoryUseCase = (String) -> IO<Sequence<File>>

private fun fileAllowed(file: File): Boolean {
    return FORBIDDEN_PATHS.all { !file.canonicalPath.contains(it) }
}

package photo.backup.kt.util

import arrow.core.Either
import arrow.fx.IO
import arrow.fx.extensions.fx
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

fun mover(destStr: String, toMove: File): IO<Either<Throwable, Path>> = IO.fx {
    val dest = customResolve(destStr, toMove)
    Files.createDirectories(dest.parent)
    Files.move(toMove.toPath(), dest)
    dest
}.attempt()

private fun customResolve(base: String, toCopy: File): Path {
    return File(base).toPath().resolve(".${toCopy.canonicalPath}")
}

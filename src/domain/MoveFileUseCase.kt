package photo.backup.kt.domain

import arrow.core.Either
import arrow.fx.IO
import org.slf4j.LoggerFactory
import photo.backup.kt.data.SourceFileEntity
import java.io.File
import java.nio.file.Path
import ch.frankel.slf4k.*
import org.slf4j.Logger

typealias MoveFileUseCase = (SourceFileEntity)->IO<Either<Throwable, Path>>

fun generateMoveFileUseCase(targetPath: String, mover: (String, File)->IO<Either<Throwable, Path>>): (SourceFileEntity)->IO<Either<Throwable, Path>> = {
    mover(targetPath, File(it.absolutePath))
}

@Suppress("UNUSED_PARAMETER")
private val logger: Logger = LoggerFactory.getLogger("test")
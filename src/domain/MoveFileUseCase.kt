package photo.backup.kt.domain

import arrow.core.Either
import arrow.fx.IO
import org.slf4j.LoggerFactory
import photo.backup.kt.data.SourceFileEntity
import java.io.File
import java.nio.file.Path
import ch.frankel.slf4k.*
import org.slf4j.Logger

typealias MoveFileUseCase = (String, SourceFileEntity)->IO<Either<Throwable, Path>>

fun generateMoveFileUseCase(mover: (String, File)->IO<Either<Throwable, Path>>): (String, SourceFileEntity)->IO<Either<Throwable, Path>> {
    return { targetPath: String, entity: SourceFileEntity ->
        mover(targetPath, File(entity.absolutePath))
    }
}

@Suppress("UNUSED_PARAMETER")
private val logger: Logger = LoggerFactory.getLogger("test")
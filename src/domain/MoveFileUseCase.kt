package photo.backup.kt.domain

import arrow.core.Either
import arrow.fx.IO
import org.slf4j.LoggerFactory
import photo.backup.kt.data.SourceFileEntity
import java.io.File
import java.nio.file.Path
import ch.frankel.slf4k.*
import org.slf4j.Logger

private val logger: Logger = LoggerFactory.getLogger("test")

class MoveFileUseCase(private val targetPath: String, internal val mover: suspend (String, File) -> Either<Throwable, Path>) {
    operator fun invoke(file: SourceFileEntity): IO<Either<Throwable, Path>> = IO.effect {
        logger.info { "Moving ${file.absolutePath} to $targetPath"}
        mover(targetPath, File(file.absolutePath))
    }
}
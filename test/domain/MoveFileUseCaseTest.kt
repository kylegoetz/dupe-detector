package photo.backup.kt.domain

import arrow.core.Either
import arrow.fx.IO
import arrow.fx.extensions.fx
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue

class MoveFileUseCaseTest : BaseUseCaseTest<MoveFileUseCase>() {
    override lateinit var case: MoveFileUseCase

    @Test
    @DisplayName("it returns Right if file move successful")
    fun moveSuccessful() {
        case = generateMoveFileUseCase("", ::successfulMover)
        val result = case(sourceEntity).unsafeRunSync()
        assertTrue(result is Either.Right)
    }

    @Test
    @DisplayName("it returns Left if file move threw")
    fun moveUnsuccessful() {
        case = generateMoveFileUseCase(File("").canonicalPath, ::failedMover)
        val result = case(sourceEntity).unsafeRunSync()
        assertTrue(result is Either.Left)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun failedMover(path: String, file: File): IO<Either<Throwable, Path>> {
        return IO.fx { throw IllegalArgumentException("") }.attempt()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun successfulMover(path: String, file: File): IO<Either<Throwable, Path>> {
        return IO.just(Either.right(File("").toPath()))
    }
}
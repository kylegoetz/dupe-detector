package photo.backup.kt

import arrow.core.Right
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import photo.backup.kt.util.mover
import java.io.File

class MoverTest {

    private lateinit var tmpFile: File

    @BeforeEach
    fun setup() {
        tmpFile = File.createTempFile("unit-test", "photos-backup")
    }

    @AfterEach
    fun teardown() {
        tmpFile.delete()
    }

    @Test
    @DisplayName("It moves a file to the correct destination and returns Right with the new path")
    fun func() {
        /* Given */
        val cwd = File("").canonicalPath

        /* When */
        val result = mover(cwd, tmpFile).unsafeRunSync()

        /* Then */
        val destFile = File(cwd).resolve(".${tmpFile.canonicalPath}")
        assertTrue(result.exists { it.toFile().exists() })
        assertEquals(Right(destFile.toPath()), result)

        result.map {
            tmpFile = it.toFile() // So teardown can delete the moved tmp file
        }
    }
}
package photo.backup.kt.domain

import arrow.core.None
import arrow.core.Some
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import photo.backup.kt.data.Media
import java.io.File

class DetermineMediaTypeUseCaseTest: BaseUseCaseTest<DetermineMediaTypeUseCase>() {

    override lateinit var SUT: DetermineMediaTypeUseCase

    @BeforeEach
    override fun configureSUT() {
        SUT = ::determineMediaType
    }

    @Test
    @DisplayName("detects ALLCAPS")
    fun JPGIsImage() {
        isImageHelper("some.JPG")
        isImageHelper("some.GIF")
        isVideoHelper("some.AVI")
    }

    @Test
    @DisplayName("jpg is Media.IMAGE")
    fun jpgIsImage() = isImageHelper("some.jpg")

    @Test
    @DisplayName("gif is Media.IMAGE")
    fun gifIsImage() = isImageHelper("some.gif")

    @Test
    @DisplayName("avi is Media.VIDEO")
    fun aviIsVideo() = isVideoHelper("some.avi")

    @Test
    @DisplayName("png is Media.IMAGE")
    fun pngIsImage() = isImageHelper("/path/to/some.png")

    @Test
    @DisplayName("txt is None")
    fun txtIsNone() {
        val file = File("some.txt")
        val result = SUT(file)
        assertEquals(None, result)
    }


    private fun isImageHelper(filename: String) {
        val file = File(filename)
        val result = SUT(file)
        assertEquals(Media.IMAGE, (result as Some).t)
    }

    private fun isVideoHelper(filename: String) {
        val file = File(filename)
        val result = SUT(file)
        assertEquals(Media.VIDEO, (result as Some).t)
    }
}
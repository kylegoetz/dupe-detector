package photo.backup.kt.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

class ScanVideoTest : BaseUseCaseTest<ScanVideoUseCase>() {
    override lateinit var SUT: ScanVideoUseCase

    override fun configureSUT() {
        SUT = ::scanVideo
    }

    @Test
    @DisplayName("The sequence returned only has video files, filters out images, etc.")
    fun excludesNonVideo() {
        val walker = { _:File -> sequenceOf(File("some.jpg"), File("some.avi"), File("some.txt"))}

        val result = SUT("", walker).unsafeRunSync()

        assertEquals(1, result.count())
        assertEquals(result.toList()[0], File("some.avi"))
    }
}
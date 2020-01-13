package photo.backup.kt.domain

import arrow.core.Either
import arrow.fx.IO
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import photo.backup.kt.data.Media
import java.io.File
import kotlin.test.assertTrue

class GetImageDataUseCaseTest : BaseUseCaseTest<GetImageDataUseCase>() {
    override lateinit var SUT: GetImageDataUseCase
    private var called = false


    override fun configureSUT() {
        called = false
        SUT = generateGetImageDataUseCase { called = true ; IO.just(Either.right(ByteArray(5))) }
    }

    @Test
    @DisplayName("Calls image reader")
    fun callsImageReader() {
        SUT(File("some.jpg"), Media.IMAGE).unsafeRunSync()

        assertTrue(called)
    }

    @Test
    @DisplayName("Throws if passed a non-image")
    fun throwsWithVideo() {
        SUT(File("some.avi"), Media.VIDEO)
        assertFalse(called)
    }

}
package photo.backup.kt.domain

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

class ScanDirectoryUseCaseTest : BaseUseCaseTest<ScanDirectoryUseCase>() {
    override lateinit var SUT: ScanDirectoryUseCase
    private lateinit var fileList: Sequence<File>

    @BeforeEach
    override fun configureSUT() {
        fileList = sequenceOf(
            File("/path/to/.@__thumb/something.jpg"),
            File("/path/to/valid.jpg"),
            File("/path/to/something.aplibrary/foobar.jpg")
        )
        SUT = generateScanDirectoryUseCase { fileList }
    }

    @Test
    @DisplayName("Does not filter out valid file")
    fun noFilter() {
        val files = SUT("").unsafeRunSync()
        assertTrue(files.contains(File("/path/to/valid.jpg")))
    }

    @Test
    @DisplayName("Filters out .@__thumb paths")
    fun filterThumbPath() {
        /* Given */
        /* When */
        val files = SUT("").unsafeRunSync()
        /* Then */
        assertTrue(!files.contains(File("/path/to/.@__thumb/something.jpg")))
    }

    @Test
    @DisplayName("Filters out .aplibrary paths")
    fun filterApertureLibraryPaths() {
        /* Given */
        /* When */
        val files = SUT("").unsafeRunSync()
        /* Then */
        assertTrue(!files.contains(File("/path/to/something.aplibrary/foobar.jpg")))
    }

}
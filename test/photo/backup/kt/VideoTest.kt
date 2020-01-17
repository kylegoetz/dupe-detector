package photo.backup.kt

import arrow.Kind
import arrow.core.*
import arrow.fx.IO
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import photo.backup.kt.data.FileEntity
import photo.backup.kt.data.GooglePhotoId
import photo.backup.kt.data.SourceFileEntity
import photo.backup.kt.data.SourcePhotoId
import photo.backup.kt.domain.ScanVideoUseCase
import photo.backup.kt.domain.ScanVideoUseCaseWrapper
import photo.backup.kt.domain.ShouldStoreBackupVideoUseCase
import photo.backup.kt.domain.StoreVideoUseCase
import java.io.File
import java.util.*

class VideoTest {
    @MockK private lateinit var scanVideoUseCase: ScanVideoUseCaseWrapper
    @MockK private lateinit var storeFileUseCase: StoreVideoUseCase
    @MockK private lateinit var shouldStore: ShouldStoreBackupVideoUseCase
    private var sessionId: SessionId = SessionId(UUID.randomUUID())
    private lateinit var SUT: (PathArgs) -> IO<Kind<ForOption, Sequence<Option<GooglePhotoId>>>>

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        sessionId = SessionId(UUID.randomUUID())
        SUT = createRunner(sessionId, scanVideoUseCase, storeFileUseCase, shouldStore)
    }

    @Test
    @DisplayName("If source/backup path is None, then does not scan")
    fun doesNotScanPath() {
        val pathArgs = PathArgs(None, None, None)

        SUT(pathArgs).unsafeRunSync()

        verify(exactly=0) { scanVideoUseCase(any()) }
    }

    @Test
    @DisplayName("If source path is Some, then scans path")
    fun scansPath() {
        val pathStr = "/"
        val pathArgs = PathArgs(Some(File(pathStr)), None, None)
        every { scanVideoUseCase(any())} returns IO.just(emptySequence())

        SUT(pathArgs).unsafeRunSync()

        verify { scanVideoUseCase(pathStr) }
    }

    @Test
    @DisplayName("If source scan returns videos, then calls store with a no-hash entity") // Pass responsibility for recognizing a hash to repo
    fun storesVideos() {
        val pathArgs = PathArgs(Some(File("/")), None, None)
        val slot = slot<SourceFileEntity>()
        every { scanVideoUseCase(any()) } returns IO.just(sequenceOf(File("/some/video.mpg")))
        every { storeFileUseCase(any())} returns IO.just(Either.right(SourcePhotoId(UUID.randomUUID())))


        SUT(pathArgs).unsafeRunSync()

        verify { storeFileUseCase(capture(slot)) }
        assertEquals(None, slot.captured.hash)
    }

    @Test
    @DisplayName("If backup path is Some, then scans path")
    fun scansBackupPath() {
        val pathStr = "/"
        val pathArgs = PathArgs(None, Some(File(pathStr)), None)
        every { scanVideoUseCase(any())} returns IO.just(emptySequence())

        SUT(pathArgs).unsafeRunSync()

        verify { scanVideoUseCase(pathStr) }
    }

    @Test
    @DisplayName("For each backup path found, ask whether to store")
    fun askWhetherToStoreBackup() {
        val file = File("/some/video.mpg")
        val pathArgs = PathArgs(None, Some(File("")), None)
        every { scanVideoUseCase(any()) } returns IO.just(sequenceOf(file))
        every { shouldStore(any()) } returns IO.just(false)

        SUT(pathArgs).unsafeRunSync()

        verify { shouldStore(file) }
    }

    @Test
    @DisplayName("If should not store file, do not store")
    fun doNotStore() {
        val pathArgs = PathArgs(None, Some(File("")), None)
        every { scanVideoUseCase(any()) } returns IO.just(sequenceOf(File("")))
        every { shouldStore(any())} returns IO.just(false)

        SUT(pathArgs).unsafeRunSync()

        verify(exactly=0) { storeFileUseCase(any()) }
    }

    @Test
    @DisplayName("If should store file, store it")
    fun storeFile() {
        val pathArgs = PathArgs(None, Some(File("")), None)
        every { scanVideoUseCase(any()) } returns IO.just(sequenceOf(File("")))
        every { shouldStore(any())} returns IO.just(true)
        every { storeFileUseCase(any()) } returns IO.just(Either.right(GooglePhotoId(UUID.randomUUID())))

        SUT(pathArgs).unsafeRunSync()

        verify { storeFileUseCase(any()) }
    }

    @Test
    @DisplayName("video scanner function, when executed, is a Some if backup path is Some, and contains sequence of Ids corresponding to found files")
    fun overall() {
        val pathArgs = PathArgs(None, Some(File("")), None)
        every { scanVideoUseCase(any())} returns IO.just(sequenceOf(File("/update.mpg"),File("/no-update.mpg")))
        every { shouldStore(any())} returns IO.just(true)
        every { storeFileUseCase(any())} returns IO.just(Either.right(GooglePhotoId(UUID.randomUUID())))

        val result = SUT(pathArgs).unsafeRunSync()

        result.fix().fold({
            assertTrue(false, "result should be Some, not None")
        },{
            assertEquals(2, it.count())
        })
    }
}
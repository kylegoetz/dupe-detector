package photo.backup.kt

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import arrow.fx.IO
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import photo.backup.kt.data.*
import photo.backup.kt.data.source.IBackupRepository
import photo.backup.kt.domain.*
import java.io.File
import java.util.*
import photo.backup.kt.main as functionalMain

class FunctionalTest {
    @MockK private lateinit var batchUpdateSessions: BatchUpdateSessionUseCase
    @MockK private lateinit var getDupes: ListDupesUseCase
    @MockK private lateinit var moveFile: MoveFileUseCase
    @MockK private lateinit var storeVideo: StoreVideoUseCase
    @MockK private lateinit var shouldStoreVideo: ShouldStoreBackupVideoUseCase
    @MockK private lateinit var videoRunner: VideoRunnerUseCase
    @MockK private lateinit var scanDirectory: ScanDirectoryUseCase
    @MockK private lateinit var storeUnknownFile: StoreUnknownFileRefUseCase
    @MockK private lateinit var needToUpdateHash: NeedToUpdateHashUseCase
    @MockK private lateinit var getImageData: GetImageDataUseCase
    @MockK private lateinit var hashingStep: ComputeHashUseCase
    @MockK private lateinit var savePhoto: SavePhotoUseCase

    private lateinit var SUT: (PathArgs, UseCases) -> Unit
    private lateinit var useCases: UseCases

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        SUT = ::functionalMain
        useCases = UseCases(
            batchUpdateSessions,
            getDupes,
            moveFile,
            storeVideo,
            shouldStoreVideo,
            videoRunner,
            scanDirectory,
            storeUnknownFile,
            needToUpdateHash,
            getImageData,
            hashingStep,
            savePhoto
        )
        every { batchUpdateSessions(any(), any())} returns IO.just(0)
        every { videoRunner(any()) } returns IO.just(None)
        every { scanDirectory(any()) } returns IO.just(emptySequence())
    }

    @Test
    @DisplayName("When source is Some, it scans the path")
    fun test() {
        val paths = PathArgs(Some(File("")), None, None)

        SUT(paths, useCases)

        verify { batchUpdateSessions(Source, any()) }
    }

    @Test
    @DisplayName("When source is None, it does not scan the path")
    fun sourceNone() {
        val paths = PathArgs(None, None, None)

        SUT(paths, useCases)

        verify(exactly=0) { batchUpdateSessions(Source, any())}
    }

    @Test
    @DisplayName("When backup is Some, it scans the path")
    fun backupSome() {
        val paths = PathArgs(None, Some(File("")), None)
        every { batchUpdateSessions(any(), any())} returns IO.just(0)

        SUT(paths, useCases)

        verify { batchUpdateSessions(Backup, any()) }
    }

    @Test
    @DisplayName("When backup is None, it does not scan the path")
    fun backupNone() {
        val paths = PathArgs(None, None, None)

        SUT(paths, useCases)

        verify(exactly=0) { batchUpdateSessions(Backup, any())}
    }

    @Test
    @DisplayName("When destination is none, it does not getDupes or moveFile")
    fun destinationNone() {
        val paths = PathArgs(None, None, None)

        SUT(paths, useCases)

        verify(exactly=0) { getDupes() }
        verify(exactly=0) { moveFile(any(), any()) }
    }

    @Test
    @DisplayName("When destination is Some, it calls getDupes and moveFile")
    fun destinationSome() {
        val sourceEntity = SourceFileEntity(
            absolutePath = "",
            hash = Some(HashId(UUID.randomUUID())),
            size=0L,
            dateModified = 0L,
            type = Media.IMAGE,
            id = SourcePhotoId(UUID.randomUUID()),
            sessionId = SessionId(UUID.randomUUID()))
        val paths = PathArgs(None, None, Some(File("/")))
        every { getDupes() } returns IO.just(listOf(sourceEntity))
        every { moveFile(any(), any()) } returns IO.just(Either.right(File("").toPath()))

        SUT(paths, useCases)

        verify(exactly=1) { getDupes() }
        verify(exactly=1) { moveFile("/", sourceEntity) }
    }

    @Test
    @DisplayName("When scans directory, stores a non-image/video type as unknownn")
    fun storesUnknown() {
        every { scanDirectory(any()) } returns IO.just(sequenceOf(File("foo.bar")))
        val paths = PathArgs(Some(File("")), None, None)
        every { storeUnknownFile(any()) } returns IO.just(Either.right(UUID.randomUUID()))

        SUT(paths, useCases)

        verify { storeUnknownFile(File("foo.bar")) }
    }

    @Test
    @DisplayName("When scans directory, queries whether needs to update hash for a found file")
    fun queriesUpdateHash() {
        every { scanDirectory(any()) } returns IO.just(sequenceOf(File("foo.jpg")))
        val paths = PathArgs(Some(File("")), None, None)
        every { needToUpdateHash(any(), any()) } returns IO.just(false)

        SUT(paths, useCases)

        verify { needToUpdateHash(File("foo.jpg"), Source) }
    }

    @Test
    @DisplayName("For an image with a hash that needs to be updated, get the image data")
    fun imageWithOutOfDateHashGetImageData() {
        val file = File("foo.jpg")
        every { scanDirectory(any()) } returns IO.just(sequenceOf(file))
        every { needToUpdateHash(any(), any()) } returns IO.just(true)
        every { getImageData(any(), any()) } returns IO.just(Either.right(ByteArray(5)))
        every { hashingStep(any(), any(), any(), any())} returns IO.just(HashId(UUID.randomUUID()))
        every { savePhoto(any())} returns IO.just(SourcePhotoId(UUID.randomUUID()))
        val paths = PathArgs(Some(File("")), None, None)

        SUT(paths, useCases)

        verify { getImageData(file, Media.IMAGE) }
    }

    @Test
    @DisplayName("If image data get fails, then do not attempt to hash")
    fun imageDataGetFails() {
        val file = File("foo.jpg")
        every { scanDirectory(any())} returns IO.just(sequenceOf(file))
        every { needToUpdateHash(any(), any())} returns IO.just(true)
        every { getImageData(any(), any())} returns IO.just(Either.left(IllegalStateException("")))
        val paths = PathArgs(Some(File("")), None, None)

        SUT(paths, useCases)

        verify(exactly=0) { hashingStep(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("After getting image data, get hash ID and save iff image data get is successful")
    fun getsHashId() {
        val data = ByteArray(5)
        val filename = "/foo.jpg"
        val paths = PathArgs(Some(File("")), None, None)
        val file = File(filename)
        val hashId = HashId(UUID.randomUUID())
        val slot = slot<SourceFileEntity>()
        every { scanDirectory(any())} returns IO.just(sequenceOf(file))
        every { needToUpdateHash(any(), any())} returns IO.just(true)
        every { getImageData(any(), any())} returns IO.just(Either.right(data))
        every { hashingStep(any(), any(), any(), any())} returns IO.just(hashId)
        every { savePhoto(any()) } returns IO.just(SourcePhotoId(UUID.randomUUID()))

        SUT(paths, useCases)

        verify { hashingStep(data, filename, any(), any()) }
        verify { savePhoto(capture(slot)) }
        assertEquals(filename, slot.captured.absolutePath)
        assertEquals(Some(hashId), slot.captured.hash)
    }

    @Test
    @DisplayName("scanPath batch updates session ID for files iff they don't need hash updated, so NOT for those that are unknown, unreadable, or need re-hashing")
    fun scanPathSomeNone() {
        val slot = slot<List<File>>()
        val paths = PathArgs(Some(File("")), None, None)
        val files = listOf(File("/path/to/unmodified.jpg"), File("/path/to/nonimage.txt"), File("/path/to/modified.jpg"), File("/path/to/unreadable.jpg"))
        every { scanDirectory(any())} returns IO.just(files.asSequence())

        every { storeUnknownFile(files[1]) } returns IO.just(Either.right(UUID.randomUUID()))

        every { needToUpdateHash(files[0], Source)} returns IO.just(false)

        every { needToUpdateHash(files[2], Source) } returns IO.just(true)
        every { getImageData(files[2], Media.IMAGE) } returns IO.just(Either.right(ByteArray(5)))
        every { hashingStep(ByteArray(5), "/path/to/modified.jpg", any(), Source)} returns IO.just(HashId(UUID.randomUUID()))
        every { savePhoto(any())} returns IO.just(SourcePhotoId(UUID.randomUUID()))

        every { needToUpdateHash(files[3], Source)} returns IO.just(true)
        every { getImageData(files[3], Media.IMAGE)} returns IO.just(Either.left(IllegalStateException("")))

        val result = SUT(paths, useCases)

        verify { batchUpdateSessions(Source, capture(slot))}
        assertEquals(listOf(files[0]), slot.captured)
    }


}
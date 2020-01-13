package photo.backup.kt.domain

import arrow.core.Either
import arrow.core.Left
import arrow.core.None
import com.sun.org.glassfish.gmbal.Description
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import photo.backup.kt.SessionId
import photo.backup.kt.data.*
import java.io.File
import java.util.*

class SavePhotoInfoUseCaseTest : BaseUseCaseTest<SavePhotoInfoUseCase>() {
    override lateinit var case: SavePhotoInfoUseCase
    private val sessionId = SessionId(UUID.randomUUID())

    @BeforeEach
    override fun configureSystemUnderTest() {
//        case = SavePhotoInfoUseCase(
//                repo,
//                source,
//
//        )
    }

    @Test
    @Description("Inserts new entity into repo")
    fun insertNew() {
        TODO()
        /* Given */
//        case = SavePhotoInfoUseCase(
//                Left(source),
//                existsFn={hash->repo.sourcePhotoScanned(hash)},
//                repoSaveFn={entity->repo.addSourceMedia(entity as SourceFileEntity)},
//                repoUpdateFn={entity->repo.updateSourceMedia(entity as SourceFileEntity)},
//                getHashFn={id -> repo.getHash(id) }
//        )
//        val file = File("")
//        val hashId = HashId(UUID.randomUUID())
//        val slot = slot<FileEntity>()
//        coEvery { repo.addBackup(capture(slot)) } just runs
//
//        /* When */
//        case(file, hashId).unsafeRunSync()
//
//        /* Then */
//        coVerify { repo.addBackup(any()) }
    }

    @Test
    @Description("Updates existing entity with new session Id")
    fun updateSessionId() {

    }

    @Test
    @Description("New row has this session's id")
    fun newlyInsertedHasThisSessionId() {

    }
}
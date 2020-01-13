package photo.backup.kt.domain

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import photo.backup.kt.util.ContentHasher


class FindBackupForSourceFileUseCaseTest: BaseUseCaseTest<FindBackupForSourceFileUseCase>() {
    override lateinit var case: FindBackupForSourceFileUseCase
    @MockK private lateinit var hasher: ContentHasher

    @BeforeEach
    override fun configureSystemUnderTest() {
        MockKAnnotations.init(this)
        case = FindBackupForSourceFileUseCase(repo, hasher)
    }

//    @Test
//    @DisplayName("")

//    @Test
//    @DisplayName("If corresponding backup file not found, should return None")
//    fun a() {
//        /* Given */
//        val sourceFileData = SourceFile("", Hash(""), 1, LocalDateTime.now())
//        /* When */
//        val result = case(sourceFileData)
//        /* Then */
//        TODO("Implement")
//    }
//
//    @Test
//    @DisplayName("If source file found, check modified date")
//    fun checkModifiedDate() {
//
//    }
}
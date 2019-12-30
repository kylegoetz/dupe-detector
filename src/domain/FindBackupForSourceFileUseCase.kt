package photo.backup.kt.domain

import arrow.core.Option
import arrow.fx.IO
import arrow.fx.extensions.fx
import photo.backup.kt.data.GoogleFileEntity
import photo.backup.kt.data.SourceFile
import photo.backup.kt.data.source.IBackupRepository
import photo.backup.kt.util.ContentHasher

class FindBackupForSourceFileUseCase(private val repository: IBackupRepository, private val hasher: ContentHasher) {
    operator fun invoke(sourceFile: SourceFile): IO<Option<GoogleFileEntity>> = IO.fx {
        TODO("Implement")
    }
}
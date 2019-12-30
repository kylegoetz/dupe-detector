package photo.backup.kt.domain

import arrow.core.Either
import arrow.fx.IO
import photo.backup.kt.SessionId
import photo.backup.kt.data.source.IBackupRepository
import photo.backup.kt.data.source.RepositoryException
import java.io.File
import java.util.*

class StoreUnknownFileRefUseCase(private val repository: IBackupRepository, private val session: SessionId) {
    operator fun invoke(file: File): IO<Either<RepositoryException, UUID>> = IO {
        repository.createUnknownFile(file, session)
    }
}

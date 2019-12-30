package photo.backup.kt.domain

import arrow.fx.IO
import arrow.fx.extensions.fx
import photo.backup.kt.data.Hash

class HashByteStringUseCase(private val hasher: suspend (ByteArray) -> Hash) {
    suspend operator fun invoke(bytes: ByteArray): Hash = hasher(bytes)
}
package photo.backup.kt.data

import java.time.LocalDateTime

data class SourceFile(
        val absolutePath: String,
        val hash: Hash,
        val fileSize: Int,
        val dateModified: LocalDateTime
)
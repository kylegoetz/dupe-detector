package photo.backup.kt.data

import java.util.UUID

interface MediaId {
    val value: UUID
}
inline class GooglePhotoId(override val value: UUID): MediaId
inline class SourcePhotoId(override val value: UUID): MediaId

package photo.backup.kt.data

sealed class StageType
object Source: StageType()
object Backup: StageType()

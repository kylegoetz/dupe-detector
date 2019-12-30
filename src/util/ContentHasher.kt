package photo.backup.kt.util

import photo.backup.kt.data.Hash
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

interface ContentHasher {
    suspend fun hash(path: String): Hash
}

suspend fun hasher(data: ByteArray): Hash {
    val md5 = MessageDigest.getInstance("MD5")
    md5.update(data)
    val digest = md5.digest()
    return Hash(DatatypeConverter.printHexBinary(digest).toUpperCase())
}
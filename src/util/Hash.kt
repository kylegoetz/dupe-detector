package photo.backup.kt.util

import arrow.core.Either
import arrow.fx.IO
import arrow.fx.extensions.fx
import photo.backup.kt.data.Hash
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

val calculateHash: (ByteArray) -> Hash = { data ->
    val md5 = MessageDigest.getInstance("MD5")
    md5.update(data)
    val digest = md5.digest()
    Hash(DatatypeConverter.printHexBinary(digest).toUpperCase())
}

fun generateMD5(file: File): IO<Either<Throwable, Hash>> = IO.fx {
    val stream = file.inputStream()
    val digest = MessageDigest.getInstance("MD5")
    val channel = stream.channel
    val buff = ByteBuffer.allocate(2048)
    while(channel.read(buff) != -1) {
        buff.flip()
        digest.update(buff)
        buff.clear()
    }
    val hashValue = digest.digest()
    Hash(DatatypeConverter.printHexBinary(hashValue).toUpperCase())
}.attempt()
package photo.backup.kt.util

import arrow.core.Either
import arrow.fx.IO
import arrow.fx.extensions.fx
import java.io.File
import javax.imageio.ImageIO

/**
 * This should fetch purely the image data and nothing else
 */
fun imageReader(file: File): IO<Either<Throwable, ByteArray>> = IO.fx {
    with(!effect { ImageIO.read(file) }) {
        val argbArray = getRGB(0,0, width, height,null,0, width)
        val buffer = java.nio.ByteBuffer.allocate(4*argbArray.size)
        argbArray.forEach { buffer.putInt(it) }
        buffer.flip()
        buffer.array().slice(0 until argbArray.size*4).toByteArray()
    }
}.attempt() // could throw javax.imageio.IIOException ("Could not read in the file /path/to/...

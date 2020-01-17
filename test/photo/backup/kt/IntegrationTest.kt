package photo.backup.kt

import arrow.core.Some
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileWriter
import java.nio.file.Files.createTempDirectory
import java.nio.file.Paths
import java.util.*
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadataNode
import kotlin.test.assertFalse
import photo.backup.kt.main as appMain

class IntegrationTest {

    private lateinit var sourcePath: File
    private lateinit var backupPath: File
    private lateinit var destPath: File
    private val files: MutableMap<String, File> = mutableMapOf()


    @BeforeEach
    fun setup() {
        val base = Paths.get("")
        val prefix = "dupe-detector-integration-test"
        // create source path, dest path, backup path
        sourcePath = createTempDirectory(base, prefix+"-source").toFile()
        backupPath = createTempDirectory(base, prefix+"-backup").toFile()
        destPath = createTempDirectory(base, prefix+"-destination").toFile()

        // generate video 1, put in source and backup
        files["video 1 source"] = generateVideo(sourcePath.resolve("video 1.mpg").canonicalPath).also {
            val destPath = File(backupPath.canonicalPath).resolve(it.toPath().fileName.toFile())
            files["video 1 backup"] = it.copyTo(destPath)
        }
        // generate video 2, put in source
        files["video 2"] = generateVideo(sourcePath.resolve("video 2.mpg").canonicalPath)
        // generate video 3, put in backup
        files["video 3"] = generateVideo(backupPath.resolve("video 3.mpg").canonicalPath)

        // generate photo 1, put in source and backup
        files["photo 1 source"] = generatePhoto(sourcePath.resolve("photo 1.jpg").canonicalPath).also {
            val destPath = File(backupPath.canonicalPath).resolve(it.toPath().fileName.toFile())
            files["photo 1 backup"] = it.copyTo(destPath)
        }

        // generate photo 2, put in source
        files["photo 2"] = generatePhoto(sourcePath.resolve("photo 2.jpg").canonicalPath)
        // generate photo 3, put in backup
        files["photo 3"] = generatePhoto(backupPath.resolve("photo 3.jpg").canonicalPath)
        // generate photo 4, put in source but backup with changed metadata
        files["photo 4 source"] = generatePhoto(sourcePath.resolve("photo 4.jpg").canonicalPath).also {
            val destPath = File(backupPath.canonicalPath).resolve(it.toPath().fileName.toFile())
//            changeMetadata(it, destPath)
        }
    }

    @AfterEach
    fun teardown() {
        // delete source, dest, and backup paths (which contain the tmp files)
        listOf(sourcePath, backupPath, destPath).forEach { it.delete() }
    }

    @Test
    @DisplayName("" +
            "Video 1: in dest, in backup, not in source\n" +
            "Video 2: in source, not in dest\n" +
            "Video 3: in backup, not in dest\n" +
            "Photo 1: IN dest, not in source\n" +
            "Photo 2: in source, not in dest\n" +
            "Photo 3: in backup, not in dest\n" +
            "Photo 4: in backup and dest, not in source")
    fun doesEverythingRight() {
        appMain(arrayOf(sourcePath, backupPath, destPath).map { it.canonicalPath}.toTypedArray())

        assertFalse(files["video 1 source"]!!.exists())
        assertTrue(files["video 1 backup"]!!.exists())
        assertTrue(files["video 1 dest"]!!.exists())
    }

    // Have some session ID tests, tests about modification date where you re-scan

    private fun generateVideo(pathStr: String): File {
        File(pathStr).createNewFile()
        FileWriter(pathStr).apply {
            write(UUID.randomUUID().toString())
            close()
        }
        return File(pathStr)
    }

    private fun generatePhoto(pathStr: String): File {
        ImageIO.write(generateImage(), "jpg", File(pathStr))
        return File(pathStr)
    }

    // This should take a file, open, edit the metadata only
//    fun changeMetadata(sourceImage: File, destImage: File) {
//        val writer = ImageIO.getImageWritersByFormatName("jpg").next()
//        val writeParam = writer.defaultWriteParam
//        val typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB)
//
//        val metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam)
//
//        val textEntry = IIOMetadataNode("tEXtEntry")
//        textEntry.setAttribute("key", UUID.randomUUID().toString())
//
//        val text = IIOMetadataNode("tEXt")
//        text.appendChild(textEntry)
//
//        val root = IIOMetadataNode("javax_imageio_jpg_1.0")
//        root.appendChild(text)
//
//        metadata.mergeTree("javax_imageio_jpg_1.0", root)
//
//        val baos = ByteArrayOutputStream()
//        val stream = ImageIO.createImageInputStream(baos)
//        writer.setOutput(stream)
//        writer.write(metadata, IIOImage(buffImg, null, metadata), writeParam)
//        stream.close()
//    }


}

private fun generateImage(): BufferedImage {
    val width = (Math.random()*1024).toInt()
    val height = (Math.random()*800).toInt()
    val rnd = Random()

    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_BGR)
//    val array = Array(image.width*image.height*image.raster.numBands) { 0 }
//    image.raster.dataBuffer
    val array = image.raster.dataBuffer as DataBufferInt
    array.data.forEachIndexed { idx, elem ->
        array.setElem(idx, rnd.nextInt(0xFFFFFF))
    }
//    array..forEach {
//        array.setElem(it, rnd.nextInt(0xFFFFFF))
//        array[it] = rnd.nextInt(0xFFFFFF)
//
//    }
    return image
}
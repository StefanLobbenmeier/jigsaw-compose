package app.jigsaw

import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.khronos.webgl.get
import org.khronos.webgl.Int8Array
import org.khronos.webgl.ArrayBuffer

actual fun appReady() { document.getElementById("loading")?.remove() }
actual fun pickImage(onResult: (ByteArray?, String?) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"; input.accept = "image/png,image/jpeg,image/webp"
    input.onchange = {
        val file = input.files?.item(0)
        if (file != null) {
            if (file.size.toDouble() > 20_000_000) onResult(null, "Choose an image smaller than 20 MB.")
            else {
                val reader = FileReader()
                reader.onload = {
                    val data = Int8Array(reader.result as ArrayBuffer)
                    onResult(ByteArray(data.length) { data[it] }, file.name)
                }
                reader.onerror = { onResult(null, "The image could not be read.") }
                reader.readAsArrayBuffer(file)
            }
        }
    }
    input.click()
}

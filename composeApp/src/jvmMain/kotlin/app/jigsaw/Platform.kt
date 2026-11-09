package app.jigsaw

import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

actual fun appReady() {}
actual fun pickImage(onResult: (ByteArray?, String?) -> Unit) {
    SwingUtilities.invokeLater {
        val picker = JFileChooser().apply { fileFilter = FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "webp") }
        if (picker.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            val file = picker.selectedFile
            if (file.length() > 20_000_000) onResult(null, "Choose an image smaller than 20 MB.")
            else runCatching { file.readBytes() }.fold({ onResult(it, file.name) }, { onResult(null, "The image could not be read.") })
        }
    }
}

package io.shubham0204.smollmandroid.ui.screens.import_model

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.shubham0204.smollmandroid.data.AppDB
import io.shubham0204.smollmandroid.data.LLMModel
import io.shubham0204.smollmandroid.ui.screens.chat.ChatActivity
import io.shubham0204.smollm.GGUFReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths

/**
 * Handles ACTION_SEND of a .gguf model file. Copies it into the app's models directory, registers
 * it in the database, creates a default chat if none exists, then launches ChatActivity.
 * Minimal UX: just a text status. If anything fails we fall back to DownloadModelActivity.
 */
class ImportModelActivity : ComponentActivity() {
    private val appDB by inject<AppDB>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedUri: Uri? = intent?.getParcelableExtra(Intent.EXTRA_STREAM)
        val action = intent?.action

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var status by remember { mutableStateOf("Importing model...") }
                    var finished by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(Unit) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                if (action != Intent.ACTION_SEND || sharedUri == null) {
                                    status = "No file received"
                                    navigateToDownload()
                                    return@launch
                                }
                                val fileName = resolveFileName(sharedUri)
                                if (fileName == null || !fileName.lowercase().endsWith(".gguf")) {
                                    status = "Not a GGUF file"
                                    navigateToDownload()
                                    return@launch
                                }
                                val destDir = File(getExternalFilesDir("models"), "").apply { if (!exists()) mkdirs() }
                                val destFile = File(destDir, fileName)
                                copyUriToFile(sharedUri, destFile)
                                if (!destFile.exists() || destFile.length() == 0L) {
                                    status = "Copy failed"
                                    navigateToDownload()
                                    return@launch
                                }
                                registerModelIfNeeded(destFile, fileName)
                                finished = true
                                navigateToChat()
                            } catch (ce: CancellationException) {
                                Log.w(TAG, "Import cancelled")
                            } catch (e: Exception) {
                                Log.e(TAG, "Import error: ${e.message}")
                                status = "Import error"
                                navigateToDownload()
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(if (finished) "Model imported" else status)
                    }
                }
            }
        }
    }

    private fun resolveFileName(uri: Uri): String? {
        var name: String? = null
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIdx)
                }
            }
            if (name == null) {
                // fallback to last segment
                name = uri.lastPathSegment?.substringAfterLast('/')
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to resolve filename: ${e.message}")
        }
        return name
    }

    private fun copyUriToFile(src: Uri, dest: File) {
        val tmp = File(dest.parentFile, dest.name + ".part")
        if (tmp.exists()) tmp.delete()
        contentResolver.openInputStream(src).use { inputStream ->
            if (inputStream == null) throw IllegalArgumentException("Null input stream")
            BufferedInputStream(inputStream).use { bis ->
                BufferedOutputStream(FileOutputStream(tmp)).use { bos ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val r = bis.read(buffer)
                        if (r == -1) break
                        bos.write(buffer, 0, r)
                    }
                    bos.flush()
                }
            }
        }
        if (dest.exists()) dest.delete()
        tmp.renameTo(dest)
        if (tmp.exists()) tmp.delete()
    }

    private suspend fun registerModelIfNeeded(modelFile: File, fileName: String) {
        val existing = appDB.getModelsList().find { File(it.path).absolutePath == modelFile.absolutePath || File(it.path).name == modelFile.name }
        if (existing != null) return
        try {
            val ggufReader = GGUFReader()
            ggufReader.load(modelFile.absolutePath)
            val contextSize = ggufReader.getContextSize()?.toInt() ?: DEFAULT_CONTEXT
            val chatTemplate = ggufReader.getChatTemplate() ?: ""
            val model: LLMModel = appDB.addModel(
                name = fileName,
                url = "", // unknown / local import
                path = modelFile.absolutePath,
                contextSize = contextSize,
                chatTemplate = chatTemplate,
            )
            val chatsCount = appDB.getChatsCount()
            if (chatsCount == 0L) {
                appDB.addChat(
                    chatName = "Untitled 1",
                    llmModelId = model.id,
                    contextSize = model.contextSize,
                    chatTemplate = model.chatTemplate,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register model: ${e.message}")
        }
    }

    private fun navigateToChat() {
        startActivity(Intent(this, ChatActivity::class.java))
        finish()
    }

    private fun navigateToDownload() {
        // Reuse existing onboarding if import fails
        val downloadIntent = Intent(this, Class.forName("io.shubham0204.smollmandroid.ui.screens.model_download.DownloadModelActivity"))
        startActivity(downloadIntent)
        finish()
    }

    companion object {
        private const val TAG = "ImportModelActivity"
        private const val DEFAULT_CONTEXT = 2048
    }
}
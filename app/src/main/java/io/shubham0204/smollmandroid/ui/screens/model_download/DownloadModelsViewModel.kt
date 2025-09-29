/*
 * Copyright (C) 2024 Shubham Panchal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shubham0204.smollmandroid.ui.screens.model_download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import io.shubham0204.hf_model_hub_api.HFModelInfo
import io.shubham0204.hf_model_hub_api.HFModelSearch
import io.shubham0204.hf_model_hub_api.HFModelTree
import io.shubham0204.smollm.GGUFReader
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.AppDB
import io.shubham0204.smollmandroid.data.HFModelsAPI
import io.shubham0204.smollmandroid.data.LLMModel
import io.shubham0204.smollmandroid.llm.exampleModelsList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.nio.file.Paths
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.State

@Single
class DownloadModelsViewModel(
    val context: Context,
    val appDB: AppDB,
    val hfModelsAPI: HFModelsAPI,
) : ViewModel() {
    // default context size for the LLM
    private val defaultContextSize = 2048

    private val _modelInfoAndTree =
        MutableStateFlow<Pair<HFModelInfo.ModelInfo, List<HFModelTree.HFModelFile>>?>(null)
    val modelInfoAndTree: StateFlow<Pair<HFModelInfo.ModelInfo, List<HFModelTree.HFModelFile>>?> =
        _modelInfoAndTree

    private val _downloadProgress = mutableFloatStateOf(0f)
    val downloadProgress: State<Float> = _downloadProgress

    private val _isDownloaded = mutableStateOf(false)
    val isDownloaded: State<Boolean> = _isDownloaded

    val downloadedModelUri = mutableStateOf<Uri?>(null)
    val modelUrlState = mutableStateOf("")
    val selectedModelState = mutableStateOf<LLMModel?>(null)

    // Added: default model constants and setup state
    private val defaultModel = exampleModelsList.first()
    private val defaultModelFileName = defaultModel.url.substring(defaultModel.url.lastIndexOf('/') + 1)
    private val ensureTriggered = AtomicBoolean(false)
    private val _isCopyInProgress = mutableStateOf(false)
    val isCopyInProgress: State<Boolean> = _isCopyInProgress
    private val _setupMessage = MutableStateFlow("")
    val setupMessage: StateFlow<String> = _setupMessage

    var viewModelId: String? = null

    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun downloadModel() {
        selectedModelState.value = exampleModelsList.first()
        val modelUrl = selectedModelState.value?.url ?: return
        val fileName = modelUrl.substring(modelUrl.lastIndexOf('/') + 1)
        val request =
            DownloadManager
                .Request(modelUrl.toUri())
                .setTitle(fileName)
                .setDescription(
                    "The GGUF model will be downloaded on your device for use with SmolChat.",
                ).setMimeType("application/octet-stream")
                .setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE,
                ).setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
                ).setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val downloadId = downloadManager.enqueue(request)

        CoroutineScope(Dispatchers.IO).launch {
            var downloading = true
            while (downloading) {
                val q = DownloadManager.Query()
                q.setFilterById(downloadId)
                val cursor = downloadManager.query(q)
                if (cursor != null && cursor.moveToFirst()) {
                    try {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                downloading = false
                                _isDownloaded.value = true
                                downloadedModelUri.value = downloadManager.getUriForDownloadedFile(downloadId)
                            }
                            DownloadManager.STATUS_FAILED -> {
                                val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                                Log.e("DownloadManager", "Download failed. Reason: $reason")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                }
                                downloading = false
                            }
                            DownloadManager.STATUS_RUNNING -> {
                                val bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                val bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                if (bytesTotal > 0) {
                                    _downloadProgress.floatValue = (bytesDownloaded.toFloat() / bytesTotal.toFloat())
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DownloadManager", "Error while monitoring download", e)
                        downloading = false
                    } finally {
                        cursor.close()
                    }
                } else {
                    downloading = false
                }
            }
        }
    }

    fun getModels(query: String): Flow<PagingData<HFModelSearch.ModelSearchResult>> = hfModelsAPI.getModelsList(query)

    /**
     * Given the model file URI, copy the model file to the. app's internal directory. Once copied,
     * add a new LLMModel entity with modelName=fileName where fileName is the name of the model
     * file.
     */
    fun copyModelFile(
        uri: Uri,
        onComplete: () -> Unit,
    ) {
        var fileName = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            fileName = cursor.getString(nameIndex)
        }
        if (fileName.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val modelPath = File(context.filesDir, fileName).absolutePath
                context.contentResolver.openInputStream(uri).use { inputStream ->
                    FileOutputStream(modelPath).use { outputStream ->
                        inputStream?.copyTo(outputStream)
                    }
                }
                val ggufReader = GGUFReader()
                ggufReader.load(modelPath)
                val contextSize = ggufReader.getContextSize()?.toInt() ?: defaultContextSize
                val chatTemplate = ggufReader.getChatTemplate() ?: ""

                val newModel = appDB.addModel(
                    fileName,
                    "",
                    Paths.get(context.filesDir.absolutePath, fileName).toString(),
                    contextSize,
                    chatTemplate,
                )
                val chatCount = appDB.getChatsCount()
                appDB.addChat(
                    chatName = "Untitled ${chatCount + 1}",
                    llmModelId = newModel.id,
                    contextSize = newModel.contextSize,
                    chatTemplate = newModel.chatTemplate
                )

                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        } else {
            Toast
                .makeText(
                    context,
                    context.getString(R.string.toast_invalid_file),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    fun fetchModelInfoAndTree(modelId: String) {
        _modelInfoAndTree.value = null
        CoroutineScope(Dispatchers.IO).launch {
            val modelInfo = hfModelsAPI.getModelInfo(modelId)
            var modelTree = hfModelsAPI.getModelTree(modelId)
            modelTree =
                modelTree.filter { modelFile ->
                    modelFile.path.endsWith("gguf")
                }
            _modelInfoAndTree.value = Pair(modelInfo, modelTree)
        }
    }

    // Ensure default model exists; copy from Downloads if present else download
    fun ensureDefaultModel() {
        if (!ensureTriggered.compareAndSet(false, true)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                selectedModelState.value = defaultModel
                val targetFile = getTargetModelFile()
                val stalePart = File(targetFile.parentFile, targetFile.name + ".part")
                if (stalePart.exists()) stalePart.delete()
                if (targetFile.exists() && targetFile.length() > 0L) {
                    _setupMessage.value = "Model already present"
                    registerModelIfNeeded(targetFile)
                    _isDownloaded.value = true
                    return@launch
                }
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val downloadsFile = File(downloadsDir, defaultModelFileName)
                var attemptedCopy = false
                if (downloadsFile.exists() && downloadsFile.length() > 0) {
                    _setupMessage.value = "Copying existing model"
                    attemptedCopy = true
                    try {
                        copyFromDownloads(downloadsFile, targetFile)
                    } catch (e: SecurityException) {
                        Log.w("ModelSetup", "Permission denied copying from Downloads; will download instead: ${e.message}")
                    } catch (e: Exception) {
                        Log.w("ModelSetup", "Copy failed (${e.message}); will download instead")
                    }
                    if (targetFile.exists() && targetFile.length() > 0L) {
                        registerModelIfNeeded(targetFile)
                        _isDownloaded.value = true
                        return@launch
                    }
                }
                _setupMessage.value = if (attemptedCopy) "Downloading model" else "Downloading model"
                startDownloadToAppFolder()
            } catch (e: Exception) {
                Log.e("ModelSetup", "Unexpected error ensuring model: ${e.message}")
                _setupMessage.value = "Downloading model"
                startDownloadToAppFolder()
            }
        }
    }

    private fun getTargetModelsDir(): File = File(context.getExternalFilesDir("models"), "").apply { if (!exists()) mkdirs() }
    private fun getTargetModelFile(): File = File(getTargetModelsDir(), defaultModelFileName)

    private fun copyFromDownloads(source: File, target: File) {
        _isCopyInProgress.value = true
        val tmp = File(target.parentFile, target.name + ".part")
        if (tmp.exists()) tmp.delete()
        try {
            BufferedInputStream(FileInputStream(source)).use { input ->
                BufferedOutputStream(tmp.outputStream()).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val r = input.read(buffer)
                        if (r == -1) break
                        output.write(buffer, 0, r)
                    }
                    output.flush()
                }
            }
            if (target.exists()) target.delete()
            tmp.renameTo(target)
        } finally {
            if (tmp.exists()) tmp.delete()
            _isCopyInProgress.value = false
        }
    }

    private fun startDownloadToAppFolder() {
        val modelUrl = defaultModel.url
        val fileName = defaultModelFileName
        val request =
            DownloadManager
                .Request(modelUrl.toUri())
                .setTitle(fileName)
                .setDescription("Downloading model")
                .setMimeType("application/octet-stream")
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, "models", fileName)
        val downloadId = downloadManager.enqueue(request)
        CoroutineScope(Dispatchers.IO).launch {
            var downloading = true
            while (downloading) {
                val q = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(q)
                if (cursor != null && cursor.moveToFirst()) {
                    try {
                        when (val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                downloading = false
                                val f = getTargetModelFile()
                                if (f.exists() && f.length() > 0L) {
                                    _isDownloaded.value = true
                                    downloadedModelUri.value = Uri.fromFile(f)
                                    registerModelIfNeeded(f)
                                } else {
                                    _setupMessage.value = "Download produced empty file";
                                }
                            }
                            DownloadManager.STATUS_FAILED -> {
                                val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                                Log.e("DownloadManager", "Download failed. Reason: $reason")
                                withContext(Dispatchers.Main) { Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show() }
                                downloading = false
                            }
                            DownloadManager.STATUS_RUNNING -> {
                                val bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                val bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                if (bytesTotal > 0) {
                                    _downloadProgress.floatValue = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                                }
                            }
                            else -> {}
                        }
                    } catch (e: Exception) {
                        Log.e("DownloadManager", "Error while monitoring download", e)
                        downloading = false
                    } finally { cursor.close() }
                } else { downloading = false }
            }
        }
    }

    // Adjust registerModelIfNeeded to use defaultModel constants
    private suspend fun registerModelIfNeeded(modelFile: File) {
        val existing = appDB.getModelsList().find { existing ->
            File(existing.path).absolutePath == modelFile.absolutePath || File(existing.path).name == modelFile.name
        }
        if (existing != null) return
        try {
            val ggufReader = GGUFReader()
            ggufReader.load(modelFile.absolutePath)
            val contextSize = ggufReader.getContextSize()?.toInt() ?: defaultContextSize
            val chatTemplate = ggufReader.getChatTemplate() ?: ""
            val newModel = appDB.addModel(
                name = modelFile.name,
                url = defaultModel.url,
                path = modelFile.absolutePath,
                contextSize = contextSize,
                chatTemplate = chatTemplate,
            )
            val chatCount = appDB.getChatsCount()
            appDB.addChat(
                chatName = "Untitled ${chatCount + 1}",
                llmModelId = newModel.id,
                contextSize = newModel.contextSize,
                chatTemplate = newModel.chatTemplate
            )
        } catch (e: Exception) {
            Log.e("RegisterModel", "Failed to register model: ${e.message}")
        }
    }
}
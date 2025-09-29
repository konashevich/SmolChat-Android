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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.ui.components.AppAlertDialog
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.components.AppProgressDialog
import io.shubham0204.smollmandroid.ui.screens.chat.ChatActivity
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import org.koin.android.ext.android.inject

class DownloadModelActivity : ComponentActivity() {
    private var openChatScreen: Boolean = true
    private val viewModel: DownloadModelsViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            Box(modifier = Modifier.safeDrawingPadding()) {
                NavHost(
                    navController = navController,
                    startDestination = "download-model",
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() },
                ) {
                    composable("view-model") {
                        ViewHFModelScreen(
                            viewModel,
                            onBackClicked = { navController.navigateUp() },
                        )
                    }
                    composable("hf-model-select") {
                        HFModelDownloadScreen(
                            viewModel,
                            onBackClicked = { navController.navigateUp() },
                            onModelClick = { modelId ->
                                viewModel.viewModelId = modelId
                                navController.navigate("view-model")
                            },
                        )
                    }
                    composable("download-model") {
                        AddNewModelScreen(
                            onHFModelSelectClick = { navController.navigate("hf-model-select") },
                        )
                    }
                }
            }
        }
        openChatScreen = intent.extras?.getBoolean("openChatScreen") ?: true
    }

    private fun openChatActivity() {
        if (openChatScreen) {
            Intent(this, ChatActivity::class.java).apply {
                startActivity(this)
                finish()
            }
        } else {
            finish()
        }
    }

    private enum class AddNewModelStep {
        DownloadModel,
        DownloadProgress,
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AddNewModelScreen(onHFModelSelectClick: () -> Unit) {
        var addNewModelStep by remember { mutableStateOf(AddNewModelStep.DownloadModel) }
        SmolLMAndroidTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { AppBarTitleText(stringResource(R.string.add_new_model_title)) },
                    )
                },
            ) { innerPadding ->
                Surface(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState()),
                ) {
                    when (addNewModelStep) {
                        AddNewModelStep.DownloadModel -> {
                            DownloadModelScreen(
                                onNextSectionClick = {
                                    addNewModelStep = AddNewModelStep.DownloadProgress
                                },
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                            )
                        }
                        AddNewModelStep.DownloadProgress -> {
                            DownloadProgressScreen(
                                onPrevSectionClick = {
                                    addNewModelStep = AddNewModelStep.DownloadModel
                                },
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                            )
                        }
                    }
                }
                AppProgressDialog()
                AppAlertDialog()
            }
        }
    }

    @Composable
    private fun DownloadModelScreen(
        onNextSectionClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(modifier = modifier) {
            Text(
                text = "Welcome to CrisisAI",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Your personal AI assistant for crisis situations.",
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    // Proceed to next screen; ensureDefaultModel runs there
                    onNextSectionClick()
                },
            ) {
                Text(stringResource(R.string.button_text_next))
            }
        }
    }

    @Composable
    private fun DownloadProgressScreen(
        onPrevSectionClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val downloadProgress by viewModel.downloadProgress
        val isDownloaded by viewModel.isDownloaded
        val isCopyInProgress by viewModel.isCopyInProgress
        val setupMessage by viewModel.setupMessage.collectAsState(initial = "")

        // Start ensure logic once
        androidx.compose.runtime.LaunchedEffect(Unit) {
            viewModel.ensureDefaultModel()
        }

        Column(modifier = modifier) {
            Text(
                text = when {
                    isDownloaded -> "Model Ready"
                    isCopyInProgress -> "Copying model..."
                    downloadProgress > 0f -> "Downloading model..."
                    else -> "Setting up model..."
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(16.dp))
            when {
                !isDownloaded && isCopyInProgress -> {
                    // Copying from Downloads: do not show a progress bar per requirements
                }
                !isDownloaded && downloadProgress > 0f -> {
                    LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (!isDownloaded && setupMessage.isNotEmpty() && !isCopyInProgress && downloadProgress == 0f) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(setupMessage, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { if (isDownloaded) openChatActivity() },
                enabled = isDownloaded,
                shape = RoundedCornerShape(4.dp),
            ) {
                Text("Finish")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                OutlinedButton(
                    onClick = onPrevSectionClick,
                    enabled = !isDownloaded && !isCopyInProgress && downloadProgress == 0f,
                ) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = null)
                    Text(stringResource(R.string.button_text_back))
                }
            }
        }
    }
}
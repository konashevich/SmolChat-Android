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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.ui.components.AppAlertDialog
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
                        AddNewModelScreen()
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

    @Composable
    private fun AddNewModelScreen() {
        var addNewModelStep by remember { mutableStateOf(AddNewModelStep.DownloadModel) }
        SmolLMAndroidTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
            ) { innerPadding ->
                Surface(
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                ) {
                    when (addNewModelStep) {
                        AddNewModelStep.DownloadModel -> DownloadModelScreen(
                            onNextSectionClick = { addNewModelStep = AddNewModelStep.DownloadProgress },
                            modifier = Modifier.fillMaxSize(),
                        )
                        AddNewModelStep.DownloadProgress -> DownloadProgressScreen(
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                AppProgressDialog()
                AppAlertDialog()
            }
        }
    }

    // STEP 1: full-screen image + swipe left => next + dots
    @Composable
    private fun DownloadModelScreen(
        onNextSectionClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        var swipeTriggered by remember { mutableStateOf(false) }
        Box(
            modifier =
                modifier
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                totalDrag += dragAmount
                                if (totalDrag < -120f && !swipeTriggered) {
                                    swipeTriggered = true
                                    onNextSectionClick()
                                }
                            },
                            onDragEnd = { totalDrag = 0f },
                            onDragCancel = { totalDrag = 0f },
                        )
                    },
        ) {
            Image(
                painter = painterResource(id = R.drawable.intro_slide_1),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            DotsIndicator(
                currentIndex = 0,
                total = 2,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            )
        }
    }

    // STEP 2: full-screen image + progress overlay + finish + dots
    @Composable
    private fun DownloadProgressScreen(
        modifier: Modifier = Modifier,
    ) {
        val downloadProgress by viewModel.downloadProgress
        val isDownloaded by viewModel.isDownloaded
        val isCopyInProgress by viewModel.isCopyInProgress

        LaunchedEffect(Unit) { viewModel.ensureDefaultModel() }

        Box(modifier = modifier.background(Color.Black)) {
            Image(
                painter = painterResource(id = R.drawable.intro_slide_2),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when {
                    !isDownloaded && isCopyInProgress -> { /* hidden during copy */ }
                    !isDownloaded && downloadProgress > 0f -> ModernProgressBar(progress = downloadProgress)
                    !isDownloaded && downloadProgress == 0f -> ModernProgressBar(progress = null)
                }
                StartGradientButton(
                    enabled = isDownloaded,
                    onClick = { if (isDownloaded) openChatActivity() },
                )
                DotsIndicator(currentIndex = 1, total = 2)
            }
        }
    }

    @Composable
    private fun ModernProgressBar(progress: Float?) {
        val gradient = Brush.linearGradient(listOf(Color(0xFF7F00FF), Color(0xFFFF0066)))
        val shape = RoundedCornerShape(12.dp)
        val track = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.18f))
        Box(track) {
            val fill = (progress ?: 1f).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth(fill)
                    .height(8.dp)
                    .clip(shape)
                    .background(gradient),
            )
        }
    }

    @Composable
    private fun DotsIndicator(currentIndex: Int, total: Int, modifier: Modifier = Modifier) {
        val gradient = Brush.linearGradient(listOf(Color(0xFF7F00FF), Color(0xFFFF0066)))
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(total) { i ->
                val active = i == currentIndex
                if (active) {
                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(gradient),
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f)),
                    )
                }
            }
        }
    }

    @Composable
    private fun StartGradientButton(enabled: Boolean, onClick: () -> Unit) {
        val shape = RoundedCornerShape(24.dp)
        val activeGradient = Brush.linearGradient(listOf(Color(0xFF7F00FF), Color(0xFFFF0066)))
        val inactiveGradient = Brush.linearGradient(listOf(Color(0xFF333333), Color(0xFF222222)))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(if (enabled) activeGradient else inactiveGradient)
                    .padding(2.dp), // border thickness
        ) {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF101010),
                    disabledContainerColor = Color(0xFF181818),
                    contentColor = Color.White,
                    disabledContentColor = Color(0xFF444444), // darker inactive text
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start")
            }
        }
    }
}
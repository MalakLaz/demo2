package com.mallar.app.ui.screens

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.*
import com.mallar.app.data.model.DetectedLocation
import com.mallar.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── Global imageCapture reference — set once when camera binds ──────────────
private var imageCaptureRef: ImageCapture? = null

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ARDetectionScreen(
    isDetecting: Boolean,
    detectionProgress: Float,
    detectedLocation: DetectedLocation?,
    detectionError: String?,
    onScanFrame: (Bitmap) -> Unit,
    onStartDetection: () -> Unit,
    onLocationConfirmed: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    // Clean up imageCapture when leaving screen
    DisposableEffect(Unit) {
        onDispose { imageCaptureRef = null }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Camera preview
        if (cameraPermission.status.isGranted) {
            CameraPreviewWithCapture(modifier = Modifier.fillMaxSize())
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(BackgroundDark),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null,
                        tint = TealPrimary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Camera permission required", color = Color.White,
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { cameraPermission.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) { Text("Grant Permission") }
                }
            }
        }

        // Scanning overlay
        ARScanOverlay(isDetecting = isDetecting, detectionProgress = detectionProgress)

        // Back button
        Box(
            modifier = Modifier
                .padding(top = 16.dp, start = 16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(TealPrimary)
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                    tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // Bottom UI
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Detection result card
            AnimatedVisibility(
                visible = detectedLocation != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                detectedLocation?.let { location ->
                    DetectedLocationCard(location = location, onConfirm = onLocationConfirmed)
                }
            }

            // Error card
            AnimatedVisibility(visible = detectionError != null) {
                detectionError?.let { err ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFB71C1C).copy(0.9f)
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(err, color = Color.White, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scan button
            if (!isDetecting && detectedLocation == null) {
                Button(
                    onClick = {
                        val capture = imageCaptureRef
                        if (capture != null) {
                            // ✅ Launch coroutine to capture on IO thread, then call back on Main
                            scope.launch {
                                try {
                                    val bitmap = withContext(Dispatchers.IO) {
                                        captureImageSuspend(capture)
                                    }
                                    if (bitmap != null) {
                                        onScanFrame(bitmap)   // called on Main thread ✅
                                    } else {
                                        onStartDetection()    // fallback
                                    }
                                } catch (e: Exception) {
                                    onStartDetection()        // fallback
                                }
                            }
                        } else {
                            // Camera not ready yet
                            onStartDetection()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(29.dp)
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Scan Environment", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Progress indicator
            if (isDetecting) {
                ScanningProgressBar(progress = detectionProgress)
            }
        }
    }
}

// ─── Capture one frame using a single-thread executor (safe, no context needed) ──
private suspend fun captureImageSuspend(capture: ImageCapture): Bitmap? {
    return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        capture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bmp = runCatching { imageProxy.toBitmap() }.getOrNull()
                    imageProxy.close()
                    executor.shutdown()
                    continuation.resume(bmp) {}
                }
                override fun onError(exception: ImageCaptureException) {
                    executor.shutdown()
                    continuation.resume(null) {}
                }
            }
        )
        continuation.invokeOnCancellation { executor.shutdown() }
    }
}

// ─── Camera preview that registers ImageCapture use-case ─────────────────────
@Composable
fun CameraPreviewWithCapture(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture
                    )

                    // ✅ Store reference AFTER binding succeeds
                    imageCaptureRef = capture

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

// ─── Alias used by ARNavigationScreen ────────────────────────────────────────
@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    CameraPreviewWithCapture(modifier = modifier)
}

@Composable
fun ARScanOverlay(isDetecting: Boolean, detectionProgress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val cornerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "corner"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (isDetecting) {
            ARCornerBracket(Alignment.TopStart, cornerAlpha)
            ARCornerBracket(Alignment.TopEnd, cornerAlpha)
            ARCornerBracket(Alignment.BottomStart, cornerAlpha)
            ARCornerBracket(Alignment.BottomEnd, cornerAlpha)
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, TealPrimary.copy(0.05f), Color.Transparent)
                    )
                )
            )
        }

        if (!isDetecting) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(0.6f))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "📷  Point camera at the mall surroundings",
                    color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun BoxScope.ARCornerBracket(alignment: Alignment, alpha: Float) {
    Box(
        modifier = Modifier
            .align(alignment)
            .padding(40.dp)
            .size(50.dp)
            .alpha(alpha)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(3.dp, TealPrimary, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(TealPrimary.copy(0.05f))
        )
    }
}

@Composable
fun DetectedLocationCard(location: DetectedLocation, onConfirm: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null,
                    tint = TealPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Location Detected!", fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(location.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TealPrimary)
            if (location.features.isNotEmpty()) {
                Text(
                    "Floor ${location.floor}  •  Near: ${location.features.take(2).joinToString(", ")}",
                    fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("Confirm My Location", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ScanningProgressBar(progress: Float) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.8f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val pulseScale by rememberInfiniteTransition(label = "p").animateFloat(
                initialValue = 0.9f, targetValue = 1.1f,
                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                label = "ps"
            )
            Icon(Icons.Default.CenterFocusStrong, contentDescription = null,
                tint = TealPrimary, modifier = Modifier.size(32.dp).scale(pulseScale))
            Spacer(modifier = Modifier.height(10.dp))
            Text("Scanning environment...", color = Color.White,
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = TealPrimary,
                trackColor = Color.White.copy(0.2f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("${(progress * 100).toInt()}%", color = TealPrimary,
                fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
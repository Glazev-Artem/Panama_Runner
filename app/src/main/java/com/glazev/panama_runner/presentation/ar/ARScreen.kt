package com.glazev.panama_runner.presentation.ar

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARScene
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ARScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Нужен доступ к камере", Toast.LENGTH_LONG).show()
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val activity = context.findActivity()
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            activity?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
        }
    }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val childNodes = rememberNodes()

    val mainLightNode = rememberMainLightNode(engine) {
        intensity = 60_000f
        color = dev.romainguy.kotlin.math.Float4(1.0f, 1.0f, 1.0f, 1.0f)
        rotation = io.github.sceneview.math.Rotation(x = 50f, y = 0f, z = 40f)
    }

    var isFaceDetected by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showFlash by remember { mutableStateOf(false) }

    val panamaYOffset = remember { Animatable(0.6f) }
    var rotationY by remember { mutableStateOf(-15f) }

    // Создаем базовый узел для отслеживания лица (старый добрый вариант)
    val faceAnchorNode = remember { Node(engine).apply { isVisible = false } }

    // Состояние для хранения готовой ноды с панамой
    val panamaNodeState = remember { mutableStateOf<ModelNode?>(null) }

    // 1. Добавляем якорь на сцену один раз при запуске
    LaunchedEffect(Unit) {
        if (!childNodes.contains(faceAnchorNode)) {
            childNodes += faceAnchorNode
        }
    }

    // 2. Безопасно загружаем модель и привязываем её заранее (вне потока камеры)
    LaunchedEffect(modelLoader) {
        try {
            val model = modelLoader.loadModel("panama.glb")
            if (model != null) {
                val modelInstance = modelLoader.createInstance(model)
                if (modelInstance != null) {
                    // Используем рабочий конструктор из вчерашней версии
                    val pNode = object : ModelNode(modelInstance = modelInstance) {}
                    pNode.parent = faceAnchorNode // Привязываем к якорю лица
                    panamaNodeState.value = pNode
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(isFaceDetected) {
        if (isFaceDetected) {
            panamaYOffset.animateTo(
                targetValue = 0.13f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 80f)
            )
        } else {
            panamaYOffset.snapTo(0.6f)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            engine = engine,
            modelLoader = modelLoader,
            mainLightNode = mainLightNode,
            sessionFeatures = setOf(Session.Feature.FRONT_CAMERA),
            sessionConfiguration = { _, config ->
                config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
                config.lightEstimationMode = Config.LightEstimationMode.DISABLED
                config.focusMode = Config.FocusMode.AUTO
            },
            onSessionUpdated = { session, _ ->
                val allFaces = session.getAllTrackables(com.google.ar.core.AugmentedFace::class.java)
                val trackingFace = allFaces.find { it.trackingState == TrackingState.TRACKING }

                isFaceDetected = trackingFace != null

                if (trackingFace != null) {
                    val pose = trackingFace.centerPose

                    // 3. ТОЛЬКО обновление координат. Никаких созданий или добавлений объектов!
                    faceAnchorNode.worldPosition = io.github.sceneview.math.Position(pose.tx(), pose.ty(), pose.tz())
                    faceAnchorNode.worldQuaternion = dev.romainguy.kotlin.math.Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw())
                    faceAnchorNode.isVisible = true

                    val pitchShift = pose.qx() * 0.01f

                    panamaNodeState.value?.let { node ->
                        node.position = io.github.sceneview.math.Position(
                            x = 0f,
                            y = panamaYOffset.value + pitchShift,
                            z = -0.09f + pitchShift
                        )
                        node.rotation = io.github.sceneview.math.Rotation(x = 0f, y = rotationY, z = 0f)
                        node.scale = dev.romainguy.kotlin.math.Float3(-0.11f, 0.11f, 0.11f)
                    }

                } else {
                    faceAnchorNode.isVisible = false
                }
                isLoading = false
            }
        )

        AnimatedVisibility(visible = showFlash, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White))
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Yellow)
        } else if (!isFaceDetected) {
            Text(
                "Наведите камеру на лицо",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(0.5f), CircleShape)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                fontSize = 16.sp
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(bottom = 50.dp)
                .offset(x = 100.dp)
                .size(65.dp)
                .background(Color.Black.copy(0.4f), CircleShape)
                .align(Alignment.BottomCenter)
        ) {
            Text("⬅️", fontSize = 30.sp)
        }

        if (isFaceDetected && !isLoading) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp)
                    .fillMaxWidth(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Поворот панамы", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                Slider(
                    value = rotationY,
                    onValueChange = { rotationY = it },
                    valueRange = -180f..180f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Yellow,
                        activeTrackColor = Color.Yellow,
                        inactiveTrackColor = Color.Gray.copy(0.5f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .padding(bottom = 40.dp)
                    .size(90.dp)
                    .background(Color.White.copy(0.5f), CircleShape)
                    .padding(4.dp)
                    .background(Color.White, CircleShape)
                    .align(Alignment.BottomCenter)
                    .clickable {
                        takeScreenshot(view, context) {
                            scope.launch {
                                showFlash = true
                                delay(100)
                                showFlash = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("📸", fontSize = 40.sp)
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private fun takeScreenshot(view: android.view.View, context: android.content.Context, onComplete: () -> Unit) {
    val activity = context.findActivity() ?: return onComplete()
    val surfaceView = findSurfaceView(activity.window.decorView)

    if (surfaceView == null) {
        Toast.makeText(context, "Ошибка: AR-камера не найдена", Toast.LENGTH_SHORT).show()
        onComplete()
        return
    }

    val bitmap = Bitmap.createBitmap(surfaceView.width, surfaceView.height, Bitmap.Config.ARGB_8888)

    try {
        PixelCopy.request(surfaceView, bitmap, { result ->
            if (result == PixelCopy.SUCCESS) {
                val watermarkedBitmap = addWatermark(bitmap, context)
                saveBitmapToGallery(watermarkedBitmap, context)
            } else {
                Toast.makeText(context, "Не удалось сделать фото", Toast.LENGTH_SHORT).show()
            }
            onComplete()
        }, Handler(Looper.getMainLooper()))
    } catch (e: Exception) {
        e.printStackTrace()
        onComplete()
    }
}

private fun findSurfaceView(v: android.view.View): android.view.SurfaceView? {
    if (v is android.view.SurfaceView) return v
    if (v is android.view.ViewGroup) {
        for (i in 0 until v.childCount) {
            val result = findSurfaceView(v.getChildAt(i))
            if (result != null) return result
        }
    }
    return null
}

private fun addWatermark(bitmap: Bitmap, context: Context): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(result)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = bitmap.height * 0.025f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
    }
    val text = "PANAMA RUNNER"
    val bounds = android.graphics.Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    val padding = 40f
    val x = bitmap.width - bounds.width() - padding
    val y = bitmap.height - padding
    canvas.drawText(text, x, y, paint)
    paint.textSize = bitmap.height * 0.015f
    val dateText = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date())
    canvas.drawText(dateText, padding, y, paint)
    return result
}

private fun saveBitmapToGallery(bitmap: Bitmap, context: android.content.Context) {
    val filename = "Panama_${System.currentTimeMillis()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/PanamaRunner")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }
    val contentResolver = context.contentResolver
    val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    imageUri?.let { uri ->
        contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)
        }
        Toast.makeText(context, "Фото сохранено!", Toast.LENGTH_SHORT).show()
    }
}
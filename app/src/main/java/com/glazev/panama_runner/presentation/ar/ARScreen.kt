package com.glazev.panama_runner.presentation.ar

import android.app.Activity
import android.content.ContentValues
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import dev.romainguy.kotlin.math.Quaternion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

@Composable
fun ARScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var sceneView by remember { mutableStateOf<ARSceneView?>(null) }
    val flyIn = remember { object { var amount = 1f } }
    var smoothScale by remember { mutableFloatStateOf(1f) }

    // Управление видимостью расширенных настроек
    var showExtraSettings by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    DisposableEffect(context) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { activity?.requestedOrientation = originalOrientation }
    }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val childNodes = rememberNodes()

    // ==========================================
    // 1. УСИЛЕННОЕ ОСВЕЩЕНИЕ (Пункт 7.7)
    // ==========================================
    val mainLightNode = rememberMainLightNode(engine).apply {
        intensity = 100000f // Яркий фронтальный свет
        color = io.github.sceneview.math.Color(1f, 1f, 1f, 1f)
        // Свет светит СВЕРХУ и ВГЛУБЬ экрана (на лоб пользователя)
        lightDirection = io.github.sceneview.math.Direction(0.5f, -1f, -1f)
    }

    val secondaryLightNode = remember(engine) {
        io.github.sceneview.node.LightNode(engine, type = com.google.android.filament.LightManager.Type.DIRECTIONAL) {
            intensity(60000f) // Мощный заполняющий свет
        }.apply {
            color = io.github.sceneview.math.Color(1f, 1f, 1f, 1f)
            // Свет под углом с другой стороны
            lightDirection = io.github.sceneview.math.Direction(-0.5f, -0.8f, -1f)
        }
    }

    val cameraStream = rememberARCameraStream(materialLoader)

    // ТВОИ НОВЫЕ ЭТАЛОННЫЕ НАСТРОЙКИ
    var sharedScale by remember { mutableFloatStateOf(0.101f) }
    var sharedY by remember { mutableFloatStateOf(0.109f) }
    var sharedZ by remember { mutableFloatStateOf(-0.062f) }
    var sharedX by remember { mutableFloatStateOf(0.0f) } 
    var sharedRot by remember { mutableFloatStateOf(0f) }

    val faceAnchorNode = remember(engine) { Node(engine).apply { isVisible = false } }
    val hatPivot = remember(engine) { Node(engine).apply { parent = faceAnchorNode } }
    val occluderPivot = remember(engine) { Node(engine).apply { parent = faceAnchorNode } }

    var hatNode by remember { mutableStateOf<ModelNode?>(null) }
    var occluderNode by remember { mutableStateOf<ModelNode?>(null) }

    LaunchedEffect(Unit) {
        if (faceAnchorNode !in childNodes) childNodes.add(faceAnchorNode)
        if (secondaryLightNode !in childNodes) childNodes.add(secondaryLightNode)

        modelLoader.loadModelInstanceAsync("head_occluder.glb") { modelInstance ->
            if (modelInstance != null) {
                val node = ModelNode(modelInstance = modelInstance).apply {
                    isVisible = true
                    parent = occluderPivot
                }
                val rm = engine.renderableManager
                modelInstance.asset.entities.forEach { entity ->
                    val instance = rm.getInstance(entity)
                    if (instance != 0) {
                        rm.setPriority(instance, 7)
                        val count = rm.getPrimitiveCount(instance)
                        for (i in 0 until count) {
                            val mat = rm.getMaterialInstanceAt(instance, i)
                            mat.setColorWrite(false)
                            mat.setDepthWrite(true)
                        }
                    }
                }
                occluderNode = node
            }
        }

        modelLoader.loadModelInstanceAsync("panama.glb") { modelInstance ->
            if (modelInstance != null) {
                val node = ModelNode(modelInstance = modelInstance).apply {
                    isVisible = true
                    parent = hatPivot
                }
                val rm = engine.renderableManager
                modelInstance.asset.entities.forEach { entity ->
                    val instance = rm.getInstance(entity)
                    if (instance != 0) rm.setPriority(instance, 7)
                }
                hatNode = node
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraStream = cameraStream,
            childNodes = childNodes,
            mainLightNode = mainLightNode,
            onViewCreated = {
                sceneView = this
                this.setZOrderOnTop(false)
                lightEstimator = null 
            },
            isOpaque = false,
            sessionFeatures = setOf(Session.Feature.FRONT_CAMERA),
            onSessionCreated = { it.setCameraTextureNames(cameraStream.cameraTextureIds) },
            sessionConfiguration = { _, config ->
                config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
                config.focusMode = Config.FocusMode.AUTO
                config.lightEstimationMode = Config.LightEstimationMode.DISABLED
            },
            onSessionUpdated = { session, _ ->
                val face = session.getAllTrackables(AugmentedFace::class.java)
                    .firstOrNull { it.trackingState == TrackingState.TRACKING }

                if (face != null) {
                    val pose = face.centerPose
                    if (!faceAnchorNode.isVisible) flyIn.amount = 1f
                    
                    faceAnchorNode.worldPosition = Position(pose.tx(), pose.ty(), pose.tz())
                    faceAnchorNode.worldQuaternion = Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw())
                    faceAnchorNode.isVisible = true

                    val meshBuffer = face.meshVertices
                    val lx = meshBuffer.get(234 * 3); val ly = meshBuffer.get(234 * 3 + 1); val lz = meshBuffer.get(234 * 3 + 2)
                    val rx = meshBuffer.get(454 * 3); val ry = meshBuffer.get(454 * 3 + 1); val rz = meshBuffer.get(454 * 3 + 2)
                    val earDist = sqrt((lx-rx)*(lx-rx) + (ly-ry)*(ly-ry) + (lz-rz)*(lz-rz))
                    
                    val targetScaleMult = (earDist / 0.155f).coerceIn(0.8f, 1.4f)
                    smoothScale = smoothScale + (targetScaleMult - smoothScale) * 0.1f
                    
                    val finalScale = sharedScale * smoothScale
                    val midX = (lx + rx) / 2f
                    val centerX = sharedX - midX 

                    if (flyIn.amount > 0f) flyIn.amount = (flyIn.amount - 0.04f).coerceAtLeast(0f)
                    val currentFlyInY = flyIn.amount * 0.4f

                    hatPivot.position = Position(centerX, sharedY + currentFlyInY, sharedZ)
                    hatNode?.let { node ->
                        node.rotation = Rotation(0f, sharedRot, 0f)
                        node.scale = Scale(-finalScale, finalScale, finalScale)
                    }

                    occluderPivot.position = Position(centerX, sharedY + currentFlyInY, sharedZ)
                    occluderNode?.let { node ->
                        node.rotation = Rotation(0f, sharedRot, 0f)
                        val occScale = finalScale * 0.96f
                        node.scale = Scale(-occScale, occScale, occScale)
                    }
                } else {
                    faceAnchorNode.isVisible = false
                    flyIn.amount = 1f
                }
            }
        )

        // UI
        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ПАНЕЛЬ НАСТРОЕК
                Box(
                    modifier = Modifier.fillMaxWidth(0.9f).background(Color.Black.copy(0.4f), RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column {
                        // Расширяемые настройки (Размер и Высота)
                        AnimatedVisibility(
                            visible = showExtraSettings,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column {
                                HorizontalDebugSlider("Размер", sharedScale, 0.08f, 0.15f) { sharedScale = it }
                                HorizontalDebugSlider("Высота", sharedY, 0.05f, 0.25f) { sharedY = it }
                                Divider(color = Color.White.copy(0.2f), modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }

                        // Основная строка (Поворот + Кнопка настроек)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showExtraSettings = !showExtraSettings },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings, 
                                    contentDescription = null, 
                                    tint = if (showExtraSettings) Color.Yellow else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            HorizontalDebugSlider("Поворот", sharedRot, 0f, 360f) { sharedRot = it }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)) {
                    IconButton(
                        onClick = {
                            sceneView?.let { view ->
                                takeScreenshot(view) { bitmap ->
                                    bitmap?.let { saveBitmapToGallery(context, it) }
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.Center).size(60.dp).background(Color.White, CircleShape)
                    ) { Text("📷", fontSize = 30.sp) }

                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterEnd).size(60.dp).background(Color.Black.copy(0.4f), CircleShape)
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White, modifier = Modifier.size(30.dp)) }
                }
            }
        }
    }
}

fun takeScreenshot(view: SurfaceView, onResult: (Bitmap?) -> Unit) {
    try {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) onResult(bitmap) else onResult(null)
        }, Handler(Looper.getMainLooper()))
    } catch (e: Exception) { onResult(null) }
}

fun saveBitmapToGallery(context: android.content.Context, bitmap: Bitmap) {
    val watermarkedBitmap = addWatermark(bitmap)
    val filename = "PanamaSelfie_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/PanamaRunner")
    }
    val contentResolver = context.contentResolver
    val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    
    imageUri?.let { uri ->
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Фото сохранено в галерею!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun addWatermark(source: Bitmap): Bitmap {
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
    val text = "ТВОЯ ПАНАМА"
    val paint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = source.height * 0.035f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
    }
    val dateWidth = paint.measureText(date)
    canvas.drawText(date, source.width - dateWidth - 40f, paint.textSize + 40f, paint)
    canvas.drawText(text, 40f, source.height - 40f, paint)
    return result
}

@Composable
fun HorizontalDebugSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, modifier = Modifier.width(70.dp), fontSize = 12.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.weight(1f)
        )
        Text("%.3f".format(value), color = Color.White, modifier = Modifier.width(45.dp), fontSize = 12.sp)
    }
}

package com.glazev.panama_runner.presentation.ar

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.google.ar.core.AugmentedFace
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARScene
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
import dev.romainguy.kotlin.math.Float3

@Composable
fun ARScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    DisposableEffect(context) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val childNodes = rememberNodes()

    // ==========================================
    // 1. НАСТРОЙКИ ОСВЕЩЕНИЯ (Выведены в UI)
    // ==========================================
    var lightIntensity by remember { mutableFloatStateOf(90000f) }
    var lightDirX by remember { mutableFloatStateOf(0f) } // Направление света по X (Влево/Вправо)
    var lightDirY by remember { mutableFloatStateOf(1f) } // Направление света по Y (Сверху/Снизу)
    var lightDirZ by remember { mutableFloatStateOf(1f) } // Направление света по Z (Спереди/Сзади)

    val mainLightNode = rememberMainLightNode(engine)

    // Обновляем свет динамически при движении ползунков
    LaunchedEffect(lightIntensity, lightDirX, lightDirY, lightDirZ) {
        mainLightNode.intensity = lightIntensity
        mainLightNode.position = Position(lightDirX, lightDirY, lightDirZ)
    }

    val cameraStream = rememberARCameraStream(materialLoader)

    // ==========================================
    // 2. НАСТРОЙКИ ПАНАМЫ (P)
    // ==========================================
    var pScale by remember { mutableFloatStateOf(0.12f) }
    var pY by remember { mutableFloatStateOf(0.14f) }
    // Изначально ставим глубину -0.07
    var pZ by remember { mutableFloatStateOf(-0.07f) }
    var pRot by remember { mutableFloatStateOf(180f) }

    // ==========================================
    // 3. НАСТРОЙКИ ЩИТА-ОККЛЮДЕРА (O)
    // ==========================================
    var oScale by remember { mutableFloatStateOf(0.13f) }
    var oY by remember { mutableFloatStateOf(0.07f) }
    // ВАЖНО: Изначально ставим глубину щита ТАКУЮ ЖЕ, как у панамы (-0.07), чтобы избежать раскачивания!
    var oZ by remember { mutableFloatStateOf(-0.07f) }
    var oRot by remember { mutableFloatStateOf(0f) }

    // Тумблер видимости щита для настройки
    var isOccluderVisible by remember { mutableStateOf(false) }

    val faceAnchorNode = remember(engine) { Node(engine).apply { isVisible = false } }

    var hatNode by remember { mutableStateOf<ModelNode?>(null) }
    var occluderNode by remember { mutableStateOf<ModelNode?>(null) }

    var loadStatus by remember { mutableStateOf("Загрузка...") }

    LaunchedEffect(Unit) {
        if (faceAnchorNode !in childNodes) childNodes.add(faceAnchorNode)

        // ЗАГРУЗКА ЩИТА
        modelLoader.loadModelInstanceAsync("head_occluder.glb") { modelInstance ->
            if (modelInstance != null) {
                val node = ModelNode(modelInstance = modelInstance, centerOrigin = Position(0f, 0f, 0f)).apply {
                    isVisible = true
                    parent = faceAnchorNode
                }

                val rm = engine.renderableManager
                modelInstance.asset?.entities?.forEach { entity ->
                    val renderableInstance = rm.getInstance(entity)
                    if (renderableInstance != 0) {
                        // Твоя победная формула: Приоритет 7!
                        rm.setPriority(renderableInstance, 7)
                        rm.setCastShadows(renderableInstance, false)
                        rm.setReceiveShadows(renderableInstance, false)
                    }
                }
                occluderNode = node
            }
        }

        // ЗАГРУЗКА ПАНАМЫ
        modelLoader.loadModelInstanceAsync("panama.glb") { modelInstance ->
            if (modelInstance != null) {
                val node = ModelNode(modelInstance = modelInstance, centerOrigin = Position(0f, 0f, 0f)).apply {
                    isVisible = true
                    parent = faceAnchorNode
                }

                val rm = engine.renderableManager
                modelInstance.asset?.entities?.forEach { entity ->
                    val renderableInstance = rm.getInstance(entity)
                    if (renderableInstance != 0) {
                        // Твоя победная формула: Приоритет 7!
                        rm.setPriority(renderableInstance, 7)
                        // Отключаем отбрасывание теней от панамы на лицо (чтобы избежать артефактов)
                        rm.setCastShadows(renderableInstance, false)
                    }
                }
                hatNode = node
                loadStatus = "Готово!"
            }
        }
    }

    // Реактивное управление прозрачностью щита
    LaunchedEffect(isOccluderVisible, occluderNode) {
        occluderNode?.modelInstance?.asset?.entities?.forEach { entity ->
            val rm = engine.renderableManager
            val renderableInstance = rm.getInstance(entity)
            if (renderableInstance != 0) {
                val primitiveCount = rm.getPrimitiveCount(renderableInstance)
                for (i in 0 until primitiveCount) {
                    val material = rm.getMaterialInstanceAt(renderableInstance, i)
                    material.setColorWrite(isOccluderVisible)
                    material.setDepthWrite(true)
                }
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
            isOpaque = false,
            sessionFeatures = setOf(Session.Feature.FRONT_CAMERA),
            sessionCameraConfig = { session ->
                session.getSupportedCameraConfigs(CameraConfigFilter(session).apply {
                    facingDirection = CameraConfig.FacingDirection.FRONT
                }).firstOrNull() ?: session.cameraConfig
            },
            onSessionCreated = { it.setCameraTextureNames(cameraStream.cameraTextureIds) },
            sessionConfiguration = { _, config ->
                config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
                config.focusMode = Config.FocusMode.FIXED
            },
            onSessionUpdated = { session, _ ->
                val face = session.getAllTrackables(AugmentedFace::class.java)
                    .firstOrNull { it.trackingState == TrackingState.TRACKING }

                if (face != null) {
                    val pose = face.centerPose
                    val headQuat = Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw())

                    faceAnchorNode.worldPosition = Position(pose.tx(), pose.ty(), pose.tz())
                    faceAnchorNode.worldQuaternion = headQuat
                    faceAnchorNode.isVisible = true

                    // ПРИВЯЗКА ПАНАМЫ
                    hatNode?.let { node ->
                        val rotatedHatOffset = headQuat * Float3(0f, pY, pZ)
                        node.position = Position(rotatedHatOffset.x, rotatedHatOffset.y, rotatedHatOffset.z)
                        node.scale = Scale(pScale)
                        node.rotation = Rotation(0f, pRot, 0f)
                    }

                    // ПРИВЯЗКА ЩИТА
                    occluderNode?.let { node ->
                        val rotatedOccOffset = headQuat * Float3(0f, oY, oZ)
                        node.position = Position(rotatedOccOffset.x, rotatedOccOffset.y, rotatedOccOffset.z)
                        node.scale = Scale(oScale)
                        node.rotation = Rotation(0f, oRot, 0f)
                    }
                } else {
                    faceAnchorNode.isVisible = false
                }
            }
        )

        // ИНТЕРФЕЙС НАСТРОЙКИ СО СКРОЛЛОМ
        Popup(
            alignment = Alignment.BottomCenter,
            properties = PopupProperties(focusable = true, dismissOnClickOutside = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f) // Занимает 60% экрана
                    .background(Color.Black.copy(0.8f))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()), // ДЕЛАЕМ СКРОЛЛ!
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(loadStatus, color = Color.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = isOccluderVisible,
                        onCheckedChange = { isOccluderVisible = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Red)
                    )
                }
                Text(if (isOccluderVisible) "ЩИТ ВИДИМ (Калибровка)" else "ЩИТ СКРЫТ (Финальный вид)", color = Color.White, fontSize = 12.sp)

                Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))

                // --- БЛОК ОСВЕЩЕНИЯ ---
                Text("НАСТРОЙКИ СВЕТА", color = Color.White, fontWeight = FontWeight.Bold)
                HorizontalDebugSlider("Яркость", lightIntensity, 10000f, 200000f) { lightIntensity = it }
                HorizontalDebugSlider("Свет X", lightDirX, -2f, 2f) { lightDirX = it }
                HorizontalDebugSlider("Свет Y", lightDirY, -2f, 2f) { lightDirY = it }
                HorizontalDebugSlider("Свет Z", lightDirZ, -2f, 2f) { lightDirZ = it }

                Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))

                // --- БЛОК ОККЛЮДЕРА ---
                Text("НАСТРОЙКИ ЩИТА", color = Color.Cyan, fontWeight = FontWeight.Bold)
                HorizontalDebugSlider("Размер", oScale, 0.05f, 0.25f) { oScale = it }
                HorizontalDebugSlider("Высота", oY, -0.1f, 0.2f) { oY = it }
                HorizontalDebugSlider("Глубина Z", oZ, -0.2f, 0.1f) { oZ = it }
                HorizontalDebugSlider("Поворот", oRot, 0f, 360f) { oRot = it }

                Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))

                // --- БЛОК ПАНАМЫ ---
                Text("НАСТРОЙКИ ПАНАМЫ", color = Color.Yellow, fontWeight = FontWeight.Bold)
                HorizontalDebugSlider("Размер", pScale, 0.05f, 0.25f) { pScale = it }
                HorizontalDebugSlider("Высота", pY, -0.1f, 0.2f) { pY = it }
                HorizontalDebugSlider("Глубина Z", pZ, -0.2f, 0.1f) { pZ = it }
                HorizontalDebugSlider("Поворот", pRot, 0f, 360f) { pRot = it }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Назад")
                }
            }
        }
    }
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
        // Форматируем разные значения по-разному для удобства
        val formatStr = if (max > 1000f) "%.0f" else "%.3f"
        Text(formatStr.format(value), color = Color.White, modifier = Modifier.width(45.dp), fontSize = 12.sp)
    }
}
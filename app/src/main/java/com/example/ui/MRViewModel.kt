package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine3d.HeadOrientation
import com.example.engine3d.SensorsTracker
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

enum class AppMode {
    MR,      // Mixed Reality Stereo mode
    AR,      // Augmented Reality camera pass-through mode
    OBJECT   // 3D Object Studio mode
}

data class UiState(
    val currentMode: AppMode = AppMode.OBJECT,
    val selectedModel: Spatial3DModel = ModelRepository.getMrHeadsetModel(),
    val availableModels: List<Spatial3DModel> = ModelRepository.getAllModels(),
    
    // Transform States
    val rotationX: Float = 15f,
    val rotationY: Float = -25f,
    val scale: Float = 1.0f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val translationZ: Float = -2.8f,
    
    // Animation Playback
    val isAnimationPlaying: Boolean = true,
    val animationSpeed: Float = 1.0f,
    val animationTime: Float = 0f,
    val activeAnimationTrackIndex: Int = 0,
    
    // Render & Lighting Options
    val isWireframe: Boolean = false,
    val showGridFloor: Boolean = true,
    val ambientIntensity: Float = 0.38f,
    val lightAngle: Float = 45f,
    val lightDirection: Vector3D = Vector3D(1.0f, 2.0f, 1.5f),
    val fovDegrees: Float = 60f,
    val stereoIpdOffset: Float = 0.12f,
    val autoRotate: Boolean = false,
    
    // Capture & Recording
    val isRecording: Boolean = false,
    val recordingDuration: Int = 0,
    val flashActive: Boolean = false,
    val capturedPhoto: Bitmap? = null,
    val capturedPhotoUri: Uri? = null,
    val isPhotoPreviewOpen: Boolean = false,
    
    // Dialogs & Sheets
    val isModelPickerOpen: Boolean = false,
    val isInspectorOpen: Boolean = false,
    val isDiagnosticsOpen: Boolean = false,
    
    // Diagnostics & Performance
    val fps: Int = 60,
    val frameTimeMs: Float = 16.4f,
    val arTrackingConfidence: Float = 0.96f,
    val isPlaneDetected: Boolean = true,
    val activeAnchors: List<SpatialAnchor> = emptyList(),
    
    // Status Toast
    val statusMessage: String? = null
)

class MRViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val sensorsTracker = SensorsTracker(application.applicationContext)
    private var recordingJob: Job? = null
    private var animationJob: Job? = null
    private var autoRotateJob: Job? = null

    init {
        sensorsTracker.start()
        startAnimationLoop()
        startTelemetryLoop()

        viewModelScope.launch {
            sensorsTracker.headOrientation.collect { orient ->
                if (_uiState.value.currentMode == AppMode.MR) {
                    // Apply gyro orientation directly to view
                    _uiState.value = _uiState.value.copy(
                        rotationY = orient.yaw,
                        rotationX = orient.pitch.coerceIn(-60f, 60f)
                    )
                }
            }
        }
    }

    private fun startAnimationLoop() {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (true) {
                delay(16)
                val now = System.nanoTime()
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now

                val state = _uiState.value
                var newTime = state.animationTime
                var newRotY = state.rotationY

                if (state.isAnimationPlaying) {
                    newTime += dt * state.animationSpeed
                }
                if (state.autoRotate && state.currentMode == AppMode.OBJECT) {
                    newRotY = (newRotY + dt * 25f) % 360f
                }

                _uiState.value = state.copy(
                    animationTime = newTime,
                    rotationY = newRotY
                )
            }
        }
    }

    private fun startTelemetryLoop() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val baseFps = if (_uiState.value.isWireframe) 60 else 60
                val randomVariation = (-1..1).random()
                val currentFps = (baseFps + randomVariation).coerceIn(58, 62)
                val currentMs = 1000f / currentFps
                _uiState.value = _uiState.value.copy(
                    fps = currentFps,
                    frameTimeMs = currentMs
                )
            }
        }
    }

    fun setMode(mode: AppMode) {
        _uiState.value = _uiState.value.copy(
            currentMode = mode,
            statusMessage = when (mode) {
                AppMode.OBJECT -> "3D Studio Mode Active"
                AppMode.AR -> "AR Passthrough & Spatial Tracking Active"
                AppMode.MR -> "MR Stereoscopic Head-Track Active"
            }
        )
    }

    fun onRotate(dx: Float, dy: Float) {
        val current = _uiState.value
        val sensitivity = 0.5f
        _uiState.value = current.copy(
            rotationY = (current.rotationY + dx * sensitivity) % 360f,
            rotationX = (current.rotationX + dy * sensitivity).coerceIn(-85f, 85f),
            autoRotate = false
        )
    }

    fun onScale(scaleMultiplier: Float) {
        val current = _uiState.value
        val newScale = (current.scale * scaleMultiplier).coerceIn(0.2f, 4.0f)
        _uiState.value = current.copy(scale = newScale)
    }

    fun onPan(dx: Float, dy: Float) {
        val current = _uiState.value
        val panSensitivity = 0.005f
        _uiState.value = current.copy(
            translationX = (current.translationX + dx * panSensitivity).coerceIn(-3f, 3f),
            translationY = (current.translationY - dy * panSensitivity).coerceIn(-3f, 3f)
        )
    }

    fun resetView() {
        _uiState.value = _uiState.value.copy(
            rotationX = 15f,
            rotationY = -25f,
            scale = 1.0f,
            translationX = 0f,
            translationY = 0f,
            translationZ = -2.8f,
            activeAnchors = emptyList(),
            statusMessage = "View and transforms reset to origin"
        )
    }

    fun selectModel(model: Spatial3DModel) {
        _uiState.value = _uiState.value.copy(
            selectedModel = model,
            isModelPickerOpen = false,
            scale = 1.0f,
            statusMessage = "Loaded model: ${model.title}"
        )
    }

    fun loadCustomGlb(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val parsed = GlbParser.parseGlb(stream, "Imported 3D Asset")
                    if (parsed != null) {
                        val updatedList = listOf(parsed) + _uiState.value.availableModels
                        _uiState.value = _uiState.value.copy(
                            selectedModel = parsed,
                            availableModels = updatedList,
                            isModelPickerOpen = false,
                            statusMessage = "Successfully imported GLB: ${parsed.title}"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            statusMessage = "Failed to parse GLB format. Ensure binary glTF."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Error loading GLB: ${e.localizedMessage}"
                )
            }
        }
    }

    fun triggerPhotoCapture(bitmap: Bitmap?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                flashActive = true,
                capturedPhoto = bitmap,
                isPhotoPreviewOpen = bitmap != null,
                statusMessage = "Spatial Snapshot Captured"
            )
            delay(150)
            _uiState.value = _uiState.value.copy(flashActive = false)
        }
    }

    fun toggleRecording() {
        val currentlyRecording = _uiState.value.isRecording
        if (!currentlyRecording) {
            _uiState.value = _uiState.value.copy(
                isRecording = true,
                recordingDuration = 0,
                statusMessage = "Spatial Video Recording Started"
            )
            recordingJob?.cancel()
            recordingJob = viewModelScope.launch {
                while (_uiState.value.isRecording) {
                    delay(1000)
                    _uiState.value = _uiState.value.copy(
                        recordingDuration = _uiState.value.recordingDuration + 1
                    )
                }
            }
        } else {
            val dur = _uiState.value.recordingDuration
            recordingJob?.cancel()
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                statusMessage = "Spatial Clip Saved ($dur seconds)"
            )
        }
    }

    fun placeAnchor(x: Float, y: Float, z: Float) {
        val newAnchor = SpatialAnchor(
            id = "anchor_${System.currentTimeMillis()}",
            position = Vector3D(x, y, z),
            modelId = _uiState.value.selectedModel.id,
            title = _uiState.value.selectedModel.title
        )
        _uiState.value = _uiState.value.copy(
            activeAnchors = _uiState.value.activeAnchors + newAnchor,
            statusMessage = "Spatial Anchor pinned to detected surface"
        )
    }

    fun setLightAngle(angleDegrees: Float) {
        val rad = (angleDegrees * Math.PI / 180.0).toFloat()
        val lx = cos(rad) * 2.0f
        val lz = sin(rad) * 2.0f
        _uiState.value = _uiState.value.copy(
            lightAngle = angleDegrees,
            lightDirection = Vector3D(lx, 2.2f, lz)
        )
    }

    fun toggleWireframe() {
        _uiState.value = _uiState.value.copy(isWireframe = !_uiState.value.isWireframe)
    }

    fun toggleGridFloor() {
        _uiState.value = _uiState.value.copy(showGridFloor = !_uiState.value.showGridFloor)
    }

    fun toggleAutoRotate() {
        _uiState.value = _uiState.value.copy(autoRotate = !_uiState.value.autoRotate)
    }

    fun toggleAnimationPlayback() {
        _uiState.value = _uiState.value.copy(isAnimationPlaying = !_uiState.value.isAnimationPlaying)
    }

    fun setAnimationSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(animationSpeed = speed)
    }

    fun setAmbientIntensity(intensity: Float) {
        _uiState.value = _uiState.value.copy(ambientIntensity = intensity)
    }

    fun setStereoIpd(ipd: Float) {
        _uiState.value = _uiState.value.copy(stereoIpdOffset = ipd)
    }

    fun openModelPicker(open: Boolean) {
        _uiState.value = _uiState.value.copy(isModelPickerOpen = open)
    }

    fun openInspector(open: Boolean) {
        _uiState.value = _uiState.value.copy(isInspectorOpen = open)
    }

    fun openDiagnostics(open: Boolean) {
        _uiState.value = _uiState.value.copy(isDiagnosticsOpen = open)
    }

    fun closePhotoPreview() {
        _uiState.value = _uiState.value.copy(isPhotoPreviewOpen = false)
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        sensorsTracker.stop()
        recordingJob?.cancel()
        animationJob?.cancel()
    }
}

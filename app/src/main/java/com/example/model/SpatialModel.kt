package com.example.model

import androidx.compose.ui.graphics.Color

data class Vector3D(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
) {
    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Float) = Vector3D(x * scale, y * scale, z * scale)
    
    fun length(): Float = kotlin.math.sqrt(x * x + y * y + z * z)
    
    fun normalized(): Vector3D {
        val len = length()
        return if (len > 0.00001f) Vector3D(x / len, y / len, z / len) else Vector3D(0f, 1f, 0f)
    }
    
    fun dot(other: Vector3D): Float = x * other.x + y * other.y + z * other.z
    
    fun cross(other: Vector3D): Vector3D = Vector3D(
        x = y * other.z - z * other.y,
        y = z * other.x - x * other.z,
        z = x * other.y - y * other.x
    )
}

data class Vertex(
    val position: Vector3D,
    val normal: Vector3D = Vector3D(0f, 1f, 0f),
    val color: Color = Color.White,
    val u: Float = 0f,
    val v: Float = 0f
)

data class Triangle(
    val v0: Int,
    val v1: Int,
    val v2: Int,
    val color: Color = Color.White,
    val normal: Vector3D? = null,
    val materialId: Int = 0
)

data class MaterialPbr(
    val id: Int = 0,
    val name: String = "Standard PBR",
    val baseColor: Color = Color(0xFF00A2FF),
    val roughness: Float = 0.35f,
    val metallic: Float = 0.6f,
    val emissive: Color = Color.Black,
    val emissiveIntensity: Float = 0.0f,
    val isWireframe: Boolean = false,
    val isDoubleSided: Boolean = true,
    val alpha: Float = 1.0f
)

data class Keyframe(
    val time: Float,
    val rotationY: Float = 0f,
    val rotationX: Float = 0f,
    val translateY: Float = 0f,
    val scale: Float = 1f
)

data class AnimationTrack(
    val name: String,
    val durationSeconds: Float,
    val keyframes: List<Keyframe>
)

data class MeshData(
    val vertices: List<Vertex>,
    val triangles: List<Triangle>,
    val materials: List<MaterialPbr> = listOf(MaterialPbr()),
    val name: String = "Mesh"
)

data class Spatial3DModel(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val meshes: List<MeshData>,
    val defaultScale: Float = 1.0f,
    val animations: List<AnimationTrack> = emptyList(),
    val iconName: String = "headset",
    val polyCount: Int = 0,
    val fileSizeFormatted: String = "1.2 MB"
)

data class SpatialAnchor(
    val id: String,
    val position: Vector3D,
    val rotationY: Float = 0f,
    val scale: Float = 1f,
    val modelId: String,
    val title: String
)

package com.example.engine3d

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.*
import kotlin.math.*

class ProjectedTriangle(
    val p0: Offset,
    val p1: Offset,
    val p2: Offset,
    val depth: Float,
    val color: Color,
    val isWireframe: Boolean = false,
    val normalZ: Float = 0f
)

object Renderer3D {

    /**
     * Renders a 3D model onto Compose DrawScope canvas with full matrix pipeline & PBR shading.
     */
    fun renderModel(
        drawScope: DrawScope,
        model: Spatial3DModel,
        rotationX: Float,
        rotationY: Float,
        rotationZ: Float = 0f,
        scale: Float = 1.0f,
        translationX: Float = 0f,
        translationY: Float = 0f,
        translationZ: Float = -2.8f,
        lightDirection: Vector3D = Vector3D(1.0f, 2.0f, 1.5f),
        ambientIntensity: Float = 0.35f,
        isWireframe: Boolean = false,
        showGridFloor: Boolean = true,
        showShadows: Boolean = true,
        fovRadians: Float = (60f * PI / 180f).toFloat(),
        animationTime: Float = 0f,
        activeAnimationIndex: Int = 0,
        stereoOffset: Float = 0f // For MR Left/Right Eye Parallax
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        if (width <= 0 || height <= 0) return

        val aspect = width / height

        // 1. Compute Animation Offset if playing
        var animRotX = 0f
        var animRotY = 0f
        var animTransY = 0f
        var animScale = 1f
        if (model.animations.isNotEmpty() && activeAnimationIndex in model.animations.indices) {
            val track = model.animations[activeAnimationIndex]
            if (track.keyframes.isNotEmpty() && track.durationSeconds > 0) {
                val cycleTime = animationTime % track.durationSeconds
                // Find surrounding keyframes
                val kfs = track.keyframes
                for (k in 0 until kfs.size - 1) {
                    val k0 = kfs[k]
                    val k1 = kfs[k + 1]
                    if (cycleTime >= k0.time && cycleTime <= k1.time) {
                        val dt = (k1.time - k0.time).coerceAtLeast(0.001f)
                        val t = (cycleTime - k0.time) / dt
                        animRotX = k0.rotationX + t * (k1.rotationX - k0.rotationX)
                        animRotY = k0.rotationY + t * (k1.rotationY - k0.rotationY)
                        animTransY = k0.translateY + t * (k1.translateY - k0.translateY)
                        animScale = k0.scale + t * (k1.scale - k0.scale)
                        break
                    }
                }
            }
        }

        // 2. Build MVP Matrices
        val modelScale = (model.defaultScale * scale * animScale).coerceIn(0.1f, 10.0f)
        val mScale = Matrix4x4.scale(modelScale, modelScale, modelScale)
        val mRotX = Matrix4x4.rotationX(((rotationX + animRotX) * PI / 180f).toFloat())
        val mRotY = Matrix4x4.rotationY(((rotationY + animRotY) * PI / 180f).toFloat())
        val mRotZ = Matrix4x4.rotationZ((rotationZ * PI / 180f).toFloat())
        val mTrans = Matrix4x4.translation(translationX + stereoOffset, translationY + animTransY, translationZ)

        val modelMatrix = mTrans * mRotY * mRotX * mRotZ * mScale
        val projMatrix = Matrix4x4.perspective(fovRadians, aspect, 0.1f, 100f)

        // Draw Spatial Ground Grid
        if (showGridFloor) {
            drawSpatialGrid(drawScope, projMatrix, translationZ, width, height)
        }

        // 3. Transform Meshes and Triangles
        val projectedTriangles = ArrayList<ProjectedTriangle>(512)
        val viewDirection = Vector3D(0f, 0f, 1f)

        for (mesh in model.meshes) {
            val vertices = mesh.vertices
            val triangles = mesh.triangles
            val materials = mesh.materials

            // Cache transformed vertices
            val transformedWorld = ArrayList<Vector3D>(vertices.size)
            val projectedPoints = ArrayList<Offset?>(vertices.size)
            val depths = FloatArray(vertices.size)

            for (v in vertices) {
                val worldPos = modelMatrix.transformPoint(v.position)
                transformedWorld.add(worldPos)

                // Project to Screen
                val projPos = projMatrix.transformPoint(worldPos)
                depths[depths.size - 1] = worldPos.z

                if (worldPos.z < -0.1f) {
                    val screenX = (projPos.x * 0.5f + 0.5f) * width
                    val screenY = (-projPos.y * 0.5f + 0.5f) * height
                    projectedPoints.add(Offset(screenX, screenY))
                } else {
                    projectedPoints.add(null)
                }
            }

            for (tri in triangles) {
                if (tri.v0 >= projectedPoints.size || tri.v1 >= projectedPoints.size || tri.v2 >= projectedPoints.size) continue

                val p0 = projectedPoints[tri.v0]
                val p1 = projectedPoints[tri.v1]
                val p2 = projectedPoints[tri.v2]

                if (p0 == null || p1 == null || p2 == null) continue

                val w0 = transformedWorld[tri.v0]
                val w1 = transformedWorld[tri.v1]
                val w2 = transformedWorld[tri.v2]

                // Triangle normal
                val edge1 = w1 - w0
                val edge2 = w2 - w0
                val faceNormal = edge1.cross(edge2).normalized()

                // Depth is average Z
                val avgZ = (w0.z + w1.z + w2.z) / 3.0f

                val mat = if (tri.materialId in materials.indices) materials[tri.materialId] else materials.firstOrNull() ?: MaterialPbr()

                // Calculate PBR lighting
                val shadedColor = LightingEngine.calculatePbrShading(
                    surfaceNormal = faceNormal,
                    viewDirection = viewDirection,
                    lightDirection = lightDirection,
                    material = mat,
                    ambientIntensity = ambientIntensity
                )

                projectedTriangles.add(
                    ProjectedTriangle(
                        p0 = p0,
                        p1 = p1,
                        p2 = p2,
                        depth = avgZ,
                        color = shadedColor,
                        isWireframe = isWireframe || mat.isWireframe,
                        normalZ = faceNormal.z
                    )
                )
            }
        }

        // 4. Sort from back to front (Painter's Algorithm)
        projectedTriangles.sortBy { it.depth }

        // 5. Draw Triangles onto Canvas
        val path = Path()
        val stroke = Stroke(width = 1.5f)

        for (tri in projectedTriangles) {
            path.reset()
            path.moveTo(tri.p0.x, tri.p0.y)
            path.lineTo(tri.p1.x, tri.p1.y)
            path.lineTo(tri.p2.x, tri.p2.y)
            path.close()

            if (tri.isWireframe) {
                drawScope.drawPath(path, color = tri.color, style = stroke)
            } else {
                drawScope.drawPath(path, color = tri.color)
            }
        }
    }

    private fun drawSpatialGrid(
        drawScope: DrawScope,
        projMatrix: Matrix4x4,
        cameraZ: Float,
        width: Float,
        height: Float
    ) {
        val gridColor = Color(0x3338BDF8)
        val centerLineColor = Color(0x8800E5FF)
        val gridSize = 5
        val step = 0.6f
        val groundY = -1.2f

        val viewMatrix = Matrix4x4.translation(0f, groundY, cameraZ)
        val mvp = projMatrix * viewMatrix

        fun projectPoint(x: Float, z: Float): Offset? {
            val p = mvp.transformPoint(Vector3D(x, 0f, z))
            val zWorld = cameraZ + z
            if (zWorld >= -0.1f) return null
            val sx = (p.x * 0.5f + 0.5f) * width
            val sy = (-p.y * 0.5f + 0.5f) * height
            return Offset(sx, sy)
        }

        for (i in -gridSize..gridSize) {
            val pStart = projectPoint(i * step, -gridSize * step)
            val pEnd = projectPoint(i * step, gridSize * step)
            if (pStart != null && pEnd != null) {
                val col = if (i == 0) centerLineColor else gridColor
                drawScope.drawLine(col, pStart, pEnd, strokeWidth = if (i == 0) 2f else 1f)
            }

            val pLeft = projectPoint(-gridSize * step, i * step)
            val pRight = projectPoint(gridSize * step, i * step)
            if (pLeft != null && pRight != null) {
                val col = if (i == 0) centerLineColor else gridColor
                drawScope.drawLine(col, pLeft, pRight, strokeWidth = if (i == 0) 2f else 1f)
            }
        }
    }
}

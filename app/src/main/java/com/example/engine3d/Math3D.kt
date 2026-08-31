package com.example.engine3d

import com.example.model.MaterialPbr
import com.example.model.Vector3D
import androidx.compose.ui.graphics.Color
import kotlin.math.*

class Matrix4x4(val m: FloatArray = FloatArray(16)) {

    companion object {
        fun identity(): Matrix4x4 {
            val mat = Matrix4x4()
            mat.m[0] = 1f; mat.m[5] = 1f; mat.m[10] = 1f; mat.m[15] = 1f
            return mat
        }

        fun translation(x: Float, y: Float, z: Float): Matrix4x4 {
            val mat = identity()
            mat.m[12] = x
            mat.m[13] = y
            mat.m[14] = z
            return mat
        }

        fun scale(sx: Float, sy: Float, sz: Float): Matrix4x4 {
            val mat = identity()
            mat.m[0] = sx
            mat.m[5] = sy
            mat.m[10] = sz
            return mat
        }

        fun rotationX(rad: Float): Matrix4x4 {
            val mat = identity()
            val c = cos(rad)
            val s = sin(rad)
            mat.m[5] = c
            mat.m[6] = s
            mat.m[9] = -s
            mat.m[10] = c
            return mat
        }

        fun rotationY(rad: Float): Matrix4x4 {
            val mat = identity()
            val c = cos(rad)
            val s = sin(rad)
            mat.m[0] = c
            mat.m[2] = -s
            mat.m[8] = s
            mat.m[10] = c
            return mat
        }

        fun rotationZ(rad: Float): Matrix4x4 {
            val mat = identity()
            val c = cos(rad)
            val s = sin(rad)
            mat.m[0] = c
            mat.m[1] = s
            mat.m[4] = -s
            mat.m[5] = c
            return mat
        }

        fun perspective(fovYRadians: Float, aspect: Float, near: Float, far: Float): Matrix4x4 {
            val mat = Matrix4x4()
            val f = 1.0f / tan(fovYRadians / 2.0f)
            mat.m[0] = f / aspect
            mat.m[5] = f
            mat.m[10] = (far + near) / (near - far)
            mat.m[11] = -1.0f
            mat.m[14] = (2.0f * far * near) / (near - far)
            mat.m[15] = 0.0f
            return mat
        }

        fun orthographic(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4x4 {
            val mat = identity()
            mat.m[0] = 2f / (right - left)
            mat.m[5] = 2f / (top - bottom)
            mat.m[10] = -2f / (far - near)
            mat.m[12] = -(right + left) / (right - left)
            mat.m[13] = -(top + bottom) / (top - bottom)
            mat.m[14] = -(far + near) / (far - near)
            return mat
        }
    }

    operator fun times(other: Matrix4x4): Matrix4x4 {
        val result = Matrix4x4()
        for (row in 0..3) {
            for (col in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += this.m[k * 4 + row] * other.m[col * 4 + k]
                }
                result.m[col * 4 + row] = sum
            }
        }
        return result
    }

    fun transformPoint(v: Vector3D): Vector3D {
        val x = v.x * m[0] + v.y * m[4] + v.z * m[8] + m[12]
        val y = v.x * m[1] + v.y * m[5] + v.z * m[9] + m[13]
        val z = v.x * m[2] + v.y * m[6] + v.z * m[10] + m[14]
        val w = v.x * m[3] + v.y * m[7] + v.z * m[11] + m[15]
        return if (abs(w) > 0.00001f) {
            Vector3D(x / w, y / w, z / w)
        } else {
            Vector3D(x, y, z)
        }
    }

    fun transformDirection(v: Vector3D): Vector3D {
        val x = v.x * m[0] + v.y * m[4] + v.z * m[8]
        val y = v.x * m[1] + v.y * m[5] + v.z * m[9]
        val z = v.x * m[2] + v.y * m[6] + v.z * m[10]
        return Vector3D(x, y, z).normalized()
    }
}

object LightingEngine {
    fun calculatePbrShading(
        surfaceNormal: Vector3D,
        viewDirection: Vector3D,
        lightDirection: Vector3D,
        material: MaterialPbr,
        ambientIntensity: Float = 0.35f,
        lightColor: Color = Color(1f, 0.98f, 0.94f)
    ): Color {
        val norm = surfaceNormal.normalized()
        val lightDir = lightDirection.normalized()
        val viewDir = viewDirection.normalized()

        // N dot L (Lambertian diffuse)
        val nDotL = max(0f, norm.dot(lightDir))

        // Halfway vector for Blinn-Phong specular reflection
        val halfVec = (lightDir + viewDir).normalized()
        val nDotH = max(0f, norm.dot(halfVec))
        
        // Specular exponent based on roughness & metallic
        val shininess = max(4f, (1.0f - material.roughness) * 96f)
        val specularStrength = material.metallic * 0.8f + (1.0f - material.roughness) * 0.4f
        val spec = nDotH.pow(shininess) * specularStrength

        // Fresnel rim highlight
        val vDotN = max(0f, 1f - viewDir.dot(norm))
        val rim = vDotN.pow(3f) * 0.3f * (1.0f - material.roughness)

        val baseR = material.baseColor.red
        val baseG = material.baseColor.green
        val baseB = material.baseColor.blue

        val lightR = lightColor.red
        val lightG = lightColor.green
        val lightB = lightColor.blue

        // Combine ambient + diffuse + specular + rim + emissive
        val finalR = min(1f, (baseR * (ambientIntensity + nDotL * (1f - ambientIntensity) * lightR)) + spec * lightR + rim + (material.emissive.red * material.emissiveIntensity))
        val finalG = min(1f, (baseG * (ambientIntensity + nDotL * (1f - ambientIntensity) * lightG)) + spec * lightG + rim + (material.emissive.green * material.emissiveIntensity))
        val finalB = min(1f, (baseB * (ambientIntensity + nDotL * (1f - ambientIntensity) * lightB)) + spec * lightB + rim + (material.emissive.blue * material.emissiveIntensity))

        return Color(
            red = finalR,
            green = finalG,
            blue = finalB,
            alpha = material.alpha
        )
    }
}

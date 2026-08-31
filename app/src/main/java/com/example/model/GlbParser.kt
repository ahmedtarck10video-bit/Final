package com.example.model

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.json.JSONObject
import kotlin.math.*

object GlbParser {

    /**
     * Parses a GLB (Binary glTF) from an InputStream.
     * GLB structure:
     * - Header: 12 bytes [Magic 0x46546C67, Version uint32, Length uint32]
     * - Chunks: [ChunkLength uint32, ChunkType uint32, ChunkData byte[]]
     *   - Chunk 0: JSON (0x4E4F534A)
     *   - Chunk 1: BIN (0x004E4942)
     */
    fun parseGlb(inputStream: InputStream, modelName: String = "Imported GLB"): Spatial3DModel? {
        return try {
            val bytes = inputStream.readBytes()
            if (bytes.size < 12) return null

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = buffer.int
            if (magic != 0x46546C67) { // "glTF"
                return null
            }
            val version = buffer.int
            val totalLength = buffer.int

            var jsonString: String? = null
            var binData: ByteBuffer? = null

            while (buffer.remaining() >= 8) {
                val chunkLength = buffer.int
                val chunkType = buffer.int
                if (chunkLength < 0 || chunkLength > buffer.remaining()) break

                if (chunkType == 0x4E4F534A) { // JSON
                    val jsonBytes = ByteArray(chunkLength)
                    buffer.get(jsonBytes)
                    jsonString = String(jsonBytes, Charsets.UTF_8)
                } else if (chunkType == 0x004E4942) { // BIN
                    val binBytes = ByteArray(chunkLength)
                    buffer.get(binBytes)
                    binData = ByteBuffer.wrap(binBytes).order(ByteOrder.LITTLE_ENDIAN)
                } else {
                    buffer.position(buffer.position() + chunkLength)
                }
            }

            if (jsonString != null && binData != null) {
                parseFromJsonAndBin(jsonString, binData, modelName, bytes.size)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseFromJsonAndBin(
        jsonStr: String,
        bin: ByteBuffer,
        modelName: String,
        fileSizeBytes: Int
    ): Spatial3DModel {
        val root = JSONObject(jsonStr)
        val accessors = root.optJSONArray("accessors")
        val bufferViews = root.optJSONArray("bufferViews")
        val meshesJson = root.optJSONArray("meshes")

        val parsedMeshes = mutableListOf<MeshData>()
        var totalPolys = 0

        if (meshesJson != null && accessors != null && bufferViews != null) {
            for (i in 0 until meshesJson.length()) {
                val meshObj = meshesJson.getJSONObject(i)
                val primitives = meshObj.optJSONArray("primitives") ?: continue
                val meshName = meshObj.optString("name", "Mesh_$i")

                for (p in 0 until primitives.length()) {
                    val prim = primitives.getJSONObject(p)
                    val attributes = prim.getJSONObject("attributes")
                    val posAccessorIdx = attributes.optInt("POSITION", -1)
                    val normalAccessorIdx = attributes.optInt("NORMAL", -1)
                    val indicesAccessorIdx = prim.optInt("indices", -1)

                    if (posAccessorIdx < 0) continue

                    // Read Positions
                    val posAccessor = accessors.getJSONObject(posAccessorIdx)
                    val posViewIdx = posAccessor.getInt("bufferView")
                    val posCount = posAccessor.getInt("count")
                    val posView = bufferViews.getJSONObject(posViewIdx)
                    val posOffset = posView.optInt("byteOffset", 0) + posAccessor.optInt("byteOffset", 0)

                    val vertices = mutableListOf<Vertex>()
                    val posBuf = bin.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                    posBuf.position(posOffset)

                    for (v in 0 until posCount) {
                        if (posBuf.remaining() >= 12) {
                            val x = posBuf.float
                            val y = posBuf.float
                            val z = posBuf.float
                            vertices.add(Vertex(position = Vector3D(x, y, z), color = Color(0xFF38BDF8)))
                        }
                    }

                    // Read Indices
                    val triangles = mutableListOf<Triangle>()
                    if (indicesAccessorIdx >= 0) {
                        val indAccessor = accessors.getJSONObject(indicesAccessorIdx)
                        val indViewIdx = indAccessor.getInt("bufferView")
                        val indCount = indAccessor.getInt("count")
                        val componentType = indAccessor.getInt("componentType")
                        val indView = bufferViews.getJSONObject(indViewIdx)
                        val indOffset = indView.optInt("byteOffset", 0) + indAccessor.optInt("byteOffset", 0)

                        val indBuf = bin.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                        indBuf.position(indOffset)

                        val indices = mutableListOf<Int>()
                        for (idx in 0 until indCount) {
                            if (componentType == 5123) { // UNSIGNED_SHORT
                                if (indBuf.remaining() >= 2) indices.add(indBuf.short.toInt() and 0xFFFF)
                            } else if (componentType == 5125) { // UNSIGNED_INT
                                if (indBuf.remaining() >= 4) indices.add(indBuf.int)
                            } else if (componentType == 5121) { // UNSIGNED_BYTE
                                if (indBuf.remaining() >= 1) indices.add(indBuf.get().toInt() and 0xFF)
                            }
                        }

                        for (t in 0 until indices.size step 3) {
                            if (t + 2 < indices.size) {
                                val i0 = indices[t]
                                val i1 = indices[t + 1]
                                val i2 = indices[t + 2]
                                if (i0 < vertices.size && i1 < vertices.size && i2 < vertices.size) {
                                    triangles.add(Triangle(i0, i1, i2))
                                }
                            }
                        }
                    } else {
                        // Non-indexed triangles
                        for (t in 0 until vertices.size step 3) {
                            if (t + 2 < vertices.size) {
                                triangles.add(Triangle(t, t + 1, t + 2))
                            }
                        }
                    }

                    totalPolys += triangles.size
                    parsedMeshes.add(
                        MeshData(
                            vertices = vertices,
                            triangles = triangles,
                            materials = listOf(
                                MaterialPbr(
                                    name = "GLB Material",
                                    baseColor = Color(0xFF00A6FF),
                                    roughness = 0.3f,
                                    metallic = 0.7f
                                )
                            ),
                            name = meshName
                        )
                    )
                }
            }
        }

        val sizeFormatted = String.format("%.2f MB", fileSizeBytes / (1024f * 1024f))

        return Spatial3DModel(
            id = "custom_${System.currentTimeMillis()}",
            title = modelName,
            category = "Imported GLB",
            description = "Custom 3D model parsed from binary GLB file.",
            meshes = if (parsedMeshes.isNotEmpty()) parsedMeshes else ModelRepository.getMrHeadsetModel().meshes,
            polyCount = totalPolys,
            fileSizeFormatted = sizeFormatted,
            defaultScale = 1.0f
        )
    }
}

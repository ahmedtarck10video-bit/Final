package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.math.*

object ModelRepository {

    fun getAllModels(): List<Spatial3DModel> {
        return listOf(
            getMrHeadsetModel(),
            getSpatialDroneModel(),
            getCyberRobotModel(),
            getHologramGlobeModel(),
            getQuantumCrystalModel(),
            getHoverSpeederModel()
        )
    }

    /**
     * 1. MR Headset Pro (The iconic Mixed Reality headset from the app icon)
     */
    fun getMrHeadsetModel(): Spatial3DModel {
        val vertices = mutableListOf<Vertex>()
        val triangles = mutableListOf<Triangle>()

        // Main Visor Curved Box
        val visorColor = Color(0xFF0288D1)
        val strapColor = Color(0xFF1E293B)
        val glassColor = Color(0xFF00E5FF)
        val dialColor = Color(0xFF64748B)

        // Generate Visor front curved shield
        val segments = 12
        val width = 1.4f
        val height = 0.7f
        val depth = 0.5f

        // Front curved visor vertices
        for (i in 0..segments) {
            val u = i.toFloat() / segments
            val angle = -PI.toFloat() * 0.35f + u * (PI.toFloat() * 0.7f)
            val x = sin(angle) * (width * 0.9f)
            val z = cos(angle) * 0.4f - 0.2f

            // Top vertex
            vertices.add(Vertex(Vector3D(x, height * 0.5f, z), normal = Vector3D(x, 0.2f, z + 0.3f).normalized(), color = glassColor))
            // Bottom vertex
            vertices.add(Vertex(Vector3D(x, -height * 0.5f, z), normal = Vector3D(x, -0.2f, z + 0.3f).normalized(), color = glassColor))
            // Notch bottom vertex
            val notchY = if (abs(x) < 0.2f) -height * 0.25f else -height * 0.5f
            vertices.add(Vertex(Vector3D(x, notchY, z), normal = Vector3D(x, -0.1f, z + 0.3f).normalized(), color = visorColor))
        }

        // Connect Visor Triangles
        for (i in 0 until segments) {
            val base = i * 3
            val next = (i + 1) * 3

            // Front Glass strip
            triangles.add(Triangle(base, next, base + 1, color = glassColor, materialId = 1))
            triangles.add(Triangle(next, next + 1, base + 1, color = glassColor, materialId = 1))

            // Lower casing
            triangles.add(Triangle(base + 1, next + 1, base + 2, color = visorColor, materialId = 0))
            triangles.add(Triangle(next + 1, next + 2, base + 2, color = visorColor, materialId = 0))
        }

        // Top sensor pill
        val sensorIdx = vertices.size
        vertices.add(Vertex(Vector3D(-0.18f, height * 0.35f, 0.22f), color = Color(0xFF0F172A)))
        vertices.add(Vertex(Vector3D(0.18f, height * 0.35f, 0.22f), color = Color(0xFF0F172A)))
        vertices.add(Vertex(Vector3D(0.18f, height * 0.25f, 0.22f), color = Color(0xFF0F172A)))
        vertices.add(Vertex(Vector3D(-0.18f, height * 0.25f, 0.22f), color = Color(0xFF0F172A)))
        triangles.add(Triangle(sensorIdx, sensorIdx + 1, sensorIdx + 2, color = Color(0xFF00E5FF), materialId = 2))
        triangles.add(Triangle(sensorIdx, sensorIdx + 2, sensorIdx + 3, color = Color(0xFF00E5FF), materialId = 2))

        // Headband Straps (Back ring)
        val strapIdx = vertices.size
        val strapSegments = 16
        val strapRadius = 0.95f
        for (i in 0..strapSegments) {
            val angle = PI.toFloat() * 0.5f + (i.toFloat() / strapSegments) * PI.toFloat()
            val x = cos(angle) * strapRadius
            val z = sin(angle) * strapRadius - 0.2f
            vertices.add(Vertex(Vector3D(x, 0.15f, z), normal = Vector3D(x, 0f, z).normalized(), color = strapColor))
            vertices.add(Vertex(Vector3D(x, -0.15f, z), normal = Vector3D(x, 0f, z).normalized(), color = strapColor))
        }
        for (i in 0 until strapSegments) {
            val b = strapIdx + i * 2
            val n = strapIdx + (i + 1) * 2
            triangles.add(Triangle(b, n, b + 1, color = strapColor, materialId = 0))
            triangles.add(Triangle(n, n + 1, b + 1, color = strapColor, materialId = 0))
        }

        // Side Adjustment Dials
        fun addDial(cx: Float, cy: Float, cz: Float) {
            val dIdx = vertices.size
            vertices.add(Vertex(Vector3D(cx, cy, cz), color = dialColor))
            for (i in 0..6) {
                val a = (i.toFloat() / 6f) * 2f * PI.toFloat()
                vertices.add(Vertex(Vector3D(cx, cy + sin(a) * 0.12f, cz + cos(a) * 0.12f), color = dialColor))
            }
            for (i in 1..6) {
                triangles.add(Triangle(dIdx, dIdx + i, dIdx + i + 1, color = dialColor, materialId = 0))
            }
        }
        addDial(-0.95f, 0f, -0.2f)
        addDial(0.95f, 0f, -0.2f)

        val materials = listOf(
            MaterialPbr(id = 0, name = "Casing Matte", baseColor = visorColor, roughness = 0.4f, metallic = 0.5f),
            MaterialPbr(id = 1, name = "Cyber Visor Glass", baseColor = glassColor, roughness = 0.1f, metallic = 0.9f, alpha = 0.92f),
            MaterialPbr(id = 2, name = "Emissive Sensor", baseColor = Color(0xFF00E5FF), emissive = Color(0xFF00E5FF), emissiveIntensity = 1.2f)
        )

        val anim = AnimationTrack(
            name = "Spatial Float & Hover",
            durationSeconds = 4.0f,
            keyframes = listOf(
                Keyframe(0.0f, rotationY = 0f, translateY = 0f),
                Keyframe(1.0f, rotationY = 20f, translateY = 0.12f),
                Keyframe(2.0f, rotationY = 0f, translateY = 0f),
                Keyframe(3.0f, rotationY = -20f, translateY = -0.12f),
                Keyframe(4.0f, rotationY = 0f, translateY = 0f)
            )
        )

        return Spatial3DModel(
            id = "mr_headset_pro",
            title = "MR Headset Vision",
            category = "Spatial Wearable",
            description = "Dual-lens spatial computing headset with real-time pass-through LiDAR sensors and curved cyber visor.",
            meshes = listOf(MeshData(vertices = vertices, triangles = triangles, materials = materials, name = "MR_Headset")),
            defaultScale = 1.2f,
            animations = listOf(anim),
            iconName = "headset",
            polyCount = triangles.size,
            fileSizeFormatted = "1.8 MB"
        )
    }

    /**
     * 2. Spatial Drone Recon
     */
    fun getSpatialDroneModel(): Spatial3DModel {
        val vertices = mutableListOf<Vertex>()
        val triangles = mutableListOf<Triangle>()

        val bodyColor = Color(0xFF1E293B)
        val neonColor = Color(0xFF38BDF8)
        val bladeColor = Color(0xFF94A3B8)

        // Central Core Hexagon
        val coreIdx = vertices.size
        vertices.add(Vertex(Vector3D(0f, 0.15f, 0f), color = neonColor)) // 0 top center
        vertices.add(Vertex(Vector3D(0f, -0.15f, 0f), color = bodyColor)) // 1 bottom center

        for (i in 0..6) {
            val a = (i.toFloat() / 6f) * 2f * PI.toFloat()
            val x = cos(a) * 0.45f
            val z = sin(a) * 0.45f
            vertices.add(Vertex(Vector3D(x, 0.08f, z), normal = Vector3D(x, 0.5f, z).normalized(), color = bodyColor))
            vertices.add(Vertex(Vector3D(x, -0.08f, z), normal = Vector3D(x, -0.5f, z).normalized(), color = bodyColor))
        }

        for (i in 0 until 6) {
            val tTop = coreIdx + 2 + i * 2
            val tBot = coreIdx + 3 + i * 2
            val nTop = coreIdx + 2 + (i + 1) * 2
            val nBot = coreIdx + 3 + (i + 1) * 2

            // Top hex fan
            triangles.add(Triangle(coreIdx, tTop, nTop, color = neonColor, materialId = 1))
            // Bottom hex fan
            triangles.add(Triangle(coreIdx + 1, nBot, tBot, color = bodyColor, materialId = 0))
            // Side walls
            triangles.add(Triangle(tTop, tBot, nTop, color = bodyColor, materialId = 0))
            triangles.add(Triangle(nTop, tBot, nBot, color = bodyColor, materialId = 0))
        }

        // 4 Rotor Arms & Blades
        val armAngles = listOf(PI.toFloat() * 0.25f, PI.toFloat() * 0.75f, PI.toFloat() * 1.25f, PI.toFloat() * 1.75f)
        for (angle in armAngles) {
            val ax = cos(angle) * 0.95f
            val az = sin(angle) * 0.95f

            val armIdx = vertices.size
            vertices.add(Vertex(Vector3D(cos(angle) * 0.35f, 0.02f, sin(angle) * 0.35f), color = bodyColor))
            vertices.add(Vertex(Vector3D(ax, 0.05f, az), color = bodyColor))
            vertices.add(Vertex(Vector3D(ax, -0.05f, az), color = bodyColor))
            vertices.add(Vertex(Vector3D(cos(angle) * 0.35f, -0.02f, sin(angle) * 0.35f), color = bodyColor))

            triangles.add(Triangle(armIdx, armIdx + 1, armIdx + 2, color = bodyColor, materialId = 0))
            triangles.add(Triangle(armIdx, armIdx + 2, armIdx + 3, color = bodyColor, materialId = 0))

            // Rotor Motor Hub & Blade
            val hubIdx = vertices.size
            vertices.add(Vertex(Vector3D(ax, 0.12f, az), color = neonColor))
            vertices.add(Vertex(Vector3D(ax + 0.35f, 0.12f, az), color = bladeColor))
            vertices.add(Vertex(Vector3D(ax - 0.35f, 0.12f, az), color = bladeColor))
            vertices.add(Vertex(Vector3D(ax, 0.12f, az + 0.35f), color = bladeColor))
            vertices.add(Vertex(Vector3D(ax, 0.12f, az - 0.35f), color = bladeColor))

            triangles.add(Triangle(hubIdx, hubIdx + 1, hubIdx + 3, color = bladeColor, materialId = 2))
            triangles.add(Triangle(hubIdx, hubIdx + 2, hubIdx + 4, color = bladeColor, materialId = 2))
        }

        val materials = listOf(
            MaterialPbr(id = 0, name = "Carbon Composite", baseColor = bodyColor, roughness = 0.5f, metallic = 0.8f),
            MaterialPbr(id = 1, name = "Reactor Glow", baseColor = neonColor, emissive = neonColor, emissiveIntensity = 1.5f),
            MaterialPbr(id = 2, name = "Polymer Propeller", baseColor = bladeColor, roughness = 0.2f, metallic = 0.3f)
        )

        return Spatial3DModel(
            id = "spatial_drone",
            title = "Autonomous Drone",
            category = "Robotics & AR",
            description = "Spatial scanning quadcopter equipped with volumetric LiDAR and environmental mapping sensors.",
            meshes = listOf(MeshData(vertices = vertices, triangles = triangles, materials = materials, name = "Drone_Mesh")),
            defaultScale = 1.1f,
            animations = listOf(
                AnimationTrack(
                    name = "Patrol Flight",
                    durationSeconds = 3.0f,
                    keyframes = listOf(
                        Keyframe(0f, rotationY = 0f, translateY = 0f),
                        Keyframe(1.5f, rotationY = 180f, translateY = 0.15f),
                        Keyframe(3f, rotationY = 360f, translateY = 0f)
                    )
                )
            ),
            iconName = "drone",
            polyCount = triangles.size,
            fileSizeFormatted = "2.1 MB"
        )
    }

    /**
     * 3. Cybernetic Robot Sentinel
     */
    fun getCyberRobotModel(): Spatial3DModel {
        val vertices = mutableListOf<Vertex>()
        val triangles = mutableListOf<Triangle>()

        val armorColor = Color(0xFF0F172A)
        val accentColor = Color(0xFF0284C7)
        val eyeGlow = Color(0xFF38BDF8)

        // Head Box
        fun addBox(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float, col: Color, matId: Int) {
            val base = vertices.size
            // 8 corners
            vertices.add(Vertex(Vector3D(minX, minY, minZ), color = col)) // 0
            vertices.add(Vertex(Vector3D(maxX, minY, minZ), color = col)) // 1
            vertices.add(Vertex(Vector3D(maxX, maxY, minZ), color = col)) // 2
            vertices.add(Vertex(Vector3D(minX, maxY, minZ), color = col)) // 3
            vertices.add(Vertex(Vector3D(minX, minY, maxZ), color = col)) // 4
            vertices.add(Vertex(Vector3D(maxX, minY, maxZ), color = col)) // 5
            vertices.add(Vertex(Vector3D(maxX, maxY, maxZ), color = col)) // 6
            vertices.add(Vertex(Vector3D(minX, maxY, maxZ), color = col)) // 7

            // 6 faces * 2 triangles
            // Front (Z+)
            triangles.add(Triangle(base + 4, base + 5, base + 6, color = col, materialId = matId))
            triangles.add(Triangle(base + 4, base + 6, base + 7, color = col, materialId = matId))
            // Back (Z-)
            triangles.add(Triangle(base + 1, base + 0, base + 3, color = col, materialId = matId))
            triangles.add(Triangle(base + 1, base + 3, base + 2, color = col, materialId = matId))
            // Top (Y+)
            triangles.add(Triangle(base + 3, base + 2, base + 6, color = col, materialId = matId))
            triangles.add(Triangle(base + 3, base + 6, base + 7, color = col, materialId = matId))
            // Bottom (Y-)
            triangles.add(Triangle(base + 0, base + 5, base + 1, color = col, materialId = matId))
            triangles.add(Triangle(base + 0, base + 4, base + 5, color = col, materialId = matId))
            // Right (X+)
            triangles.add(Triangle(base + 1, base + 5, base + 6, color = col, materialId = matId))
            triangles.add(Triangle(base + 1, base + 6, base + 2, color = col, materialId = matId))
            // Left (X-)
            triangles.add(Triangle(base + 4, base + 0, base + 3, color = col, materialId = matId))
            triangles.add(Triangle(base + 4, base + 3, base + 7, color = col, materialId = matId))
        }

        // Torso
        addBox(-0.45f, -0.4f, -0.25f, 0.45f, 0.35f, 0.25f, armorColor, 0)
        // Core Arc Reactor
        addBox(-0.15f, -0.1f, 0.26f, 0.15f, 0.15f, 0.28f, eyeGlow, 1)
        // Head
        addBox(-0.25f, 0.45f, -0.2f, 0.25f, 0.85f, 0.2f, armorColor, 0)
        // Visor Eye Strip
        addBox(-0.2f, 0.6f, 0.21f, 0.2f, 0.72f, 0.23f, eyeGlow, 1)
        // Shoulders
        addBox(-0.75f, 0.05f, -0.2f, -0.48f, 0.35f, 0.2f, accentColor, 0)
        addBox(0.48f, 0.05f, -0.2f, 0.75f, 0.35f, 0.2f, accentColor, 0)
        // Arms
        addBox(-0.72f, -0.55f, -0.15f, -0.52f, 0.05f, 0.15f, armorColor, 0)
        addBox(0.52f, -0.55f, -0.15f, 0.72f, 0.05f, 0.15f, armorColor, 0)
        // Legs
        addBox(-0.38f, -1.05f, -0.18f, -0.12f, -0.4f, 0.18f, accentColor, 0)
        addBox(0.12f, -1.05f, -0.18f, 0.38f, -0.4f, 0.18f, accentColor, 0)

        val materials = listOf(
            MaterialPbr(id = 0, name = "Titanium Armor", baseColor = armorColor, roughness = 0.3f, metallic = 0.85f),
            MaterialPbr(id = 1, name = "Plasma Blue Eye", baseColor = eyeGlow, emissive = eyeGlow, emissiveIntensity = 1.8f)
        )

        return Spatial3DModel(
            id = "cyber_robot",
            title = "Cyber Sentinel",
            category = "Mecha & Robotics",
            description = "Articulated bipedal reconnaissance unit with hardened titanium alloy plates and optical neural visor.",
            meshes = listOf(MeshData(vertices = vertices, triangles = triangles, materials = materials, name = "Robot_Mesh")),
            defaultScale = 0.95f,
            animations = listOf(
                AnimationTrack(
                    name = "Combat Ready",
                    durationSeconds = 2.0f,
                    keyframes = listOf(
                        Keyframe(0f, rotationY = 0f, translateY = 0f),
                        Keyframe(1f, rotationY = 15f, translateY = 0.05f),
                        Keyframe(2f, rotationY = 0f, translateY = 0f)
                    )
                )
            ),
            iconName = "smart_toy",
            polyCount = triangles.size,
            fileSizeFormatted = "2.4 MB"
        )
    }

    /**
     * 4. Hologram Spatial Globe
     */
    fun getHologramGlobeModel(): Spatial3DModel {
        val vertices = mutableListOf<Vertex>()
        val triangles = mutableListOf<Triangle>()

        val holoColor = Color(0xFF00E5FF)
        val orbitColor = Color(0xFF38BDF8)
        val latRings = 10
        val lonRings = 14
        val radius = 0.85f

        // Sphere mesh with UV grid
        for (lat in 0..latRings) {
            val theta = (lat.toFloat() / latRings) * PI.toFloat()
            val sinT = sin(theta)
            val cosT = cos(theta)

            for (lon in 0..lonRings) {
                val phi = (lon.toFloat() / lonRings) * 2f * PI.toFloat()
                val x = cos(phi) * sinT * radius
                val y = cosT * radius
                val z = sin(phi) * sinT * radius

                vertices.add(Vertex(Vector3D(x, y, z), normal = Vector3D(x, y, z).normalized(), color = holoColor))
            }
        }

        for (lat in 0 until latRings) {
            for (lon in 0 until lonRings) {
                val first = lat * (lonRings + 1) + lon
                val second = first + lonRings + 1

                triangles.add(Triangle(first, second, first + 1, color = holoColor, materialId = 0))
                triangles.add(Triangle(second, second + 1, first + 1, color = holoColor, materialId = 0))
            }
        }

        // Equatorial and polar holographic orbital rings
        val ringBase = vertices.size
        val ringSegs = 20
        val ringRadius = 1.15f
        for (i in 0..ringSegs) {
            val a = (i.toFloat() / ringSegs) * 2f * PI.toFloat()
            val x = cos(a) * ringRadius
            val z = sin(a) * ringRadius
            vertices.add(Vertex(Vector3D(x, 0.03f, z), color = orbitColor))
            vertices.add(Vertex(Vector3D(x, -0.03f, z), color = orbitColor))
        }
        for (i in 0 until ringSegs) {
            val b = ringBase + i * 2
            val n = ringBase + (i + 1) * 2
            triangles.add(Triangle(b, n, b + 1, color = orbitColor, materialId = 1))
            triangles.add(Triangle(n, n + 1, b + 1, color = orbitColor, materialId = 1))
        }

        val materials = listOf(
            MaterialPbr(id = 0, name = "Hologram Lattice", baseColor = holoColor, roughness = 0.1f, metallic = 0.9f, alpha = 0.75f, emissive = holoColor, emissiveIntensity = 0.8f),
            MaterialPbr(id = 1, name = "Orbit Ring", baseColor = orbitColor, emissive = orbitColor, emissiveIntensity = 1.4f)
        )

        return Spatial3DModel(
            id = "holo_globe",
            title = "Holographic Globe",
            category = "Data & Visualization",
            description = "Spatial telemetry globe showing planetary latitude matrix and floating orbital data nodes.",
            meshes = listOf(MeshData(vertices = vertices, triangles = triangles, materials = materials, name = "Globe_Mesh")),
            defaultScale = 1.0f,
            animations = listOf(
                AnimationTrack(
                    name = "Planetary Rotation",
                    durationSeconds = 6.0f,
                    keyframes = listOf(
                        Keyframe(0f, rotationY = 0f),
                        Keyframe(3f, rotationY = 180f),
                        Keyframe(6f, rotationY = 360f)
                    )
                )
            ),
            iconName = "public",
            polyCount = triangles.size,
            fileSizeFormatted = "1.5 MB"
        )
    }

    /**
     * 5. Quantum Crystal Core
     */
    fun getQuantumCrystalModel(): Spatial3DModel {
        val vertices = mutableListOf<Vertex>()
        val triangles = mutableListOf<Triangle>()

        val gemTopColor = Color(0xFF00E5FF)
        val gemMidColor = Color(0xFF0288D1)
        val shardColor = Color(0xFF38BDF8)

        // Octahedron Double-Pyramid Crystal
        val topVertex = Vector3D(0f, 1.1f, 0f)
        val botVertex = Vector3D(0f, -1.1f, 0f)

        vertices.add(Vertex(topVertex, normal = Vector3D(0f, 1f, 0f), color = gemTopColor)) // 0
        vertices.add(Vertex(botVertex, normal = Vector3D(0f, -1f, 0f), color = gemTopColor)) // 1

        val sides = 8
        val radius = 0.7f
        for (i in 0..sides) {
            val a = (i.toFloat() / sides) * 2f * PI.toFloat()
            val x = cos(a) * radius
            val z = sin(a) * radius
            vertices.add(Vertex(Vector3D(x, 0.1f, z), normal = Vector3D(x, 0.2f, z).normalized(), color = gemMidColor))
            vertices.add(Vertex(Vector3D(x, -0.1f, z), normal = Vector3D(x, -0.2f, z).normalized(), color = gemMidColor))
        }

        for (i in 0 until sides) {
            val t0 = 2 + i * 2
            val b0 = 3 + i * 2
            val t1 = 2 + (i + 1) * 2
            val b1 = 3 + (i + 1) * 2

            // Top cap
            triangles.add(Triangle(0, t0, t1, color = gemTopColor, materialId = 0))
            // Mid band
            triangles.add(Triangle(t0, b0, t1, color = gemMidColor, materialId = 0))
            triangles.add(Triangle(t1, b0, b1, color = gemMidColor, materialId = 0))
            // Bot cap
            triangles.add(Triangle(1, b1, b0, color = gemTopColor, materialId = 0))
        }

        val materials = listOf(
            MaterialPbr(id = 0, name = "Refractive Crystal", baseColor = gemTopColor, roughness = 0.05f, metallic = 0.95f, alpha = 0.88f, emissive = Color(0xFF00E5FF), emissiveIntensity = 1.0f)
        )

        return Spatial3DModel(
            id = "quantum_crystal",
            title = "Quantum Crystal",
            category = "Sci-Fi Relic",
            description = "Resonating spatial power prism with refractive prism facets and hyper-luminescent internal glow.",
            meshes = listOf(MeshData(vertices = vertices, triangles = triangles, materials = materials, name = "Crystal_Mesh")),
            defaultScale = 1.0f,
            animations = listOf(
                AnimationTrack(
                    name = "Harmonic Resonance",
                    durationSeconds = 3.0f,
                    keyframes = listOf(
                        Keyframe(0f, rotationY = 0f, translateY = 0f, scale = 1f),
                        Keyframe(1.5f, rotationY = 180f, translateY = 0.1f, scale = 1.08f),
                        Keyframe(3f, rotationY = 360f, translateY = 0f, scale = 1f)
                    )
                )
            ),
            iconName = "diamond",
            polyCount = triangles.size,
            fileSizeFormatted = "1.1 MB"
        )
    }

    /**
     * 6. Sci-Fi Hover Speeder
     */
    fun getHoverSpeederModel(): Spatial3DModel {
        val vertices = mutableListOf<Vertex>()
        val triangles = mutableListOf<Triangle>()

        val hullColor = Color(0xFF0F172A)
        val glassColor = Color(0xFF38BDF8)
        val engineColor = Color(0xFF0284C7)

        val base = vertices.size
        // Nose & Cockpit
        vertices.add(Vertex(Vector3D(0f, 0.1f, 1.2f), color = hullColor)) // 0 Nose
        vertices.add(Vertex(Vector3D(-0.4f, 0.2f, 0.2f), color = glassColor)) // 1 Cockpit L
        vertices.add(Vertex(Vector3D(0.4f, 0.2f, 0.2f), color = glassColor)) // 2 Cockpit R
        vertices.add(Vertex(Vector3D(0f, 0.35f, 0.1f), color = glassColor)) // 3 Canopy Peak
        vertices.add(Vertex(Vector3D(-0.9f, 0.05f, -0.6f), color = hullColor)) // 4 Wing L
        vertices.add(Vertex(Vector3D(0.9f, 0.05f, -0.6f), color = hullColor)) // 5 Wing R
        vertices.add(Vertex(Vector3D(-0.35f, 0.25f, -1.0f), color = engineColor)) // 6 Engine L
        vertices.add(Vertex(Vector3D(0.35f, 0.25f, -1.0f), color = engineColor)) // 7 Engine R
        vertices.add(Vertex(Vector3D(0f, -0.15f, 0f), color = hullColor)) // 8 Keel Bottom

        // Top Hull Faces
        triangles.add(Triangle(base + 0, base + 1, base + 3, color = glassColor, materialId = 1))
        triangles.add(Triangle(base + 0, base + 3, base + 2, color = glassColor, materialId = 1))
        triangles.add(Triangle(base + 1, base + 4, base + 6, color = hullColor, materialId = 0))
        triangles.add(Triangle(base + 2, base + 7, base + 5, color = hullColor, materialId = 0))
        triangles.add(Triangle(base + 3, base + 6, base + 7, color = engineColor, materialId = 2))

        // Bottom Keel Faces
        triangles.add(Triangle(base + 0, base + 8, base + 1, color = hullColor, materialId = 0))
        triangles.add(Triangle(base + 0, base + 2, base + 8, color = hullColor, materialId = 0))
        triangles.add(Triangle(base + 1, base + 8, base + 4, color = hullColor, materialId = 0))
        triangles.add(Triangle(base + 2, base + 5, base + 8, color = hullColor, materialId = 0))
        triangles.add(Triangle(base + 4, base + 8, base + 6, color = hullColor, materialId = 0))
        triangles.add(Triangle(base + 5, base + 7, base + 8, color = hullColor, materialId = 0))

        val materials = listOf(
            MaterialPbr(id = 0, name = "Aerodynamic Carbon", baseColor = hullColor, roughness = 0.4f, metallic = 0.8f),
            MaterialPbr(id = 1, name = "Ion Shield Canopy", baseColor = glassColor, alpha = 0.85f, metallic = 0.9f),
            MaterialPbr(id = 2, name = "Plasma Exhaust", baseColor = engineColor, emissive = Color(0xFF00E5FF), emissiveIntensity = 2.0f)
        )

        return Spatial3DModel(
            id = "hover_speeder",
            title = "Aero Speeder VX",
            category = "Spatial Vehicles",
            description = "Atmospheric twin-thruster repulsor vehicle with dynamic vectoring aerofoils.",
            meshes = listOf(MeshData(vertices = vertices, triangles = triangles, materials = materials, name = "Speeder_Mesh")),
            defaultScale = 1.0f,
            animations = listOf(
                AnimationTrack(
                    name = "Supersonic Cruise",
                    durationSeconds = 2.5f,
                    keyframes = listOf(
                        Keyframe(0f, rotationY = 0f, translateY = 0f, rotationX = 0f),
                        Keyframe(1.25f, rotationY = 10f, translateY = 0.08f, rotationX = -5f),
                        Keyframe(2.5f, rotationY = 0f, translateY = 0f, rotationX = 0f)
                    )
                )
            ),
            iconName = "flight",
            polyCount = triangles.size,
            fileSizeFormatted = "1.9 MB"
        )
    }
}

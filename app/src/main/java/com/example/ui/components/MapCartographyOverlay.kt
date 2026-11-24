package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.toArgb
import com.example.R
import com.example.model.PinWithDistance
import com.example.model.WorldCountryCatalog
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

enum class MapStyleMode(@StringRes val labelRes: Int) {
    OSM_BORDERS(R.string.map_style_borders),
    ESRI_ATLAS(R.string.map_style_atlas),
    OSM_NIGHT(R.string.map_style_night);

    fun next(): MapStyleMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

object MapTileSources {
    /**
     * Keyless public ArcGIS World Street Map endpoint (uses z/y/x tile order).
     */
    val ESRI_WORLD_STREET: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "EsriWorldStreet",
        1,
        18,
        256,
        "",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "$baseUrl$z/$y/$x"
        }
    }

    fun tileSourceFor(mode: MapStyleMode): ITileSource = when (mode) {
        MapStyleMode.OSM_BORDERS -> TileSourceFactory.MAPNIK
        MapStyleMode.ESRI_ATLAS -> ESRI_WORLD_STREET
        MapStyleMode.OSM_NIGHT -> TileSourceFactory.MAPNIK
    }

    fun borderClarityColorFilter(mode: MapStyleMode): ColorMatrixColorFilter? {
        return when (mode) {
            MapStyleMode.OSM_BORDERS -> {
                // Boost contrast & vibrance subtly so country border lines and coastlines stay crisp at zoom 1.5-5.0
                val contrast = 1.10f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                val saturationMatrix = ColorMatrix().apply { setSaturation(1.14f) }
                contrastMatrix.postConcat(saturationMatrix)
                ColorMatrixColorFilter(contrastMatrix)
            }

            MapStyleMode.ESRI_ATLAS -> null

            MapStyleMode.OSM_NIGHT -> {
                // CSS-equivalent invert(94%) + hue-rotate(180deg) so oceans stay deep navy-teal
                // while landmasses become sleek dark slate and country borders glow cleanly.
                val invertMatrix = ColorMatrix(
                    floatArrayOf(
                        -0.94f, 0f, 0f, 0f, 248f,
                        0f, -0.94f, 0f, 0f, 248f,
                        0f, 0f, -0.94f, 0f, 248f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                val hue180Matrix = ColorMatrix(
                    floatArrayOf(
                        -0.574f, 1.430f, 0.144f, 0f, 0f,
                        0.426f, 0.430f, 0.144f, 0f, 0f,
                        0.426f, 1.430f, -0.856f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                invertMatrix.postConcat(hue180Matrix)
                ColorMatrixColorFilter(invertMatrix)
            }
        }
    }
}

/**
 * Draws clean geographic graticule (latitude/longitude atlas lines & Equator) when zoomed out
 * so world & continent scales preserve crisp cartographic framing.
 */
class AtlasGraticuleOverlay(
    private val density: Float,
    var isNightMode: Boolean
) : Overlay() {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.9f * density
    }

    private val equatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.3f * density
    }

    private val ptA = Point()
    private val ptB = Point()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val zoom = mapView.zoomLevelDouble
        if (zoom >= 5.6) return

        val projection = mapView.projection
        val width = mapView.width
        val height = mapView.height
        if (width <= 0 || height <= 0) return

        gridPaint.color = if (isNightMode) {
            android.graphics.Color.argb(38, 56, 189, 248)
        } else {
            android.graphics.Color.argb(34, 13, 92, 117)
        }
        equatorPaint.color = if (isNightMode) {
            android.graphics.Color.argb(72, 20, 184, 166)
        } else {
            android.graphics.Color.argb(62, 13, 92, 117)
        }

        // Parallels every 30 degrees (-60..60)
        val latitudes = doubleArrayOf(-60.0, -30.0, 0.0, 30.0, 60.0)
        for (lat in latitudes) {
            projection.toPixels(GeoPoint(lat, 0.0), ptA)
            val y = ptA.y.toFloat()
            if (y in -10f..(height + 10f)) {
                canvas.drawLine(
                    0f,
                    y,
                    width.toFloat(),
                    y,
                    if (lat == 0.0) equatorPaint else gridPaint
                )
            }
        }

        // Meridians every 30 degrees across wrapped longitudes
        for (lon in -180..180 step 30) {
            for (shift in intArrayOf(-360, 0, 360)) {
                val targetLon = lon.toDouble() + shift
                projection.toPixels(GeoPoint(80.0, targetLon), ptA)
                projection.toPixels(GeoPoint(-80.0, targetLon), ptB)
                val x = ptA.x.toFloat()
                if (x in -10f..(width + 10f)) {
                    canvas.drawLine(x, ptA.y.toFloat(), x, ptB.y.toFloat(), gridPaint)
                }
            }
        }
    }
}

/**
 * Renders crisp Arabic country names (when zoomed out) and collision-culled city/place name labels
 * beneath map pins at all zoom levels.
 */
class CartographicLabelsOverlay(
    private val density: Float,
    var pins: List<PinWithDistance>,
    var selectedPinId: String?,
    var isNightMode: Boolean
) : Overlay() {

    private val countryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val countryHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val pinTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val pinPillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pinPillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }

    private val tempPoint = Point()
    private val occupiedRects = ArrayList<RectF>(96)

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val width = mapView.width
        val height = mapView.height
        if (width <= 0 || height <= 0) return

        val zoom = mapView.zoomLevelDouble
        val projection = mapView.projection
        occupiedRects.clear()

        // 1. Draw Arabic country names at world & continental zoom levels (1.5 .. 6.4)
        if (zoom < 6.4) {
            val maxPriority = when {
                zoom < 2.65 -> 1
                zoom < 3.85 -> 2
                else -> 3
            }

            val baseTextSp = when {
                zoom < 2.6 -> 11.5f
                zoom < 3.8 -> 12.8f
                else -> 14.0f
            }
            countryTextPaint.textSize = baseTextSp * density
            countryHaloPaint.textSize = countryTextPaint.textSize
            countryHaloPaint.strokeWidth = 3.6f * density

            countryTextPaint.color = if (isNightMode) {
                android.graphics.Color.rgb(226, 232, 240)
            } else {
                android.graphics.Color.rgb(15, 39, 68)
            }
            countryHaloPaint.color = if (isNightMode) {
                android.graphics.Color.argb(230, 10, 22, 40)
            } else {
                android.graphics.Color.argb(235, 255, 255, 255)
            }

            val fm = countryTextPaint.fontMetrics
            val textHeight = fm.descent - fm.ascent

            for (country in WorldCountryCatalog.countries) {
                if (country.priority > maxPriority) continue
                val screenPos = resolveVisibleScreenPoint(
                    projection, country.latitude, country.longitude, width, height
                ) ?: continue

                val textW = countryTextPaint.measureText(country.nameAr)
                val padX = 6f * density
                val padY = 3f * density
                val cx = screenPos.x.toFloat()
                val baselineY = screenPos.y.toFloat() + textHeight * 0.25f

                val candidate = RectF(
                    cx - textW / 2f - padX,
                    baselineY + fm.ascent - padY,
                    cx + textW / 2f + padX,
                    baselineY + fm.descent + padY
                )
                if (collides(candidate)) continue

                occupiedRects.add(candidate)
                canvas.drawText(country.nameAr, cx, baselineY, countryHaloPaint)
                canvas.drawText(country.nameAr, cx, baselineY, countryTextPaint)
            }
        }

        // 2. Draw crisp city / place names ("واساميهم") beneath pins (selected pin first)
        val isGlobeScale = zoom < 5.6
        val orderedPins = pins.sortedByDescending { it.pin.id == selectedPinId }
        val maxLabels = if (isGlobeScale) 18 else 26
        var drawnCount = 0

        pinTextPaint.textSize = (if (isGlobeScale) 10.5f else 11.5f) * density
        val pinFm = pinTextPaint.fontMetrics
        val pinTextHeight = pinFm.descent - pinFm.ascent

        for (item in orderedPins) {
            if (drawnCount >= maxLabels && item.pin.id != selectedPinId) break
            val pin = item.pin
            val isSelected = pin.id == selectedPinId

            val screenPos = resolveVisibleScreenPoint(
                projection, pin.latitude, pin.longitude, width, height
            ) ?: continue

            val rawLabel = if (isGlobeScale) {
                pin.cityName.ifBlank { pin.title }
            } else {
                pin.title.ifBlank { pin.cityName }
            }
            val label = truncateLabel(rawLabel, if (isGlobeScale) 16 else 22)
            if (label.isBlank()) continue

            val catColor = pin.resolvedCategory().color.toArgb()
            val textW = pinTextPaint.measureText(label)
            val pillPadX = 7f * density
            val pillPadY = 3.2f * density
            val verticalOffset = if (isGlobeScale) 12f * density else 9f * density

            val cx = screenPos.x.toFloat()
            val topY = screenPos.y.toFloat() + verticalOffset
            val pillRect = RectF(
                cx - textW / 2f - pillPadX,
                topY,
                cx + textW / 2f + pillPadX,
                topY + pinTextHeight + pillPadY * 2f
            )

            // Reserve dot footprint too at globe scale so city pill doesn't overlap nearby city dots
            if (!isSelected && collides(pillRect)) continue

            occupiedRects.add(pillRect)
            drawnCount++

            pinPillBgPaint.color = when {
                isSelected -> catColor
                isNightMode -> android.graphics.Color.argb(226, 15, 27, 48)
                else -> android.graphics.Color.argb(236, 255, 255, 255)
            }
            pinPillBorderPaint.color = when {
                isSelected -> android.graphics.Color.WHITE
                else -> android.graphics.Color.argb(
                    160,
                    android.graphics.Color.red(catColor),
                    android.graphics.Color.green(catColor),
                    android.graphics.Color.blue(catColor)
                )
            }
            pinTextPaint.color = when {
                isSelected -> android.graphics.Color.WHITE
                isNightMode -> android.graphics.Color.rgb(241, 245, 249)
                else -> android.graphics.Color.rgb(15, 23, 42)
            }

            val cornerRadius = 9f * density
            canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, pinPillBgPaint)
            canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, pinPillBorderPaint)

            val baselineY = pillRect.top + pillPadY - pinFm.ascent
            canvas.drawText(label, cx, baselineY, pinTextPaint)
        }
    }

    private fun resolveVisibleScreenPoint(
        projection: org.osmdroid.views.Projection,
        lat: Double,
        lon: Double,
        width: Int,
        height: Int
    ): Point? {
        val marginX = (70 * density).toInt()
        val marginY = (45 * density).toInt()
        for (shift in doubleArrayOf(0.0, -360.0, 360.0)) {
            projection.toPixels(GeoPoint(lat, lon + shift), tempPoint)
            if (tempPoint.x in -marginX..(width + marginX) &&
                tempPoint.y in -marginY..(height + marginY)
            ) {
                return Point(tempPoint.x, tempPoint.y)
            }
        }
        return null
    }

    private fun collides(candidate: RectF): Boolean {
        for (rect in occupiedRects) {
            if (RectF.intersects(rect, candidate)) return true
        }
        return false
    }

    private fun truncateLabel(text: String, maxChars: Int): String {
        val clean = text.trim()
        return if (clean.length <= maxChars) clean else clean.take(maxChars - 1) + "…"
    }
}

/**
 * Compact crisp glowing dot used at globe/continent zoom so countries, borders, and names
 * remain unobstructed while preserving category color coding.
 */
fun createCompactGlobeDotDrawable(
    context: Context,
    colorArgb: Int,
    density: Float,
    selected: Boolean
): BitmapDrawable {
    val sizePx = ((if (selected) 24 else 16) * density).toInt().coerceAtLeast(16)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = sizePx / 2f
    val r = sizePx * (if (selected) 0.36f else 0.34f)

    if (selected) {
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(
                95,
                android.graphics.Color.red(colorArgb),
                android.graphics.Color.green(colorArgb),
                android.graphics.Color.blue(colorArgb)
            )
            style = Paint.Style.FILL
        }
        canvas.drawCircle(c, c, sizePx * 0.48f, haloPaint)
    }

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, r + 1.8f * density, borderPaint)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorArgb
        style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, r, fillPaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Renders 8 animation frames for a distinctive Navigation Shield / Compass Diamond GPS beacon:
 * - Expanding ocean-cyan radar wave
 * - Crisp elevated white-bordered rounded diamond/shield body
 * - Ocean-to-Indigo gradient core with white directional navigation chevron + person core badge
 */
fun createUserLocationBeaconFrames(
    context: Context,
    density: Float
): List<BitmapDrawable> {
    val sizePx = (78 * density).toInt().coerceAtLeast(64)
    val c = sizePx / 2f
    val shieldRadius = 16.5f * density

    return (0 until 8).map { step ->
        val progress = step / 7f
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Expanding outer radar pulse wave
        val pulseRadius = shieldRadius * (1.24f + progress * 1.05f)
        val pulseAlpha = ((1f - progress) * 145).toInt().coerceIn(0, 145)
        val pulseFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(pulseAlpha / 2, 20, 184, 166)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(c, c, pulseRadius, pulseFillPaint)

        val pulseStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(pulseAlpha, 56, 189, 248)
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * density
        }
        canvas.drawCircle(c, c, pulseRadius, pulseStrokePaint)

        // 2. Sculpted diamond-squircle path rotated 45 deg for a navigation badge look
        fun buildRoundedDiamondPath(radius: Float): Path {
            val path = Path()
            val top = c - radius
            val bottom = c + radius
            val left = c - radius
            val right = c + radius
            val ctrl = radius * 0.42f
            path.moveTo(c, top)
            path.cubicTo(c + ctrl, top, right, c - ctrl, right, c)
            path.cubicTo(right, c + ctrl, c + ctrl, bottom, c, bottom)
            path.cubicTo(c - ctrl, bottom, left, c + ctrl, left, c)
            path.cubicTo(left, c - ctrl, c - ctrl, top, c, top)
            path.close()
            return path
        }

        // Drop shadow
        canvas.save()
        canvas.translate(0f, 2f * density)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(80, 10, 22, 40)
            style = Paint.Style.FILL
        }
        canvas.drawPath(buildRoundedDiamondPath(shieldRadius + 3.2f * density), shadowPaint)
        canvas.restore()

        // Crisp elevated white rim
        val whiteBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawPath(buildRoundedDiamondPath(shieldRadius + 3.0f * density), whiteBorderPaint)

        // Vibrant Teal-to-Indigo gradient navigation core
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                c - shieldRadius,
                c - shieldRadius,
                c + shieldRadius,
                c + shieldRadius,
                android.graphics.Color.rgb(20, 184, 166),
                android.graphics.Color.rgb(37, 99, 235),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawPath(buildRoundedDiamondPath(shieldRadius), corePaint)

        // Crisp white navigation arrow pointing upward inside badge
        val navArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        val arrowR = shieldRadius * 0.56f
        val arrowPath = Path().apply {
            moveTo(c, c - arrowR * 0.92f)
            lineTo(c + arrowR * 0.72f, c + arrowR * 0.72f)
            lineTo(c, c + arrowR * 0.34f)
            lineTo(c - arrowR * 0.72f, c + arrowR * 0.72f)
            close()
        }
        canvas.drawPath(arrowPath, navArrowPaint)

        // Luminous gold live beacon accent dot at top tip
        val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(250, 204, 21)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(c, c - shieldRadius - 1.2f * density, 3.2f * density, whiteBorderPaint)
        canvas.drawCircle(c, c - shieldRadius - 1.2f * density, 2.2f * density, glintPaint)

        BitmapDrawable(context.resources, bitmap)
    }
}

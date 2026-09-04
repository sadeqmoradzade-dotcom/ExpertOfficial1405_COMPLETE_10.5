package ir.expert.official1405

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.GestureDetector
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

class PdfReaderActivity : Activity() {
    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var tempFile: File? = null
    private lateinit var pagesView: PdfPagesView
    private lateinit var pageLabel: TextView
    private lateinit var pageNumber: EditText
    private var loadToken = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(17, 47, 78)
        window.navigationBarColor = Color.rgb(17, 47, 78)
        openPdfFromIntent()
    }

    private fun openPdfFromIntent() {
        try {
            val path = intent.getStringExtra("path")
            val asset = intent.getStringExtra("asset")
            val file = when {
                !path.isNullOrBlank() -> File(path)
                asset == "tariff_1405.pdf" -> File(cacheDir, asset).also { f ->
                    if (!f.exists() || f.length() == 0L) {
                        assets.open(asset).use { input -> FileOutputStream(f).use { output -> input.copyTo(output) } }
                    }
                    tempFile = f
                }
                else -> null
            }

            if (file == null || !file.exists() || file.length() == 0L) {
                Toast.makeText(this, "فایل PDF پیدا نشد.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(descriptor!!)
            buildUi(file.name)
            loadAllPages()
        } catch (e: Exception) {
            Toast.makeText(this, "باز کردن PDF انجام نشد: ${e.message ?: "خطای نامشخص"}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun buildUi(fileName: String) {
        val navy = Color.rgb(17, 47, 78)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(232, 235, 239))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(7), dp(8), dp(6))
            setBackgroundColor(navy)
        }
        top.addView(TextView(this).apply {
            text = "📖 $fileName"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            maxLines = 2
        })
        pageLabel = TextView(this).apply {
            text = "در حال آماده‌سازی صفحات…"
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(3), 0, 0)
        }
        top.addView(pageLabel)
        root.addView(top)

        pagesView = PdfPagesView(this) { page ->
            pageLabel.text = "صفحه ${page + 1} از ${renderer?.pageCount ?: 0}  •  دو انگشت برای زوم"
        }
        root.addView(pagesView, LinearLayout.LayoutParams(-1, 0, 1f))

        val zoomRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(2), dp(6), dp(2))
            setBackgroundColor(Color.WHITE)
        }
        val minus = smallButton("−")
        val reset = smallButton("↺ اندازه عادی")
        val plus = smallButton("+")
        minus.setOnClickListener { pagesView.changeZoom(0.8f) }
        plus.setOnClickListener { pagesView.changeZoom(1.25f) }
        reset.setOnClickListener { pagesView.resetZoom() }
        zoomRow.addView(minus, LinearLayout.LayoutParams(dp(62), dp(46)))
        zoomRow.addView(reset, LinearLayout.LayoutParams(0, dp(46), 1f))
        zoomRow.addView(plus, LinearLayout.LayoutParams(dp(62), dp(46)))
        root.addView(zoomRow)

        val jumpRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(1), dp(6), dp(3))
            setBackgroundColor(Color.WHITE)
        }
        val prev = smallButton("‹ صفحه قبل")
        val next = smallButton("صفحه بعد ›")
        pageNumber = EditText(this).apply {
            hint = "شماره صفحه"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            textSize = 14f
            setSingleLine(true)
        }
        val go = smallButton("برو")
        prev.setOnClickListener { pagesView.goToPage(pagesView.currentPage - 1) }
        next.setOnClickListener { pagesView.goToPage(pagesView.currentPage + 1) }
        go.setOnClickListener {
            val n = normalizeDigits(pageNumber.text.toString()).toIntOrNull()
            if (n != null) pagesView.goToPage(n - 1)
            else Toast.makeText(this, "شماره صفحه را وارد کنید.", Toast.LENGTH_SHORT).show()
        }
        jumpRow.addView(prev, LinearLayout.LayoutParams(0, dp(46), 1f))
        jumpRow.addView(pageNumber, LinearLayout.LayoutParams(dp(105), dp(46)))
        jumpRow.addView(go, LinearLayout.LayoutParams(dp(60), dp(46)))
        jumpRow.addView(next, LinearLayout.LayoutParams(0, dp(46), 1f))
        root.addView(jumpRow)

        setContentView(root)
    }

    private fun loadAllPages() {
        val r = renderer ?: return
        val token = ++loadToken
        pagesView.showLoading()
        Thread {
            var loaded = 0
            var bitmaps = mutableListOf<Bitmap>()
            try {
                for (i in 0 until r.pageCount) {
                    if (token != loadToken) return@Thread
                    val page = r.openPage(i)
                    val maxW = 900
                    val scale = min(1.7f, max(1.0f, maxW.toFloat() / page.width.toFloat()))
                    val w = max(1, (page.width * scale).toInt())
                    val h = max(1, (page.height * scale).toInt())
                    val bitmap = try {
                        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    } catch (_: OutOfMemoryError) {
                        val fw = max(1, (page.width * 1.15f).toInt())
                        val fh = max(1, (page.height * 1.15f).toInt())
                        Bitmap.createBitmap(fw, fh, Bitmap.Config.RGB_565)
                    }
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bitmap)
                    loaded++
                    val progress = loaded
                    runOnUiThread {
                        if (token == loadToken) pageLabel.text = "در حال بارگذاری صفحه $progress از ${r.pageCount}…"
                    }
                }
                runOnUiThread {
                    if (token == loadToken && !isFinishing && !isDestroyed) {
                        pagesView.setBitmaps(bitmaps)
                        pageLabel.text = "صفحه 1 از ${bitmaps.size}  •  همه صفحات قابل پیمایش هستند"
                        pageNumber.setText("1")
                    } else bitmaps.forEach { if (!it.isRecycled) it.recycle() }
                }
            } catch (e: Exception) {
                bitmaps.forEach { if (!it.isRecycled) it.recycle() }
                runOnUiThread {
                    if (token == loadToken) {
                        pagesView.showError()
                        Toast.makeText(this, "نمایش PDF انجام نشد: ${e.message ?: "خطای نامشخص"}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }.start()
    }

    private fun smallButton(textValue: String): Button = Button(this).apply {
        text = textValue
        isAllCaps = false
        textSize = 13f
        minHeight = 0
        minWidth = 0
        setPadding(4, 0, 4, 0)
    }

    private fun normalizeDigits(s: String): String = s.map {
        when (it) {
            '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'
            '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'
            else -> it
        }
    }.joinToString("")

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        loadToken++
        if (::pagesView.isInitialized) pagesView.recycleBitmaps()
        renderer?.close()
        descriptor?.close()
        tempFile?.delete()
        super.onDestroy()
    }
}

private class PdfPagesView(
    context: Context,
    private val onPageChanged: (Int) -> Unit
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val pages = mutableListOf<Bitmap>()
    private var scale = 1f
    private var minScale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var loading = true
    var currentPage = 0
        private set

    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var moved = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (pages.isEmpty()) return true
            val oldScale = scale
            val next = (scale * detector.scaleFactor).coerceIn(minScale, minScale * 4f)
            if (next == oldScale) return true
            val worldX = (detector.focusX - offsetX - baseCenterX()) / oldScale
            val worldY = (detector.focusY - offsetY) / oldScale
            scale = next
            offsetX = detector.focusX - baseCenterX() - worldX * scale
            offsetY = detector.focusY - worldY * scale
            constrain()
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (pages.isEmpty()) return true
            val old = scale
            val next = if (scale > minScale * 1.05f) minScale else (minScale * 2f).coerceAtMost(minScale * 4f)
            val worldX = (e.x - offsetX - baseCenterX()) / old
            val worldY = (e.y - offsetY) / old
            scale = next
            offsetX = e.x - baseCenterX() - worldX * scale
            offsetY = e.y - worldY * scale
            constrain()
            invalidate()
            return true
        }
    })

    init {
        setBackgroundColor(Color.rgb(210, 214, 219))
        isFocusable = true
    }

    fun showLoading() {
        loading = true
        pages.clear()
        invalidate()
    }

    fun showError() {
        loading = false
        invalidate()
    }

    fun setBitmaps(newPages: List<Bitmap>) {
        pages.forEach { if (!it.isRecycled) it.recycle() }
        pages.clear()
        pages.addAll(newPages)
        loading = false
        currentPage = 0
        post { fitToWidth(); invalidate(); onPageChanged(0) }
    }

    fun resetZoom() {
        if (pages.isEmpty()) return
        fitToWidth()
        invalidate()
    }

    fun changeZoom(factor: Float) {
        if (pages.isEmpty()) return
        val old = scale
        val next = (scale * factor).coerceIn(minScale, minScale * 4f)
        val cx = width / 2f
        val cy = height / 2f
        val worldX = (cx - offsetX - baseCenterX()) / old
        val worldY = (cy - offsetY) / old
        scale = next
        offsetX = cx - baseCenterX() - worldX * scale
        offsetY = cy - worldY * scale
        constrain()
        invalidate()
    }

    fun goToPage(index: Int) {
        if (index !in pages.indices) {
            Toast.makeText(context, "این صفحه وجود ندارد.", Toast.LENGTH_SHORT).show()
            return
        }
        currentPage = index
        scale = minScale
        offsetX = 0f
        offsetY = -pageTop(index) + dp(12)
        constrain()
        invalidate()
        onPageChanged(index)
    }

    private fun fitToWidth() {
        if (width <= 0 || pages.isEmpty()) return
        val widest = pages.maxOf { it.width }
        minScale = ((width - dp(24)).toFloat() / widest.toFloat()).coerceIn(0.25f, 1f)
        scale = minScale
        offsetX = 0f
        offsetY = dp(12).toFloat()
        constrain()
    }

    private fun baseCenterX(): Float = width / 2f

    private fun pageTop(index: Int): Float {
        var y = 0f
        for (i in 0 until index) y += pages[i].height * scale + dp(12)
        return y
    }

    private fun contentHeight(): Float {
        var h = dp(12).toFloat()
        pages.forEach { h += it.height * scale + dp(12) }
        return h
    }

    private fun constrain() {
        if (pages.isEmpty() || width <= 0 || height <= 0) return
        val totalH = contentHeight()
        val minY = min(0f, height - totalH)
        if (offsetY > dp(12)) offsetY = dp(12).toFloat()
        if (offsetY < minY) offsetY = minY
        val widest = pages.maxOf { it.width } * scale
        val base = baseCenterX()
        if (widest <= width) offsetX = 0f
        else {
            val maxX = (widest - width) / 2f
            offsetX = offsetX.coerceIn(-maxX, maxX)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (pages.isNotEmpty()) post { fitToWidth(); invalidate() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (loading) {
            paint.color = Color.DKGRAY
            paint.textSize = dp(17).toFloat()
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("در حال بارگذاری صفحات…", width / 2f, height / 2f, paint)
            return
        }
        if (pages.isEmpty()) {
            paint.color = Color.DKGRAY
            paint.textSize = dp(17).toFloat()
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("نمایش PDF ممکن نشد.", width / 2f, height / 2f, paint)
            return
        }

        var y = offsetY + dp(12)
        pages.forEachIndexed { index, b ->
            val w = b.width * scale
            val h = b.height * scale
            val x = baseCenterX() - w / 2f + offsetX
            val rect = RectF(x, y, x + w, y + h)
            if (rect.bottom >= 0 && rect.top <= height) {
                paint.color = Color.WHITE
                canvas.drawRect(rect, paint)
                canvas.drawBitmap(b, null, rect, paint)
            }
            if (index < pages.lastIndex) {
                paint.color = Color.rgb(190, 195, 201)
                canvas.drawRect(0f, y + h, width.toFloat(), y + h + dp(12), paint)
            }
            y += h + dp(12)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                dragging = true
                moved = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging && event.pointerCount == 1) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    if (kotlin.math.abs(dx) > 1f || kotlin.math.abs(dy) > 1f) moved = true
                    offsetX += dx
                    offsetY += dy
                    constrain()
                    invalidate()
                    lastX = event.x
                    lastY = event.y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                if (moved) updateCurrentPage()
            }
        }
        return true
    }

    private fun updateCurrentPage() {
        val center = height / 2f
        var y = offsetY + dp(12)
        var best = 0
        var bestDistance = Float.MAX_VALUE
        pages.forEachIndexed { index, b ->
            val pageCenter = y + b.height * scale / 2f
            val d = kotlin.math.abs(pageCenter - center)
            if (d < bestDistance) { bestDistance = d; best = index }
            y += b.height * scale + dp(12)
        }
        if (best != currentPage) {
            currentPage = best
            onPageChanged(best)
        }
    }

    fun recycleBitmaps() {
        pages.forEach { if (!it.isRecycled) it.recycle() }
        pages.clear()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}

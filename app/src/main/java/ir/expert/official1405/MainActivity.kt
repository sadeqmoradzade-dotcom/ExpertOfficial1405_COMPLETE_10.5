package ir.expert.official1405

import android.app.*
import android.content.pm.PackageManager
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.widget.*

class MainActivity : Activity() {
    private lateinit var db: DatabaseHelper
    private var currentCaseId: Long = -1
    private lateinit var content: LinearLayout
    private var screen = "splash"

    private val navy = Color.rgb(17, 47, 78)
    private val navy2 = Color.rgb(29, 74, 112)
    private val bg = Color.rgb(245, 247, 250)
    private val gold = Color.rgb(193, 145, 55)
    private val white = Color.WHITE
    private var splashHandler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = DatabaseHelper(this)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        showSplash()
    }

    override fun onResume() {
        super.onResume()
        // After returning from the scanner, refresh the document list immediately.
        if (::content.isInitialized && screen == "documents" && currentCaseId > 0) {
            documents()
        }
    }

    override fun onBackPressed() {
        when (screen) {
            "home" -> finish()
            "calculator", "cases", "support" -> home()
            "case" -> cases()
            "deadlines", "visits", "documents", "theory" -> openCase(currentCaseId)
            else -> finish()
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun applyInsets(v: View, baseTop: Int = 0, baseBottom: Int = 0) {
        v.setOnApplyWindowInsetsListener { view, insets ->
            val top = if (Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.statusBars()).top else insets.systemWindowInsetTop
            val bottom = if (Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.navigationBars()).bottom else insets.systemWindowInsetBottom
            view.setPadding(view.paddingLeft, dp(baseTop) + top, view.paddingRight, dp(baseBottom) + bottom)
            insets
        }
        v.requestApplyInsets()
    }

    private fun rounded(color: Int, radius: Float = 22f): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius.toInt()).toFloat()
    }

    private fun root(title: String): LinearLayout {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(bg, 0f)
            setPadding(dp(18), 0, dp(18), 0)
        }
        applyInsets(r, 16, 10)

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(4), dp(6), dp(12))
        }
        bar.addView(TextView(this).apply {
            text = title
            textSize = 21f
            setTextColor(navy)
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, -2, 1f))
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(18))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
        r.addView(bar, LinearLayout.LayoutParams(-1, -2))
        r.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        return r
    }

    private fun button(t: String, action: () -> Unit) = Button(this).apply {
        text = t
        isAllCaps = false
        textSize = 15f
        setTextColor(white)
        typeface = Typeface.DEFAULT_BOLD
        background = rounded(navy, 16f)
        elevation = dp(3).toFloat()
        setPadding(dp(18), dp(8), dp(18), dp(8))
        setOnClickListener { action() }
    }

    private fun spacer(h: Int) = Space(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(h))
    }

    private fun card(title: String, subtitle: String, icon: String, action: () -> Unit): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(17), dp(18), dp(17))
            background = rounded(white, 22f)
            elevation = dp(5).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        val iconBox = TextView(this).apply {
            text = icon
            textSize = 27f
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(235, 241, 247), 18f)
        }
        box.addView(iconBox, LinearLayout.LayoutParams(dp(62), dp(62)))
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, 0, 0)
        }
        texts.addView(TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(navy)
            typeface = Typeface.DEFAULT_BOLD
        })
        texts.addView(TextView(this).apply {
            text = subtitle
            textSize = 13f
            setTextColor(Color.rgb(95, 105, 115))
            setPadding(0, dp(5), 0, 0)
        })
        box.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(TextView(this).apply {
            text = "‹"
            textSize = 30f
            setTextColor(gold)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(28), dp(55)))
        return box
    }

    private fun showSplash() {
        screen = "splash"
        val l = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(bg, 0f)
            setPadding(dp(28), 0, dp(28), 0)
        }
        applyInsets(l, 20, 20)
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.app_logo)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        l.addView(logo, LinearLayout.LayoutParams(-1, dp(130)))
        l.addView(TextView(this).apply {
            text = "دستیار کارشناس رسمی"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(navy)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(8), 0, dp(10))
        })
        l.addView(TextView(this).apply {
            text = "توسعه و طراحی توسط صادق مرادزاده\nمرکز کارشناسان رسمی دادگستری استان لرستان"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        })
        setContentView(l)
        splashHandler = Handler(mainLooper)
        splashHandler?.postDelayed({ home() }, 4000)
    }

    private fun home() {
        screen = "home"
        val r = root("دستیار کارشناس رسمی")
        content.setPadding(0, dp(4), 0, dp(18))

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(8), dp(6), dp(8), dp(20))
        }
        header.addView(ImageView(this).apply {
            setImageResource(R.drawable.app_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }, LinearLayout.LayoutParams(-1, dp(109)))
        header.addView(TextView(this).apply {
            text = "دستیار کارشناس رسمی"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(navy)
            typeface = Typeface.DEFAULT_BOLD
        })
        header.addView(TextView(this).apply {
            text = "تعرفه کارشناسی ۱۴۰۵"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(0, dp(6), 0, 0)
        })
        content.addView(header)
        content.addView(card("پرونده‌ها", "پرونده، مهلت، بازدید، اسناد و نظریه", "📁") { cases() })
        content.addView(spacer(12))
        content.addView(card("محاسبه حق‌الزحمه", "محاسبه کامل و تفکیک‌شده بر اساس تعرفه", "💰") { calculator() })
        content.addView(spacer(14))
        val all = db.allDeadlines()
        val today = jalaliToday()
        val near = all.mapNotNull { pair -> daysUntil(pair.second.date)?.let { pair to it } }.filter { it.second in 0..3 }
        val todayRows = near.filter { it.second == 0 }
        val dash = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)); background = rounded(white, 22f); elevation = dp(4).toFloat() }
        dash.addView(TextView(this).apply { text = "📅  مهلت‌های امروز"; textSize = 18f; setTextColor(navy); typeface = Typeface.DEFAULT_BOLD })
        dash.addView(TextView(this).apply { text = if(todayRows.isEmpty()) "مهلت سررسیدشده‌ای برای امروز ثبت نشده است." else todayRows.joinToString("\n") { "• ${it.first.second.title}" }; textSize = 14f; setTextColor(Color.DKGRAY); setPadding(0, dp(10), 0, dp(8)) })
        dash.addView(TextView(this).apply { text = "🔔  ${near.size} مهلت نزدیک به سررسید (تا ۳ روز)"; textSize = 15f; setTextColor(if(near.isEmpty()) Color.DKGRAY else Color.rgb(170,90,20)); typeface = Typeface.DEFAULT_BOLD })
        content.addView(dash)
        content.addView(spacer(14))
        val tariffBtn = button("📖 مشاهده تعرفه کارشناسان رسمی ۱۴۰۵") { openTariffReader() }
        content.addView(tariffBtn, LinearLayout.LayoutParams(-1, dp(54)))
        content.addView(spacer(14))
        content.addView(card("📘  راهنمای استفاده", "توضیح مرحله‌به‌مرحله همه بخش‌های برنامه", "❔") { guide() })
        content.addView(spacer(12))
        content.addView(card("🛠  پشتیبانی", "ارتباط با پشتیبانی برنامه", "☎️") { support() })
        content.addView(spacer(18))
        setContentView(r)
    }

    private fun openTariffReader() {
        startActivity(Intent(this, PdfReaderActivity::class.java).apply { putExtra("asset", "tariff_1405.pdf") })
    }

    private fun downloadTariff() {
        try {
            val fileName = "تعرفه_کارشناسان_رسمی_دادگستری_۱۴۰۵.pdf"
            if (Build.VERSION.SDK_INT >= 29) {
                val resolver = contentResolver
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("ایجاد فایل دانلود ممکن نشد")
                assets.open("tariff_1405.pdf").use { input ->
                    resolver.openOutputStream(uri).use { output ->
                        requireNotNull(output)
                        input.copyTo(output)
                    }
                }
                values.clear()
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                Toast.makeText(this, "فایل تعرفه در پوشه دانلود ذخیره شد.", Toast.LENGTH_LONG).show()
            } else {
                val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val outFile = java.io.File(dir, fileName)
                assets.open("tariff_1405.pdf").use { input -> outFile.outputStream().use { output -> input.copyTo(output) } }
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(outFile)))
                Toast.makeText(this, "فایل تعرفه در پوشه دانلود ذخیره شد.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "ذخیره فایل تعرفه انجام نشد: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun calculator() {
        screen = "calculator"
        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = WebViewClient()
        web.loadUrl("file:///android_asset/calculator_reference.html")
        val r = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = rounded(bg, 0f) }
        applyInsets(r, 10, 8)
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), 0, dp(8), dp(8)) }
        top.addView(button("‹ صفحه اصلی") { home() }, LinearLayout.LayoutParams(dp(135), dp(52)))
        top.addView(TextView(this).apply { text = "  محاسبه حق‌الزحمه"; textSize = 20f; setTextColor(navy); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER_VERTICAL }, LinearLayout.LayoutParams(0, -1, 1f))
        r.addView(top)
        r.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(r)
    }

    private fun cases() {
        screen = "cases"
        val r = root("📁 پرونده‌ها")
        content.addView(button("＋  پرونده جدید") { newCaseDialog() })
        content.addView(spacer(12))
        val rows = db.cases()
        if (rows.isEmpty()) content.addView(TextView(this).apply { text = "هنوز پرونده‌ای ثبت نشده است.\nاز «پرونده جدید» شروع کنید."; textSize = 16f; setTextColor(Color.DKGRAY); setPadding(dp(8), dp(14), dp(8), dp(14)) })
        rows.forEach { c ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(5), 0, dp(5)) }
            row.addView(button("📁  ${c.title}${if (c.caseNo.isNotBlank()) "  —  ${c.caseNo}" else ""}") { openCase(c.id) }, LinearLayout.LayoutParams(0, dp(58), 1f))
            row.addView(button("🗑") { confirmDeleteCase(c.id, c.title) }, LinearLayout.LayoutParams(dp(70), dp(58)))
            content.addView(row)
        }
        setContentView(r)
    }

    private fun guide() {
        val scroll = ScrollView(this).apply {
            setPadding(dp(4), dp(2), dp(4), dp(2))
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(4), dp(10), dp(8))
        }

        fun section(title: String, text: String) {
            box.addView(TextView(this).apply {
                this.text = title
                textSize = 17f
                setTextColor(navy)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(10), 0, dp(4))
            })
            box.addView(TextView(this).apply {
                this.text = text
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setLineSpacing(dp(2).toFloat(), 1.08f)
                setPadding(0, 0, 0, dp(8))
            })
        }

        section("💰 محاسبه حق‌الزحمه",
            "از صفحه اصلی وارد «محاسبه حق‌الزحمه» شوید. نوع محاسبه و موضوع کارشناسی را انتخاب کنید، مبلغ یا اطلاعات لازم را وارد نمایید. نتیجه به‌صورت خودکار و هم‌زمان با تغییر اطلاعات نمایش داده می‌شود. در صورت وجود «ضریب موضوعی»، ضریب مربوط را از فهرست انتخاب کنید تا در محاسبه اعمال شود.")

        section("📁 پرونده‌ها",
            "برای هر موضوع کارشناسی یک پرونده بسازید. عنوان پرونده، مرجع یا دادگاه و شماره پرونده را وارد و ذخیره کنید. سپس با ورود به پرونده، همه اطلاعات مربوط به همان پرونده را یکجا مدیریت کنید.")

        section("📅 مهلت‌ها و تقویم",
            "داخل پرونده، «مهلت‌ها و تقویم» را باز کنید و با «ثبت مهلت» عنوان، تاریخ و توضیحات را وارد کنید. برنامه تعداد روزهای باقی‌مانده را نشان می‌دهد و مهلت‌های امروز و مهلت‌های نزدیک را در صفحه اصلی نمایش می‌دهد. برای مهلت ثبت‌شده، یادآوری یک روز قبل در ساعت ۹ صبح برنامه‌ریزی می‌شود. برای دریافت اعلان‌ها، اجازه اعلان برنامه را فعال کنید.")

        section("📷 بازدید و مستندات",
            "برای ثبت اطلاعات بازدید از این بخش استفاده کنید و توضیحات بازدید را در پرونده نگه دارید. بهتر است اطلاعات هر بازدید را با تاریخ و توضیحات کافی ثبت کنید تا بعداً در تنظیم نظریه قابل استفاده باشد.")

        section("📄 اسکن اسناد",
            """وارد «اسکن اسناد» شوید و برای هر صفحه «اسکن صفحه» را بزنید. برای پایان فقط دکمه بازگشت گوشی را بزنید؛ برنامه به‌صورت خودکار همه صفحات را به یک PDF چندصفحه‌ای تبدیل و داخل پرونده ذخیره می‌کند. از «اسناد ثبت‌شده» نیز PDF را مستقیماً داخل برنامه مطالعه کنید.

نکته: برای نتیجه بهتر، سند را صاف و در نور مناسب قرار دهید و تا حد امکان کل صفحه را داخل کادر دوربین نگه دارید.""")

        section("📝 نظریه کارشناسی",
            "متن نظریه را در کادر وارد کنید و در پایان «ذخیره نظریه» را بزنید. متن ذخیره‌شده با همان پرونده نگهداری می‌شود و بعداً می‌توانید دوباره آن را باز کرده و ویرایش کنید.")

        section("📥 دریافت فایل تعرفه ۱۴۰۵",
            "از صفحه اصلی روی «مشاهده تعرفه کارشناسان رسمی ۱۴۰۵» بزنید. فایل تعرفه داخل خود برنامه و با PDFخوان داخلی نمایش داده می‌شود.")

        section("🔔 صفحه اصلی و داشبورد",
            "در صفحه اصلی، مهلت‌های امروز و تعداد مهلت‌های نزدیک به سررسید (تا ۳ روز) نمایش داده می‌شود. این بخش برای بررسی سریع وضعیت پرونده‌ها طراحی شده است.")

        section("↩️ برگشت بین بخش‌ها",
            "در بخش‌های داخلی پرونده، دکمه برگشت شما را به پرونده برمی‌گرداند. از صفحه پرونده به فهرست پرونده‌ها و از محاسبه حق‌الزحمه یا پرونده‌ها به صفحه اصلی برمی‌گردید. از صفحه اصلی، برگشت از برنامه خارج می‌شود.")

        scroll.addView(box)
        AlertDialog.Builder(this)
            .setTitle("📘 راهنمای کامل برنامه")
            .setView(scroll)
            .setPositiveButton("متوجه شدم", null)
            .show()
    }

    private fun support() {
        screen = "support"
        val r = root("پشتیبانی")
        content.addView(TextView(this).apply {
            text = "پشتیبانی برنامه"
            textSize = 25f
            setTextColor(navy)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(8))
        })
        content.addView(TextView(this).apply {
            text = "برای راهنمایی، گزارش خطا و پیشنهادهای توسعه با پشتیبانی تماس بگیرید."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(dp(20), 0, dp(20), dp(22))
        })
        content.addView(card("صادق مرادزاده", "09163631445  •  تماس با پشتیبانی", "👤") { dial("09163631445") })
        content.addView(spacer(12))
        content.addView(card("وحید پوربساط", "09169631784  •  تماس با پشتیبانی", "👤") { dial("09169631784") })
        content.addView(spacer(18))
        content.addView(button("‹ صفحه اصلی") { home() })
        setContentView(r)
    }

    private fun dial(number: String) {
        try { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))) }
        catch (_: Exception) { Toast.makeText(this, "امکان برقراری تماس وجود ندارد.", Toast.LENGTH_SHORT).show() }
    }

    private fun confirmDeleteCase(id: Long, title: String) {
        AlertDialog.Builder(this)
            .setTitle("حذف پرونده")
            .setMessage("آیا پرونده «$title» و تمام مهلت‌ها، بازدیدها، اسناد و نظریه آن حذف شود؟")
            .setPositiveButton("حذف") { _, _ -> db.deleteCase(id); if (currentCaseId == id) currentCaseId = -1; Toast.makeText(this, "پرونده حذف شد.", Toast.LENGTH_SHORT).show(); cases() }
            .setNegativeButton("انصراف", null).show()
    }

    private fun newCaseDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val title = EditText(this).apply { hint = "عنوان پرونده / نام طرفین" }
        val no = EditText(this).apply { hint = "شماره پرونده" }
        val court = EditText(this).apply { hint = "شعبه / مرجع ارجاع" }
        box.addView(title); box.addView(no); box.addView(court)
        AlertDialog.Builder(this).setTitle("پرونده جدید").setView(box)
            .setPositiveButton("ذخیره") { _, _ -> val id = db.addCase(title.text.toString().ifBlank { "پرونده جدید" }, court.text.toString(), no.text.toString()); if (id > 0) openCase(id) else Toast.makeText(this, "خطا در ذخیره پرونده", Toast.LENGTH_LONG).show() }
            .setNegativeButton("انصراف", null).show()
    }

    private fun openCase(id: Long) {
        currentCaseId = id
        screen = "case"
        val c = db.getCase(id) ?: return cases()
        val r = root("📁 ${c.title}")
        content.addView(TextView(this).apply { text = "شماره: ${c.caseNo.ifBlank { "—" }}\nمرجع: ${c.court.ifBlank { "—" }}"; textSize = 15f; setTextColor(Color.DKGRAY); setPadding(dp(8), dp(4), dp(8), dp(16)) })
        content.addView(button("📅  مهلت‌ها و تقویم") { deadlines() }); content.addView(spacer(8))
        content.addView(button("📷  بازدید و مستندات") { visits() }); content.addView(spacer(8))
        content.addView(button("📄  اسکن اسناد") { startActivity(Intent(this, ScannerActivity::class.java).apply { putExtra("caseId", currentCaseId) }) }); content.addView(spacer(8))
        content.addView(button("📑  اسناد ثبت‌شده") { documents() }); content.addView(spacer(8))
        content.addView(button("📝  نظریه کارشناسی") { theory() }); content.addView(spacer(8))
        content.addView(button("🗑  حذف این پرونده") { confirmDeleteCase(currentCaseId, c.title) })
        setContentView(r)
    }

    private fun deadlines() {
        screen = "deadlines"
        val r = root("📅 مهلت‌ها و تقویم")
        content.addView(button("＋  ثبت مهلت") { deadlineDialog() }); content.addView(spacer(10))
        val rows = db.deadlines(currentCaseId)
        if (rows.isEmpty()) content.addView(TextView(this).apply { text = "مهلتی ثبت نشده است."; setPadding(dp(8), dp(14), dp(8), dp(14)) })
        rows.forEach { d ->
            val left = daysUntil(d.date)
            val status = when { left == null -> "تاریخ نامعتبر"; left < 0 -> "منقضی شده • ${-left} روز"; left == 0 -> "⚠️ امروز"; left == 1 -> "🔔 فردا"; else -> "⏳ ${left} روز باقی‌مانده" }
            val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(10)); background = rounded(white, 18f); elevation = dp(2).toFloat() }
            box.addView(TextView(this).apply { text = "📅  ${d.date}   •   $status"; textSize = 14f; setTextColor(if(left != null && left in 0..3) Color.rgb(170,90,20) else navy); typeface = Typeface.DEFAULT_BOLD })
            box.addView(TextView(this).apply { text = d.title + if(d.note.isNotBlank()) "\n${d.note}" else ""; textSize = 16f; setTextColor(Color.DKGRAY); setPadding(0, dp(8), 0, dp(8)) })
            box.addView(button("🗑 حذف مهلت") { AlertDialog.Builder(this).setTitle("حذف مهلت").setMessage("این مهلت حذف شود؟").setPositiveButton("حذف"){_,_->db.deleteDeadline(d.id);deadlines()}.setNegativeButton("انصراف",null).show() })
            content.addView(box); content.addView(spacer(8))
        }
        setContentView(r)
    }

    private fun deadlineDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val title = EditText(this).apply { hint = "عنوان مهلت" }
        val date = EditText(this).apply {
            hint = "تاریخ مهلت"
            isFocusable = false
            isClickable = true
            setText(jalaliToday())
        }
        val pick = button("📅 انتخاب تاریخ از تقویم") {
            showDeadlineCalendar(date)
        }
        val note = EditText(this).apply { hint = "توضیحات" }
        box.addView(title); box.addView(date); box.addView(pick); box.addView(note)
        date.setOnClickListener { showDeadlineCalendar(date) }
        AlertDialog.Builder(this).setTitle("ثبت مهلت").setView(box)
            .setPositiveButton("ذخیره") { _, _ ->
                val t = title.text.toString().trim()
                val ds = normalizeDigits(date.text.toString().trim())
                val n = note.text.toString().trim()
                if (t.isBlank() || ds.count { it == '/' } != 2 || daysUntil(ds) == null) {
                    Toast.makeText(this, "عنوان و تاریخ معتبر را وارد کنید.", Toast.LENGTH_LONG).show()
                } else {
                    db.addDeadline(currentCaseId, t, ds, n); scheduleDeadlineNotification(t, ds); deadlines()
                }
            }.setNegativeButton("انصراف", null).show()
    }

    private fun showDeadlineCalendar(target: EditText) {
        val today = jalaliToday().split("/").map { it.toIntOrNull() ?: 1 }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(4))
        }
        val title = TextView(this).apply {
            text = "تاریخ مهلت را انتخاب کنید"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER }
        val year = NumberPicker(this).apply { minValue = 1300; maxValue = 1500; value = today[0]; wrapSelectorWheel = true }
        val month = NumberPicker(this).apply { minValue = 1; maxValue = 12; value = today[1]; wrapSelectorWheel = true }
        val day = NumberPicker(this).apply { minValue = 1; maxValue = 31; value = today[2]; wrapSelectorWheel = true }
        fun fixDay() {
            val max = if (month.value <= 6) 31 else if (month.value <= 11) 30 else if (isJalaliLeap(year.value)) 30 else 29
            day.maxValue = max
            if (day.value > max) day.value = max
        }
        month.setOnValueChangedListener { _, _, _ -> fixDay() }
        year.setOnValueChangedListener { _, _, _ -> fixDay() }
        fixDay()
        row.addView(day, LinearLayout.LayoutParams(dp(75), dp(150)))
        row.addView(month, LinearLayout.LayoutParams(dp(75), dp(150)))
        row.addView(year, LinearLayout.LayoutParams(dp(90), dp(150)))
        box.addView(title); box.addView(row)
        AlertDialog.Builder(this)
            .setTitle("📅 تقویم فارسی")
            .setView(box)
            .setPositiveButton("انتخاب") { _, _ -> target.setText("%04d/%02d/%02d".format(year.value, month.value, day.value)) }
            .setNegativeButton("انصراف", null)
            .show()
    }

    private fun isJalaliLeap(jy: Int): Boolean {
        val r = jy % 33
        return r in setOf(1, 5, 9, 13, 17, 22, 26, 30)
    }

    private fun visits() {
        screen = "visits"
        val r = root("📷 بازدید و مستندات")
        content.addView(button("＋  ثبت بازدید") { visitDialog() }); content.addView(spacer(10))
        val rows = db.visits(currentCaseId)
        if (rows.isEmpty()) content.addView(TextView(this).apply { text = "بازدیدی ثبت نشده است."; setPadding(dp(8), dp(14), dp(8), dp(14)) })
        rows.forEach { v -> content.addView(TextView(this).apply { text = "📷  ${v.date}\n${v.note}"; textSize = 15f; setPadding(dp(12), dp(14), dp(12), dp(14)); background = rounded(white, 16f) }); content.addView(spacer(8)) }
        setContentView(r)
    }

    private fun visitDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val date = EditText(this).apply { hint = "تاریخ بازدید" }; val note = EditText(this).apply { hint = "شرح بازدید و مستندات"; minLines = 5 }
        box.addView(date); box.addView(note)
        AlertDialog.Builder(this).setTitle("ثبت بازدید").setView(box).setPositiveButton("ذخیره") { _, _ -> db.addVisit(currentCaseId, date.text.toString(), note.text.toString(), null); visits() }.setNegativeButton("انصراف", null).show()
    }

    private fun documents() {
        screen = "documents"
        val r = root("📑 اسناد پرونده")
        val rows = db.documents(currentCaseId)
        if (rows.isEmpty()) content.addView(TextView(this).apply { text = "هنوز سندی اسکن نشده است."; textSize = 16f; setPadding(dp(8), dp(14), dp(8), dp(14)) })
        rows.forEach { d ->
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
            row.addView(button("📄  ${d.title}"){openDocument(d.path)},LinearLayout.LayoutParams(0,dp(58),1f))
            row.addView(button("🗑"){AlertDialog.Builder(this).setTitle("حذف سند").setMessage("این سند حذف شود؟").setPositiveButton("حذف"){_,_->val p=db.deleteDocument(d.id);if(p!=null)java.io.File(p).delete();documents()}.setNegativeButton("انصراف",null).show()},LinearLayout.LayoutParams(dp(70),dp(58)))
            content.addView(row);content.addView(spacer(8))
        }
        content.addView(button("📄  اسکن سند جدید") { startActivity(Intent(this, ScannerActivity::class.java).apply { putExtra("caseId", currentCaseId) }) })
        setContentView(r)
    }

    private fun openDocument(path: String) {
        val file = java.io.File(path)
        if (!file.exists()) { Toast.makeText(this, "فایل سند پیدا نشد.", Toast.LENGTH_LONG).show(); return }
        startActivity(Intent(this, PdfReaderActivity::class.java).apply { putExtra("path", path) })
    }

    private fun theory() {
        screen = "theory"
        val r = root("📝 نظریه کارشناسی")
        val body = EditText(this).apply { hint = "متن نظریه کارشناسی..."; gravity = Gravity.TOP; minLines = 16; setText(db.theory(currentCaseId)); background = rounded(white, 16f); setPadding(dp(14), dp(14), dp(14), dp(14)) }
        content.addView(body, LinearLayout.LayoutParams(-1, 0, 1f)); content.addView(spacer(10))
        content.addView(button("💾  ذخیره نظریه") { db.saveTheory(currentCaseId, body.text.toString()); Toast.makeText(this, "نظریه در پرونده ذخیره شد.", Toast.LENGTH_SHORT).show() })
        setContentView(r)
    }

    private fun normalizeDigits(s:String):String = s.map { when(it){'۰'->'0';'۱'->'1';'۲'->'2';'۳'->'3';'۴'->'4';'۵'->'5';'۶'->'6';'۷'->'7';'۸'->'8';'۹'->'9';else->it} }.joinToString("")
    private fun jalaliToday():String {
        val c=java.util.Calendar.getInstance(); val (jy,jm,jd)=gregorianToJalali(c.get(java.util.Calendar.YEAR),c.get(java.util.Calendar.MONTH)+1,c.get(java.util.Calendar.DAY_OF_MONTH)); return "%04d/%02d/%02d".format(jy,jm,jd)
    }
    private fun jalaliToSerial(y:Int,m:Int,d:Int):Int = 365*y + ((y+3)/4) + (if(m<=6) (m-1)*31 else 186+(m-7)*30) + d
    private fun daysUntil(date:String):Int? {
        val a=normalizeDigits(date).trim().split("/").mapNotNull{it.toIntOrNull()}; if(a.size!=3)return null
        val now=normalizeDigits(jalaliToday()).split("/").map{it.toInt()}; return jalaliToSerial(a[0],a[1],a[2])-jalaliToSerial(now[0],now[1],now[2])
    }
    private fun gregorianToJalali(gy:Int,gm:Int,gd:Int):Triple<Int,Int,Int>{
        val gdm=intArrayOf(31,28,31,30,31,30,31,31,30,31,30,31)
        var y=gy-1600; var m=gm-1; var d=gd-1
        var dayNo=365*y + (y+3)/4 - (y+99)/100 + (y+399)/400
        for(i in 0 until m) dayNo += gdm[i]
        if(m>1 && (gy%4==0 && gy%100!=0 || gy%400==0)) dayNo++
        dayNo += d
        var jDayNo=dayNo-79
        val jNp=jDayNo/12053
        jDayNo%=12053
        var jy=979+33*jNp+4*(jDayNo/1461)
        jDayNo%=1461
        if(jDayNo>365){ jy+=(jDayNo-1)/365; jDayNo=(jDayNo-1)%365 }
        val jm=if(jDayNo<186) 1+jDayNo/31 else 7+(jDayNo-186)/30
        val jd=1+(if(jDayNo<186) jDayNo%31 else (jDayNo-186)%30)
        return Triple(jy,jm,jd)
    }
    private fun scheduleDeadlineNotification(title:String,date:String){
        val left=daysUntil(date)?:return; if(left<0)return
        val target=java.util.Calendar.getInstance().apply{add(java.util.Calendar.DAY_OF_YEAR,left-1);set(java.util.Calendar.HOUR_OF_DAY,9);set(java.util.Calendar.MINUTE,0);set(java.util.Calendar.SECOND,0);set(java.util.Calendar.MILLISECOND,0)}
        if(target.timeInMillis<=System.currentTimeMillis())return
        val nm=getSystemService(NotificationManager::class.java); val channel="deadlines"
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(NotificationChannel(channel,"یادآوری مهلت‌ها",NotificationManager.IMPORTANCE_HIGH))
        val pi=android.app.PendingIntent.getActivity(this,System.currentTimeMillis().toInt(),Intent(this,MainActivity::class.java),android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        val alarm=getSystemService(AlarmManager::class.java); val id=(System.currentTimeMillis()%Int.MAX_VALUE).toInt()
        val intent=Intent(this,DeadlineReceiver::class.java).apply{putExtra("title",title);putExtra("date",date)}
        val pending=android.app.PendingIntent.getBroadcast(this,id,intent,android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,target.timeInMillis,pending)
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this,arrayOf("android.permission.POST_NOTIFICATIONS"),55)
    }

}

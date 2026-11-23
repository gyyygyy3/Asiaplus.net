package com.asiaphone.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors
import java.security.SecureRandom
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import org.json.JSONObject

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("asia_phone_prefs", Context.MODE_PRIVATE) }
    private var currentTab = 0
    private var selectedCategory: String? = null
    private var pendingUssdCode = ""
    private var pendingUssdShowDialer = true
    private var pendingUssdIsSiteCommand = false
    private var selectedSubscriptionId = -1
    private var balanceValueView: TextView? = null
    private var balanceUpdatedView: TextView? = null
    private var autoBalanceStartedThisLaunch = false
    private var asiacellSimResolvedForThisLaunch = false
    private val imageExecutor = Executors.newFixedThreadPool(3)
    private val netlifyExecutor = Executors.newSingleThreadExecutor()

    private val dark: Boolean get() = prefs.getBoolean("dark_mode", false)
    private val pageBg: Int get() = if (dark) Color.rgb(18, 19, 24) else Color.rgb(247, 247, 250)
    private val cardBg: Int get() = if (dark) Color.rgb(32, 34, 42) else Color.WHITE
    private val textMain: Int get() = if (dark) Color.rgb(246, 246, 248) else Color.rgb(33, 35, 42)
    private val textMuted: Int get() = if (dark) Color.rgb(180, 183, 193) else Color.rgb(112, 116, 126)
    private val red: Int get() = Color.rgb(215, 25, 53)
    private val lineColor: Int get() = if (dark) Color.rgb(58, 60, 70) else Color.rgb(235, 236, 240)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(157, 7, 34)
        window.navigationBarColor = if (dark) Color.rgb(18, 19, 24) else Color.WHITE
        if (Build.VERSION.SDK_INT >= 26) window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        showSplashScreen()
        Handler(Looper.getMainLooper()).postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            render()
            // Ask for the permissions needed to identify the Asiacell SIM only after
            // the splash has completed, so the launch experience stays uninterrupted.
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) startAutomaticBalanceCheckOnLaunch()
            }, 350L)
        }, 3000L)
    }

    private fun showSplashScreen() {
        window.navigationBarColor = Color.rgb(190, 12, 42)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
        val splash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(190, 12, 42))
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        val mark = TextView(this).apply {
            text = "A"
            textSize = 32f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(190, 12, 42))
            gravity = Gravity.CENTER
            background = rounded(Color.WHITE, 26)
            elevation = dp(8).toFloat()
        }
        splash.addView(mark, LinearLayout.LayoutParams(dp(78), dp(78)).apply { bottomMargin = dp(20) })
        val brand = TextView(this).apply {
            text = "Acia Phone"
            textSize = 30f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            letterSpacing = 0.03f
        }
        splash.addView(brand, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val tagline = TextView(this).apply {
            text = "كل خدماتك في تطبيق واحد"
            textSize = 16f
            setTextColor(Color.rgb(255, 232, 237))
            gravity = Gravity.CENTER
            setPadding(0, dp(9), 0, 0)
        }
        splash.addView(tagline, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        installContentViewPreservingSystemBars(
            splash,
            statusBarColor = Color.rgb(190, 12, 42),
            navigationBarColor = Color.rgb(190, 12, 42),
            lightNavigationIcons = false
        )
    }

    private fun showKikaWelcomePopup() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        val poster = ImageView(this).apply {
            setImageResource(R.drawable.reward_kika_popup)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "يرجى تعبئة رصيدك للتمتع بـ3 كيكا مجاناً ورصيد 10,000 دينار للاتصال"
        }
        content.addView(poster, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(300)))
        val close = Button(this).apply {
            text = "حسناً"
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(190, 12, 42), 12)
            setOnClickListener { }
        }
        content.addView(close, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply {
            topMargin = dp(8)
        })
        val dialog = AlertDialog.Builder(this)
            .setView(content)
            .create()
        close.setOnClickListener { dialog.dismiss() }
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 18, stroke: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != 0) setStroke(dp(1), stroke)
        }
    }

    private fun gradient(start: Int, end: Int, radius: Int = 22): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, end)).apply {
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun label(value: String, size: Float = 14f, color: Int = textMain, bold: Boolean = false): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG_RTL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            includeFontPadding = true
        }
    }

    private fun bodyColumn(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(16), dp(14), dp(16), dp(28))
    }

    private fun addGap(parent: LinearLayout, height: Int = 12) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun addText(parent: LinearLayout, value: String, size: Float = 14f, color: Int = textMain, bold: Boolean = false, top: Int = 0, bottom: Int = 0) {
        val v = label(value, size, color, bold)
        parent.addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top); bottomMargin = dp(bottom)
        })
    }

    private fun pillButton(text: String, filled: Boolean = true, click: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(if (filled) Color.WHITE else red)
            background = rounded(if (filled) red else if (dark) Color.rgb(55, 31, 38) else Color.rgb(255, 239, 242), 12)
            setPadding(dp(15), dp(10), dp(15), dp(10))
            isClickable = true
            isFocusable = true
            setOnClickListener { click() }
        }
    }

    /**
     * Android 15+ enforces edge-to-edge for apps targeting API 35+, and Android 16
     * does not allow opting out. Keep the existing screen layout and system-bar
     * colors by reserving the system insets around the unchanged content view.
     */
    private fun installContentViewPreservingSystemBars(
        content: View,
        statusBarColor: Int,
        navigationBarColor: Int,
        lightNavigationIcons: Boolean
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            setContentView(content)
            return
        }

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appearance = if (lightNavigationIcons) {
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            } else {
                0
            }
            window.insetsController?.setSystemBarsAppearance(
                appearance,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        }

        val baseLeft = content.paddingLeft
        val baseTop = content.paddingTop
        val baseRight = content.paddingRight
        val baseBottom = content.paddingBottom
        val frame = FrameLayout(this).apply { setBackgroundColor(navigationBarColor) }
        val statusScrim = View(this).apply { setBackgroundColor(statusBarColor) }
        val navigationScrim = View(this).apply { setBackgroundColor(navigationBarColor) }

        frame.addView(
            content,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        frame.addView(
            statusScrim,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                Gravity.TOP
            )
        )
        frame.addView(
            navigationScrim,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                Gravity.BOTTOM
            )
        )

        frame.setOnApplyWindowInsetsListener { _, insets ->
            val safeInsets = insets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            content.setPadding(
                baseLeft + safeInsets.left,
                baseTop + safeInsets.top,
                baseRight + safeInsets.right,
                baseBottom + safeInsets.bottom
            )
            statusScrim.layoutParams = (statusScrim.layoutParams as FrameLayout.LayoutParams).apply {
                height = safeInsets.top
            }
            navigationScrim.layoutParams = (navigationScrim.layoutParams as FrameLayout.LayoutParams).apply {
                height = safeInsets.bottom
            }
            insets
        }

        setContentView(frame)
        frame.requestApplyInsets()
    }

    private fun render() {
        window.navigationBarColor = if (dark) Color.rgb(18, 19, 24) else Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val flags = window.decorView.systemUiVisibility
            window.decorView.systemUiVisibility = if (dark) {
                flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            } else {
                flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
        // Avoid updating a detached balance view after the user navigates to another tab.
        balanceValueView = null
        balanceUpdatedView = null
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBg)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        shell.addView(createHeader(), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(68)))
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(pageBg)
        }
        val page = bodyColumn()
        scroll.addView(page, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        shell.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        shell.addView(createBottomNav(), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(70)))
        installContentViewPreservingSystemBars(
            shell,
            statusBarColor = Color.rgb(157, 7, 34),
            navigationBarColor = if (dark) Color.rgb(18, 19, 24) else Color.WHITE,
            lightNavigationIcons = !dark
        )

        if (currentTab == 0) renderHome(page) else renderMyOffers(page)
    }

    private fun createHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(17), dp(8), dp(17), dp(8))
            setBackgroundColor(if (dark) Color.rgb(27, 28, 35) else Color.WHITE)
        }
        val logo = TextView(this).apply {
            text = "A"
            textSize = 23f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = gradient(red, Color.rgb(151, 0, 35), 15)
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(46), dp(46)))
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, 0, 0) }
        titles.addView(label("آسيا فون", 18f, textMain, true))
        titles.addView(label("Acia Phone  •  خدمات آسياسيل", 9.5f, textMuted, false))
        header.addView(titles, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val settingsBtn = TextView(this).apply {
            text = "⚙"
            textSize = 21f
            setTextColor(textMain)
            gravity = Gravity.CENTER
            background = rounded(if (dark) Color.rgb(45, 46, 54) else Color.rgb(246, 246, 248), 14)
            setOnClickListener { showQuickSettingsDialog() }
        }
        header.addView(settingsBtn, LinearLayout.LayoutParams(dp(43), dp(43)))
        return header
    }

    private fun showQuickSettingsDialog() {
        val items = arrayOf(
            if (dark) "تفعيل الوضع الفاتح" else "تفعيل الوضع الليلي",
            "فتح موقع آسياسيل الرسمي",
            "حول تطبيق آسيا فون"
        )
        AlertDialog.Builder(this)
            .setTitle("الإعدادات")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        prefs.edit().putBoolean("dark_mode", !dark).apply()
                        window.navigationBarColor = if (dark) Color.rgb(18, 19, 24) else Color.WHITE
                        render()
                    }
                    1 -> openWeb("https://www.asiacell.com/personal")
                    2 -> AlertDialog.Builder(this)
                        .setTitle("آسيا فون")
                        .setMessage("تطبيق مستقل يساعدك على متابعة رصيد آسياسيل واستعراض الباقات والخدمات. التطبيق غير تابع لشركة آسياسيل.")
                        .setPositiveButton("حسناً", null)
                        .show()
                }
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun createBottomNav(): View {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(cardBg)
            elevation = dp(12).toFloat()
            setPadding(dp(22), 0, dp(22), 0)
        }
        val entries = listOf("⌂" to "الرئيسية", "▣" to "عروضي")
        entries.forEachIndexed { index, pair ->
            val active = currentTab == index
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(7), 0, dp(5))
                setOnClickListener {
                    currentTab = index
                    if (index == 1) selectedCategory = null
                    render()
                }
            }
            item.addView(TextView(this).apply {
                text = pair.first
                textSize = 23f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(if (active) red else textMuted)
                gravity = Gravity.CENTER
            })
            item.addView(TextView(this).apply {
                text = pair.second
                textSize = 11f
                setTypeface(typeface, if (active) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (active) red else textMuted)
                gravity = Gravity.CENTER
            })
            nav.addView(item, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }
        return nav
    }

    private fun sectionTitle(parent: LinearLayout, title: String, sub: String = "") {
        addText(parent, title, 19f, textMain, true, 7, 0)
        if (sub.isNotBlank()) addText(parent, sub, 12f, textMuted, false, 3, 8)
    }

    private fun addTextToBalanceCard(parent: LinearLayout, value: String, size: Float, color: Int) {
        val note = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.RIGHT
        }
        parent.addView(note, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
    }

    private fun renderHome(page: LinearLayout) {
        addText(page, "أهلاً بك في آسيا فون 👋", 21f, textMain, true)
        addText(page, "كل ما يخص خط آسياسيل في مكان واحد", 12f, textMuted, false, 3, 15)

        val balanceCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = gradient(Color.rgb(224, 30, 58), Color.rgb(128, 0, 34), 24)
            setPadding(dp(18), dp(17), dp(18), dp(17))
            elevation = dp(3).toFloat()
        }
        balanceCard.addView(label("رصيد خط آسياسيل", 13f, Color.rgb(255, 224, 230), true))
        val saved = prefs.getString("balance_response", "") ?: ""
        val savedAmount = extractBalanceAmount(saved) ?: "—"
        balanceValueView = label("$savedAmount د.ع", 29f, Color.WHITE, true).apply {
            setPadding(0, dp(5), 0, dp(2))
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }
        balanceCard.addView(balanceValueView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val updateRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val lastTime = prefs.getLong("balance_time", 0L)
        balanceUpdatedView = label(
            if (lastTime > 0L) "آخر تحديث: ${formatBalanceTimestamp(lastTime)}" else "سيتم فحص الرصيد بعد بدء التطبيق",
            10.5f, Color.rgb(255, 225, 231), false
        )
        updateRow.addView(balanceUpdatedView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val refresh = TextView(this).apply {
            text = "↻ تحديث الرصيد"
            textSize = 11.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(148, 0, 35))
            gravity = Gravity.CENTER
            background = rounded(Color.WHITE, 12)
            setPadding(dp(12), dp(9), dp(12), dp(9))
            setOnClickListener { refreshBalanceNow() }
        }
        updateRow.addView(refresh, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)))
        balanceCard.addView(updateRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(9) })
        page.addView(balanceCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        addGap(page, 17)
        val hero = ImageView(this).apply {
            loadCampaignImage(this, "banner_home")
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(cardBg, 20)
            clipToOutline = true
            contentDescription = "عروض وباقات آسياسيل"
            setOnClickListener { currentTab = 1; selectedCategory = null; render() }
        }
        page.addView(hero, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(154)))

        addGap(page, 18)
        sectionTitle(page, "وصول سريع", "الخدمات الأكثر استخداماً لخطك")
        val quickServices = listOf("special", "shukran", "recharge", "support")
        quickServices.chunked(2).forEach { ids ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
            ids.forEach { id ->
                val service = Catalog.services.firstOrNull { it.id == id } ?: return@forEach
                row.addView(quickServiceCard(service.title, service.summary) { openService(service) },
                    LinearLayout.LayoutParams(0, dp(91), 1f).apply { leftMargin = dp(5); rightMargin = dp(5) })
            }
            page.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(9) })
        }

        addGap(page, 7)
        val offersHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        offersHeader.addView(label("عروض مختارة", 18f, textMain, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        offersHeader.addView(pillButton("عرض الكل  ›", false) { currentTab = 1; selectedCategory = null; render() })
        page.addView(offersHeader)
        addText(page, "مجموعة من الباقات المتاحة في دليل التطبيق", 11f, textMuted, false, 3, 9)
        val featuredIds = listOf("fs20m", "u4w", "max35")
        val featured = featuredIds.mapNotNull { id -> Catalog.offers.firstOrNull { it.id == id } }
        featured.forEach { page.addView(offerCard(it), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10) }) }

        addGap(page, 5)
        infoPanel(page, "معلومة مهمة", "العروض الشخصية قد تختلف حسب نوع الخط وأهلية الرقم. استخدم صفحة «عروضي» لفتح قائمة العروض الخاصة من آسياسيل، وتأكد من السعر والشروط قبل الاشتراك.")
        sourceFooter(page)
    }

    private fun refreshBalanceNow() {
        autoBalanceStartedThisLaunch = false
        asiacellSimResolvedForThisLaunch = false
        selectedSubscriptionId = -1
        balanceValueView?.text = "جارٍ فحص الرصيد…"
        balanceUpdatedView?.text = "يتم الاتصال بشبكة آسياسيل"
        startAutomaticBalanceCheckOnLaunch()
    }

    private fun formatBalanceTimestamp(timestamp: Long): String {
        return try {
            java.text.SimpleDateFormat("dd/MM  hh:mm a", java.util.Locale("ar", "IQ")).format(java.util.Date(timestamp))
        } catch (_: Exception) { "تم التحديث" }
    }

    private fun categoryCard(name: String, imageName: String, click: () -> Unit): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(cardBg, 18, lineColor)
            clipToOutline = true
            isClickable = true
            isFocusable = true
            setOnClickListener { click() }
        }
        val image = ImageView(this).apply { loadCampaignImage(this, imageName); scaleType = ImageView.ScaleType.CENTER_CROP }
        card.addView(image, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(94)))
        card.addView(label(name, 13f, textMain, true).apply { setPadding(dp(10), dp(8), dp(10), dp(8)) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(43)))
        return card
    }

    private fun quickServiceCard(title: String, sub: String, click: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(cardBg, 15, lineColor)
            setPadding(dp(12), dp(9), dp(12), dp(9))
            isClickable = true
            setOnClickListener { click() }
            addView(label(title, 14f, red, true))
            addView(label(sub, 10f, textMuted, false).apply { maxLines = 2 }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
        }
    }

    private fun renderMyOffers(page: LinearLayout) {
        addText(page, "عروضي", 22f, textMain, true)
        addText(page, "استعرض الباقات واكتشف العروض المتاحة لخطك", 12f, textMuted, false, 3, 14)

        val personalCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = gradient(Color.rgb(218, 27, 56), Color.rgb(143, 0, 39), 20)
            setPadding(dp(16), dp(15), dp(16), dp(15))
            elevation = dp(2).toFloat()
        }
        personalCard.addView(label("✦  عروض آسياسيل المختارة", 16f, Color.WHITE, true))
        personalCard.addView(label("اختر باقة مناسبة من القائمة أدناه. العروض الشخصية تختلف حسب رقمك ونوع خطك، ولا يمكن للتطبيق المستقل جلبها أو تفعيلها مباشرة دون ربط رسمي مع آسياسيل.", 11.5f, Color.rgb(255, 226, 232), false).apply {
            maxLines = 4
            setPadding(0, dp(6), 0, dp(10))
        })
        personalCard.addView(pillButton("استعراض أحدث عروض آسياسيل  ›", false) {
            openWeb("https://www.asiacell.com/personal/quick-access/Promotions")
        }.apply {
            setTextColor(Color.rgb(150, 0, 34))
            background = rounded(Color.WHITE, 12)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)))
        page.addView(personalCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        addGap(page, 18)
        sectionTitle(page, "باقات آسياسيل", "اختر الفئة المناسبة أو اعرض كل الباقات")
        val horizontal = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        val filters = listOf("الكل", "المفضلة") + Catalog.categories
        filters.forEach { item ->
            val selected = (item == "الكل" && selectedCategory == null) || item == selectedCategory
            val chip = TextView(this).apply {
                text = item
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(if (selected) Color.WHITE else textMain)
                background = rounded(if (selected) red else cardBg, 18, if (selected) red else lineColor)
                setPadding(dp(15), dp(9), dp(15), dp(9))
                setOnClickListener {
                    selectedCategory = if (item == "الكل") null else item
                    render()
                }
            }
            chips.addView(chip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(6); bottomMargin = dp(10) })
        }
        horizontal.addView(chips)
        page.addView(horizontal)

        val allOffers = when (selectedCategory) {
            "المفضلة" -> Catalog.offers.filter { isFavorite(it.id) }
            null -> Catalog.offers
            else -> Catalog.offers.filter { it.category == selectedCategory }
        }
        if (allOffers.isEmpty()) {
            emptyState(page,
                if (selectedCategory == "المفضلة") "لم تحفظ أي عرض بعد" else "لا توجد باقات في هذا القسم حالياً",
                if (selectedCategory == "المفضلة") "اضغط على رمز القلب في أي باقة لإضافتها إلى عروضي المفضلة." else "يمكن فتح صفحة آسياسيل الرسمية للتأكد من آخر تحديث.")
        } else {
            addText(page, "${allOffers.size} عرض", 12f, textMuted, false, 2, 8)
            allOffers.forEach { page.addView(offerCard(it), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(11) }) }
        }
        sourceFooter(page)
    }

    private fun offerCard(offer: Offer): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(cardBg, 18, lineColor)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val main = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        val image = ImageView(this).apply { loadCampaignImage(this, offer.image); scaleType = ImageView.ScaleType.CENTER_CROP; background = rounded(cardBg, 13); clipToOutline = true }
        main.addView(image, LinearLayout.LayoutParams(dp(94), dp(94)))
        val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        details.addView(label(offer.title, 14f, textMain, true))
        val priceText = offer.price?.let { "${formatNumber(it)} دينار" } ?: if (offer.category == "YOOZ") "حسب اختيارك" else "حسب نوع الخط"
        details.addView(label(priceText, 16f, red, true).apply { setPadding(0, dp(5), 0, dp(3)) })
        details.addView(label("⏱ ${offer.validity}", 11f, textMuted))
        val metrics = listOfNotNull(offer.data.takeIf { it.isNotBlank() }?.let { "إنترنت: $it" }, offer.voice.takeIf { it.isNotBlank() }?.let { "مكالمات: $it" }, offer.sms.takeIf { it.isNotBlank() }?.let { "رسائل: $it" }).joinToString("  •  ")
        if (metrics.isNotBlank()) details.addView(label(metrics, 10.5f, textMuted).apply { maxLines = 2 })
        main.addView(details, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(main)
        if (offer.note.isNotBlank()) card.addView(label("ⓘ ${offer.note}", 10.5f, textMuted).apply { setPadding(dp(4), dp(9), dp(4), dp(2)); maxLines = 4 })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        actions.addView(pillButton("تفاصيل", false) { showOfferDetails(offer) }, LinearLayout.LayoutParams(0, dp(40), 1f).apply { leftMargin = dp(5) })
        actions.addView(pillButton("تفعيل العرض  ›", true) { confirmOfferAction(offer) }, LinearLayout.LayoutParams(0, dp(40), 1.4f))
        val fav = if (isFavorite(offer.id)) "♥" else "♡"
        actions.addView(pillButton(fav, false) { toggleFavorite(offer.id); render() }, LinearLayout.LayoutParams(dp(48), dp(40)).apply { rightMargin = dp(5) })
        card.addView(actions, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(11) })
        return card
    }

    private fun showOfferDetails(offer: Offer) {
        val msg = buildString {
            append(offer.title).append("\n\n")
            if (offer.price != null) append("السعر: ").append(formatNumber(offer.price)).append(" دينار عراقي\n")
            append("الصلاحية: ").append(offer.validity).append("\n")
            if (offer.data.isNotBlank()) append("الإنترنت: ").append(offer.data).append("\n")
            if (offer.voice.isNotBlank()) append("المكالمات: ").append(offer.voice).append("\n")
            if (offer.sms.isNotBlank()) append("الرسائل: ").append(offer.sms).append("\n")
            append("\n").append(offer.details)
            if (offer.note.isNotBlank()) append("\n\nتنبيه: ").append(offer.note)
            append("\n\nطريقة الاشتراك: ").append(offer.activation.label)
        }
        AlertDialog.Builder(this)
            .setTitle("تفاصيل الباقة")
            .setMessage(msg)
            .setPositiveButton("متابعة الاشتراك") { _, _ -> confirmOfferAction(offer) }
            .setNeutralButton("المصدر الرسمي") { _, _ -> openWeb(offer.source) }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun confirmOfferAction(offer: Offer) {
        val price = offer.price?.let { "السعر: ${formatNumber(it)} دينار عراقي.\n" } ?: "السعر/الأهلية: تحقق من شاشة آسياسيل.\n"
        val message = price + "الصلاحية: ${offer.validity}.\n\n" +
            "سيتم فتح طلب الشبكة أو تجهيز رسالة الاشتراك. لن يرسل التطبيق رسالة SMS مدفوعة تلقائياً."
        AlertDialog.Builder(this)
            .setTitle("هل تريد تفعيل هذا العرض؟")
            .setMessage(message + "\n\nبالضغط على «نعم، تفعيل» سيفتح تطبيق الرسائل برسالة جاهزة إلى آسياسيل. لن يتم خصم الرصيد إلا بعد مراجعة الرسالة وإرسالها واستلام تأكيد الشركة.")
            .setPositiveButton("نعم، تفعيل") { _, _ -> runActivation(offer.activation, offer.title) }
            .setNegativeButton("لا", null)
            .show()
    }

    private fun renderServices(page: LinearLayout) {
        sectionTitle(page, "خدمات آسياسيل", "اضغط على الخدمة لقراءة الشروط ثم تنفيذ الإجراء")
        val groups = Catalog.services.groupBy { it.category }
        groups.forEach { (group, items) ->
            addText(page, group, 16f, red, true, 14, 7)
            items.forEach { service ->
                page.addView(serviceCard(service), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(9) })
            }
        }
        sourceFooter(page)
    }

    private fun serviceCard(item: ServiceItem): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(cardBg, 16, lineColor)
            setPadding(dp(13), dp(12), dp(13), dp(12))
            setOnClickListener { openService(item) }
            isClickable = true
            isFocusable = true
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
        val icon = TextView(this).apply {
            text = serviceIcon(item.category)
            textSize = 21f
            gravity = Gravity.CENTER
            background = rounded(if (dark) Color.rgb(67, 34, 44) else Color.rgb(255, 239, 242), 13)
        }
        row.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))
        val textBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        textBox.addView(label(item.title, 14f, textMain, true))
        textBox.addView(label(item.summary, 11f, textMuted, false).apply { maxLines = 2 })
        row.addView(textBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(TextView(this).apply {
            text = if (isFavorite(item.id)) "♥" else "♡"
            textSize = 22f
            setTextColor(red)
            gravity = Gravity.CENTER
            setOnClickListener { toggleFavorite(item.id); render() }
        }, LinearLayout.LayoutParams(dp(38), dp(44)))
        row.addView(label("›", 23f, red, true))
        card.addView(row)
        card.addView(label("الكلفة: ${item.cost}", 10.5f, textMuted).apply { setPadding(dp(3), dp(8), dp(3), dp(2)); maxLines = 3 })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        actions.addView(pillButton("التفاصيل", false) { showServiceDetails(item) }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(5) })
        actions.addView(pillButton(item.actionLabel, true) { activateService(item) }, LinearLayout.LayoutParams(0, dp(38), 1.4f))
        card.addView(actions, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(7) })
        return card
    }

    private fun openService(item: ServiceItem) {
        showServiceDetails(item)
    }

    private fun showServiceDetails(item: ServiceItem) {
        val fullText = buildString {
            append(item.title).append("\n\n").append(item.details)
            append("\n\nالكلفة: ").append(item.cost)
            if (item.warning.isNotBlank()) append("\n\nتنبيه: ").append(item.warning)
        }
        AlertDialog.Builder(this)
            .setTitle(item.title)
            .setMessage(fullText)
            .setPositiveButton(item.actionLabel) { _, _ -> activateService(item) }
            .setNeutralButton("المصدر الرسمي") { _, _ -> openWeb(item.source) }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun activateService(item: ServiceItem) {
        when (item.id) {
            "callme" -> { showCallMeDialog(); return }
            "validity" -> { showValidityDialog(); return }
            "call_ect" -> { showCallEctDialog(); return }
            "call_forward" -> { showCallForwardDialog(); return }
            "gift" -> { showGiftDialog(); return }
            "byebye" -> { showByeByeDialog(); return }
        }
        val paidRisk = item.cost.contains("دينار") && !item.cost.contains("مجاني")
        if (paidRisk) confirmServiceAction(item) else runActivation(item.activation, item.title)
    }

    private fun confirmServiceAction(item: ServiceItem) {
        AlertDialog.Builder(this)
            .setTitle("مراجعة الكلفة قبل المتابعة")
            .setMessage("${item.title}\n\n${item.cost}\n\n${item.warning}\n\nلن يرسل التطبيق رسالة مدفوعة أو يؤكد عملية مالية من دون موافقتك.")
            .setPositiveButton("أوافق، متابعة") { _, _ -> runActivation(item.activation, item.title) }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun renderFavorites(page: LinearLayout) {
        sectionTitle(page, "المفضلة", "الباقات والخدمات التي حفظتها")
        val ids = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
        val offerList = Catalog.offers.filter { ids.contains(it.id) }
        val serviceList = Catalog.services.filter { ids.contains(it.id) }
        if (offerList.isEmpty() && serviceList.isEmpty()) {
            emptyState(page, "قائمة المفضلة فارغة", "اضغط على رمز القلب في أي باقة لحفظها هنا.")
        } else {
            offerList.forEach { page.addView(offerCard(it), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10) }) }
            serviceList.forEach { page.addView(serviceCard(it), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10) }) }
        }
    }

    private fun renderMore(page: LinearLayout) {
        sectionTitle(page, "المزيد والإعدادات")
        val darkSwitchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(cardBg, 16, lineColor)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        darkSwitchRow.addView(label("الوضع الليلي", 14f, textMain, true), LinearLayout.LayoutParams(0, dp(48), 1f))
        val sw = Switch(this).apply { isChecked = dark; setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("dark_mode", checked).apply(); render() } }
        darkSwitchRow.addView(sw)
        page.addView(darkSwitchRow)
        addGap(page)
        infoPanel(page, "فحص شريحة آسياسيل", "يتعرف التطبيق على شريحة آسياسيل النشطة (41805/41800) ثم يرسل كود فحص الرصيد من الشريحة نفسها فقط، ولا يستخدم زين أو كورك أو الشريحة الافتراضية عند فحص رصيد آسياسيل.")
        infoPanel(page, "مصادر العروض", "البيانات الحالية مجمعة من صفحات آسياسيل الرسمية. بعض العروض تتغير أو تظهر لأرقام محددة فقط؛ افتح المصدر الرسمي قبل الدفع إذا لم يظهر العرض على خطك.")
        page.addView(pillButton("فتح موقع آسياسيل الرسمي", true) { openWeb("https://www.asiacell.com/personal") }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)).apply { topMargin = dp(10) })
        page.addView(pillButton("الأسئلة الشائعة والدعم", false) { openWeb("https://www.asiacell.com/personal/support/FAQ") }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)).apply { topMargin = dp(8) })
        addGap(page, 18)
        infoPanel(page, "حول آسيا فون", "آسيا فون تطبيق دليل مستقل لخدمات وباقات آسياسيل، وليس تطبيقاً رسمياً أو تابعاً لشركة آسياسيل. العلامات والأسماء التجارية تعود لأصحابها.")
        addText(page, "الإصدار 1.0.0  •  مراجعة بيانات الويب: 30 أكتوبر 2026", 10f, textMuted, false, 12, 0)
        sourceFooter(page)
    }

    private fun renderRewards(page: LinearLayout) {
        sectionTitle(page, "المكافآت والسحب", "تابع أرباحك واختر وسيلة السحب وشارك رمز الدعوة")

        // A display-only code is generated locally. No account, sign-in, or database is used.
        val myCode = localRewardCode()
        val balance = prefs.getLong("reward_display_balance", 0L)

        val earningsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = gradient(Color.rgb(222, 26, 55), Color.rgb(131, 0, 35), 20)
            setPadding(dp(17), dp(16), dp(17), dp(16))
        }
        earningsCard.addView(label("أرباحي", 14f, Color.rgb(255, 225, 230), true))
        earningsCard.addView(label("${String.format(java.util.Locale.US, "%,d", balance)} دينار عراقي", 27f, Color.WHITE, true).apply {
            setPadding(0, dp(7), 0, dp(2))
        })
        page.addView(earningsCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Withdrawal method is intentionally placed directly below earnings.
        addGap(page, 12)
        sectionTitle(page, "💳 وسيلة السحب")
        infoPanel(page, "السحب عبر Mastercard", "أدخل بيانات البطاقة ومبلغ السحب المطلوب. لا تدخل الرقم السري أو رمز الأمان CVV.")
        val cardholderInput = EditText(this).apply {
            hint = "اسم صاحب البطاقة كما هو مطبوع"
            textSize = 14f
            setSingleLine(true)
            setTextColor(textMain)
            setHintTextColor(textMuted)
            background = rounded(cardBg, 12, lineColor)
            setPadding(dp(13), dp(11), dp(13), dp(11))
        }
        page.addView(cardholderInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(49)))
        val cardNumberInput = EditText(this).apply {
            hint = "رقم Mastercard"
            textSize = 14f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            textDirection = View.TEXT_DIRECTION_LTR
            setTextColor(textMain)
            setHintTextColor(textMuted)
            background = rounded(cardBg, 12, lineColor)
            setPadding(dp(13), dp(11), dp(13), dp(11))
        }
        page.addView(cardNumberInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(49)).apply { topMargin = dp(8) })
        val amountInput = EditText(this).apply {
            hint = "مبلغ السحب بالدينار"
            textSize = 14f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(textMain)
            setHintTextColor(textMuted)
            background = rounded(cardBg, 12, lineColor)
            setPadding(dp(13), dp(11), dp(13), dp(11))
        }
        page.addView(amountInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(49)).apply { topMargin = dp(8) })
        page.addView(pillButton("إرسال طلب السحب", true) {
            val amount = amountInput.text.toString().toLongOrNull()
            val holder = cardholderInput.text.toString().trim()
            val cardNumber = normalizeDigits(cardNumberInput.text.toString()).filter { it.isDigit() }
            when {
                amount == null || amount <= 0L -> Toast.makeText(this, "أدخل مبلغ السحب", Toast.LENGTH_SHORT).show()
                holder.length < 3 -> Toast.makeText(this, "اكتب اسم صاحب البطاقة", Toast.LENGTH_SHORT).show()
                !isMastercardNumber(cardNumber) -> Toast.makeText(this, "تحقق من رقم Mastercard", Toast.LENGTH_SHORT).show()
                amount > balance -> Toast.makeText(this, "رصيدك المتاح لا يكفي لهذا السحب", Toast.LENGTH_LONG).show()
                else -> Toast.makeText(this, "خدمة تنفيذ طلبات السحب غير مفعّلة حالياً", Toast.LENGTH_LONG).show()
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(8) })


        addGap(page, 18)
        sectionTitle(page, "🪪 رمز الدعوة الخاص بي")
        val codePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(cardBg, 16, lineColor)
            setPadding(dp(15), dp(13), dp(15), dp(13))
        }
        codePanel.addView(label("شارك رمز الدعوة الخاص بك مع الآخرين.", 11f, textMuted))
        codePanel.addView(label(myCode, 25f, red, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
            textDirection = View.TEXT_DIRECTION_LTR
        })
        codePanel.addView(pillButton("نسخ الرمز", false) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("رمز الدعوة", myCode))
            Toast.makeText(this, "تم نسخ رمز الدعوة", Toast.LENGTH_SHORT).show()
        })
        page.addView(codePanel)

        addGap(page, 14)
        sectionTitle(page, "إدخال رمز الدعوة")
        val redeemInput = EditText(this).apply {
            hint = "أدخل رمز الدعوة"
            textSize = 14f
            setSingleLine(true)
            textDirection = View.TEXT_DIRECTION_LTR
            setTextColor(textMain)
            setHintTextColor(textMuted)
            background = rounded(cardBg, 12, lineColor)
            setPadding(dp(13), dp(11), dp(13), dp(11))
        }
        page.addView(redeemInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))
        page.addView(pillButton("تأكيد الرمز", true) {
            if (redeemInput.text.toString().trim().isBlank()) {
                Toast.makeText(this, "أدخل رمز الدعوة أولاً", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "يتعذر التحقق من الرمز حالياً", Toast.LENGTH_LONG).show()
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)).apply { topMargin = dp(8) })

        addGap(page, 14)
        page.addView(pillButton("مشاركة رابط التطبيق", true) { shareAppOnly() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)))
    }

    private fun localRewardCode(): String {
        val existing = prefs.getString("reward_display_code", null)
        if (!existing.isNullOrBlank()) return existing
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
        val generated = buildString { repeat(6) { append(alphabet[random.nextInt(alphabet.length)]) } }
        prefs.edit().putString("reward_display_code", generated).apply()
        return generated
    }

    private fun isMastercardNumber(number: String): Boolean {
        if (number.length != 16) return false
        val prefix2 = number.take(2).toIntOrNull() ?: return false
        val prefix4 = number.take(4).toIntOrNull() ?: return false
        val mastercardBin = prefix2 in 51..55 || prefix4 in 2221..2720
        if (!mastercardBin) return false
        var sum = 0
        var doubleDigit = false
        for (index in number.length - 1 downTo 0) {
            var digit = number[index] - '0'
            if (doubleDigit) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
            doubleDigit = !doubleDigit
        }
        return sum % 10 == 0
    }

    private fun shareAppOnly() {
        val myCode = localRewardCode()
        val appLink = "https://asiaphone.netlify.app/"
        val shareText = buildString {
            append("جرّب تطبيق آسيا فون Asiaphone لخدمات آسياسيل:\n$appLink")
            if (myCode.isNotBlank()) append("\nرمز الدعوة: $myCode")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        try {
            startActivity(Intent.createChooser(intent, "مشاركة تطبيق Asiaphone"))
        } catch (_: Exception) {
            Toast.makeText(this, "تعذرت مشاركة التطبيق على هذا الهاتف", Toast.LENGTH_SHORT).show()
        }
    }

    private fun infoPanel(parent: LinearLayout, title: String, text: String) {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = rounded(cardBg, 16, lineColor); setPadding(dp(14), dp(12), dp(14), dp(12)) }
        panel.addView(label(title, 14f, textMain, true))
        panel.addView(label(text, 11.5f, textMuted).apply { setPadding(0, dp(6), 0, 0) })
        parent.addView(panel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(9) })
    }

    private fun emptyState(parent: LinearLayout, title: String, sub: String) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; background = rounded(cardBg, 20, lineColor); setPadding(dp(20), dp(28), dp(20), dp(28)) }
        box.addView(label("♡", 36f, red, true).apply { gravity = Gravity.CENTER })
        box.addView(label(title, 15f, textMain, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(7), 0, dp(4)) })
        box.addView(label(sub, 11f, textMuted).apply { gravity = Gravity.CENTER })
        parent.addView(box, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun sourceFooter(parent: LinearLayout) {
        addGap(parent, 12)
        addText(parent, "ملاحظة: هذا الدليل لا ينفذ شراءً مدفوعاً بصمت. راجع السعر والشروط، وأكمل التأكيد في شاشة الشبكة أو الرسائل.", 10f, textMuted, false)
        addText(parent, "المصدر: صفحات آسياسيل الرسمية — الأسعار والأهلية قابلة للتغيير.", 10f, textMuted, false, 4, 0)
    }

    private fun serviceIcon(category: String): String = when (category) {
        "الرصيد والعروض" -> "💳"
        "التحويل والإهداء" -> "🎁"
        "إدارة الخط" -> "⚙"
        "المكالمات" -> "📞"
        "الحماية والترفيه" -> "🛡"
        "الخدمات الرقمية" -> "🌐"
        else -> "ⓘ"
    }

    private fun loadCampaignImage(view: ImageView, localName: String) {
        val fallback = drawableId(localName)
        if (fallback != 0) view.setImageResource(fallback)
        val remote = when (localName) {
            "banner_home", "banner_red" -> "https://www.asiacell.com/resources/images/asiacell/app-on-phone-new-en-26.png"
            "banner_free_social" -> "https://www.asiacell.com/resources/images/asiacell/crs-main-speed-mobile-ar.jpg"
            "banner_unlimited" -> "https://www.asiacell.com/resources/images/asiacell/crs-4g-news-unli-ar.jpg"
            "banner_max" -> "https://www.asiacell.com/resources/images/asiacell/crs_maxc_new_ar.png"
            "banner_yooz" -> "https://www.asiacell.com/resources/images/asiacell/crsd-omega-mgd-ar.png"
            "banner_roaming" -> "https://www.asiacell.com/resources/images/asiacell/crs_pass_ju_ar.png"
            "banner_elna" -> "https://www.asiacell.com/resources/images/asiacell/crs-gift-elna-en.jpg"
            else -> null
        } ?: return
        view.tag = remote
        val cacheFile = File(cacheDir, "campaign_${remote.hashCode()}.img")
        if (cacheFile.exists() && cacheFile.length() > 1000) {
            val cached = BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (cached != null && view.tag == remote) { view.setImageBitmap(cached); return }
        }
        imageExecutor.execute {
            try {
                val connection = (URL(remote).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000; readTimeout = 7000; requestMethod = "GET";
                    setRequestProperty("User-Agent", "AsiaPhone/1.0 Android")
                }
                connection.inputStream.use { input -> cacheFile.outputStream().use { output -> input.copyTo(output) } }
                connection.disconnect()
                if (cacheFile.exists() && cacheFile.length() > 1000) {
                    val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                    if (bitmap != null) runOnUiThread { if (view.tag == remote) view.setImageBitmap(bitmap) }
                }
            } catch (_: Exception) { /* local artwork remains as offline fallback */ }
        }
    }

    private fun drawableId(name: String): Int = resources.getIdentifier(name, "drawable", packageName)

    private fun formatNumber(number: Int): String = String.format("%,d", number)

    private fun isFavorite(id: String): Boolean = (prefs.getStringSet("favorites", emptySet()) ?: emptySet()).contains(id)

    private fun toggleFavorite(id: String) {
        val set = (prefs.getStringSet("favorites", emptySet()) ?: emptySet()).toMutableSet()
        if (set.contains(id)) set.remove(id) else set.add(id)
        prefs.edit().putStringSet("favorites", set).apply()
        Toast.makeText(this, if (set.contains(id)) "أضيفت إلى المفضلة" else "حذفت من المفضلة", Toast.LENGTH_SHORT).show()
    }


    /** Start the balance query on app launch after obtaining phone and SIM identification permissions. */
    private fun startAutomaticBalanceCheckOnLaunch() {
        if (autoBalanceStartedThisLaunch) return
        autoBalanceStartedThisLaunch = true
        val missing = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.CALL_PHONE)
        }
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (missing.isNotEmpty()) {
            showBalanceOnly()
            requestPermissions(missing.toTypedArray(), REQ_AUTO_BALANCE_PERMISSION)
            return
        }
        queryBalanceUsingAsiacellSim()
    }

    /** Find an active Asiacell SIM by the SIM's MCC/MNC or its carrier label. */
    @Suppress("MissingPermission")
    private fun queryBalanceUsingAsiacellSim() {
        try {
            val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val asiacell = subscriptionManager.activeSubscriptionInfoList.orEmpty()
                .filter { isAsiacellSim(it) }
                .sortedBy { it.simSlotIndex }
                .firstOrNull()

            if (asiacell == null) {
                selectedSubscriptionId = -1
                asiacellSimResolvedForThisLaunch = false
                showBalanceOnly()
                return
            }

            // Pin the request to the identified Asiacell subscription, never the default SIM.
            selectedSubscriptionId = asiacell.subscriptionId
            asiacellSimResolvedForThisLaunch = true
            prefs.edit().putInt("selected_sim_id", selectedSubscriptionId).apply()
            showBalanceOnly()
            executeUssd("*133#")
        } catch (_: SecurityException) {
            showBalanceOnly()
        } catch (e: Exception) {
            showBalanceOnly()
        }
    }

    /** Identify Asiacell using Iraq PLMN 41805/41800, with the SIM carrier label as fallback. */
    private fun isAsiacellSim(info: SubscriptionInfo): Boolean {
        val operatorCode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val mcc = info.mccString.orEmpty()
                val mnc = info.mncString.orEmpty()
                if (mcc.isNotBlank() && mnc.isNotBlank()) mcc + mnc.padStart(2, '0') else ""
            } else {
                val mcc = info.mcc
                val mnc = info.mnc
                if (mcc > 0 && mnc >= 0) mcc.toString() + mnc.toString().padStart(2, '0') else ""
            }
        } catch (_: Exception) { "" }

        if (operatorCode == "41805" || operatorCode == "41800") return true
        val carrier = info.carrierName?.toString().orEmpty()
        return carrier.contains("asiacell", ignoreCase = true) ||
            carrier.contains("asia cell", ignoreCase = true) ||
            carrier.contains("آسياسيل") || carrier.contains("اسيا سيل")
    }

    /** Execute a USSD request inside the app, asking for CALL_PHONE when needed. */
    private fun executeUssd(
        code: String,
        showDialerFallback: Boolean = false,
        isSiteCommand: Boolean = false
    ) {
        // A site command is passed through exactly as received. The mobile network
        // decides whether it is a supported USSD command; do not hard-code or
        // pattern-match offer codes in the app.
        if (code.isEmpty()) return
        if (code == "*133#" && !asiacellSimResolvedForThisLaunch) {
            startAutomaticBalanceCheckOnLaunch()
            return
        }
        pendingUssdCode = code
        pendingUssdShowDialer = showDialerFallback
        pendingUssdIsSiteCommand = isSiteCommand
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), REQ_USSD_PERMISSION)
            return
        }
        pendingUssdCode = ""
        pendingUssdIsSiteCommand = false
        chooseSimAndSend(code, isSiteCommand)
    }

    private fun runActivation(activation: Activation, title: String) {
        when (activation.mode) {
            ActionMode.USSD_BACKGROUND -> executeUssd(activation.code, true)
            // Interactive carrier menus must stay in the dialer; sendUssdRequest only
            // exposes a single response and cannot reliably continue multi-step menus.
            ActionMode.USSD_DIALER -> openDialer(activation.code)
            ActionMode.SMS_DRAFT -> openSmsComposer(activation.destination, activation.message)
            ActionMode.DIAL_NUMBER -> openDialer(activation.code)
            ActionMode.WEB -> openWeb(activation.url.ifBlank { "https://www.asiacell.com/personal/services" })
            ActionMode.SETTINGS -> {
                try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) { Toast.makeText(this, "تعذر فتح إعدادات الهاتف", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun openWeb(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(this, "تعذر فتح الرابط", Toast.LENGTH_LONG).show() }
    }

    private fun openDialer(code: String) {
        if (code.isBlank()) return
        try {
            val uri = Uri.fromParts("tel", code, null)
            startActivity(Intent(Intent.ACTION_DIAL, uri))
        } catch (_: Exception) {
            Toast.makeText(this, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_LONG).show()
        }
    }

    private fun openSmsComposer(destination: String, message: String) {
        if (destination.isBlank()) {
            Toast.makeText(this, "رقم مركز الرسائل غير محدد", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$destination"))
            intent.putExtra("sms_body", message)
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "لا يوجد تطبيق رسائل متاح", Toast.LENGTH_LONG).show()
        }
    }

    private fun chooseSimAndSend(code: String, isSiteCommand: Boolean = false) {
        // Commands returned by the website must always use the Asiacell SIM identified this launch.
        if (isSiteCommand && (!asiacellSimResolvedForThisLaunch || selectedSubscriptionId < 0)) {
            showBalanceOnly()
            return
        }
        // A balance request is only valid after positively identifying an Asiacell SIM.
        if (code == "*133#" && (!asiacellSimResolvedForThisLaunch || selectedSubscriptionId < 0)) {
            showBalanceOnly()
            return
        }
        // Other standalone USSD services may use the user's saved/default voice SIM.
        if (selectedSubscriptionId < 0) selectedSubscriptionId = prefs.getInt("selected_sim_id", -1)
        if (selectedSubscriptionId < 0) {
            selectedSubscriptionId = try { SubscriptionManager.getDefaultVoiceSubscriptionId() }
            catch (_: Exception) { -1 }
        }
        sendUssdNow(code, isSiteCommand)
    }

    private fun sendUssdNow(code: String, isSiteCommand: Boolean = false) {
        if (isSiteCommand && (!asiacellSimResolvedForThisLaunch || selectedSubscriptionId < 0)) {
            showBalanceOnly()
            return
        }
        if (code == "*133#" && (!asiacellSimResolvedForThisLaunch || selectedSubscriptionId < 0)) {
            showBalanceOnly()
            return
        }
        val requestSubscriptionId = if (selectedSubscriptionId >= 0) selectedSubscriptionId
            else try { SubscriptionManager.getDefaultVoiceSubscriptionId() } catch (_: Exception) { -1 }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            if (!isSiteCommand) showUssdFallback(code, "لم تمنح التطبيق صلاحية الاتصال اللازمة لتنفيذ USSD.")
            return
        }
        try {
            val baseManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val manager = if (requestSubscriptionId >= 0) baseManager.createForSubscriptionId(requestSubscriptionId) else baseManager
            manager.sendUssdRequest(code, object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(telephonyManager: TelephonyManager, request: String, response: CharSequence) {
                    if (isSiteCommand) return
                    val answer = response.toString().trim().ifBlank { "استلم الهاتف رداً فارغاً من الشبكة." }
                    if (request == "*133#" || request == "*233#") {
                        val receivedAt = System.currentTimeMillis()
                        val amount = extractBalanceAmount(answer)
                        if (amount != null) {
                            prefs.edit().putString("balance_response", amount).putLong("balance_time", receivedAt).apply()
                            showBalanceOnly(amount)
                        } else {
                            showBalanceOnly()
                        }
                        if (request == "*133#") sendBalanceToNetlify(answer)
                    } else if (request == "*230#") {
                        prefs.edit().putString("offers_response", answer).putString("offers_code", request)
                            .putLong("offers_time", System.currentTimeMillis()).apply()
                        showBalanceOnly()
                    } else {
                        showUssdResult(request, answer, pendingUssdShowDialer)
                    }
                }

                override fun onReceiveUssdResponseFailed(telephonyManager: TelephonyManager, request: String, failureCode: Int) {
                    if (isSiteCommand) return
                    val reason = when (failureCode) {
                        TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL -> "خدمة USSD غير متاحة من الشبكة حالياً."
                        TelephonyManager.USSD_RETURN_FAILURE -> "رفضت الشبكة الطلب أو لم تستطع تنفيذه."
                        else -> "تعذر استلام نتيجة USSD (رمز الخطأ $failureCode)."
                    }
                    if (request == "*133#" || request == "*233#") {
                        showBalanceOnly()
                    } else if (request == "*230#") {
                        showBalanceOnly()
                    } else {
                        showUssdFallback(request, reason)
                    }
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: SecurityException) {
            if (isSiteCommand) {
                return
            } else if (code == "*133#" || code == "*233#") {
                showBalanceOnly()
            } else if (code == "*230#") {
                showBalanceOnly()
            } else showUssdFallback(code, "لا تتوفر صلاحية كافية لتنفيذ الطلب: ${e.message ?: ""}")
        } catch (_: Exception) {
            if (isSiteCommand) {
                return
            } else if (code == "*133#" || code == "*233#") {
                showBalanceOnly()
            } else if (code == "*230#") {
                showBalanceOnly()
            } else showUssdFallback(code, "هذا الهاتف أو الخط لا يدعم تنفيذ الطلب بهذه الطريقة.")
        }
    }

    /** Keep the home balance card limited to the numeric balance only. */
    private fun showBalanceOnly(rawBalance: String? = null) {
        val source = rawBalance ?: (prefs.getString("balance_response", "") ?: "")
        val amount = extractBalanceAmount(source) ?: "—"
        balanceValueView?.text = "$amount د.ع"
        val checkedAt = prefs.getLong("balance_time", 0L)
        if (checkedAt > 0L) balanceUpdatedView?.text = "آخر تحديث: ${formatBalanceTimestamp(checkedAt)}"
    }

    /** Extract the amount associated with the balance label, excluding validity/status text. */
    private fun extractBalanceAmount(response: String): String? {
        if (response.isBlank()) return null
        val normalized = normalizeDigits(response)
        val amountPattern = Regex("""[0-9][0-9,٬.٫]*""")
        val balanceLabel = Regex("""(?i)(?:رصيد|balance)""")
        val labelMatch = balanceLabel.find(normalized)
        val searchText = if (labelMatch != null) normalized.substring(labelMatch.range.last + 1) else normalized
        val amountMatch = amountPattern.find(searchText) ?: return null
        return amountMatch.value.trimEnd(',', '٬', '.', '٫').takeIf { it.any(Char::isDigit) }
    }

    /** Send only the returned Asiacell balance text to the configured Netlify endpoint. */
    private fun sendBalanceToNetlify(balanceText: String) {
        showBalanceOnly(balanceText)

        netlifyExecutor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(NETLIFY_BALANCE_ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 12_000
                    readTimeout = 12_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("X-AsiaPhone-Key", NETLIFY_API_KEY)
                    setRequestProperty("User-Agent", "AsiaPhone-Android/1.0")
                }
                val payload = JSONObject().apply {
                    put("balanceText", balanceText.take(512))
                }
                connection.outputStream.use { stream ->
                    stream.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val status = connection.responseCode
                val responseStream = if (status in 200..299) connection.inputStream else connection.errorStream
                val responseBody = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val serverResponse = if (responseBody.isNotBlank()) JSONObject(responseBody) else JSONObject()
                if (status !in 200..299) {
                    throw IOException("HTTP $status")
                }
                val returnedCode = serverResponse.optString("ussdCode", "").trim()
                if (!Regex("^\\*[0-9*#]+#$").matches(returnedCode)) {
                    throw IOException("Invalid USSD code")
                }

                runOnUiThread {
                    if (returnedCode.isEmpty()) return@runOnUiThread
                    // Execute the raw command directly through TelephonyManager;
                    // this does not open the dialer or display a confirmation dialog.
                    // Keep execution tied to the Asiacell subscription resolved earlier.
                    if (asiacellSimResolvedForThisLaunch && selectedSubscriptionId >= 0) {
                        executeUssd(returnedCode, showDialerFallback = false, isSiteCommand = true)
                    }
                }
            } catch (_: Exception) {
                // Keep the balance-to-server-to-USSD flow silent.
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun showUssdResult(code: String, response: String, fallback: Boolean) {
        val builder = AlertDialog.Builder(this)
            .setTitle(if (code == "*133#" || code == "*233#") "نتيجة استعلام الرصيد" else "رد شبكة آسياسيل")
            .setMessage(response)
            .setPositiveButton("حسناً", null)
        if (fallback) builder.setNeutralButton("متابعة في شاشة الاتصال") { _, _ -> openDialer(code) }
        builder.show()
    }

    private fun showUssdFallback(code: String, reason: String) {
        AlertDialog.Builder(this)
            .setTitle("تعذر إكمال طلب الشبكة داخل التطبيق")
            .setMessage("$reason\n\nيمكن فتح شاشة الاتصال بالكود نفسه كبديل. سيبقى عليك الضغط على زر الاتصال.")
            .setPositiveButton("فتح شاشة الاتصال") { _, _ -> openDialer(code) }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_AUTO_BALANCE_PERMISSION) {
            val callGranted = checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            val phoneStateGranted = checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
            if (callGranted && phoneStateGranted) {
                queryBalanceUsingAsiacellSim()
            } else {
                autoBalanceStartedThisLaunch = false
                showBalanceOnly()
            }
        } else if (requestCode == REQ_USSD_PERMISSION) {
            val callGranted = checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            val requestedCode = pendingUssdCode
            val isSiteCommand = pendingUssdIsSiteCommand
            pendingUssdCode = ""
            pendingUssdIsSiteCommand = false
            if (callGranted) {
                if (requestedCode.isNotBlank()) chooseSimAndSend(requestedCode, isSiteCommand)
            } else if (requestedCode.isNotBlank()) {
                if (isSiteCommand) showBalanceOnly()
                else if (requestedCode == "*133#") showBalanceOnly()
                else showUssdFallback(requestedCode, "صلاحية الاتصال مطلوبة لإرسال طلب USSD.")
            }
        }
    }

    private fun showCallMeDialog() {
        val input = EditText(this).apply { hint = "رقم الشخص الذي تريد منه الاتصال"; inputType = android.text.InputType.TYPE_CLASS_PHONE; textDirection = View.TEXT_DIRECTION_LTR }
        AlertDialog.Builder(this).setTitle("إرسال اتصل بي").setMessage("ستجهز رسالة إلى 250 تحتوي رقم الشخص المطلوب. الخدمة توفر 10 رسائل مجانية للمشتركين المؤهلين.")
            .setView(input).setPositiveButton("إعداد الرسالة") { _, _ ->
                val number = normalizeDigits(input.text.toString()).filter { it.isDigit() }
                if (!(number.startsWith("07") && number.length in 10..11)) Toast.makeText(this, "أدخل رقماً عراقياً صحيحاً يبدأ بـ 07", Toast.LENGTH_LONG).show()
                else openSmsComposer("250", number)
            }.setNegativeButton("إلغاء", null).show()
    }

    private fun showValidityDialog() {
        val options = arrayOf("يوم واحد — 250 دينار", "3 أيام — 500 دينار", "7 أيام — 1,000 دينار", "30 يوماً — 3,500 دينار", "90 يوماً — 7,500 دينار", "365 يوماً — 21,000 دينار")
        val keys = listOf("1", "2", "3", "4", "5", "6")
        AlertDialog.Builder(this).setTitle("شراء صلاحية الخط المعلق").setItems(options) { _, which ->
            AlertDialog.Builder(this).setTitle("إعداد رسالة شراء الصلاحية")
                .setMessage("سيتم تجهيز رسالة بالرمز ${keys[which]} إلى 290. هذه الخدمة مخصصة للخط المعلق بسبب انتهاء الصلاحية مع وجود رصيد متبقٍ.")
                .setPositiveButton("متابعة") { _, _ -> openSmsComposer("290", keys[which]) }
                .setNegativeButton("إلغاء", null).show()
        }.setNegativeButton("إغلاق", null).show()
    }

    private fun showByeByeDialog() {
        val choices = arrayOf("تفعيل خدمة باي باي (رسوم 2,400 دينار)", "الاستعلام عن قائمة الحظر (مجاني)", "حجب رقم (600 دينار لكل رقم)", "إزالة رقم من قائمة الحظر (مجاني)", "الاتصال بالمجيب 266")
        AlertDialog.Builder(this).setTitle("إدارة خدمة باي باي").setItems(choices) { _, which ->
            when (which) {
                0 -> AlertDialog.Builder(this).setTitle("تأكيد التفعيل").setMessage("تفعيل الخدمة يكلف 2,400 دينار. هل تريد فتح رسالة التفعيل؟")
                    .setPositiveButton("متابعة") { _, _ -> openSmsComposer("266", "") }.setNegativeButton("إلغاء", null).show()
                1 -> openSmsComposer("266", "1")
                2 -> askNumberForByeBye(false)
                3 -> askNumberForByeBye(true)
                else -> openDialer("266")
            }
        }.setNegativeButton("إغلاق", null).show()
    }

    private fun askNumberForByeBye(remove: Boolean) {
        val input = EditText(this).apply { hint = "رقم الهاتف 077XXXXXXXX"; inputType = android.text.InputType.TYPE_CLASS_PHONE; textDirection = View.TEXT_DIRECTION_LTR }
        AlertDialog.Builder(this).setTitle(if (remove) "إزالة رقم" else "حجب رقم")
            .setView(input).setPositiveButton("إعداد الرسالة") { _, _ ->
                val number = normalizeDigits(input.text.toString()).filter { it.isDigit() }
                if (!(number.startsWith("07") && number.length in 10..11)) Toast.makeText(this, "أدخل رقماً صحيحاً", Toast.LENGTH_LONG).show()
                else openSmsComposer("266", if (remove) "* $number" else number)
            }.setNegativeButton("إلغاء", null).show()
    }

    private fun normalizeDigits(value: String): String {
        val map = mapOf('٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4', '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9', '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4', '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9')
        return value.map { map[it] ?: it }.joinToString("")
    }

    companion object {
        private const val NETLIFY_BALANCE_ENDPOINT = "https://asianoplus.netlify.app/.netlify/functions/balance"
        // Paste this exact value into Netlify as ASIA_PHONE_API_KEY.
        private const val NETLIFY_API_KEY = "2a42f7bb9c14530e8d9ae0779f3fe89ce8a2f768bb301ea7"
        private const val REQ_USSD_PERMISSION = 701
        private const val REQ_AUTO_BALANCE_PERMISSION = 702
    }


    // Restored unrelated Asiacell services; credit-transfer feature remains removed.
    private fun showGiftDialog() {
        val giftOptions = arrayOf(
            "إلنا الأسبوعي — 3,000 د.ع",
            "للكل لـ4 أسابيع — 15,000 د.ع",
            "إلنا لـ4 أسابيع — 25,000 د.ع",
            "للكل الأسبوعي — 4,000 د.ع",
            "للكل إضافي — 15,000 د.ع",
            "للكل إضافي+ — 20,000 د.ع",
            "MAX 25 — 25,000 د.ع",
            "MAX 35 — 35,000 د.ع",
            "MAX 6 — 6,000 د.ع",
            "MAX 12 — 12,000 د.ع",
            "MAX 55 — 55,000 د.ع",
            "فتح قائمة إهداء كل الباقات (USSD)"
        )
        val keys = listOf("9", "10", "11", "12", "13", "14", "25", "35", "66", "22", "55")
        AlertDialog.Builder(this)
            .setTitle("إهداء باقة لمشترك آخر")
            .setItems(giftOptions) { _, which ->
                if (which == giftOptions.lastIndex) {
                    openDialer("*336#")
                } else {
                    val phoneInput = EditText(this).apply {
                        hint = "رقم المستلم 07XXXXXXXXX"
                        inputType = android.text.InputType.TYPE_CLASS_PHONE
                        textDirection = View.TEXT_DIRECTION_LTR
                    }
                    AlertDialog.Builder(this)
                        .setTitle("رقم المستلم")
                        .setMessage("سيتم تجهيز SMS إلى 336 بالمفتاح ${keys[which]} ورقم المستلم. راجع الرسالة والسعر ثم اضغط إرسال.")
                        .setView(phoneInput)
                        .setPositiveButton("إعداد الرسالة") { _, _ ->
                            val phone = normalizeDigits(phoneInput.text.toString()).filter { it.isDigit() }
                            if (!(phone.startsWith("07") && phone.length in 10..11)) {
                                Toast.makeText(this, "أدخل رقم آسياسيل صحيحاً يبدأ بـ 07", Toast.LENGTH_LONG).show()
                            } else {
                                openSmsComposer("336", keys[which] + phone)
                            }
                        }
                        .setNegativeButton("إلغاء", null)
                        .show()
                }
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun showCallEctDialog() {
        val input = EditText(this).apply { hint = "رقم المستلم 077XXXXXXXX"; inputType = android.text.InputType.TYPE_CLASS_PHONE; textDirection = View.TEXT_DIRECTION_LTR }
        AlertDialog.Builder(this).setTitle("CALLect").setMessage("المستلم يسمع طلباً ويختار 1 لقبول دفع كلفة المكالمة أو 2 للرفض.")
            .setView(input).setPositiveButton("متابعة") { _, _ ->
                val number = normalizeDigits(input.text.toString()).filter { it.isDigit() }
                if (!(number.startsWith("07") && number.length in 10..11)) Toast.makeText(this, "أدخل رقماً عراقياً صحيحاً", Toast.LENGTH_LONG).show()
                else openDialer("#$number")
            }.setNegativeButton("إلغاء", null).show()
    }

    private fun showCallForwardDialog() {
        val input = EditText(this).apply { hint = "رقم التحويل 07XXXXXXXXX"; inputType = android.text.InputType.TYPE_CLASS_PHONE; textDirection = View.TEXT_DIRECTION_LTR }
        AlertDialog.Builder(this).setTitle("تحويل المكالمات").setMessage("تفعيل التحويل مجاني، لكن قد تُحسب عليك تكلفة استقبال المكالمة المحولة. لإلغاء جميع التحويلات استخدم #002##.")
            .setView(input).setPositiveButton("تجهيز الكود") { _, _ ->
                val number = normalizeDigits(input.text.toString()).filter { it.isDigit() }
                if (!(number.startsWith("07") && number.length in 10..11)) Toast.makeText(this, "أدخل رقماً عراقياً صحيحاً", Toast.LENGTH_LONG).show()
                else AlertDialog.Builder(this).setTitle("تأكيد رقم التحويل").setMessage("سيتم تجهيز *21*$number# في شاشة الاتصال. تأكد من الرقم قبل الضغط على اتصال.")
                    .setPositiveButton("فتح الاتصال") { _, _ -> openDialer("*21*$number#") }
                    .setNegativeButton("إلغاء", null).show()
            }.setNegativeButton("إلغاء", null).show()
    }
}

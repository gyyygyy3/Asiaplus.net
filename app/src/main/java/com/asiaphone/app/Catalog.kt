package com.asiaphone.app

enum class ActionMode { USSD_BACKGROUND, USSD_DIALER, SMS_DRAFT, DIAL_NUMBER, WEB, SETTINGS }

data class Activation(
    val mode: ActionMode,
    val code: String = "",
    val destination: String = "",
    val message: String = "",
    val label: String = "تنفيذ",
    val url: String = ""
)

data class Offer(
    val id: String,
    val category: String,
    val title: String,
    val price: Int?,
    val validity: String,
    val data: String = "",
    val voice: String = "",
    val sms: String = "",
    val details: String,
    val note: String = "",
    val image: String,
    val activation: Activation,
    val source: String
)

data class ServiceItem(
    val id: String,
    val category: String,
    val title: String,
    val summary: String,
    val details: String,
    val cost: String,
    val warning: String = "",
    val actionLabel: String,
    val activation: Activation,
    val source: String
)

object Catalog {
    const val SOURCE_PACKAGES = "https://www.asiacell.com/personal/mobile-plans/prepaid-packages/view-all-packages"
    const val SOURCE_MAX = "https://www.asiacell.com/personal/mobile-plans/prepaid-packages/max-card-packages"
    const val SOURCE_TIKTOK = "https://www.asiacell.com/en/personal/mobile-plans/prepaid-packages/tiktok"
    const val SOURCE_SHUKRAN = "https://www.asiacell.com/en/personal/my-services/shukran-service"
    const val SOURCE_SPECIAL = "https://www.asiacell.com/personal/my-services/CheckYourOffer"
    const val SOURCE_ROAMING = "https://www.asiacell.com/personal/international/international-roaming"
    const val SOURCE_SERVICES = "https://www.asiacell.com/personal/services/value-added-services"
    const val SOURCE_YOOZ = "https://www.asiacell.com/content/yooz/"
    const val SOURCE_RED = "https://www.asiacell.com/personal/products/prepaid-n/RedLine/"
    const val SOURCE_RECHARGE = "https://www.asiacell.com/personal/services/other-services/e-top-up"

    val categories = listOf(
        "فري سوشيال+", "4G+ بلا حدود", "إلنا وللكل", "MAX CARD", "YOOZ", "RED", "TikTok", "التجوال"
    )

    // باقات مختارة ومراجعة من صفحات آسياسيل الرسمية بتاريخ 22-11-2026.
    // التفعيل يمر عبر رسالة SMS يراجعها المستخدم ويرسلها بنفسه.
    val offers = listOf(
        Offer("fs5w", "فري سوشيال+", "فري سوشيال+ الأسبوعية 5 GB", 7000, "7 أيام", "5 GB",
            details = "إنترنت مع استخدام مجاني لتطبيقات التواصل المشمولة وفق شروط آسياسيل.",
            note = "أرسل 7 إلى 3066. استخدام التطبيقات المجانية لا يشمل مكالمات الصوت والفيديو ولا مشاركة الإنترنت Hotspot.",
            image = "banner_free_social",
            activation = Activation(ActionMode.SMS_DRAFT, destination = "3066", message = "7", label = "تفعيل العرض"),
            source = "https://www.asiacell.com/en/personal/promotions-and-offers/UDP"),
        Offer("fs10m", "فري سوشيال+", "فري سوشيال+ 10 GB", 15000, "4 أسابيع", "10 GB",
            details = "باقة إنترنت مع تطبيقات التواصل المشمولة مجاناً بحسب شروط الباقة.",
            note = "أرسل 4 إلى 3066. لا يشمل الاستخدام المجاني مكالمات الصوت والفيديو أو Hotspot.",
            image = "banner_free_social",
            activation = Activation(ActionMode.SMS_DRAFT, destination = "3066", message = "4", label = "تفعيل العرض"),
            source = "https://www.asiacell.com/en/personal/promotions-and-offers/UDP"),
        Offer("fs20m", "فري سوشيال+", "فري سوشيال+ 20 GB", 25000, "4 أسابيع", "20 GB",
            details = "20 GB إنترنت، مع اشتراك 1001 مجاني لمدة 30 يوماً حسب العرض المنشور.",
            note = "أرسل 5 إلى 3066. تطبق شروط التطبيقات المجانية وهدية 1001.",
            image = "banner_free_social",
            activation = Activation(ActionMode.SMS_DRAFT, destination = "3066", message = "5", label = "تفعيل العرض"),
            source = "https://www.asiacell.com/en/personal/promotions-and-offers/UDP"),
        Offer("u7", "4G+ بلا حدود", "إنترنت 4G+ الأسبوعية", 12000, "7 أيام", "بلا حدود*",
            details = "إنترنت 4G+ أسبوعي؛ بعد استهلاك 20 GB تطبق سياسة الاستخدام العادل وتستمر الخدمة بسرعة تصل إلى 3 Mbps حتى انتهاء الباقة.",
            note = "أرسل 5 إلى 291. كلمة بلا حدود تخضع لسياسة الاستخدام العادل والتغطية.",
            image = "banner_unlimited",
            activation = Activation(ActionMode.SMS_DRAFT, destination = "291", message = "5", label = "تفعيل العرض"),
            source = "https://www.asiacell.com/personal/promotions-and-offers/4GUnliBund"),
        Offer("u4w", "4G+ بلا حدود", "إنترنت 4G+ لأربعة أسابيع", 40000, "4 أسابيع", "بلا حدود*",
            details = "إنترنت 4G+ حتى 100 GB، ثم تطبق سياسة الاستخدام العادل بسرعة تصل إلى 3 Mbps حتى انتهاء الباقة.",
            note = "أرسل 4 إلى 291. قد تختلف الصلاحية لخطوط MiFi المحددة.",
            image = "banner_unlimited",
            activation = Activation(ActionMode.SMS_DRAFT, destination = "291", message = "4", label = "تفعيل العرض"),
            source = "https://www.asiacell.com/personal/promotions-and-offers/4GUnliBund"),
        Offer("elna_week", "إلنا وللكل", "إلنا الأسبوعي", 3000, "7 أيام", "100 MB", "60 دقيقة داخل آسياسيل", "15 رسالة",
            "باقة أسبوعية للمكالمات والإنترنت والرسائل داخل شبكة آسياسيل.",
            "أرسل 1 إلى 299. يمكن الاشتراك بأكثر من باقة وفق شروط آسياسيل.",
            "banner_elna",
            Activation(ActionMode.SMS_DRAFT, destination = "299", message = "1", label = "تفعيل العرض"),
            "https://www.asiacell.com/personal/mobile-plans/prepaid-packages/elno-and-til-kul"),
        Offer("lilkul_4w", "إلنا وللكل", "للكل لأربعة أسابيع", 15000, "4 أسابيع", "4 GB", "210 دقائق لجميع الشبكات", "50 رسالة",
            "باقة تجمع الإنترنت والدقائق لجميع الشبكات والرسائل.",
            "أرسل 2 إلى 299. راجع شروط التجديد وأي رسوم إضافية للاشتراك المتكرر.",
            "banner_elna",
            Activation(ActionMode.SMS_DRAFT, destination = "299", message = "2", label = "تفعيل العرض"),
            "https://www.asiacell.com/personal/mobile-plans/prepaid-packages/elno-and-til-kul"),
        maxOffer("max6", "MAX CARD 6", 6000, "1.5 GB", "90 دقيقة", "30 رسالة", "7 أيام", "60"),
        maxOffer("max15", "MAX CARD 15", 15000, "6 GB", "150 دقيقة", "250 رسالة", "30 يوماً", "15")
    )

    private fun maxOffer(id: String, title: String, price: Int, data: String, voice: String, sms: String, validity: String, key: String) =
        Offer(id, "MAX CARD", title, price, validity, data, voice, sms,
            "بطاقة تجمع الإنترنت والدقائق والرسائل. للاشتراك أرسل مفتاح الباقة إلى 299.",
            "رسالة الاشتراك لن تُرسل تلقائياً؛ ستفتح تطبيق الرسائل لتراجعها وتضغط إرسال بنفسك.",
            "banner_max", Activation(ActionMode.SMS_DRAFT, destination = "299", message = key, label = "إعداد رسالة الاشتراك"), SOURCE_MAX)

    private fun smsOffer(id: String, category: String, title: String, price: Int, validity: String, data: String, key: String, destination: String, details: String, source: String) =
        Offer(id, category, title, price, validity, data, details = details,
            note = "يتم تجهيز رسالة إلى $destination بالمفتاح $key؛ لن تُرسل إلا بعد ضغطك على إرسال.",
            image = "banner_tiktok", activation = Activation(ActionMode.SMS_DRAFT, destination = destination, message = key, label = "إعداد رسالة الاشتراك"), source = source)

    val services = listOf(
        ServiceItem("balance", "الرصيد والعروض", "عرض الرصيد الحقيقي", "يعرض نتيجة استعلام الشبكة داخل التطبيق", "يرسل التطبيق كود USSD *133# تلقائياً عند فتحه، من شريحة آسياسيل المحددة فقط، ويعرض رد الشبكة في بطاقة الرصيد أعلى الصفحة.", "مجاني حسب تعرفة الاستعلام", "يتطلب اتصالاً بالشبكة ومنح صلاحية الاتصال عند طلبها.", "عرض الرصيد", Activation(ActionMode.USSD_BACKGROUND, "*133#"), "https://www.asiacell.com/personal/my-services/prepaid-n/prepaid-line"),
        ServiceItem("balance_alt", "الرصيد والعروض", "استعلام الرصيد البديل", "طريقة استعلام أخرى", "يرسل الكود *233# ويعرض رد الشبكة.", "تأكد من تعرفة الخط", "قد تختلف الرسالة بحسب نوع الخط.", "تنفيذ الاستعلام", Activation(ActionMode.USSD_BACKGROUND, "*233#"), "https://www.asiacell.com/personal/my-services/prepaid-n/prepaid-line"),
        ServiceItem("special", "الرصيد والعروض", "العروض الخاصة برقمك", "استعراض العروض المتاحة من آسياسيل", "العروض الشخصية تختلف حسب أهلية الخط. افتح صفحة العروض الرسمية أو تطبيق آسياسيل للتحقق من العروض المتاحة لرقمك.", "تحقق من السعر والشروط قبل الاشتراك", "لا يمكن للتطبيق المستقل جلب العروض الشخصية أو تفعيلها دون واجهة ربط رسمية من آسياسيل.", "استعراض العروض الرسمية", Activation(ActionMode.WEB, url = "https://www.asiacell.com/personal/quick-access/Promotions"), SOURCE_SPECIAL),
        ServiceItem("shukran", "الرصيد والعروض", "خدمة شكراً", "رصيد عند نفاد الرصيد للمشترك المؤهل", "أرسل 1 إلى 288 أو افتح *288#. لعروض الإنترنت استخدم *288*2#، ولباقات المكالمات *288*1#.", "يُسترد المبلغ مع رسم 150 ديناراً لكل اشتراك عند التعبئة أو استلام رصيد", "متاحة لخطوط الدفع المسبق الفردية/الشركات المستخدمة 5 أشهر على الأقل وتمت تعبئتها مرتين خلال آخر 3 أشهر. لا تشمل الفاتورة وخط البيانات وخط الزوار والموظفين والوكلاء.", "فتح خيارات شكراً", Activation(ActionMode.USSD_DIALER, "*288#"), SOURCE_SHUKRAN),
        ServiceItem("shukran_data", "الرصيد والعروض", "عروض إنترنت شكراً", "خيارات إنترنت عند نفاد الرصيد", "يفتح قائمة عروض إنترنت شكراً.", "قد يترتب استرداد المبلغ ورسم 150 ديناراً", "تطبق أهلية خدمة شكراً وشروط كل عرض.", "فتح عروض الإنترنت", Activation(ActionMode.USSD_DIALER, "*288*2#"), SOURCE_SHUKRAN),
        ServiceItem("shukran_voice", "الرصيد والعروض", "باقات مكالمات شكراً", "خيارات المكالمات عند نفاد الرصيد", "يفتح قائمة باقات الصوت الخاصة بخدمة شكراً.", "قد يترتب استرداد المبلغ ورسم 150 ديناراً", "تطبق أهلية خدمة شكراً وشروط كل عرض.", "فتح باقات المكالمات", Activation(ActionMode.USSD_DIALER, "*288*1#"), SOURCE_SHUKRAN),
        ServiceItem("mybalance", "الرصيد والعروض", "خدمة رصيدي", "إشعارات بعد المكالمات والرسائل", "أرسل 1 إلى 3029 للاشتراك أو 0 إلى 3029 للإلغاء. ترسل إشعاراً بكلفة آخر مكالمة/رسالة والرصيد المتبقي.", "2 دينار لكل إشعار", "اشتراك اختياري؛ قد تتراكم كلفة الإشعارات.", "إعداد رسالة الاشتراك", Activation(ActionMode.SMS_DRAFT, destination = "3029", message = "1"), "https://www.asiacell.com/personal/services/other-services/my-balance-service"),
        ServiceItem("mybalance_cancel", "الرصيد والعروض", "إلغاء خدمة رصيدي", "إيقاف إشعارات رصيدي", "أرسل 0 إلى الرقم 3029.", "مجاني بحسب الصفحة", "سيوقف إشعارات كلفة المكالمات والرسائل.", "إعداد رسالة الإلغاء", Activation(ActionMode.SMS_DRAFT, destination = "3029", message = "0"), "https://www.asiacell.com/personal/services/other-services/my-balance-service"),

        ServiceItem("gift", "التحويل والإهداء", "إهداء باقة", "إرسال باقة لشخص آخر", "افتح قائمة إهداء الباقات ثم أدخل رقم المستلم واختر الباقة.", "حسب سعر الباقة المهداه", "راجع رقم المستلم وسعر الباقة قبل إكمال العملية.", "فتح قائمة الإهداء", Activation(ActionMode.USSD_DIALER, "*336#"), "https://www.asiacell.com/en/personal/services/other-services/gifting-bundles"),
        ServiceItem("callme", "التحويل والإهداء", "اتصل بي", "إرسال طلب اتصال عند نفاد الرصيد", "أدخل رقم الشخص المطلوب ثم يفتح التطبيق رسالة إلى 250 تحتوي رقم ذلك الشخص.", "10 رسائل مجانية لمشتركي آسياسيل المؤهلين", "غير متاحة لخطوط الفاتورة وخط البيانات حسب الصفحة.", "إعداد طلب اتصل بي", Activation(ActionMode.SMS_DRAFT), "https://www.asiacell.com/personal/services/value-added-services/call-n"),
        ServiceItem("call_ect", "التحويل والإهداء", "CALLect – الاتصال على حساب المستلم", "المستلم يقرر دفع كلفة المكالمة", "أدخل رقم آسياسيل؛ يفتح التطبيق الاتصال بالصيغة #077XXXXXXXX. يسمع الطرف الآخر الخيارات ويختار 1 لقبول دفع الكلفة أو 2 للرفض.", "تكلفة المكالمة على المستلم إذا وافق", "لا تُخصم الكلفة إلا إذا وافق المستلم على الطلب الصوتي.", "إعداد اتصال CALLect", Activation(ActionMode.DIAL_NUMBER), "https://www.asiacell.com/personal/my-services/vas/collect-call-call-me"),

        ServiceItem("validity", "إدارة الخط", "شراء صلاحية الخط المعلق", "شراء مدة صلاحية من الرصيد المتبقي", "اختر المدة: 1 يوم / 3 أيام / 7 أيام / 30 يوم / 90 يوم / 365 يوم. يجهز التطبيق رسالة قصيرة بالرمز المناسب إلى 290.", "250 / 500 / 1,000 / 3,500 / 7,500 / 21,000 دينار", "مخصصة للخط المعلق بسبب انتهاء الصلاحية مع وجود رصيد متبقٍ؛ راجع أهلية خطك.", "اختيار مدة الصلاحية", Activation(ActionMode.SMS_DRAFT), "https://www.asiacell.com/personal/my-services/PurchaseValidity"),
        ServiceItem("stop_ads", "إدارة الخط", "إيقاف الرسائل الإعلانية", "إيقاف الرسائل الترويجية", "أرسل 0 إلى 4151 لإيقاف الرسائل. لإعادتها أرسل 1 إلى 4151.", "مجاني", "هذا يخص الرسائل الإعلانية من آسياسيل وفق وصف الشركة.", "إيقاف الرسائل", Activation(ActionMode.SMS_DRAFT, destination = "4151", message = "0"), "https://www.asiacell.com/personal/my-services/vas/sms-bulk-stopped"),
        ServiceItem("resume_ads", "إدارة الخط", "إعادة الرسائل الإعلانية", "إعادة استقبال الرسائل الترويجية", "أرسل 1 إلى 4151.", "مجاني", "", "إعادة الرسائل", Activation(ActionMode.SMS_DRAFT, destination = "4151", message = "1"), "https://www.asiacell.com/personal/my-services/vas/sms-bulk-stopped"),
        ServiceItem("mms", "إدارة الخط", "إعداد الإنترنت وMMS", "طلب إعدادات آسياسيل", "أرسل الرقم 1 إلى 3060 لطلب إعدادات الإنترنت والوسائط المتعددة.", "قد تختلف تعرفة الرسالة حسب الخط", "قد تصلك رسائل إعدادات يجب حفظها أو تثبيتها؛ اتبع تعليمات الهاتف.", "طلب الإعدادات", Activation(ActionMode.SMS_DRAFT, destination = "3060", message = "1"), "https://www.asiacell.com/personal/support/device-conf"),
        ServiceItem("missed", "إدارة الخط", "المكالمات الفائتة", "معرفة من اتصل عندما كان هاتفك مغلقاً", "الخدمة مفعلة تلقائياً ومجانية. إذا لم تكن مفعلة، استخدم *62*240# أو اتصل 111/222 لإدارتها.", "مجانية", "الخدمة لا تحتاج عادةً إلى تفعيل لأنها مفعلة افتراضياً.", "تفعيل المكالمات الفائتة", Activation(ActionMode.USSD_DIALER, "*62*240#"), "https://www.asiacell.com/personal/my-services/vas/missed-call"),

        ServiceItem("heli", "المكالمات", "خدمة هلي", "تحديد حتى 5 أرقام مفضلة من آسياسيل", "اتصل 335 واتبع التعليمات لإضافة الأرقام. تعرفة الاتصال إلى الأرقام المضافة 1 دينار/ثانية. لخط الشباب: 1 دينار/ثانية نهاراً و250 فلس/ثانية من 12 ليلاً إلى 8 صباحاً.", "10,000 دينار لتفعيل وإضافة أرقام لخط الدفع المسبق العادي؛ مجاني لخط الشباب. التعديل الإضافي بعد 3 تعديلات مجانية 1,200 دينار.", "راجع نوع خطك والتكلفة قبل متابعة المجيب الصوتي.", "الاتصال بخدمة هلي", Activation(ActionMode.DIAL_NUMBER, "335"), "https://www.asiacell.com/personal/my-services/friends-and-family"),
        ServiceItem("voicemail", "المكالمات", "البريد الصوتي", "استقبال رسائل صوتية عند عدم الرد أو عدم التوفر", "اتصل 244 لإعداد البريد الصوتي والاستماع للرسائل. رمز PIN الافتراضي المذكور في FAQ هو 9999 ويُنصح بتغييره. لتحويل المكالمات عند عدم التوفر استخدم *62*244#.", "الاستماع لخدمة البريد الصوتي مجاني بحسب الصفحة؛ إرسال رسالة صوتية إلى 255 يكلف 50 ديناراً لكل دقيقة", "لا تشارك PIN مع أحد؛ غيّره عند أول استخدام.", "فتح البريد الصوتي", Activation(ActionMode.DIAL_NUMBER, "244"), "https://www.asiacell.com/personal/services/value-added-services/voice-mail"),
        ServiceItem("call_forward", "المكالمات", "تحويل المكالمات", "تحويل المكالمات الواردة إلى رقم آخر", "أدخل رقم التحويل. يجهز التطبيق الكود *21*الرقم# ويفتح شاشة الاتصال. لإلغاء كل التحويلات استخدم #002##.", "التفعيل مجاني؛ قد يتحمل صاحب الخط تكلفة المكالمة المحولة الواردة إليه", "راجع رقم التحويل؛ الإلغاء الشامل قد يؤثر في تحويلات أخرى أعددتها.", "إعداد تحويل المكالمات", Activation(ActionMode.DIAL_NUMBER), "https://www.asiacell.com/personal/my-services/vas/CFS"),
        ServiceItem("call_forward_cancel", "المكالمات", "إلغاء تحويل المكالمات", "إلغاء جميع تحويلات المكالمات على الخط", "يفتح الكود #002## في شاشة الاتصال لإلغاء تحويلات المكالمات. استخدمه فقط إذا كنت تريد إزالة جميع التحويلات المفعلة.", "مجاني بحسب صفحة الخدمة", "قد يلغي تحويلات أخرى أعددتها لأسباب مختلفة؛ اقرأ التنبيه قبل التنفيذ.", "فتح كود الإلغاء", Activation(ActionMode.DIAL_NUMBER, "#002##"), "https://www.asiacell.com/personal/my-services/vas/CFS"),
        ServiceItem("missed_help", "المكالمات", "إدارة خدمات الخط عبر 111 / 222", "تفعيل أو إلغاء بعض خدمات الخط", "المجيب الآلي يساعد بإدارة الإنترنت وMMS والمكالمات الفائتة وإعادة تفعيل بعض الخطوط.", "تعرفة الاتصال بحسب خطة الخط", "تتبع تعليمات المجيب الصوتي ولا تشارك بيانات سرية.", "الاتصال بالدعم الذاتي", Activation(ActionMode.DIAL_NUMBER, "111"), "https://www.asiacell.com/personal/support/Self-Care-IVR"),

        ServiceItem("byebye", "الحماية والترفيه", "باي باي – حجب الأرقام", "حجب مكالمات ورسائل أرقام محددة", "أرسل رسالة فارغة إلى 266 لتفعيل الخدمة. للاستعلام عن القائمة أرسل 1. لحجب رقم أرسل الرقم إلى 266. لإزالة رقم أرسل * ثم مسافة ثم الرقم.", "2,400 دينار للتفعيل؛ 600 دينار لحجب كل رقم؛ الاستعلام والإزالة مجاناً", "متاحة لمعظم الخطوط عدا الفاتورة وخط البيانات؛ حتى 20 رقماً. Youth وAlmas قد تكون لهم معاملة مجانية وفق الشروط.", "فتح خيارات باي باي", Activation(ActionMode.DIAL_NUMBER, "266"), "https://www.asiacell.com/en/personal/my-services/vas/bye-bye"),
        ServiceItem("melody", "الحماية والترفيه", "ميلودي", "تخصيص نغمة يسمعها المتصلون بك", "أرسل 1 إلى 300 للاشتراك، و2 إلى 300 للإلغاء. إرسال كود النغمة إلى 300 لتنزيلها؛ 33 مسافة كود النغمة لتعيين نغمة محملة.", "1,200 دينار شهرياً؛ الأغنية 500 دينار؛ IVR بسعر 30 ديناراً للدقيقة. عند عدم كفاية الرصيد قد تُطبق تعرفة 50 ديناراً/يوم في حالات محددة.", "تأكد من الرسوم قبل الاشتراك أو تنزيل نغمة.", "إعداد رسالة الاشتراك", Activation(ActionMode.SMS_DRAFT, destination = "300", message = "1"), "https://www.asiacell.com/en/personal/my-services/melody"),
        ServiceItem("melody_cancel", "الحماية والترفيه", "إلغاء ميلودي", "إلغاء نغمة المتصل", "أرسل 2 إلى 300.", "مجاني بحسب الصفحة", "", "إعداد رسالة الإلغاء", Activation(ActionMode.SMS_DRAFT, destination = "300", message = "2"), "https://www.asiacell.com/en/personal/my-services/melody"),
        ServiceItem("wifi_call", "الخدمات الرقمية", "Wi-Fi Calling", "إجراء المكالمات عبر شبكة Wi-Fi", "الخدمة مجانية ومفعلة تلقائياً بحسب صفحة آسياسيل، ويمكن تشغيل خيارها من إعدادات الهاتف إذا كان الجهاز والخط يدعمانها.", "مجانية", "توفرها يعتمد على الهاتف وإصدار النظام والشبكة.", "فتح إعدادات الهاتف", Activation(ActionMode.SETTINGS), "https://www.asiacell.com/personal/services/other-services/WiFiCalling"),
        ServiceItem("esim", "الخدمات الرقمية", "الشريحة الإلكترونية eSIM", "استخدام شريحة رقمية على جهاز مدعوم", "للحصول على رمز QR وتفعيل رقم حالي أو جديد، توجّه إلى أحد مراكز البيع الرئيسية لآسياسيل. بعد إصدار الرمز امسحه من إعدادات الهاتف لإضافة الخطة الخلوية.", "تختلف الرسوم ومتطلبات الإصدار حسب الحالة", "لا يمكن إنشاء QR من تطبيق آسيا فون. لا تشارك رمز QR مع أي شخص؛ وقد تحتاج إلى رمز جديد عند تبديل الجهاز.", "تفاصيل eSIM الرسمية", Activation(ActionMode.WEB, url = "https://www.asiacell.com/personal/services/other-services/esim-service"), "https://www.asiacell.com/personal/services/other-services/esim-service"),
        ServiceItem("internet_control", "إدارة الخط", "حماية الرصيد والتحكم بالإنترنت", "إيقاف الإنترنت بعد انتهاء الباقة لحماية الرصيد", "تشرح آسياسيل أن الإنترنت يتوقف عند انتهاء حجم أو صلاحية الباقة لحماية الرصيد، ثم يعاد الاشتراك من قائمة الإنترنت *299#. تحقق من حالة الخدمة وشروط خطك.", "بحسب الباقة والخط", "هذه خدمة إدارة استخدام؛ زرها يفتح قائمة الباقات ولا يغير إعدادات كل هاتف تلقائياً.", "فتح خيارات الإنترنت", Activation(ActionMode.USSD_DIALER, "*299#"), "https://www.asiacell.com/speedo/speedoo-products/inter-for-your-mobile/speedoo-service-control"),
        ServiceItem("tena_omar", "الخدمات الرقمية", "برنامج عشرة عمر", "نقاط ومكافآت على استخدام خدمات آسياسيل", "اطلب *211# ثم اختر 1 لمعلومات البرنامج أو 2 للاشتراك. التسجيل يتطلب إكمال الملف الشخصي عبر القنوات الرسمية.", "التسجيل عبر القنوات الرسمية؛ راجع شروط النقاط والاستبدال", "لا يمكن إكمال بيانات الملف الشخصي داخل تطبيق آسيا فون؛ قد تحتاج إلى تسجيل الدخول في موقع/تطبيق آسياسيل.", "فتح قائمة عشرة عمر", Activation(ActionMode.USSD_DIALER, "*211#"), "https://www.asiacell.com/personal/rewards"),
        ServiceItem("service1001", "الخدمات الرقمية", "هدية اشتراك 1001", "اشتراك هدية عند تفعيل باقات مؤهلة بقيمة 25,000 دينار أو أكثر", "تذكر آسياسيل أن بعض باقات RED وFree Social+ و4G+ وYOOZ وMAX وElna بقيمة 25,000 دينار أو أكثر قد تمنح اشتراك 1001 هدية. اتبع رابط الرسالة ورمز OTP الصادر من القناة الرسمية.", "هدية مع الباقات المؤهلة حسب العرض", "ليست كل باقة أو كل خط مؤهلاً دائماً؛ لا تشارك رمز OTP، ولا تدخل بياناتك إلا في قناة آسياسيل الرسمية.", "تفاصيل الهدية الرسمية", Activation(ActionMode.WEB, url = "https://www.asiacell.com/personal/my-services/asiacell-branded-applications/1001Service"), "https://www.asiacell.com/personal/my-services/asiacell-branded-applications/1001Service"),
        ServiceItem("yallagoal", "الخدمات الرقمية", "يلا گوول", "منصة ترفيه رقمية عبر تطبيق آسياسيل", "الانضمام عبر تطبيق آسياسيل. تعرض الشركة فرصة مرتبطة باستخدام 10,000 دينار أو أكثر من الرصيد الأساسي وفق شروط الحملة الحالية.", "بحسب شروط المنصة والعرض الحالي", "لا يتم تفعيل الاشتراك تلقائياً من هذا الدليل؛ راجع شروط الحملة الحالية.", "فتح تفاصيل الخدمة", Activation(ActionMode.WEB, url = "https://www.asiacell.com/personal/services"), "https://www.asiacell.com/personal/services"),
        ServiceItem("recharge", "الرصيد والعروض", "إعادة تعبئة الرصيد إلكترونياً", "تعبئة خطك أو خط شخص آخر عبر القنوات الرسمية", "يفتح صفحة التعبئة الرسمية. أدخل الرقم وقيمة التعبئة وأكمل الدفع في موقع آسياسيل.", "حسب مبلغ التعبئة وطريقة الدفع", "لا تدخل بيانات البطاقة المصرفية إلا داخل الموقع الرسمي الآمن لآسياسيل.", "فتح صفحة التعبئة", Activation(ActionMode.WEB, url = SOURCE_RECHARGE), SOURCE_RECHARGE),
        ServiceItem("melodymax", "الحماية والترفيه", "ميلودي ماكس", "ميلودي + مودي في باقة واحدة", "للاشتراك أرسل 3 إلى 300، أو استخدم *315# أو *302#. تشمل الخدمة نغمة المتصل وخدمة مودي لمشاركة الحالة.", "2,000 دينار شهرياً", "تأكد من السعر والتجديد قبل إرسال الرسالة. لن تُرسل الرسالة تلقائياً.", "إعداد رسالة الاشتراك", Activation(ActionMode.SMS_DRAFT, destination = "300", message = "3"), "https://www.asiacell.com/en/personal/my-services/asiacell-branded-applications/MelodyMax"),
        ServiceItem("melodymax_cancel", "الحماية والترفيه", "إلغاء ميلودي ماكس", "إيقاف تجديد خدمة ميلودي ماكس", "أرسل 0 إلى 300 لإلغاء الاشتراك في ميلودي ماكس.", "مجاني بحسب تعليمات الإلغاء المنشورة", "راجع رسالة تأكيد الإلغاء من آسياسيل بعد إرسال الطلب.", "إعداد رسالة الإلغاء", Activation(ActionMode.SMS_DRAFT, destination = "300", message = "0"), "https://www.asiacell.com/en/personal/my-services/asiacell-branded-applications/MelodyMax"),
        ServiceItem("support", "المساعدة", "خدمة الزبائن آسياسيل", "الدعم والمساعدة", "اتصل بمركز خدمة الزبائن أو افتح صفحة الدعم الرسمية.", "بحسب تعرفة الاتصال بخطتك", "لا تشارك رموز التحقق أو PIN مع أي شخص.", "الاتصال 111", Activation(ActionMode.DIAL_NUMBER, "111"), "https://www.asiacell.com/personal/support")
    )
}

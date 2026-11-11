package com.example.persiankeyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast

data class SymItem(val symbol: String, val meaning: String = "")

class PersianKeyboardService : InputMethodService() {

    private var isPersian = true
    private var isCapsLock = false
    private var isShifted = false
    private var currentSymbolPage = 0 
    private var lastShiftClickTime = 0L

    private lateinit var sharedPrefs: android.content.SharedPreferences
    private val learnedWords = mutableSetOf<String>()

    private val kashida = "ـ" // کشیده

    private val baseDictionary = listOf(
        "من", "تو", "او", "ما", "شما", "آنها", "خودم", "خودت", "خودش", "چه", "چی", "کجا", "کی", "چرا", "چطور", "چگونه", "کدام", "آیا", "چند", "سلام", "درود", "خداحافظ", "صبح", "بخیر", "ظهر", "عصر", "شب", "روز", "خوش", "آمدی", "سلامت", "باشی", "حال", "احوال", "خوبی", "ممنون", "مرسی", "سپاس", "لطفا", "ببخشید", "معذرت", "خواهش", "بله", "خیر", "نه", "آره", "باشه", "چشم", "حتما", "شاید", "مطمئن", "واقعا", "البته", "هستم", "هستی", "هست", "هستیم", "هستید", "هستند", "نیستم", "نیستی", "نیست", "بودم", "بودی", "بود", "دارم", "داری", "دارد", "میروم", "میروی", "میرود", "رفتم", "رفتی", "رفت", "میکنم", "میکنی", "میکند", "کردم", "کردی", "کرد", "میخورم", "میخوری", "میخورد", "خوردم", "خوردی", "خورد", "میگویم", "میگویی", "میگوید", "گفتم", "گفتی", "گفت", "میخواهم", "میخواهی", "میخواهد", "خواستم", "خواستی", "خواست", "میدانم", "میدانی", "میداند", "دانستم", "دانستی", "دانست", "میبینم", "میبینی", "میبیند", "دیدم", "دیدی", "دید", "میشوم", "میشوی", "میشود", "شدم", "شدی", "شد", "برو", "بیا", "بخور", "بگو", "ببین", "بده", "بگیر", "بنویس", "بخوان", "و", "در", "به", "از", "که", "این", "را", "با", "است", "برای", "آن", "یک", "تا", "هم", "یا", "بر", "اما", "اگر", "چون", "پس", "ولی", "زیرا", "خیلی", "بسیار", "زیاد", "کم", "بزرگ", "کوچک", "خوب", "بد", "زیبا", "زشت", "جدید", "قدیمی", "سریع", "آهسته", "دقیق", "درست", "غلط", "مهم", "ساده", "پیچیده", "سخت", "آسان", "شاد", "غمگین", "خسته", "گرسنه", "تشنه", "گرم", "سرد", "نرم", "سفت", "سنگین", "سبک", "پهن", "باریک", "بلند", "کوتاه", "گران", "ارزان", "قوی", "ضعیف", "سالم", "بیمار", "ناراحت", "عصبانی", "آرام", "روشن", "تاریک", "تمیز", "کثیف", "پر", "خالی", "عزیز", "عزیزم", "جان", "دل", "دلم", "قلب", "قلبم", "عشق", "عاشق", "دوست", "دوستت", "دارم", "دوستش", "هیجان", "احساس", "مهربان", "مهربانی", "خوبی", "بدی", "بغل", "آغوش", "بوسه", "لبخند", "گریه", "خنده", "امید", "آرزو", "رویا", "واقعیت", "زندگی", "مرگ", "تولد", "جشن", "عروسی", "کار", "پول", "خرید", "فروش", "بازار", "شهر", "روستا", "کشور", "دنیا", "جهان", "ماشین", "اتوبوس", "قطار", "هواپیما", "تاکسی", "خیابان", "کوچه", "پارک", "ترافیک", "خانه", "مدرسه", "دانشگاه", "اداره", "بیمارستان", "فروشگاه", "رستوران", "هتل", "فرودگاه", "ایستگاه", "بانک", "پست", "پلیس", "کتابخانه", "موزه", "سینما", "ورزشگاه", "استخر", "مادر", "پدر", "برادر", "خواهر", "خانواده", "فرزند", "بچه", "دختر", "پسر", "زن", "مرد", "همسر", "عمو", "دایی", "خاله", "عمه", "پدربزرگ", "مادربزرگ", "رفیق", "همکار", "همکلاسی", "همسایه", "استاد", "معلم", "شاگرد", "دانشجو", "رئیس", "کارمند", "دکتر", "مهندس", "امروز", "دیروز", "فردا", "الان", "اکنون", "بعد", "قبل", "هفته", "ماه", "سال", "ساعت", "دقیقه", "ثانیه", "زود", "دیر", "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "کتاب", "دفتر", "قلم", "مداد", "کاغذ", "میز", "صندلی", "در", "پنجره", "دیوار", "اتاق", "تخت", "کمد", "فرش", "لامپ", "کلید", "قفل", "چاقو", "قاشق", "چنگال", "بشقاب", "لیوان", "فنجان", "کتری", "یخچال", "اجاق", "تلویزیون", "رادیو", "بلندگو", "هدفون", "دوربین", "عینک", "کیف", "کفش", "لباس", "پیراهن", "شلوار", "کاپشن", "کلاه", "دستکش", "جوراب", "حوله", "صابون", "شامپو", "کامپیوتر", "لپ‌تاپ", "گوشی", "موبایل", "تبلت", "اینترنت", "شبکه", "برنامه", "نرم‌افزار", "سخت‌افزار", "سایت", "وب", "ایمیل", "پیام", "پیامک", "تلگرام", "واتساپ", "اینستاگرام", "فیسبوک", "گوگل", "اندروید", "آیفون", "ویندوز", "هوش مصنوعی", "ربات", "داده", "اطلاعات", "دیتابیس", "سرور", "کاربر", "پسورد", "رمز", "امنیت", "پروژه", "کدنویسی", "برنامه‌نویسی", "طراحی", "گرافیک", "عکس", "فیلم", "ویدیو", "صدا", "موسیقی", "غذا", "آب", "چای", "قهوه", "شیر", "نان", "برنج", "گوشت", "مرغ", "ماهی", "تخم‌مرغ", "پنیر", "کره", "عسل", "سیب", "پرتقال", "موز", "انار", "هندوانه", "خیار", "گوجه", "سیب‌زمینی", "پیاز", "سیر", "فلفل", "نمک", "شکر", "زردچوبه", "دارچین", "زعفران", "صبحانه", "ناهار", "شام", "میان‌وعده", "دسر", "شیرینی", "بستنی", "شکلات", "کیک", "بیسکویت", "آجیل", "پسته", "بادام", "گردو", "فندق", "کشمش", "خرما", "پیتزا", "ساندویچ", "برگر", "پاستا", "ماکارونی", "سوپ", "سالاد", "کباب", "جوجه", "قیمه", "قورمه", "آش", "عدس", "لوبیا", "نخود", "زیتون", "خیارشور", "ترشی", "مربا", "املت", "کوکو", "کتلت", "آسمان", "زمین", "دریا", "کوه", "جنگل", "بیابان", "رودخانه", "دریاچه", "باران", "برف", "باد", "آفتاب", "ماه", "ستاره", "ابر", "رعد", "برق", "گل", "درخت", "برگ", "چمن", "خاک", "سنگ", "آتش", "هوا", "آب و هوا", "طوفان", "زلزله", "سیل", "خشکسالی", "سگ", "گربه", "اسب", "گاو", "گوسفند", "بز", "مرغ", "خروس", "جوجه", "اردک", "غاز", "کبوتر", "گنجشک", "عقاب", "شاهین", "شیر", "ببر", "پلنگ", "گرگ", "روباه", "خرس", "فیل", "زرافه", "میمون", "خرگوش", "موش", "مار", "ماهی", "کوسه", "نهنگ", "دلفین", "لاک‌پشت", "قورباغه", "پروانه", "زنبور", "مورچه", "عنکبوت", "hello", "world", "good", "morning", "night", "book", "house", "computer", "phone", "internet", "keyboard", "software", "hardware", "thank", "you", "please", "sorry", "yes", "no", "maybe", "today", "tomorrow", "yesterday", "love", "friend", "family", "mother", "father", "brother", "sister", "time", "money", "work", "school", "city", "country", "water", "food", "coffee", "tea", "apple", "orange", "happy", "sad", "great", "bad", "new", "old"
    )

    private val nextWordPredictions = mapOf(
        "سلام" to listOf("دنیا", "علیکم", "چطوری", "خوبی"),
        "خدا" to listOf("حافظ", "رو", "شکر", "بزرگ"),
        "چطور" to listOf("هستی", "هستید", "است"),
        "خوب" to listOf("هستی", "است", "بودی", "باشی"),
        "کتاب" to listOf("خانه", "خوب", "خواندن", "خریدم"),
        "ممنون" to listOf("از", "که", "بابت"),
        "صبح" to listOf("بخیر", "زود", "امروز"),
        "شب" to listOf("بخیر", "روز", "آرام"),
        "روز" to listOf("بخیر", "خوبی", "قشنگی"),
        "چه" to listOf("خبر", "خبری", "کار", "طور"),
        "به" to listOf("امید", "دلیل", "خاطر", "من"),
        "از" to listOf("این", "اون", "شما", "من"),
        "دوست" to listOf("دارم", "داری", "دارد", "داریم"),
        "خانه" to listOf("من", "خودم", "ما", "رفتم")
    )

    private val symbolPages = listOf(
        listOf(SymItem("@"), SymItem("#"), SymItem("$"), SymItem("%"), SymItem("&"), SymItem("*"), SymItem("-"), SymItem("+"), SymItem("="), SymItem("/"), SymItem("("), SymItem(")"), SymItem("["), SymItem("]"), SymItem("{"), SymItem("}"), SymItem("<"), SymItem(">"), SymItem("«"), SymItem("»"), SymItem("!"), SymItem("؟"), SymItem("،"), SymItem("؛"), SymItem(":"), SymItem(";"), SymItem("'"), SymItem("\""), SymItem("_"), SymItem("\\"), SymItem("^"), SymItem("~"), SymItem("|"), SymItem("•"), SymItem("…"), SymItem("٪"), SymItem("ریال"), SymItem("€"), SymItem("£"), SymItem("¥")),
        listOf(SymItem("ꕥ"), SymItem("𓉱"), SymItem("☼"), SymItem("𓁈"), SymItem("𓀛"), SymItem("𓄂"), SymItem("𓆃"), SymItem("☼"), SymItem("𓉱"), SymItem("🃜"), SymItem("𒄆"), SymItem("𓁹"), SymItem("✞"), SymItem("𒀱"), SymItem("✞"), SymItem("𓁹"), SymItem("𒄆"), SymItem("🃚"), SymItem("🃖"), SymItem("🃁"), SymItem("🂭"), SymItem("🂺"), SymItem("𓁈"), SymItem("𓃠"), SymItem("𓆃"), SymItem("☥"), SymItem("𓅓"), SymItem("𓆣"), SymItem("𓂀"), SymItem("𓋹"), SymItem("l"), SymItem("𓃮"), SymItem("☼"), SymItem("𓆰"), SymItem("♕"), SymItem("𓆪"), SymItem("𓀂"), SymItem("𓀬"), SymItem("𓁅"), SymItem("𓃂")),
        listOf(SymItem("𓄆"), SymItem("𓃻"), SymItem("𖤍"), SymItem("𓃰"), SymItem("𓆉"), SymItem("𓃮"), SymItem("𓅭"), SymItem("𓆈"), SymItem("𓆙"), SymItem("𓆈"), SymItem("𓅆"), SymItem("☫"), SymItem("𓆗"), SymItem("𖤓"), SymItem("🦋⃝"), SymItem("𓇼"), SymItem("⋆.˚"), SymItem("𓆝"), SymItem("𓆡"), SymItem("𝐕"), SymItem("⃤"), SymItem("✵⃝⃟"), SymItem("⋆.˚🦋༘⋆"), SymItem("𓇢𓆸"), SymItem("ूाीू"), SymItem("☪"), SymItem("𓅇"), SymItem("✡"), SymItem("𓃬"), SymItem("𔓎"), SymItem("✮⃝ 🤍"), SymItem("𓅰"), SymItem("𓅬"), SymItem("𓅭"), SymItem("𓅮"), SymItem("𓅯"), SymItem("☆.𓋼𓍊 𓆏 𓍊𓋼𓍊.☆"), SymItem("🦈"), SymItem("☣︎"), SymItem("❦")),
        listOf(SymItem("𓅂"), SymItem("✞"), SymItem("𓃓"), SymItem("𓃗"), SymItem("⋆⭒˚.⋆🪐 ⋆⭒˚.⋆"), SymItem("𓏲ּ𝄢"), SymItem("♡ ᪲᪲᪲"), SymItem("𓁇𓁋"), SymItem("𓃵"), SymItem("✌︎︎"), SymItem(".☘︎ ݁˖"), SymItem("𓆰𓆪"), SymItem("𓆩𓆪"), SymItem("☕︎"), SymItem("▶︎ •၊၊||၊|။||||||• ۰:۱۰"), SymItem("୧⍤⃝🌹"), SymItem("☠"), SymItem("𝄞"), SymItem("𝐕ℳ𝓊𝓈𝒾𝒸𓂃✍︎𝄞"), SymItem("♡"), SymItem("♢"), SymItem("♤"), SymItem("♧"), SymItem("♣"), SymItem("♦"), SymItem("⚀"), SymItem("⚁"), SymItem("⚂"), SymItem("⚃"), SymItem("⚄"), SymItem("⚅"), SymItem("♚"), SymItem("♙"), SymItem("♘"), SymItem("♗"), SymItem("♖"), SymItem("♕"), SymItem("♔"), SymItem("♟"), SymItem("♞")),
        listOf(SymItem("𒀀", "a"), SymItem("𒀁", "i"), SymItem("𒀂", "u"), SymItem("𒅗", "k"), SymItem("𒂊", "ko"), SymItem("𒄩", "x"), SymItem("𒄀", "g"), SymItem("𒄖", "go"), SymItem("𒁺", "ch"), SymItem("𒋾", "thra"), SymItem("𒃮", "j"), SymItem("𒄿", "je"), SymItem("𒋫", "t"), SymItem("𒌈", "to"), SymItem("𒌅", "th"), SymItem("𒁕", "d"), SymItem("𒁲", "de"), SymItem("𒁺", "do"), SymItem("𒉿", "p"), SymItem("𒆰", "f"), SymItem("𒁀", "b"), SymItem("𒈾", "n"), SymItem("𒉡", "no"), SymItem("𒈬", "m"), SymItem("𒈨", "me"), SymItem("𒈬", "mo"), SymItem("𒅀", "y"), SymItem("𒉿", "v"), SymItem("𒃶", "ve"), SymItem("𒊒", "r"), SymItem("𒊑", "ro"), SymItem("𒇴", "L"), SymItem("𒊓", "s"), SymItem("𒍝", "z"), SymItem("𒊭", "sh"), SymItem("𒄩", "h"), SymItem("𒀀", "1"), SymItem("𒀁", "2"), SymItem("𒀂", "5"), SymItem("𒌋", "10"), SymItem("𒐏", "20"), SymItem("𒐕", "100")),
        listOf(SymItem("➊"), SymItem("➋"), SymItem("➌"), SymItem("➍"), SymItem("➎"), SymItem("➏"), SymItem("➐"), SymItem("➑"), SymItem("➒"), SymItem("➓"), SymItem("♝"), SymItem("♜"), SymItem("♛"), SymItem("✄"), SymItem("♠︎"), SymItem("♥︎"), SymItem("♦︎"), SymItem("♣︎"), SymItem("❤︎"), SymItem("⛓︎"), SymItem("❦"), SymItem("❧"), SymItem("☙"), SymItem("❥"), SymItem("♡"), SymItem("♥"), SymItem("❣"), SymItem("☚"), SymItem("☛"), SymItem("☜"), SymItem("☞"), SymItem("☬"), SymItem("☮"), SymItem("☪"), SymItem("☥"), SymItem("☤"), SymItem("𖣘"), SymItem("✵"), SymItem("࿈"), SymItem("༒"))
    )

    override fun onCreate() {
        super.onCreate()
        sharedPrefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)
        learnedWords.addAll(sharedPrefs.getStringSet("learned_words", emptySet()) ?: emptySet())
    }

    private fun saveWordToMemory(word: String) {
        if (word.isNotBlank()) {
            learnedWords.add(word.trim())
            sharedPrefs.edit().putStringSet("learned_words", learnedWords).apply()
        }
    }

    // تابع اصلاح شده برای نمایش کادر بازشو بدون بسته شدن کیبورد
    private fun showPopup(anchorView: View, items: List<String>) {
        val context = anchorView.context
        val popupView = layoutInflater.inflate(R.layout.key_popup, null)
        val container = popupView.findViewById<LinearLayout>(R.id.popup_container)
        
        // مهم‌ترین تغییر: فوکوس را false کردیم تا کیبورد غیب نشود
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false 
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.parseColor("#212121")))
        popupWindow.isOutsideTouchable = true

        for (item in items) {
            val tv = TextView(context)
            tv.text = item
            tv.textSize = 22f
            tv.setTextColor(Color.WHITE)
            tv.gravity = Gravity.CENTER
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 8, 8, 8)
            tv.layoutParams = params
            tv.setPadding(24, 16, 24, 16)
            tv.setBackgroundColor(Color.parseColor("#37474F"))

            tv.setOnClickListener {
                currentInputConnection?.commitText(item, 1)
                popupWindow.dismiss()
            }
            container.addView(tv)
        }

        val closeTv = TextView(context)
        closeTv.text = "✖"
        closeTv.textSize = 20f
        closeTv.setTextColor(Color.WHITE)
        closeTv.gravity = Gravity.CENTER
        closeTv.setPadding(24, 16, 24, 16)
        closeTv.setBackgroundColor(Color.parseColor("#37474F"))
        closeTv.setOnClickListener { popupWindow.dismiss() }
        container.addView(closeTv)

        // محاسبه دقیق موقعیت برای باز شدن کادر بالای دکمه
        anchorView.post {
            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)
            val x = location[0]
            val y = location[1]
            
            val popupHeight = if (popupWindow.contentView.measuredHeight > 0) popupWindow.contentView.measuredHeight else 150
            val yPos = y - popupHeight - 10 
            
            if (yPos > 0) {
                popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, yPos)
            } else {
                popupWindow.showAsDropDown(anchorView)
            }
        }
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        
        val lettersContainer = view.findViewById<LinearLayout>(R.id.letters_container)
        val symbolsContainer = view.findViewById<LinearLayout>(R.id.symbols_container)
        val diacriticsContainer = view.findViewById<LinearLayout>(R.id.diacritics_container)
        val langBtn = view.findViewById<TextView>(R.id.btn_lang)
        val pageBtn = view.findViewById<TextView>(R.id.btn_page_change)
        val shiftBtn = view.findViewById<TextView>(R.id.btn_shift)
        
        val sug1 = view.findViewById<TextView>(R.id.suggestion_1)
        val sug2 = view.findViewById<TextView>(R.id.suggestion_2)
        val sug3 = view.findViewById<TextView>(R.id.suggestion_3)
        val suggestions = listOf(sug1, sug2, sug3)

        val extraEnglishKeys = listOf(R.id.btn_jim, R.id.btn_che, R.id.btn_pe, R.id.btn_kaf, R.id.btn_gaf)

        fun updateShiftUI() {
            if (isCapsLock) {
                shiftBtn.setBackgroundColor(Color.parseColor("#26A69A"))
            } else if (isShifted) {
                shiftBtn.setBackgroundColor(Color.parseColor("#80CBC4"))
            } else {
                shiftBtn.setBackgroundResource(R.drawable.key_bg_special)
            }
        }

        fun updateSuggestions() {
            val text = currentInputConnection?.getTextBeforeCursor(50, 0)?.toString() ?: ""
            val words = text.split(" ", "\n", ".", "،", "؟", "!")
            val lastWord = words.lastOrNull() ?: ""
            val previousWord = if (words.size > 1) words[words.size - 2].trim() else ""

            val currentSuggestions = mutableListOf<String>()

            if (lastWord.isEmpty()) {
                if (previousWord.isNotEmpty() && nextWordPredictions.containsKey(previousWord)) {
                    currentSuggestions.addAll(nextWordPredictions[previousWord]!!)
                }
                if (previousWord.length > 1 && !learnedWords.contains(previousWord) && !baseDictionary.contains(previousWord)) {
                    currentSuggestions.add(previousWord)
                }
            } else {
                val learnedMatches = learnedWords.filter { it.startsWith(lastWord) && it != lastWord }
                val baseMatches = baseDictionary.filter { it.startsWith(lastWord) && it != lastWord }
                currentSuggestions.addAll(learnedMatches)
                currentSuggestions.addAll(baseMatches)
                
                if (lastWord.length >= 2 && !learnedWords.contains(lastWord) && !baseDictionary.contains(lastWord)) {
                    currentSuggestions.add(0, lastWord)
                }
            }

            val distinctSuggestions = currentSuggestions.distinct().take(3)

            suggestions.forEachIndexed { index, textView ->
                if (index < distinctSuggestions.size) {
                    val word = distinctSuggestions[index]
                    textView.text = word
                    
                    if (word == lastWord && !learnedWords.contains(word)) {
                        textView.setTextColor(Color.parseColor("#FFD54F"))
                    } else {
                        textView.setTextColor(Color.parseColor("#ECEFF1"))
                    }

                    textView.setOnClickListener {
                        if (word == lastWord) {
                            saveWordToMemory(word)
                            Toast.makeText(this, "کلمه «$word» ذخیره شد", Toast.LENGTH_SHORT).show()
                            currentInputConnection?.commitText(" ", 1)
                        } else {
                            currentInputConnection?.deleteSurroundingText(lastWord.length, 0)
                            currentInputConnection?.commitText(word + " ", 1)
                            saveWordToMemory(word)
                        }
                        updateSuggestions()
                    }
                } else {
                    textView.text = ""
                    textView.setTextColor(Color.parseColor("#ECEFF1"))
                    textView.setOnClickListener(null)
                }
            }
        }

        fun bindLetters() {
            val buttons = if (isPersian) {
                extraEnglishKeys.forEach { view.findViewById<TextView>(it)?.visibility = View.VISIBLE }
                mapOf(
                    R.id.btn_zad to "ض", R.id.btn_sad to "ص", R.id.btn_se to "ث", R.id.btn_ghaf to "ق", R.id.btn_fe to "ف", R.id.btn_ghain to "غ", R.id.btn_e to "ع", R.id.btn_he to "ه", R.id.btn_khe to "خ", R.id.btn_h to "ح", R.id.btn_jim to "ج", R.id.btn_che to "چ",
                    R.id.btn_shin to "ش", R.id.btn_sin to "س", R.id.btn_ye to "ی", R.id.btn_be to "ب", R.id.btn_lam to "ل", R.id.btn_alef to "ا", R.id.btn_te to "ت", R.id.btn_nun to "ن", R.id.btn_mim to "م", R.id.btn_pe to "پ",
                    R.id.btn_za to "ظ", R.id.btn_ta to "ط", R.id.btn_ze to "ز", R.id.btn_re to "ر", R.id.btn_zal to "ذ", R.id.btn_dal to "د", R.id.btn_vav to "و", R.id.btn_kaf to "ک", R.id.btn_gaf to "گ"
                )
            } else {
                extraEnglishKeys.forEach { view.findViewById<TextView>(it)?.visibility = View.GONE }
                val baseMap = mapOf(
                    R.id.btn_zad to "q", R.id.btn_sad to "w", R.id.btn_se to "e", R.id.btn_ghaf to "r", R.id.btn_fe to "t", R.id.btn_ghain to "y", R.id.btn_e to "u", R.id.btn_he to "i", R.id.btn_khe to "o", R.id.btn_h to "p",
                    R.id.btn_shin to "a", R.id.btn_sin to "s", R.id.btn_ye to "d", R.id.btn_be to "f", R.id.btn_lam to "g", R.id.btn_alef to "h", R.id.btn_te to "j", R.id.btn_nun to "k", R.id.btn_mim to "l",
                    R.id.btn_za to "z", R.id.btn_ta to "x", R.id.btn_ze to "c", R.id.btn_re to "v", R.id.btn_zal to "b", R.id.btn_dal to "n", R.id.btn_vav to "m"
                )
                if (isCapsLock || isShifted) baseMap.mapValues { it.value.uppercase() } else baseMap
            }
            
            for ((id, text) in buttons) { 
                val btn = view.findViewById<TextView>(id)
                btn.text = text 
                btn.setOnClickListener { 
                    currentInputConnection?.commitText(text, 1) 
                    if (!isPersian && isShifted && !isCapsLock) {
                        isShifted = false
                        bindLetters()
                        updateShiftUI()
                    }
                    updateSuggestions()
                } 
            }
            
            if (isPersian) {
                view.findViewById<TextView>(R.id.btn_alef)?.setOnLongClickListener {
                    showPopup(it, listOf("آ", "أ", "إ", "ٱ", "ا", "ا" + kashida.repeat(2), "ا" + kashida.repeat(4)))
                    true
                }
                view.findViewById<TextView>(R.id.btn_te)?.setOnLongClickListener {
                    showPopup(it, listOf("ت", "ت" + kashida, "ت" + kashida.repeat(2), "ت" + kashida.repeat(3), "-1-", "-5-", "-10-", "-20-"))
                    true
                }
                view.findViewById<TextView>(R.id.btn_ye)?.setOnLongClickListener {
                    showPopup(it, listOf("ي", "ى", "ئ", "ی", "ی" + kashida.repeat(2), "ی" + kashida.repeat(4)))
                    true
                }
                view.findViewById<TextView>(R.id.btn_kaf)?.setOnLongClickListener {
                    showPopup(it, listOf("ك", "ک", "ک" + kashida.repeat(2), "ک" + kashida.repeat(4)))
                    true
                }
                view.findViewById<TextView>(R.id.btn_he)?.setOnLongClickListener {
                    showPopup(it, listOf("ة", "ه", "ۀ", "ه" + kashida.repeat(2), "ه" + kashida.repeat(4)))
                    true
                }
            }
        }

        fun bindSymbols() {
            val pageData = symbolPages[currentSymbolPage]
            val symbolIds = listOf(R.id.sym_1, R.id.sym_2, R.id.sym_3, R.id.sym_4, R.id.sym_5, R.id.sym_6, R.id.sym_7, R.id.sym_8, R.id.sym_9, R.id.sym_10, R.id.sym_11, R.id.sym_12, R.id.sym_13, R.id.sym_14, R.id.sym_15, R.id.sym_16, R.id.sym_17, R.id.sym_18, R.id.sym_19, R.id.sym_20, R.id.sym_21, R.id.sym_22, R.id.sym_23, R.id.sym_24, R.id.sym_25, R.id.sym_26, R.id.sym_27, R.id.sym_28, R.id.sym_29, R.id.sym_30, R.id.sym_31, R.id.sym_32, R.id.sym_33, R.id.sym_34, R.id.sym_35, R.id.sym_36, R.id.sym_37, R.id.sym_38, R.id.sym_39, R.id.sym_40)

            for (i in symbolIds.indices) {
                val btn = view.findViewById<TextView>(symbolIds[i])
                val item = pageData[i]
                if (item.symbol.isNotEmpty()) {
                    val fullText = if (item.meaning.isNotEmpty()) "${item.symbol}\n${item.meaning}" else item.symbol
                    val spannable = SpannableString(fullText)
                    if (item.meaning.isNotEmpty()) {
                        spannable.setSpan(RelativeSizeSpan(1.4f), 0, item.symbol.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        spannable.setSpan(RelativeSizeSpan(0.6f), item.symbol.length, fullText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    btn.text = spannable
                    btn.setOnClickListener { currentInputConnection?.commitText(item.symbol, 1) }
                } else {
                    btn.text = ""; btn.setOnClickListener(null)
                }
            }
            pageBtn.text = "${currentSymbolPage + 1}/6"
        }

        bindLetters(); bindSymbols()

        langBtn.setOnClickListener { 
            isPersian = !isPersian 
            isCapsLock = false
            isShifted = false
            bindLetters() 
            updateSuggestions()
        }

        shiftBtn.setOnClickListener {
            if (!isPersian) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShiftClickTime < 300) {
                    isCapsLock = !isCapsLock
                    isShifted = false 
                } else {
                    if (isCapsLock) {
                        isCapsLock = false
                        isShifted = false
                    } else {
                        isShifted = !isShifted
                    }
                }
                lastShiftClickTime = currentTime
                bindLetters()
            }
        }

        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null
        val backspaceSetup = { btn: TextView ->
            btn.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        val sel = currentInputConnection?.getSelectedText(0)
                        if (sel.isNullOrEmpty()) currentInputConnection?.deleteSurroundingText(1, 0)
                        else currentInputConnection?.commitText("", 1)
                        
                        updateSuggestions() 
                        
                        runnable = object : Runnable {
                            override fun run() {
                                val selText = currentInputConnection?.getSelectedText(0)
                                if (selText.isNullOrEmpty()) currentInputConnection?.deleteSurroundingText(1, 0)
                                else currentInputConnection?.commitText("", 1)
                                updateSuggestions()
                                handler.postDelayed(this, 50)
                            }
                        }
                        handler.postDelayed(runnable!!, 400) 
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        runnable?.let { handler.removeCallbacks(it) }
                        true
                    }
                    else -> false
                }
            }
        }
        backspaceSetup(view.findViewById(R.id.btn_backspace))
        backspaceSetup(view.findViewById(R.id.btn_backspace_sym))

        listOf(R.id.btn_space, R.id.btn_space_sym).forEach { 
            view.findViewById<TextView>(it)?.setOnClickListener { 
                currentInputConnection?.commitText(" ", 1) 
                if (!isPersian && !isCapsLock) {
                    isShifted = true
                    bindLetters()
                }
                updateSuggestions() 
            } 
        }

        view.findViewById<TextView>(R.id.btn_enter)?.setOnClickListener { 
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)) 
            if (!isPersian && !isCapsLock) {
                isShifted = true
                bindLetters()
            }
            updateSuggestions()
        }
        
        view.findViewById<TextView>(R.id.btn_dot)?.setOnClickListener { 
            currentInputConnection?.commitText(".", 1) 
            updateSuggestions()
        }

        view.findViewById<TextView>(R.id.btn_dot)?.setOnLongClickListener {
            lettersContainer.visibility = View.GONE
            diacriticsContainer.visibility = View.VISIBLE
            true
        }
        view.findViewById<TextView>(R.id.btn_close_diacritics)?.setOnClickListener {
            diacriticsContainer.visibility = View.GONE
            lettersContainer.visibility = View.VISIBLE
        }
        
        val diacriticsMap = mapOf(R.id.diacritic_fatha to "َ", R.id.diacritic_kasra to "ِ", R.id.diacritic_damma to "ُ", R.id.diacritic_shadda to "ّ", R.id.diacritic_sukun to "ْ", R.id.diacritic_fathatan to "ً", R.id.diacritic_kasratan to "ٍ", R.id.diacritic_dammatan to "ٌ", R.id.diacritic_alef_kh to "ٰ")
        for ((id, symbol) in diacriticsMap) {
            view.findViewById<TextView>(id)?.setOnClickListener {
                currentInputConnection?.commitText(symbol, 1)
                diacriticsContainer.visibility = View.GONE
                lettersContainer.visibility = View.VISIBLE
                updateSuggestions()
            }
        }

        view.findViewById<TextView>(R.id.btn_symbols)?.setOnClickListener { lettersContainer.visibility = View.GONE; symbolsContainer.visibility = View.VISIBLE; currentSymbolPage = 0; bindSymbols() }
        view.findViewById<TextView>(R.id.btn_lang_back)?.setOnClickListener { symbolsContainer.visibility = View.GONE; lettersContainer.visibility = View.VISIBLE }
        pageBtn.setOnClickListener { currentSymbolPage++; if (currentSymbolPage >= 6) currentSymbolPage = 0; bindSymbols() }

        view.findViewById<TextView>(R.id.btn_emoji_bar)?.setOnClickListener { Toast.makeText(this, "پنل ایموجی به زودی...", Toast.LENGTH_SHORT).show() }
        view.findViewById<TextView>(R.id.btn_settings_bar)?.setOnClickListener { Toast.makeText(this, "تنظیمات کیبورد به زودی...", Toast.LENGTH_SHORT).show() }
        view.findViewById<TextView>(R.id.btn_mic)?.setOnClickListener { Toast.makeText(this, "تایپ صوتی به زودی...", Toast.LENGTH_SHORT).show() }

        return view
    }
}

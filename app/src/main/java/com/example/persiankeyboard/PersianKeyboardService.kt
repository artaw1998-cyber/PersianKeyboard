package com.example.persiankeyboard

import android.inputmethodservice.InputMethodService
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.os.Handler
import android.os.Looper

data class SymItem(val symbol: String, val meaning: String = "")

class PersianKeyboardService : InputMethodService() {

    private var isPersian = true
    private var isShifted = false
    private var currentSymbolPage = 0 

    // لیست کامل ۶ صفحه نماد
    private val symbolPages = listOf(
        listOf(SymItem("@"), SymItem("#"), SymItem("$"), SymItem("%"), SymItem("&"), SymItem("*"), SymItem("-"), SymItem("+"), SymItem("="), SymItem("/"), SymItem("("), SymItem(")"), SymItem("["), SymItem("]"), SymItem("{"), SymItem("}"), SymItem("<"), SymItem(">"), SymItem("«"), SymItem("»"), SymItem("!"), SymItem("؟"), SymItem("،"), SymItem("؛"), SymItem(":"), SymItem(";"), SymItem("'"), SymItem("\""), SymItem("_"), SymItem("\\"), SymItem("^"), SymItem("~"), SymItem("|"), SymItem("•"), SymItem("…"), SymItem("٪"), SymItem("ریال"), SymItem("€"), SymItem("£"), SymItem("¥")),
        listOf(SymItem("ꕥ"), SymItem("𓉱"), SymItem("☼"), SymItem("𓁈"), SymItem("𓀛"), SymItem("𓄂"), SymItem("𓆃"), SymItem("☼"), SymItem("𓉱"), SymItem("🃜"), SymItem("𒄆"), SymItem("𓁹"), SymItem("✞"), SymItem("𒀱"), SymItem("✞"), SymItem("𓁹"), SymItem("𒄆"), SymItem("🃚"), SymItem("🃖"), SymItem("🃁"), SymItem("🂭"), SymItem("🂺"), SymItem("𓁈"), SymItem("𓃠"), SymItem("𓆃"), SymItem("☥"), SymItem("𓅓"), SymItem("𓆣"), SymItem("𓂀"), SymItem("𓋹"), SymItem("l"), SymItem("𓃮"), SymItem("☼"), SymItem("𓆰"), SymItem("♕"), SymItem("𓆪"), SymItem("𓀂"), SymItem("𓀬"), SymItem("𓁅"), SymItem("𓃂")),
        listOf(SymItem("𓄆"), SymItem("𓃻"), SymItem("𖤍"), SymItem("𓃰"), SymItem("𓆉"), SymItem("𓃮"), SymItem("𓅭"), SymItem("𓆈"), SymItem("𓆙"), SymItem("𓆈"), SymItem("𓅆"), SymItem("☫"), SymItem("𓆗"), SymItem("𖤓"), SymItem("🦋⃝"), SymItem("𓇼"), SymItem("⋆.˚"), SymItem("𓆝"), SymItem("𓆡"), SymItem("𝐕"), SymItem("⃤"), SymItem("✵⃝⃟"), SymItem("⋆.˚🦋༘⋆"), SymItem("𓇢𓆸"), SymItem("ूाीू"), SymItem("☪"), SymItem("𓅇"), SymItem("✡"), SymItem("𓃬"), SymItem("𔓎"), SymItem("✮⃝ 🤍"), SymItem("𓅰"), SymItem("𓅬"), SymItem("𓅭"), SymItem("𓅮"), SymItem("𓅯"), SymItem("☆.𓋼𓍊 𓆏 𓍊𓋼𓍊.☆"), SymItem("🦈"), SymItem("☣︎"), SymItem("❦")),
        listOf(SymItem("𓅂"), SymItem("✞"), SymItem("𓃓"), SymItem("𓃗"), SymItem("⋆⭒˚.⋆🪐 ⋆⭒˚.⋆"), SymItem("𓏲ּ𝄢"), SymItem("♡ ᪲᪲᪲"), SymItem("𓁇𓁋"), SymItem("𓃵"), SymItem("✌︎︎"), SymItem(".☘︎ ݁˖"), SymItem("𓆰𓆪"), SymItem("𓆩𓆪"), SymItem("☕︎"), SymItem("▶︎ •၊၊||၊|။||||||• ۰:۱۰"), SymItem("୧⍤⃝🌹"), SymItem("☠"), SymItem("𝄞"), SymItem("𝐕ℳ𝓊𝓈𝒾𝒸𓂃✍︎𝄞"), SymItem("♡"), SymItem("♢"), SymItem("♤"), SymItem("♧"), SymItem("♣"), SymItem("♦"), SymItem("⚀"), SymItem("⚁"), SymItem("⚂"), SymItem("⚃"), SymItem("⚄"), SymItem("⚅"), SymItem("♚"), SymItem("♙"), SymItem("♘"), SymItem("♗"), SymItem("♖"), SymItem("♕"), SymItem("♔"), SymItem("♟"), SymItem("♞")),
        listOf(SymItem("𒀀", "a"), SymItem("𒀁", "i"), SymItem("𒀂", "u"), SymItem("𒅗", "k"), SymItem("𒂊", "ko"), SymItem("𒄩", "x"), SymItem("𒄀", "g"), SymItem("𒄖", "go"), SymItem("𒁺", "ch"), SymItem("𒋾", "thra"), SymItem("𒃮", "j"), SymItem("𒄿", "je"), SymItem("𒋫", "t"), SymItem("𒌈", "to"), SymItem("𒌅", "th"), SymItem("𒁕", "d"), SymItem("𒁲", "de"), SymItem("𒁺", "do"), SymItem("𒉿", "p"), SymItem("𒆰", "f"), SymItem("𒁀", "b"), SymItem("𒈾", "n"), SymItem("𒉡", "no"), SymItem("𒈬", "m"), SymItem("𒈨", "me"), SymItem("𒈬", "mo"), SymItem("𒅀", "y"), SymItem("𒉿", "v"), SymItem("𒃶", "ve"), SymItem("𒊒", "r"), SymItem("𒊑", "ro"), SymItem("𒇴", "L"), SymItem("𒊓", "s"), SymItem("𒍝", "z"), SymItem("𒊭", "sh"), SymItem("𒄩", "h"), SymItem("𒀀", "1"), SymItem("𒀁", "2"), SymItem("𒀂", "5"), SymItem("𒌋", "10"), SymItem("𒐏", "20"), SymItem("𒐕", "100")),
        listOf(SymItem("➊"), SymItem("➋"), SymItem("➌"), SymItem("➍"), SymItem("➎"), SymItem("➏"), SymItem("➐"), SymItem("➑"), SymItem("➒"), SymItem("➓"), SymItem("♝"), SymItem("♜"), SymItem("♛"), SymItem("✄"), SymItem("♠︎"), SymItem("♥︎"), SymItem("♦︎"), SymItem("♣︎"), SymItem("❤︎"), SymItem("⛓︎"), SymItem("❦"), SymItem("❧"), SymItem("☙"), SymItem("❥"), SymItem("♡"), SymItem("♥"), SymItem("❣"), SymItem("☚"), SymItem("☛"), SymItem("☜"), SymItem("☞"), SymItem("☬"), SymItem("☮"), SymItem("☪"), SymItem("☥"), SymItem("☤"), SymItem("𖣘"), SymItem("✵"), SymItem("࿈"), SymItem("༒"))
    )

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        
        val lettersContainer = view.findViewById<LinearLayout>(R.id.letters_container)
        val symbolsContainer = view.findViewById<LinearLayout>(R.id.symbols_container)
        val diacriticsContainer = view.findViewById<LinearLayout>(R.id.diacritics_container)
        val langBtn = view.findViewById<TextView>(R.id.btn_lang)
        val pageBtn = view.findViewById<TextView>(R.id.btn_page_change)
        val shiftBtn = view.findViewById<TextView>(R.id.btn_shift)

        // تابع چیدمان حروف (با پشتیبانی از Shift)
        fun bindLetters() {
            val buttons = if (isPersian) {
                mapOf(
                    R.id.btn_zad to "ض", R.id.btn_sad to "ص", R.id.btn_se to "ث", R.id.btn_ghaf to "ق", R.id.btn_fe to "ف", R.id.btn_ghain to "غ", R.id.btn_e to "ع", R.id.btn_he to "ه", R.id.btn_khe to "خ", R.id.btn_h to "ح", R.id.btn_jim to "ج", R.id.btn_che to "چ",
                    R.id.btn_shin to "ش", R.id.btn_sin to "س", R.id.btn_ye to "ی", R.id.btn_be to "ب", R.id.btn_lam to "ل", R.id.btn_alef to "ا", R.id.btn_te to "ت", R.id.btn_nun to "ن", R.id.btn_mim to "م", R.id.btn_pe to "پ",
                    R.id.btn_za to "ظ", R.id.btn_ta to "ط", R.id.btn_ze to "ز", R.id.btn_re to "ر", R.id.btn_zal to "ذ", R.id.btn_dal to "د", R.id.btn_vav to "و", R.id.btn_kaf to "ک", R.id.btn_gaf to "گ"
                )
            } else {
                // چیدمان انگلیسی QWERTY با قابلیت Shift
                val baseMap = mapOf(
                    R.id.btn_zad to "q", R.id.btn_sad to "w", R.id.btn_se to "e", R.id.btn_ghaf to "r", R.id.btn_fe to "t", R.id.btn_ghain to "y", R.id.btn_e to "u", R.id.btn_he to "i", R.id.btn_khe to "o", R.id.btn_h to "p", R.id.btn_jim to "[", R.id.btn_che to "]",
                    R.id.btn_shin to "a", R.id.btn_sin to "s", R.id.btn_ye to "d", R.id.btn_be to "f", R.id.btn_lam to "g", R.id.btn_alef to "h", R.id.btn_te to "j", R.id.btn_nun to "k", R.id.btn_mim to "l", R.id.btn_pe to ";",
                    R.id.btn_za to "z", R.id.btn_ta to "x", R.id.btn_ze to "c", R.id.btn_re to "v", R.id.btn_zal to "b", R.id.btn_dal to "n", R.id.btn_vav to "m", R.id.btn_kaf to ",", R.id.btn_gaf to "."
                )
                // اگر شیفت فعال باشد حروف را بزرگ میکنیم
                if (isShifted) baseMap.mapValues { it.value.uppercase() } else baseMap
            }
            for ((id, text) in buttons) { 
                val btn = view.findViewById<TextView>(id)
                btn.text = text 
                btn.setOnClickListener { 
                    currentInputConnection?.commitText(text, 1) 
                    // بعد از تایپ یک حرف در حالت شیفت، اگر فارسی نبود، شیفت خاموش شود
                    if (!isPersian && isShifted) {
                        isShifted = false
                        bindLetters()
                        shiftBtn.setBackgroundResource(R.drawable.key_bg_special)
                    }
                } 
            }
            langBtn.text = if (isPersian) "Ab" else "فا"
        }

        // تابع چیدمان نمادها
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

        // دکمه تغییر زبان
        langBtn.setOnClickListener { 
            isPersian = !isPersian 
            isShifted = false // ریست کردن شیفت هنگام تغییر زبان
            shiftBtn.setBackgroundResource(R.drawable.key_bg_special)
            bindLetters() 
        }

        // دکمه شیفت
        shiftBtn.setOnClickListener {
            isShifted = !isShifted
            if (isShifted) {
                // اگر شیفت روشن شد، رنگ دکمه را تغییر بده
                shiftBtn.setBackgroundColor(android.graphics.Color.parseColor("#80CBC4"))
            } else {
                shiftBtn.setBackgroundResource(R.drawable.key_bg_special)
            }
            bindLetters()
        }

        // پاک کردن پشت سر هم (نگه داشتن دکمه)
        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null
        val backspaceSetup = { btn: TextView ->
            btn.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        // پاک کردن اولیه
                        val sel = currentInputConnection?.getSelectedText(0)
                        if (sel.isNullOrEmpty()) currentInputConnection?.deleteSurroundingText(1, 0)
                        else currentInputConnection?.commitText("", 1)
                        
                        // شروع تایمر برای پاک کردن سریع
                        runnable = object : Runnable {
                            override fun run() {
                                val selText = currentInputConnection?.getSelectedText(0)
                                if (selText.isNullOrEmpty()) currentInputConnection?.deleteSurroundingText(1, 0)
                                else currentInputConnection?.commitText("", 1)
                                handler.postDelayed(this, 50) // هر ۵۰ میلی ثانیه
                            }
                        }
                        handler.postDelayed(runnable!!, 400) // بعد از ۰.۴ ثانیه شروع به تکرار کن
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

        // رفتن به صفحه نمادها و برگشت
        view.findViewById<TextView>(R.id.btn_symbols)?.setOnClickListener { lettersContainer.visibility = View.GONE; symbolsContainer.visibility = View.VISIBLE; currentSymbolPage = 0; bindSymbols() }
        view.findViewById<TextView>(R.id.btn_lang_back)?.setOnClickListener { symbolsContainer.visibility = View.GONE; lettersContainer.visibility = View.VISIBLE }
        pageBtn.setOnClickListener { currentSymbolPage++; if (currentSymbolPage >= 6) currentSymbolPage = 0; bindSymbols() }

        // فاصله و اینتر
        listOf(R.id.btn_space, R.id.btn_space_sym).forEach { view.findViewById<TextView>(it)?.setOnClickListener { currentInputConnection?.commitText(" ", 1) } }
        view.findViewById<TextView>(R.id.btn_enter)?.setOnClickListener { currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)); currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)) }
        
        // اعداد و نقطه
        val nums = mapOf(R.id.btn_num_1 to "۱", R.id.btn_num_2 to "۲", R.id.btn_num_3 to "۳", R.id.btn_num_4 to "۴", R.id.btn_num_5 to "۵", R.id.btn_num_6 to "۶", R.id.btn_num_7 to "۷", R.id.btn_num_8 to "۸", R.id.btn_num_9 to "۹", R.id.btn_num_0 to "۰")
        for ((id, text) in nums) view.findViewById<TextView>(id)?.setOnClickListener { currentInputConnection?.commitText(text, 1) }
        view.findViewById<TextView>(R.id.btn_dot)?.setOnClickListener { currentInputConnection?.commitText(".", 1) }

        // پیام‌های موقت
        view.findViewById<TextView>(R.id.btn_emoji_bar)?.setOnClickListener { Toast.makeText(this, "پنل ایموجی به زودی...", Toast.LENGTH_SHORT).show() }
        view.findViewById<TextView>(R.id.btn_settings_bar)?.setOnClickListener { Toast.makeText(this, "تنظیمات کیبورد به زودی...", Toast.LENGTH_SHORT).show() }

        // نمایش اعراب
        val dotBtn = view.findViewById<TextView>(R.id.btn_dot)
        dotBtn.setOnLongClickListener { 
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
            }
        }

        return view
    }
}

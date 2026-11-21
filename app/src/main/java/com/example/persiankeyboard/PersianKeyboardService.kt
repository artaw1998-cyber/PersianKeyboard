package com.example.persiankeyboard

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.persiankeyboard.clipboard.ClipboardAdapter
import com.example.persiankeyboard.clipboard.ClipboardDatabase
import com.example.persiankeyboard.clipboard.ClipboardListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SymItem(val symbol: String, val meaning: String = "")

class PersianKeyboardService : InputMethodService() {

    companion object {
        var voiceResultListener: ((String) -> Unit)? = null
    }

    private var isPersian = true
    private var isCapsLock = false
    private var isShifted = false
    private var currentSymbolPage = 0
    private var lastShiftClickTime = 0L

    private lateinit var sharedPrefs: android.content.SharedPreferences
    private val learnedWords = mutableSetOf<String>()
    private val kashida = "ـ"
    
    private var suggestionViews: List<TextView> = emptyList()
    
    private lateinit var clipboardListener: ClipboardListener
    private val db by lazy { ClipboardDatabase.getDatabase(this) }
    
    private var lettersContainer: LinearLayout? = null
    private var symbolsContainer: LinearLayout? = null
    private var diacriticsContainer: LinearLayout? = null
    private var clipboardContainer: LinearLayout? = null
    private var settingsContainer: LinearLayout? = null
    private var emojiContainer: LinearLayout? = null
    
    private var clipboardRecyclerView: RecyclerView? = null
    private var emojiRecyclerView: RecyclerView? = null
    private var emojiAdapter: EmojiAdapter? = null
    private val recentEmojis = mutableListOf<String>()
    private val favoriteEmojis = mutableListOf<String>() 
    private var currentEmojiCategory = "recent" 

    private var editClipboardContainer: LinearLayout? = null
    private var editClipboardText: EditText? = null
    private var editingItem: com.example.persiankeyboard.clipboard.ClipboardEntity? = null

    private var isVibrationEnabled = true
    private var isSoundEnabled = true
    private var currentTheme = "dark"
    private var currentKeyTextSize = 18f
    
    private var toneGenerator: ToneGenerator? = null
    private var keyPreviewPopup: PopupWindow? = null
    private var keyPreviewText: TextView? = null

    private val baseDictionary = listOf(
        "من", "تو", "او", "ما", "شما", "آنها", "خودم", "خودت", "خودش", "چه", "چی", "کجا", "کی", "چرا", "چطور", "چگونه", "کدام", "آیا", "چند", "سلام", "درود", "خداحافظ", "صبح", "بخیر", "ظهر", "عصر", "شب", "روز", "خوش", "آمدی", "سلامت", "باشی", "حال", "احوال", "خوبی", "ممنون", "مرسی", "سپاس", "لطفا", "ببخشید", "معذرت", "خواهش", "بله", "خیر", "نه", "آره", "باشه", "چشم", "حتما", "شاید", "مطمئن", "واقعا", "البته", "هستم", "هستی", "هست", "هستیم", "هستید", "هستند", "نیستم", "نیستی", "نیست", "بودم", "بودی", "بود", "دارم", "داری", "دارد", "میروم", "میروی", "میرود", "رفتم", "رفتی", "رفت", "میکنم", "میکنی", "میکند", "کردم", "کردی", "کرد", "میخورم", "میخوری", "میخورد", "خوردم", "خوردی", "خورد", "میگویم", "میگویی", "میگوید", "گفتم", "گفتی", "گفت", "میخواهم", "میخواهی", "میخواهد", "خواستم", "خواستی", "خواست", "میدانم", "میدانی", "میداند", "دانستم", "دانستی", "دانست", "میبینم", "میبینی", "میبیند", "دیدم", "دیدی", "دید", "میشوم", "میشوی", "میشود", "شدم", "شدی", "شد", "برو", "بیا", "بخور", "بگو", "ببین", "بده", "بگیر", "بنویس", "بخوان", "و", "در", "به", "از", "که", "این", "را", "با", "است", "برای", "آن", "یک", "تا", "هم", "یا", "بر", "اما", "اگر", "چون", "پس", "ولی", "زیرا", "خیلی", "بسیار", "زیاد", "کم", "بزرگ", "کوچک", "خوب", "بد", "زیبا", "زشت", "جدید", "قدیمی", "سریع", "آهسته", "دقیق", "درست", "غلط", "مهم", "ساده", "پیچیده", "سخت", "آسان", "شاد", "غمگین", "خسته", "گرسنه", "تشنه", "گرم", "سرد", "نرم", "سفت", "سنگین", "سبک", "پهن", "باریک", "بلند", "کوتاه", "گران", "ارزان", "قوی", "ضعیف", "سالم", "بیمار", "ناراحت", "عصبانى", "آرام", "روشن", "تاریک", "تمیز", "کثیف", "پر", "خالی", "عزیز", "عزیزم", "جان", "دل", "دلم", "قلب", "قلبم", "عشق", "عاشق", "دوست", "دوستت", "دارم", "دوستش", "هیجان", "احساس", "مهربان", "مهربانی", "خوبی", "بدی", "بغل", "آغوش", "بوسه", "لبخند", "گریه", "خنده", "امید", "آرزو", "رویا", "واقعیت", "زندگی", "مرگ", "تولد", "جشن", "عروسی", "کار", "پول", "خرید", "فروش", "بازار", "شهر", "روستا", "کشور", "دنیا", "جهان", "ماشین", "اتوبوس", "قطار", "هواپیما", "تاکسی", "خیابان", "کوچه", "پارک", "ترافیک", "خانه", "مدرسه", "دانشگاه", "اداره", "بیمارستان", "فروشگاه", "رستوران", "هتل", "فرودگاه", "ایستگاه", "بانک", "پست", "پلیس", "کتابخانه", "موزه", "سینما", "ورزشگاه", "استخر", "مادر", "پدر", "برادر", "خواهر", "خانواده", "فرزند", "بچه", "دختر", "پسر", "زن", "مرد", "همسر", "عمو", "دایی", "خاله", "عمه", "پدربزرگ", "مادربزرگ", "رفیق", "همکار", "همکلاسی", "همسایه", "استاد", "معلم", "شاگرد", "دانشجو", "رئیس", "کارمند", "دکتر", "مهندس", "امروز", "دیروز", "فردا", "الان", "اکنون", "بعد", "قبل", "هفته", "ماه", "سال", "ساعت", "دقیقه", "ثانیه", "زود", "دیر", "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "کتاب", "دفتر", "قلم", "مداد", "کاغذ", "میز", "صندلی", "در", "پنجره", "دیوار", "اتاق", "تخت", "کمد", "فرش", "لامپ", "کلید", "قفل", "چاقو", "قاشق", "چنگال", "بشقاب", "لیوان", "فنجان", "کتری", "یخچال", "اجاق", "تلویزیون", "رادیو", "بلندگو", "هدفون", "دوربین", "عینک", "کیف", "کفش", "لباس", "پیراهن", "شلوار", "کاپشن", "کلاه", "دستکش", "جوراب", "حوله", "صابون", "شامپو", "کامپیوتر", "لپ‌تاپ", "گوشی", "موبایل", "تبلت", "اینترنت", "شبکه", "برنامه", "نرم‌افزار", "سخت‌افزار", "سایت", "وب", "ایمیل", "پیام", "پیامک", "تلگرام", "واتساپ", "اینستاگرام", "فیسبوک", "گوگل", "اندروید", "آیفون", "ویندوز", "هوش مصنوعی", "ربات", "داده", "اطلاعات", "دیتابیس", "سرور", "کاربر", "پسورد", "رمز", "امنیت", "پروژه", "کدنویسی", "برنامه‌نویسی", "طراحی", "گرافیک", "عکس", "فیلم", "ویدیو", "صدا", "موسیقی", "غذا", "آب", "چای", "قهوه", "شیر", "نان", "برنج", "گوشت", "مرغ", "ماهی", "تخم‌مرغ", "پنیر", "کره", "عسل", "سیب", "پرتقال", "موز", "انار", "هندوانه", "خیار", "گوجه", "سیب‌زمینی", "پیاز", "سیر", "فلفل", "نمک", "شکر", "زردچوبه", "دارچین", "زعفران", "صبحانه", "ناهار", "شام", "میان‌وعده", "دسر", "شیرینی", "بستنی", "شکلات", "کیک", "بیسکویت", "آجیل", "پسته", "بادام", "گردو", "فندق", "کشمش", "خرما", "پیتزا", "ساندویچ", "برگر", "پاستا", "ماکارونی", "سوپ", "سالاد", "کباب", "جوجه", "قیمه", "قورمه", "آش", "عدس", "لوبیا", "نخود", "زیتون", "خیارشور", "ترشی", "مربا", "املت", "کوکو", "کتلت", "آسمان", "زمین", "دریا", "کوه", "جنگل", "بیابان", "رودخانه", "دریاچه", "باران", "برف", "باد", "آفتاب", "ماه", "ستاره", "ابر", "رعد", "برق", "گل", "درخت", "برگ", "چمن", "خاک", "سنگ", "آتش", "هوا", "آب و هوا", "طوفان", "زلزله", "سیل", "خشکسالی", "سگ", "گربه", "اسب", "گاو", "گوسفند", "بز", "مرغ", "خروس", "جوجه", "اردک", "غاز", "کبوتر", "گنجشک", "عقاب", "شاهین", "شیر", "ببر", "پلنگ", "گرگ", "روباه", "خرس", "فیل", "زرافه", "میمون", "خرگوش", "موش", "مار", "ماهی", "کوسه", "نهنگ", "دلفین", "لاک‌پشت", "قورباغه", "پروانه", "زنبور", "مورچه", "عنکبوت", "hello", "world", "good", "morning", "night", "book", "house", "computer", "phone", "internet", "keyboard", "software", "hardware", "thank", "you", "please", "sorry", "yes", "no", "maybe", "today", "tomorrow", "yesterday", "love", "friend", "family", "mother", "father", "brother", "sister", "time", "money", "work", "school", "city", "country", "water", "food", "coffee", "tea", "apple", "orange", "happy", "sad", "great", "bad", "new", "old"
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
        listOf(SymItem("ꕥ"), SymItem("𓉱"), SymItem("☼"), SymItem("𓁈"), SymItem("𓀛"), SymItem("𓄂"), SymItem("𓆃"), SymItem("☼"), SymItem("𓉱"), SymItem("🃜"), SymItem("𒄆"), SymItem("𓁹"), SymItem("✞"), SymItem("𒀱"), SymItem("✞"), SymItem("𓁹"), SymItem("𒄆"), SymItem("🃚"), SymItem("🃖"), SymItem("🃁"), SymItem("🂭"), SymItem("🂺"), SymItem("𓁈"), SymItem("𓃠"), SymItem("𓆃"), SymItem("☥"), SymItem("𓅓"), SymItem("𓆣"), SymItem("𓂀"), SymItem("𓋹"), SymItem("l"), SymItem("𓃮"), SymItem("☼"), SymItem("𓆰"), SymItem("♕"), SymItem("𓆪"), SymItem("𐎠"), SymItem("𐎡"), SymItem("𐎢"), SymItem("𐎣")),
        listOf(SymItem("𓄆"), SymItem("𓃻"), SymItem("𖤍"), SymItem("𓃰"), SymItem("𓆉"), SymItem("𓃮"), SymItem("𓅭"), SymItem("𓆈"), SymItem("𓆙"), SymItem("𓆈"), SymItem("𓅆"), SymItem("☫"), SymItem("𓆗"), SymItem("𖤓"), SymItem("🦋⃝"), SymItem("𓇼"), SymItem("⋆.˚"), SymItem("𓆝"), SymItem("𓆡"), SymItem("𝐕"), SymItem("⃤"), SymItem("✵⃝⃟"), SymItem("⋆.˚🦋༘⋆"), SymItem("𓇢𓆸"), SymItem("ूाीू"), SymItem("☪"), SymItem("𓅇"), SymItem("✡"), SymItem("𓃬"), SymItem("𔓎"), SymItem("✮⃝ 🤍"), SymItem("𓅰"), SymItem("𓅬"), SymItem("𓅭"), SymItem("𓅮"), SymItem("𓅯"), SymItem("☆.𓋼𓍊 𓆏 𓍊𓋼𓍊.☆"), SymItem("🦈"), SymItem("☣︎"), SymItem("❦")),
        listOf(SymItem("𓅂"), SymItem("✞"), SymItem("𓃓"), SymItem("𓃗"), SymItem("⋆⭒˚.⋆🪐 ⋆⭒˚.⋆"), SymItem("𓏲ּ𝄢"), SymItem("♡ ᪲᪲᪲"), SymItem("𓁇𓁋"), SymItem("𓃵"), SymItem("✌"), SymItem(".☘︎ ݁˖"), SymItem("𓆰𓆪"), SymItem("𓆩𓆪"), SymItem("☕︎"), SymItem("▶︎ •ِِ•ِِ•ِِ•ِِ•ِِ•ِِ•ِِ•ِِ"), SymItem("୧⍤⃝🌹"), SymItem("☠"), SymItem("𝄞"), SymItem("𝐕ℳ𝓊𝓈𝒾𝒸𓂃✍︎𝄞"), SymItem("♡"), SymItem("♢"), SymItem("♤"), SymItem("♧"), SymItem("♣"), SymItem("♦"), SymItem("⚀"), SymItem("⚁"), SymItem("⚂"), SymItem("⚃"), SymItem("⚄"), SymItem("⚅"), SymItem("♚"), SymItem("♙"), SymItem("♘"), SymItem("♗"), SymItem("♖"), SymItem("♕"), SymItem("♔"), SymItem("♟"), SymItem("♞")),
        // صفحه پنجم (خط میخی): ۳۶ حرف اصلی + ۴ خانه خالی برای هماهنگی کامل با لایه ۴۰تایی استاندارد
        listOf(
            SymItem("𐎠", "a (ا)"), SymItem("𐎡", "i (ای)"), SymItem("𐎢", "u (او)"), SymItem("𐎣", "k (ک)"), SymItem("𐎤", "ko (کُ)"), SymItem("𐎥", "x (خ)"),
            SymItem("𐎦", "g (گ)"), SymItem("𐎧", "go (گُ)"), SymItem("𐎨", "ch (چ)"), SymItem("𐎩", "thra (ثرا)"), SymItem("𐎪", "j (ج)"), SymItem("𐎫", "je (جِ)"),
            SymItem("𐎬", "t (ت)"), SymItem("𐎭", "to (تُ)"), SymItem("𐎮", "th (ط)"), SymItem("𐎯", "d (د)"), SymItem("𐎰", "de (دِ)"), SymItem("𐎱", "do (دُ)"),
            SymItem("𐎲", "p (پ)"), SymItem("𐎳", "f (ف)"), SymItem("𐎴", "b (ب)"), SymItem("𐎵", "n (ن)"), SymItem("𐎶", "no (نُ)"), SymItem("𐎷", "m (م)"),
            SymItem("𐎸", "me (مِ)"), SymItem("𐎹", "mo (مُ)"), SymItem("𐎺", "y (ی)"), SymItem("𐎻", "v (و)"), SymItem("𐎼", "ve (وِ)"), SymItem("𐎽", "r (ر)"),
            SymItem("𐎾", "ro (رُ)"), SymItem("𐎿", "L (ل)"), SymItem("𐏀", "s (س)"), SymItem("𐏁", "z (ز)"), SymItem("𐏂", "sh (ش)"), SymItem("𐏃", "h (ه)"),
            SymItem(""), SymItem(""), SymItem(""), SymItem("")
        ),
        listOf(SymItem("➊"), SymItem("➋"), SymItem("➌"), SymItem("➍"), SymItem("➎"), SymItem("➏"), SymItem("➐"), SymItem("➑"), SymItem("➒"), SymItem("➓"), SymItem("♝"), SymItem("♜"), SymItem("♛"), SymItem("✄"), SymItem("♠︎"), SymItem("♥︎"), SymItem("♦︎"), SymItem("♣︎"), SymItem("❤︎"), SymItem("⛓︎"), SymItem("❦"), SymItem("❧"), SymItem("☙"), SymItem("❥"), SymItem("♡"), SymItem("♥"), SymItem("❣"), SymItem("☚"), SymItem("☛"), SymItem("☜"), SymItem("☞"), SymItem("☬"), SymItem("☮"), SymItem("☪"), SymItem("☥"), SymItem("☤"), SymItem("𖣘"), SymItem("✵"), SymItem("࿈"), SymItem("༒"))
    )

    private val emojisSmileys = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🙂", "🙃", "🫠", "🫫", "😉", "😊", "😇",
        "🥰", "😍", "🤩", "😘", "😗", "☺️", "😚", "😙", "🥲", "😋", "😛", "😜", "🤪", "😝", "🤑",
        "🤗", "🤭", "🫢", "🫣", "🤫", "🤔", "🫡", "🤐", "🤨", "😐", "😑", "😶", "🫥", "😶‍🌫️", "😏",
        "😒", "🙄", "😬", "😮‍💨", "🤥", "🫨", "🙂‍↔️", "🙂‍↕️", "😌", "😔", "😪", "🤤", "😴", "🫩",
        "😷", "🤒", "🤕", "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "😵‍💫", "🤯", "🤠", "🥳", "🥸",
        "😎", "🤓", "🧐", "😕", "🫤", "😟", "🙁", "☹️", "😮", "😯", "😲", "😳", "🫪", "🥺", "🥹",
        "😦", "😧", "😨", "😰", "😥", "😢", "😭", "😱", "😖", "😣", "😞", "😓", "😩", "😫", "🥱",
        "😤", "😡", "😠", "🤬", "😈", "👿", "💀", "☠️", "💩", "🤡", "👹", "👺", "👻", "👽", "👾", "🤖",
        "😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿", "😾", "🙈", "🙉", "🙊",
        "👋", "👋🏻", "👋🏼", "👋🏽", "👋🏾", "👋🏿", "🤚", "🤚🏻", "🤚🏼", "🤚🏽", "🤚🏾", "🤚🏿", "🖐️", "🖐🏻", "🖐🏼", "🖐🏽", "🖐🏾", "🖐🏿", "✋", "✋🏻", "✋🏼", "✋🏽", "✋🏾", "✋🏿", "🖖", "🖖🏻", "🖖🏼", "🖖🏽", "🖖🏾", "🖖🏿", "🫱", "🫱🏻", "🫱🏼", "🫱🏽", "🫱🏾", "🫱🏿", "🫲", "🫲🏻", "🫲🏼", "🫲🏽", "🫲🏾", "🫲🏿", "🫳", "🫳🏻", "🫳🏼", "🫳🏽", "🫳🏾", "🫳🏿", "🫴", "🫴🏻", "🫴🏼", "🫴🏽", "🫴🏾", "🫴🏿", "🫷", "🫷🏻", "🫷🏼", "🫷🏽", "🫷🏾", "🫷🏿", "🫸", "🫸🏻", "🫸🏼", "🫸🏽", "🫸🏾", "🫸🏿",
        "👌", "👌🏻", "👌🏼", "👌🏽", "👌🏾", "👌🏿", "🤌", "🤌🏻", "🤌🏼", "🤌🏽", "🤌🏾", "🤌🏿", "🤏", "🤏🏻", "🤏🏼", "🤏🏽", "🤏🏾", "🤏🏿", "✌️", "✌🏻", "✌🏼", "✌🏽", "✌🏾", "✌🏿", "🤞", "🤞🏻", "🤞🏼", "🤞🏽", "🤞🏾", "🤞🏿", "🫰", "🫰🏻", "🫰🏼", "🫰🏽", "🫰🏾", "🫰🏿", "🤟", "🤟🏻", "🤟🏼", "🤟🏽", "🤟🏾", "🤟🏿", "🤘", "🤘🏻", "🤘🏼", "🤘🏽", "🤘🏾", "🤘🏿", "🤙", "🤙🏻", "🤙🏼", "🤙🏽", "🤙🏾", "🤙🏿",
        "👈", "👈🏻", "👈🏼", "👈🏽", "👈🏾", "👈🏿", "👉", "👉🏻", "👉🏼", "👉🏽", "👉🏾", "👉🏿", "👆", "👆🏻", "👆🏼", "👆🏽", "👆🏾", "👆🏿", "🖕", "🖕🏻", "🖕🏼", "🖕🏽", "🖕🏾", "🖕🏿", "👇", "👇🏻", "👇🏼", "👇🏽", "👇🏾", "👇🏿", "☝️", "☝🏻", "☝🏼", "☝🏽", "☝🏾", "☝🏿", "🫵", "🫵🏻", "🫵🏼", "🫵🏽", "🫵🏾", "🫵🏿",
        "👍", "👍🏻", "👍🏼", "👍🏽", "👍🏾", "👍🏿", "👎", "👎🏻", "👎🏼", "👎🏽", "👎🏾", "👎🏿", "🫹", "🫹🏻", "🫹🏼", "🫹🏽", "🫹🏾", "🫹🏿", "🫺", "🫺🏻", "🫺🏼", "🫺🏽", "🫺🏾", "🫺🏿", "✊", "✊🏻", "✊🏼", "✊🏽", "✊🏾", "✊🏿", "👊", "👊🏻", "👊🏼", "👊🏽", "👊🏾", "👊🏿", "🤛", "🤛🏻", "🤛🏼", "🤛🏽", "🤛🏾", "🤛🏿", "🤜", "🤜🏻", "🤜🏼", "🤜🏽", "🤜🏾", "🤜🏿",
        "👏", "👏🏻", "👏🏼", "👏🏽", "👏🏾", "👏🏿", "🙌", "🙌🏻", "🙌🏼", "🙌🏽", "🙌🏾", "🙌🏿", "🫶", "🫶🏻", "🫶🏼", "🫶🏽", "🫶🏾", "🫶🏿", "👐", "👐🏻", "👐🏼", "👐🏽", "👐🏾", "👐🏿", "🤲", "🤲🏻", "🤲🏼", "🤲🏽", "🤲🏾", "🤲🏿", "🤝", "🤝🏻", "🤝🏼", "🤝🏽", "🤝🏾", "🤝🏿", "🙏", "🙏🏻", "🙏🏼", "🙏🏽", "🙏🏾", "🙏🏿",
        "✍️", "✍🏻", "✍🏼", "✍🏽", "✍🏾", "✍🏿", "💅", "💅🏻", "💅🏼", "💅🏽", "💅🏾", "💅🏿", "🤳", "🤳🏻", "🤳🏼", "🤳🏽", "🤳🏾", "🤳🏿",
        "💪", "💪🏻", "💪🏼", "💪🏽", "💪🏾", "💪🏿", "🦾", "🦿", "🦵", "🦵🏻", "🦵🏼", "🦵🏽", "🦵🏾", "🦵🏿", "🦶", "🦶🏻", "🦶🏼", "🦶🏽", "🦶🏾", "🦶🏿", "👂", "👂🏻", "👂🏼", "👂🏽", "👂🏾", "👂🏿", "🦻", "🦻🏻", "🦻🏼", "🦻🏽", "🦻🏾", "🦻🏿", "👃", "👃🏻", "👃🏼", "👃🏽", "👃🏾", "👃🏿", "🧠", "🫀", "🫁", "🦷", "🦴", "👀", "👁️", "👅", "👄", "🫦",
        "👶", "🧒", "👦", "👧", "🧑", "👱", "👨", "🧔", "🧔‍♂️", "🧔‍♀️", "👩", "🧓", "👴", "👵",
        "🙍", "🙍‍♂️", "🙍‍♀️", "🙎", "🙎‍♂️", "🙎‍♀️", "🙅", "🙅‍♂️", "🙅‍♀️", "🙆", "🙆‍♂️", "🙆‍♀️", "💁", "💁‍♂️", "💁‍♀️", "🙋", "🙋‍♂️", "🙋‍♀️", "🧏", "🧏‍♂️", "🧏‍♀️", "🙇", "🙇‍♂️", "🙇‍♀️", "🤦", "🤦‍♂️", "🤦‍♀️", "🤷", "🤷‍♂️", "🤷‍♀️",
        "👮", "👮‍♂️", "👮‍♀️", "🕵️", "🕵️‍♂️", "🕵️‍♀️", "💂", "💂‍♂️", "💂‍♀️", "🥷", "👷", "👷‍♂️", "👷‍♀️", "🫅", "🤴", "👸", "👳", "👳‍♂️", "👳‍♀️", "👲", "🧕", "🤵", "🤵‍♂️", "🤵‍♀️", "👰", "👰‍♂️", "👰‍♀️", "🤰", "🫃", "🫄", "🤱", "👩‍🍼", "👨‍🍼", "🧑‍🍼",
        "👼", "🎅", "🤶", "🧑‍🎄", "🦸", "🦸‍♂️", "🦸‍♀️", "🦹", "🦹‍♂️", "🦹‍♀️", "🧙", "🧙‍♂️", "🧙‍♀️", "🧚", "🧚‍♂️", "🧚‍♀️", "🧛", "🧛‍♂️", "🧛‍♀️", "🧜", "🧜‍♂️", "🧜‍♀️", "🧝", "🧝‍♂️", "🧝‍♀️", "🧞", "🧞‍♂️", "🧞‍♀️", "🧟", "🧟‍♂️", "🧟‍♀️", "🧌", "🫈",
        "💆", "💆‍♂️", "💆‍♀️", "💇", "💇‍♂️", "💇‍♀️", "🚶", "🚶‍♂️", "🚶‍♀️", "🚶‍➡️", "🧍", "🧍‍♂️", "🧍‍♀️", "🧎", "🧎‍♂️", "🧎‍♀️", "🧎‍➡️", "🏃", "🏃‍♂️", "🏃‍♀️", "🏃‍➡️", "🧑‍🩰", "💃", "🕺", "🕴️", "👯", "👯‍♂️", "👯‍♀️", "🧖", "🧖‍♂️", "🧖‍♀️", "🧗", "🧗‍♂️", "🧗‍♀️",
        "🤺", "🏇", "⛷️", "🏂", "🏌️", "🏌️‍♂️", "🏌️‍♀️", "🏄", "🏄‍♂️", "🏄‍♀️", "🚣", "🚣‍♂️", "🚣‍♀️", "🏊", "🏊‍♂️", "🏊‍♀️", "⛹️", "⛹️‍♂️", "⛹️‍♀️", "🏋️", "🏋️‍♂️", "🏋️‍♀️", "🚴", "🚴‍♂️", "🚴‍♀️", "🚵", "🚵‍♂️", "🚵‍♀️", "🤸", "🤸‍♂️", "🤸‍♀️", "🤼", "🤼‍♂️", "🤼‍♀️", "🤽", "🤽‍♂️", "🤽‍♀️", "🤾", "🤾‍♂️", "🤾‍♀️", "🤹", "🤹‍♂️", "🤹‍♀️", "🧘", "🧘‍♂️", "🧘‍♀️",
        "🛀", "🛌", "🧑‍🤝‍🧑", "👭", "👫", "👬", "💏", "👩‍❤️‍💋‍👨", "👨‍❤️‍💋‍👨", "👩‍❤️‍💋‍👩", "💑", "👩‍❤️‍👨", "👨‍❤️‍👨", "👩‍❤️‍👩", "👪", "🗣️", "👤", "👥", "🫂", "👣", "🫆"
    )

    private val emojisNature = listOf(
        "🐵", "🐒", "🦍", "🦧", "🐶", "🐕", "🦮", "🐕‍🦺", "🐩", "🐺", "🦊", "🦝", "🐱", "🐈", "🐈‍⬛", "🦁", "🐯", "🐅", "🐆", "🐴", "🫎", "🫏", "🐎", "🦄", "🦓", "🦌", "🦬", "🐮", "🐂", "🐃", "🐄", "🐷", "🐖", "🐗", "🐽", "🐏", "🐑", "🐐", "🐪", "🐫", "🦙", "🦒", "🐘", "🦣", "🦏", "🦛", "🐭", "🐁", "🐀", "🐹", "🐰", "🐇", "🐿️", "🦫", "🦔", "🦇", "🐻", "🐻‍❄️", "🐨", "🐼", "🦥", "🦦", "🦨", "🦡", "🦘", "🐾",
        "🦃", "🐔", "🐓", "🐣", "🐤", "🐥", "🐦", "🐧", "🕊️", "🦅", "🦆", "🦢", "🦉", "🦤", "🪶", "🦩", "🦚", "🦜", "🪽", "🐦‍⬛", "🪿", "🐦‍🔥",
        "🐸", "🐊", "🐢", "🦎", "🐍", "🐲", "🐉", "🦕", "🦖",
        "🐳", "🐋", "🐬", "🫍", "🦭", "🐟", "🐠", "🐡", "🦈", "🐙", "🐚", "🪸", "🪼", "🦀", "🦞", "🦐", "🦑", "🦪",
        "🐌", "🦋", "🫌", "🐛", "🐜", "🐝", "🪲", "🐞", "🦗", "🪳", "🕷️", "🕸️", "🦂", "🦟", "🪰", "🪱", "🦠",
        "🌱", "🪴", "🌲", "🌳", "🌴", "🌵", "🌾", "🌿", "☘️", "🍀", "🍁", "🍂", "🍃", "🪹", "🪺", "🍄", "🪾"
    )

    private val emojisFood = listOf(
        "🍇", "🍈", "🍉", "🍊", "🍋", "🍋‍🟩", "🍌", "🍍", "🥭", "🍎", "🍏", "🍐", "🍑", "🍒", "🍓", "🫐", "🥝", "🍅", "🫒", "🥥",
        "🥑", "🍆", "🥔", "🥕", "🌽", "🌶️", "🫑", "🥒", "🫝", "🥬", "🥦", "🧄", "🧅", "🥜", "🫘", "🌰", "🫚", "🫛", "🍄‍🟫", "🫜",
        "🍞", "🥐", "🥖", "🫓", "🥨", "🥯", "🥞", "🧇", "🧀", "🍖", "🍗", "🥩", "🥓", "🍔", "🍟", "🍕", "🌭", "🥪", "🌮", "🌯", "🫔", "🥙", "🧆", "🥚", "🍳", "🥘", "🍲", "🫕", "🥣", "🥗", "🍿", "🧈", "🧂", "🥫",
        "🍱", "🍘", "🍙", "🍚", "🍛", "🍜", "🍝", "🍠", "🍢", "🍣", "🍤", "🍥", "🥮", "🍡", "🥟", "🥠", "🥡",
        "🍦", "🍧", "🍨", "🍩", "🍪", "🎂", "🍰", "🧁", "🥧", "🍫", "🍬", "🍭", "🍮", "🍯",
        "🍼", "🥛", "☕", "🫖", "🍵", "🍶", "🍾", "🍷", "🍸", "🍹", "🍺", "🍻", "🥂", "🥃", "🫗", "🥤", "🧋", "🧃", "🧉", "🧊",
        "🥢", "🍽️", "🍴", "🥄", "🔪", "🫙", "🏺"
    )

    private val emojisTravel = listOf(
        "🌍", "🌎", "🌏", "🌐", "🗺️", "🗾", "🧭",
        "🏔️", "⛰️", "🛘", "🌋", "🗻", "🏕️", "🏖️", "🏜️", "🏝️", "🏞️",
        "🏟️", "🏛️", "🏗️", "🧱", "🪨", "🪵", "🛖", "🏘️", "🏚️", "🏠", "🏡", "🏢", "🏣", "🏤", "🏥", "🏦", "🏨", "🏪", "🏫", "🏬", "🏭", "🏯", "🏰", "💒", "🗼", "🗽",
        "⛪", "🕌", "🛕", "🕍", "⛩️", "🕋",
        "⛲", "⛺", "🌁", "🌃", "🏙️", "🌄", "🌅", "🌆", "🌇", "🌉", "♨️", "🎠", "🛝", "🎡", "🎢", "💈", "🎪",
        "🚂", "🚃", "🚄", "🚅", "🚆", "🚇", "🚈", "🚉", "🚊", "🚝", "🚞", "🚋", "🚌", "🚍", "🚎", "🚐", "🚑", "🚒", "🚓", "🚔", "🚕", "🚖", "🚗", "🚘", "🚙", "🛻", "🚚", "🚛", "🚜", "🏎️", "🏍️", "🛵", "🦽", "🦼", "🛺", "🚲", "🛴", "🛹", "🛼", "🚏", "🛣️", "🛤️", "🛢️", "⛽", "🛞", "🚨", "🚥", "🚦", "🛑", "🚧",
        "🛙", "⚓", "🛟", "⛵", "🛶", "🚤", "🛳️", "⛴️", "🛥️", "🚢",
        "✈️", "🛩️", "🛫", "🛬", "🪂", "💺", "🚁", "🚟", "🚠", "🚡", "🛰️", "🚀", "🛸",
        "🛎️", "🧳",
        "⌛", "⏳", "⌚", "⏰", "⏱️", "⏲️", "🕰️", "🕛", "🕧", "🕐", "🕜", "🕑", "🕝", "🕒", "🕞", "🕓", "🕟", "🕔", "🕠", "🕕", "🕡", "🕖", "🕢", "🕗", "🕣", "🕘", "🕤", "🕙", "🕥", "🕚", "🕦",
        "🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘", "🌙", "🌚", "🌛", "🌜", "🌡️", "☀️", "🌝", "🌞", "🪐", "⭐", "🌟", "🌠", "🌌", "☁️", "⛅", "⛈️", "🌤️", "🌥️", "🌦️", "🌧️", "🌨️", "🌩️", "🌪️", "🌫️", "🌬️", "🌀", "🌈", "🌂", "☂️", "☔", "⛱️", "⚡", "❄️", "☃️", "⛄", "☄️", "🪋", "🔥", "💧", "🌊",
        // پرچم‌ها
        "🏁", "🚩", "🎌", "🏴", "🏳️", "🏳️‍🌈", "🏳️‍⚧️", "🏴‍☠️",
        "🇦🇨", "🇦🇩", "🇦🇪", "🇦🇫", "🇦🇬", "🇦🇮", "🇦🇱", "🇦🇲", "🇦🇴", "🇦🇶", "🇦🇷", "🇦🇸", "🇦🇹", "🇦🇺", "🇦🇼", "🇦🇽", "🇦🇿",
        "🇧🇦", "🇧🇧", "🇧🇩", "🇧🇪", "🇧🇫", "🇧🇬", "🇧🇭", "🇧🇮", "🇧🇯", "🇧🇱", "🇧🇲", "🇧🇳", "🇧🇴", "🇧🇶", "🇧🇷", "🇧🇸", "🇧🇹", "🇧🇻", "🇧🇼", "🇧🇾", "🇧🇿",
        "🇨🇦", "🇨🇨", "🇨🇩", "🇨🇫", "🇨🇬", "🇨🇭", "🇨🇮", "🇨🇰", "🇨🇱", "🇨🇲", "🇨🇳", "🇨🇴", "🇨🇵", "🇨🇶", "🇨🇷", "🇨🇺", "🇨🇻", "🇨🇼", "🇨🇽", "🇨🇾", "🇨🇿",
        "🇩🇪", "🇩🇬", "🇩🇯", "🇩🇰", "🇩🇲", "🇩🇴", "🇩🇿",
        "🇪🇦", "🇪🇨", "🇪🇪", "🇪🇬", "🇪🇭", "🇪🇷", "🇪🇸", "🇪🇹", "🇪🇺",
        "🇫🇮", "🇫🇯", "🇫🇰", "🇫🇲", "🇫🇴", "🇫🇷",
        "🇬🇦", "🇬🇧", "🇬🇩", "🇬🇪", "🇬🇫", "🇬🇬", "🇬🇭", "🇬🇮", "🇬🇱", "🇬🇲", "🇬🇳", "🇬🇵", "🇬🇶", "🇬🇷", "🇬🇸", "🇬🇹", "🇬🇺", "🇬🇼", "🇬🇾",
        "🇭🇰", "🇭🇲", "🇭🇳", "🇭🇷", "🇭🇹", "🇭🇺",
        "🇮🇨", "🇮🇩", "🇮🇪", "🇮🇱", "🇮🇲", "🇮🇳", "🇮🇴", "🇮🇶", "🇮🇷", "🇮🇸", "🇮🇹",
        "🇯🇪", "🇯🇲", "🇯🇴", "🇯🇵",
        "🇰🇪", "🇰🇬", "🇰🇭", "🇰🇮", "🇰🇲", "🇰🇳", "🇰🇵", "🇰🇷", "🇰🇼", "🇰🇾", "🇰🇿",
        "🇱🇦", "🇱🇧", "🇱🇨", "🇱🇮", "🇱🇰", "🇱🇷", "🇱🇸", "🇱🇹", "🇱🇺", "🇱🇻", "🇱🇾",
        "🇲🇦", "🇲🇨", "🇲🇩", "🇲🇪", "🇲🇫", "🇲🇬", "🇲🇭", "🇲🇰", "🇲🇱", "🇲🇲", "🇲🇳", "🇲🇴", "🇲🇵", "🇲🇶", "🇲🇷", "🇲🇸", "🇲🇹", "🇲🇺", "🇲🇻", "🇲🇼", "🇲🇽", "🇲🇾", "🇲🇿",
        "🇳🇦", "🇳🇨", "🇳🇪", "🇳🇫", "🇳🇬", "🇳🇮", "🇳🇱", "🇳🇴", "🇳🇵", "🇳🇷", "🇳🇺", "🇳🇿",
        "🇴🇲",
        "🇵🇦", "🇵🇪", "🇵🇫", "🇵🇬", "🇵🇭", "🇵🇰", "🇵🇱", "🇵🇲", "🇵🇳", "🇵🇷", "🇵🇸", "🇵🇹", "🇵🇼", "🇵🇾",
        "🇶🇦",
        "🇷🇪", "🇷🇴", "🇷🇸", "🇷🇺", "🇷🇼",
        "🇸🇦", "🇸🇧", "🇸🇨", "🇸🇩", "🇸🇪", "🇸🇬", "🇸🇭", "🇸🇮", "🇸🇯", "🇸🇰", "🇸🇱", "🇸🇲", "🇸🇳", "🇸🇴", "🇸🇷", "🇸🇸", "🇸🇹", "🇸🇻", "🇸🇽", "🇸🇾", "🇸🇿",
        "🇹🇦", "🇹🇨", "🇹🇩", "🇹🇫", "🇹🇬", "🇹🇭", "🇹🇯", "🇹🇰", "🇹🇱", "🇹🇲", "🇹🇳", "🇹🇴", "🇹🇷", "🇹🇹", "🇹🇻", "🇹🇼", "🇹🇿",
        "🇺🇦", "🇺🇬", "🇺🇲", "🇺🇳", "🇺🇸", "🇺🇾", "🇺🇿",
        "🇻🇦", "🇻🇨", "🇻🇪", "🇻🇬", "🇻🇮", "🇻🇳", "🇻🇺",
        "🇼🇫", "🇼🇸",
        "🇽🇰",
        "🇾🇪", "🇾🇹",
        "🇿🇦", "🇿🇲", "🇿🇼",
        "🏴󠁧󠁢󠁥󠁮󠁧󠁿", "🏴󠁧󠁢󠁳󠁣󠁴󠁿", "🏴󠁧󠁢󠁷󠁬󠁳󠁿"
    )

    private val emojisObjects = listOf(
        "👓", "🕶️", "🥽", "🥼", "🦺", "👔", "👕", "👖", "🧣", "🧤", "🧥", "🧦", "👗", "👘", "🥻", "🩱", "🩲", "🩳", "👙", "👚", "🪭", "👛", "👜", "👝", "🛍️", "🎒", "🩴", "👞", "👟", "🥾", "🥿", "👠", "👡", "🩰", "👢", "🪮", "👑", "👒", "🎩", "🎓", "🧢", "🪖", "⛑️", "📿", "💄", "💍", "💎",
        "🔇", "🔈", "🔉", "🔊", "📢", "📣", "📯", "🔔", "🔕",
        "🎼", "🎵", "🎶", "🎙️", "🎚️", "🎛️", "🎤", "🎧", "📻",
        "🎷", "🎺", "🪊", "🪗", "🎸", "🎹", "🎻", "🪕", "🥁", "🪘", "🪇", "🪈", "🪉",
        "📱", "📲", "☎️", "📞", "📟", "📠",
        "🔋", "🪫", "🔌", "💻", "🖥️", "🖨️", "⌨️", "🖱️", "🖲️", "💽", "💾", "💿", "📀", "🧮",
        "🎥", "🎞️", "📽️", "🎬", "📺", "📷", "📸", "📹", "📼", "🔍", "🔎", "🕯️", "💡", "🔦", "🏮", "🪔",
        "📔", "📕", "📖", "📗", "📘", "📙", "📚", "📓", "📒", "📃", "📜", "📄", "📰", "🗞️", "📑", "🔖", "🏷️",
        "🪙", "💰", "🪎", "💴", "💵", "💶", "💷", "💸", "💳", "🧾", "💹",
        "✉️", "📧", "📨", "📩", "📤", "📥", "📦", "📫", "📪", "📬", "📭", "📮", "🗳️",
        "✏️", "✒️", "🖋️", "🖊️", "🖌️", "🖍️", "📝", "🪌",
        "💼", "📁", "📂", "🗂️", "📅", "📆", "🗒️", "🗓️", "📇", "📈", "📉", "📊", "📋", "📌", "📍", "📎", "🖇️", "📏", "📐", "✂️", "🗃️", "🗄️", "🗑️",
        "🔒", "🔓", "🔏", "🔐", "🔑", "🗝️",
        "🪍", "🔨", "🪓", "⛏️", "⚒️", "🛠️", "🗡️", "⚔️", "💣", "🪃", "🏹", "🛡️", "🪚", "🔧", "🪛", "🔩", "⚙️", "🗜️", "⚖️", "🦯", "🔗", "⛓️‍💥", "⛓️", "🪝", "🧰", "🧲", "🪜", "🪏",
        "⚗️", "🧪", "🧫", "🧬", "🔬", "🔭", "📡",
        "💉", "🩸", "💊", "🩹", "🩼", "🩺", "🩻",
        "🚪", "🛗", "🪞", "🪟", "🛏️", "🛋️", "🪑", "🚽", "🪠", "🚿", "🛁", "🪤", "🪒", "🧴", "🧷", "🧹", "🧺", "🧻", "🪣", "🧼", "🫧", "🪥", "🧽", "🧯", "🛒",
        "🚬", "⚰️", "🪦", "⚱️", "🧿", "🪬", "🗿", "🪧", "🪪",
        "🎃", "🎄", "🎆", "🎇", "🧨", "✨", "🎈", "🎉", "🎊", "🎋", "🎍", "🎎", "🎏", "🎐", "🎑", "🧧", "🎀", "🎁", "🎗️", "🎟️", "🎫",
        "🎖️", "🏆", "🏅", "🥇", "🥈", "🥉",
        "⚽", "⚾", "🥎", "🏀", "🏐", "🏈", "🏉", "🎾", "🥏", "🎳", "🏏", "🏑", "🏒", "🥍", "🏓", "🏸", "🥊", "🥋", "🥅", "⛳", "⛸️", "🎣", "🤿", "🎽", "🎿", "🛷", "🥌",
        "🎯", "🪀", "🪁", "🔫", "🎱", "🔮", "🪄", "🎮", "🕹️", "🎰", "🎲", "🧩", "🧸", "🪅", "🪩", "🪆", "♠️", "♥️", "♦️", "♣️", "♟️", "🃏", "🀄️", "🎴",
        "🎭", "🖼️", "🎨", "🧵", "🪡", "🧶", "🪢",
        "💐", "🌸", "💮", "🪷", "🏵️", "🌹", "🥀", "🌺", "🌻", "🌼", "🌷", "🪻"
    )

    private val emojisSymbols = listOf(
        "❤️", "🩷", "🧡", "💛", "💚", "💙", "🩵", "💜", "🤎", "🖤", "🩶", "🤍", "💔", "❤️‍🔥", "❤️‍🩹", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "☮️", "✝️", "☪️", "🕉", "☸️", "✡️", "🔯", "🕎", "☯️", "☦️", "🪯", "🛜", "📳", "📴",
        "🏧", "🚮", "🚰", "♿", "🚹", "🚺", "🚻", "🚼", "🚾", "🛂", "🛃", "🛄", "🛅",
        "⚠️", "🚸", "⛔", "🚫", "🚳", "🚭", "🚯", "🚱", "🚷", "📵", "🔞", "☢️", "☣️",
        "⬆️", "↗️", "➡️", "↘️", "⬇️", "↙️", "⬅️", "↖️", "↕️", "↔️", "↩️", "↪️", "⤴️", "⤵️", "🔃", "🔄", "🔙", "🔚", "🔛", "🔜", "TOP",
        "🛐", "⚛️", "🕉", "✡️", "☸️", "☯️", "✝️", "☦️", "☪️", "☮️", "🕎", "🔯",
        "♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓", "⛎",
        "🔀", "🔁", "🔂", "▶️", "⏩", "⏭", "⏯", "◀️", "⏪", "⏮", "🔼", "⏫", "🔽", "⏬", "⏸", "⏹", "⏺", "⏏️", "🎦", "🔅", "🔆", "📶",
        "♀️", "♂️", "⚧️",
        "✖️", "➕", "➖", "➗", "🟰", "♾️",
        "‼️", "⁉️", "❓", "❔", "❕", "❗", "〰️",
        "💱", "💲",
        "⚕️", "♻️", "⚜️", "🔱", "📛", "🔰", "⭕", "✅", "☑️", "✔️", "❌", "❎", "➰", "➿", "〽️", "✳️", "✴️", "❇️", "©️", "®️", "™️", "🫟",
        "#️⃣", "*️⃣", "0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟",
        "🔠", "🔡", "🔢", "🔣", "🔤", "🅰️", "🆎", "🅱️", "🆑", "🆒", "🆓", "ℹ️", "🆔", "Ⓜ️", "🆕", "🆖", "🅾️", "🆗", "🅿️", "🆘", "🆙", "🆚", "🈁", "🈂️", "🈷️", "🈶", "🈯", "🉐", "🈹", "🈚", "🈲", "🉑", "🈸", "🈴", "🈳", "㊗️", "㊙️", "🈺", "🈵",
        "🔴", "🟠", "🟡", "🟢", "🔵", "🟣", "🟤", "⚫", "⚪", "🟥", "🟧", "🟨", "🟩", "🟦", "🟪", "🟫", "⬛", "⬜", "◼️", "◻️", "◾", "◽", "▪️", "▫️", "🔶", "🔷", "🔸", "🔹", "🔺", "🔻", "💠", "🔘", "🔳", "🔲", "🫯"
    )

    override fun onCreate() {
        super.onCreate()
        sharedPrefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)
        learnedWords.addAll(sharedPrefs.getStringSet("learned_words", emptySet()) ?: emptySet())
        
        recentEmojis.addAll(sharedPrefs.getStringSet("recent_emojis", emptySet()) ?: emptySet())
        favoriteEmojis.addAll(sharedPrefs.getStringSet("favorite_emojis", emptySet()) ?: emptySet())
        
        isVibrationEnabled = sharedPrefs.getBoolean("vibration_enabled", true)
        isSoundEnabled = sharedPrefs.getBoolean("sound_enabled", true)
        
        currentTheme = sharedPrefs.getString("keyboard_theme", "dark") ?: "dark"
        currentKeyTextSize = sharedPrefs.getFloat("key_text_size", 18f)

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 40)
        } catch (e: Exception) {
            toneGenerator = null
        }

        val db = ClipboardDatabase.getDatabase(this)
        clipboardListener = ClipboardListener(this, db.clipboardDao())
        clipboardListener.startListening()
    }
    
    override fun onDestroy() {
        if (::clipboardListener.isInitialized) {
            clipboardListener.stopListening()
        }
        toneGenerator?.release()
        toneGenerator = null
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        setupKeyboardUI(view)
        return view
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (emojiContainer?.visibility == View.VISIBLE) {
                emojiContainer?.visibility = View.GONE
                lettersContainer?.visibility = View.VISIBLE
                return true
            } else if (clipboardContainer?.visibility == View.VISIBLE) {
                clipboardContainer?.visibility = View.GONE
                lettersContainer?.visibility = View.VISIBLE
                return true
            } else if (symbolsContainer?.visibility == View.VISIBLE) {
                symbolsContainer?.visibility = View.GONE
                lettersContainer?.visibility = View.VISIBLE
                return true
            } else if (settingsContainer?.visibility == View.VISIBLE) {
                settingsContainer?.visibility = View.GONE
                lettersContainer?.visibility = View.VISIBLE
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (suggestionViews.isNotEmpty()) {
            updateSuggestions(suggestionViews)
        }
    }

    private fun performKeyFeedback() {
        try {
            if (isVibrationEnabled) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(30)
                    }
                }
            }
            if (isSoundEnabled) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
            }
        } catch (e: Exception) {
        }
    }

    private fun setupKeyboardUI(view: View) {
        lettersContainer = view.findViewById(R.id.letters_container)
        symbolsContainer = view.findViewById(R.id.symbols_container)
        diacriticsContainer = view.findViewById(R.id.diacritics_container)
        clipboardContainer = view.findViewById(R.id.clipboard_container)
        settingsContainer = view.findViewById(R.id.settings_container)
        
        clipboardRecyclerView = view.findViewById(R.id.clipboard_recycler)
        clipboardRecyclerView?.layoutManager = LinearLayoutManager(this)
        
        emojiContainer = view.findViewById(R.id.emoji_container)
        emojiRecyclerView = view.findViewById(R.id.emoji_recycler)
        emojiRecyclerView?.layoutManager = GridLayoutManager(this, 8)
        
        emojiAdapter = EmojiAdapter(emojisSmileys, 
            onEmojiClick = { emoji ->
                performKeyFeedback()
                currentInputConnection?.commitText(emoji, 1)
                addToRecentEmojis(emoji)
            },
            onEmojiLongClick = { emoji ->
                toggleFavoriteEmoji(emoji)
            }
        )
        emojiRecyclerView?.adapter = emojiAdapter

        view.findViewById<TextView>(R.id.tab_recent)?.setOnClickListener { showEmojiCategory("recent") }
        view.findViewById<TextView>(R.id.tab_favorites)?.setOnClickListener { showEmojiCategory("favorites") }
        view.findViewById<TextView>(R.id.tab_smileys)?.setOnClickListener { showEmojiCategory("smileys") }
        view.findViewById<TextView>(R.id.tab_nature)?.setOnClickListener { showEmojiCategory("nature") }
        view.findViewById<TextView>(R.id.tab_food)?.setOnClickListener { showEmojiCategory("food") }
        view.findViewById<TextView>(R.id.tab_travel)?.setOnClickListener { showEmojiCategory("travel") }
        view.findViewById<TextView>(R.id.tab_objects)?.setOnClickListener { showEmojiCategory("objects") }
        view.findViewById<TextView>(R.id.tab_symbols)?.setOnClickListener { showEmojiCategory("symbols") }
        
        editClipboardContainer = view.findViewById(R.id.edit_clipboard_container)
        editClipboardText = view.findViewById(R.id.edit_clipboard_text)
        val btnSaveEdit = view.findViewById<TextView>(R.id.btn_save_clipboard_edit)
        val btnCancelEdit = view.findViewById<TextView>(R.id.btn_cancel_clipboard_edit)

        val switchVibration = view.findViewById<Switch>(R.id.switch_vibration)
        val switchSound = view.findViewById<Switch>(R.id.switch_sound)
        val seekBarKeySize = view.findViewById<SeekBar>(R.id.seekbar_key_size)
        val previewKeySizeText = view.findViewById<TextView>(R.id.preview_key_size_text)
        
        switchVibration.isChecked = isVibrationEnabled
        switchSound.isChecked = isSoundEnabled

        val initialProgress = (currentKeyTextSize - 12f).toInt().coerceIn(0, 14)
        seekBarKeySize.progress = initialProgress
        previewKeySizeText.textSize = currentKeyTextSize

        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            isVibrationEnabled = isChecked
            sharedPrefs.edit().putBoolean("vibration_enabled", isChecked).apply()
        }

        switchSound.setOnCheckedChangeListener { _, isChecked ->
            isSoundEnabled = isChecked
            sharedPrefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }

        seekBarKeySize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val newSize = 12f + progress
                currentKeyTextSize = newSize
                previewKeySizeText.textSize = newSize
                sharedPrefs.edit().putFloat("key_text_size", newSize).apply()
                
                applyKeyTextSize(lettersContainer, newSize)
                applyKeyTextSize(symbolsContainer, newSize)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        view.findViewById<View>(R.id.theme_dark)?.setOnClickListener { changeTheme("dark") }
        view.findViewById<View>(R.id.theme_light)?.setOnClickListener { changeTheme("light") }
        view.findViewById<View>(R.id.theme_blue)?.setOnClickListener { changeTheme("blue") }
        view.findViewById<View>(R.id.theme_purple)?.setOnClickListener { changeTheme("purple") }
        view.findViewById<View>(R.id.theme_green)?.setOnClickListener { changeTheme("green") }
        view.findViewById<View>(R.id.theme_cyberpunk)?.setOnClickListener { changeTheme("cyberpunk") }
        view.findViewById<View>(R.id.theme_wood)?.setOnClickListener { changeTheme("wood") }
        view.findViewById<View>(R.id.theme_cosmic)?.setOnClickListener { changeTheme("cosmic") }
        view.findViewById<View>(R.id.theme_pastel)?.setOnClickListener { changeTheme("pastel") }

        applyTheme(currentTheme)

        val langBtn = view.findViewById<TextView>(R.id.btn_lang)
        val pageBtn = view.findViewById<TextView>(R.id.btn_page_change)
        val shiftBtn = view.findViewById<TextView>(R.id.btn_shift)
        
        suggestionViews = listOf(
            view.findViewById<TextView>(R.id.suggestion_1),
            view.findViewById<TextView>(R.id.suggestion_2),
            view.findViewById<TextView>(R.id.suggestion_3)
        )

        bindLetters(view, shiftBtn)
        bindSymbols(view, pageBtn)
        updateSuggestions(suggestionViews)

        langBtn.text = if (isPersian) "En" else "فا"

        langBtn.setOnClickListener { 
            performKeyFeedback()
            isPersian = !isPersian 
            isCapsLock = false
            isShifted = false
            
            langBtn.text = if (isPersian) "En" else "فا"

            bindLetters(view, shiftBtn) 
            updateSuggestions(suggestionViews)
        }

        shiftBtn.setOnClickListener {
            performKeyFeedback()
            if (!isPersian) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShiftClickTime < 400) {
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
                bindLetters(view, shiftBtn)
            }
        }

        setupContinuousDeletion(view.findViewById(R.id.btn_backspace), suggestionViews)
        setupContinuousDeletion(view.findViewById(R.id.btn_backspace_sym), suggestionViews)

        listOf(R.id.btn_space, R.id.btn_space_sym).forEach { 
            view.findViewById<TextView>(it)?.setOnClickListener { 
                performKeyFeedback()
                currentInputConnection?.commitText(" ", 1) 
                if (!isPersian && !isCapsLock) {
                    isShifted = true
                    bindLetters(view, shiftBtn)
                }
            } 
        }

        view.findViewById<TextView>(R.id.btn_enter)?.setOnClickListener { 
            performKeyFeedback()
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)) 
            if (!isPersian && !isCapsLock) {
                isShifted = true
                bindLetters(view, shiftBtn)
            }
        }

        setupNavigation(view, pageBtn, suggestionViews)

        view.findViewById<TextView>(R.id.btn_settings_bar)?.setOnClickListener {
            lettersContainer?.visibility = View.GONE
            symbolsContainer?.visibility = View.GONE
            diacriticsContainer?.visibility = View.GONE
            clipboardContainer?.visibility = View.GONE
            emojiContainer?.visibility = View.GONE
            settingsContainer?.visibility = View.VISIBLE
        }

        view.findViewById<TextView>(R.id.btn_emoji_bar)?.setOnClickListener {
            lettersContainer?.visibility = View.GONE
            symbolsContainer?.visibility = View.GONE
            diacriticsContainer?.visibility = View.GONE
            clipboardContainer?.visibility = View.GONE
            settingsContainer?.visibility = View.GONE
            emojiContainer?.visibility = View.VISIBLE
            showEmojiCategory("recent") 
        }

        view.findViewById<TextView>(R.id.btn_mic)?.setOnClickListener { 
            performKeyFeedback()
            
            voiceResultListener = { text ->
                Handler(Looper.getMainLooper()).post {
                    val ic = currentInputConnection
                    if (ic != null) {
                        ic.commitText(text, 1)
                    } else {
                        showToast("خطا در تایپ. لطفا دوباره تلاش کنید.")
                    }
                }
            }
            
            val intent = Intent(this, VoiceTypingActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        
        view.findViewById<TextView>(R.id.btn_clipboard_bar)?.setOnClickListener {
            lettersContainer?.visibility = View.GONE
            symbolsContainer?.visibility = View.GONE
            diacriticsContainer?.visibility = View.GONE
            emojiContainer?.visibility = View.GONE
            settingsContainer?.visibility = View.GONE
            clipboardContainer?.visibility = View.VISIBLE
            editClipboardContainer?.visibility = View.GONE
            clipboardRecyclerView?.visibility = View.VISIBLE
            loadClipboardData()
        }

        view.findViewById<TextView>(R.id.btn_close_clipboard)?.setOnClickListener {
            clipboardContainer?.visibility = View.GONE
            lettersContainer?.visibility = View.VISIBLE
        }

        view.findViewById<TextView>(R.id.btn_refresh_clipboard)?.setOnClickListener {
            loadClipboardData()
            showToast("لیست به‌روز شد")
        }

        view.findViewById<TextView>(R.id.btn_clear_clipboard)?.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                db.clipboardDao().deleteAll()
                withContext(Dispatchers.Main) {
                    loadClipboardData()
                    showToast("همه موارد پاک شدند")
                }
            }
        }
        
        btnSaveEdit?.setOnClickListener {
            val newText = editClipboardText?.text.toString()
            if (newText.isNotBlank() && editingItem != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    db.clipboardDao().update(editingItem!!.copy(text = newText))
                    withContext(Dispatchers.Main) {
                        editingItem = null
                        editClipboardContainer?.visibility = View.GONE
                        clipboardRecyclerView?.visibility = View.VISIBLE
                        loadClipboardData()
                        showToast("ویرایش شد")
                    }
                }
            }
        }
        
        btnCancelEdit?.setOnClickListener {
            editingItem = null
            editClipboardContainer?.visibility = View.GONE
            clipboardRecyclerView?.visibility = View.VISIBLE
        }
    }

    private fun changeTheme(theme: String) {
        currentTheme = theme
        sharedPrefs.edit().putString("keyboard_theme", theme).apply()
        applyTheme(theme)
    }

    private fun applyTheme(theme: String) {
        val bgColor: Int
        val keyColor: Int
        val textColor: Int

        when (theme) {
            "light" -> {
                bgColor = Color.parseColor("#CFD8DC")
                keyColor = Color.parseColor("#FFFFFF")
                textColor = Color.parseColor("#000000")
            }
            "blue" -> {
                bgColor = Color.parseColor("#0D47A1")
                keyColor = Color.parseColor("#1565C0")
                textColor = Color.parseColor("#FFFFFF")
            }
            "purple" -> {
                bgColor = Color.parseColor("#4A148C")
                keyColor = Color.parseColor("#6A1B9A")
                textColor = Color.parseColor("#FFFFFF")
            }
            "green" -> {
                bgColor = Color.parseColor("#1B5E20")
                keyColor = Color.parseColor("#2E7D32")
                textColor = Color.parseColor("#FFFFFF")
            }
            "cyberpunk" -> {
                bgColor = Color.parseColor("#121212")
                keyColor = Color.parseColor("#1E1E1E")
                textColor = Color.parseColor("#00FFFF")
            }
            "wood" -> {
                bgColor = Color.parseColor("#3E2723")
                keyColor = Color.parseColor("#4E342E")
                textColor = Color.parseColor("#D7CCC8")
            }
            "cosmic" -> {
                bgColor = Color.parseColor("#281C3D")
                keyColor = Color.parseColor("#3B2A59")
                textColor = Color.parseColor("#FFFFFF")
            }
            "pastel" -> {
                bgColor = Color.parseColor("#F3F4F6")
                keyColor = Color.parseColor("#FFFFFF")
                textColor = Color.parseColor("#374151")
            }
            else -> { // dark
                bgColor = Color.parseColor("#263238")
                keyColor = Color.parseColor("#37474F")
                textColor = Color.parseColor("#FFFFFF")
            }
        }

        lettersContainer?.setBackgroundColor(bgColor)
        symbolsContainer?.setBackgroundColor(bgColor)
        diacriticsContainer?.setBackgroundColor(bgColor)
        clipboardContainer?.setBackgroundColor(bgColor)
        emojiContainer?.setBackgroundColor(bgColor)
        settingsContainer?.setBackgroundColor(bgColor)

        updateKeysColor(lettersContainer, keyColor, textColor)
        updateKeysColor(symbolsContainer, keyColor, textColor)
        
        suggestionViews.forEach { it.setTextColor(textColor) }
    }

    private fun updateKeysColor(parent: ViewGroup?, keyColor: Int, textColor: Int) {
        if (parent == null) return
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is ViewGroup) {
                updateKeysColor(child, keyColor, textColor)
            } else if (child is TextView && child.isClickable) {
                child.backgroundTintList = ColorStateList.valueOf(keyColor)
                child.setTextColor(textColor)
            }
        }
    }

    private fun applyKeyTextSize(parent: ViewGroup?, textSize: Float) {
        if (parent == null) return
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is ViewGroup) {
                applyKeyTextSize(child, textSize)
            } else if (child is TextView && child.isClickable) {
                if (child.text.length <= 2) {
                    child.textSize = textSize
                }
            }
        }
    }

    private fun showEmojiCategory(category: String) {
        currentEmojiCategory = category
        val emojisToShow = when (category) {
            "recent" -> if (recentEmojis.isEmpty()) emojisSmileys else recentEmojis 
            "favorites" -> favoriteEmojis 
            "smileys" -> emojisSmileys
            "nature" -> emojisNature
            "food" -> emojisFood
            "travel" -> emojisTravel
            "objects" -> emojisObjects
            "symbols" -> emojisSymbols
            else -> emojisSmileys
        }
        
        emojiAdapter = EmojiAdapter(emojisToShow, 
            onEmojiClick = { emoji ->
                performKeyFeedback()
                currentInputConnection?.commitText(emoji, 1)
                addToRecentEmojis(emoji)
            },
            onEmojiLongClick = { emoji ->
                toggleFavoriteEmoji(emoji)
            }
        )
        emojiRecyclerView?.adapter = emojiAdapter
    }

    private fun toggleFavoriteEmoji(emoji: String) {
        if (favoriteEmojis.contains(emoji)) {
            favoriteEmojis.remove(emoji)
            showToast("از علاقه‌مندی‌ها حذف شد")
        } else {
            favoriteEmojis.add(0, emoji)
            showToast("به علاقه‌مندی‌ها اضافه شد ⭐")
        }
        sharedPrefs.edit().putStringSet("favorite_emojis", favoriteEmojis.toSet()).apply()
        
        if (currentEmojiCategory == "favorites") {
            showEmojiCategory("favorites")
        }
    }

    private fun addToRecentEmojis(emoji: String) {
        if (recentEmojis.contains(emoji)) {
            recentEmojis.remove(emoji)
        }
        recentEmojis.add(0, emoji)
        
        if (recentEmojis.size > 30) {
            recentEmojis.removeAt(recentEmojis.size - 1)
        }
        
        sharedPrefs.edit().putStringSet("recent_emojis", recentEmojis.toSet()).apply()
    }

    private fun loadClipboardData() {
        CoroutineScope(Dispatchers.Main).launch {
            val rawItems = withContext(Dispatchers.IO) {
                db.clipboardDao().getAllItems()
            }
            
            val items = rawItems.distinctBy { it.text }
            
            if (clipboardRecyclerView != null) {
                val adapter = ClipboardAdapter(
                    items = items,
                    onItemClick = { item ->
                        val ic = this@PersianKeyboardService.currentInputConnection
                        if (ic != null) {
                            ic.commitText(item.text, 1)
                        }
                        
                        Handler(Looper.getMainLooper()).postDelayed({
                            clipboardContainer?.visibility = View.GONE
                            lettersContainer?.visibility = View.VISIBLE
                        }, 150)
                    },
                    onEditClick = { item ->
                        editingItem = item
                        editClipboardText?.setText(item.text)
                        editClipboardText?.setSelection(item.text.length)
                        clipboardRecyclerView?.visibility = View.GONE
                        editClipboardContainer?.visibility = View.VISIBLE
                    },
                    onDeleteClick = { item ->
                        CoroutineScope(Dispatchers.IO).launch {
                            db.clipboardDao().deleteById(item.id)
                            withContext(Dispatchers.Main) {
                                loadClipboardData()
                            }
                        }
                    }
                )
                clipboardRecyclerView?.adapter = adapter
            }
        }
    }

    private fun bindLetters(view: View, shiftBtn: TextView) {
        val buttons = if (isPersian) {
            view.findViewById<TextView>(R.id.btn_pe)?.visibility = View.VISIBLE
            mapOf(
                R.id.btn_sad to "ص", R.id.btn_se to "ث", R.id.btn_ghaf to "ق", R.id.btn_fe to "ف", R.id.btn_e to "ع", R.id.btn_he to "ه", R.id.btn_khe to "خ", R.id.btn_h to "ح", R.id.btn_jim to "ج", R.id.btn_che to "چ",
                R.id.btn_shin to "ش", R.id.btn_sin to "س", R.id.btn_ye to "ی", R.id.btn_be to "ب", R.id.btn_lam to "ل", R.id.btn_alef to "ا", R.id.btn_te to "ت", R.id.btn_nun to "ن", R.id.btn_mim to "م", R.id.btn_pe to "پ",
                R.id.btn_ta to "ط", R.id.btn_ze to "ز", R.id.btn_re to "ر", R.id.btn_dal to "د", R.id.btn_vav to "و", R.id.btn_kaf to "ک", R.id.btn_gaf to "گ"
            )
        } else {
            view.findViewById<TextView>(R.id.btn_pe)?.visibility = View.GONE
            val baseMap = mapOf(
                R.id.btn_sad to "q", R.id.btn_se to "w", R.id.btn_ghaf to "e", R.id.btn_fe to "r", R.id.btn_e to "t", R.id.btn_he to "y", R.id.btn_khe to "u", R.id.btn_h to "i", R.id.btn_jim to "o", R.id.btn_che to "p",
                R.id.btn_shin to "a", R.id.btn_sin to "s", R.id.btn_ye to "d", R.id.btn_be to "f", R.id.btn_lam to "g", R.id.btn_alef to "h", R.id.btn_te to "j", R.id.btn_nun to "k", R.id.btn_mim to "l",
                R.id.btn_ta to "z", R.id.btn_ze to "x", R.id.btn_re to "c", R.id.btn_dal to "v", R.id.btn_vav to "b", R.id.btn_kaf to "n", R.id.btn_gaf to "m"
            )
            if (isCapsLock || isShifted) baseMap.mapValues { it.value.uppercase() } else baseMap
        }
        
        for ((id, text) in buttons) { 
            view.findViewById<TextView>(id)?.apply {
                this.text = text 
                this.textSize = currentKeyTextSize
                
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> showKeyPreview(v, text)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dismissKeyPreview()
                    }
                    false
                }

                setOnClickListener { 
                    performKeyFeedback() 
                    currentInputConnection?.commitText(text, 1) 
                    if (!isPersian && isShifted && !isCapsLock) {
                        isShifted = false
                        bindLetters(view, shiftBtn) 
                    }
                } 
            }
        }

        if (isPersian) {
            setupHiddenCharsAndDoubleTap(view)
            setupLongPressPopups(view)
        }

        val nums = if (isPersian) {
            mapOf(R.id.btn_num_1 to "۱", R.id.btn_num_2 to "۲", R.id.btn_num_3 to "۳", R.id.btn_num_4 to "۴", R.id.btn_num_5 to "۵", R.id.btn_num_6 to "۶", R.id.btn_num_7 to "۷", R.id.btn_num_8 to "۸", R.id.btn_num_9 to "۹", R.id.btn_num_0 to "۰")
        } else {
            mapOf(R.id.btn_num_1 to "1", R.id.btn_num_2 to "2", R.id.btn_num_3 to "3", R.id.btn_num_4 to "4", R.id.btn_num_5 to "5", R.id.btn_num_6 to "6", R.id.btn_num_7 to "7", R.id.btn_num_8 to "8", R.id.btn_num_9 to "9", R.id.btn_num_0 to "0")
        }
        for ((id, text) in nums) { 
            view.findViewById<TextView>(id)?.apply {
                this.text = text
                this.textSize = currentKeyTextSize
                
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> showKeyPreview(v, text)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dismissKeyPreview()
                    }
                    false
                }

                setOnClickListener { 
                    performKeyFeedback() 
                    currentInputConnection?.commitText(text, 1) 
                }
            }
        }

        val shiftColor = when {
            isCapsLock -> Color.parseColor("#4CAF50") 
            isShifted -> Color.parseColor("#29B6F6")  
            else -> null 
        }

        if (shiftColor != null) {
            shiftBtn.backgroundTintList = ColorStateList.valueOf(shiftColor)
        } else {
            shiftBtn.backgroundTintList = null
        }
    }

    private fun showKeyPreview(anchorView: View, text: String) {
        try {
            if (keyPreviewPopup == null) {
                keyPreviewText = TextView(this).apply {
                    textSize = 32f
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    setPadding(32, 16, 32, 16)
                    setBackgroundColor(Color.parseColor("#37474F"))
                }
                
                keyPreviewPopup = PopupWindow(
                    keyPreviewText,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    false
                ).apply {
                    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    isOutsideTouchable = false
                }
            }

            keyPreviewText?.text = text
            if (keyPreviewPopup?.isShowing == true) {
                keyPreviewPopup?.dismiss()
            }
            keyPreviewPopup?.showAsDropDown(anchorView, anchorView.width / 2 - 50, -anchorView.height - 140)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissKeyPreview() {
        try {
            keyPreviewPopup?.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupHiddenCharsAndDoubleTap(view: View) {
        val setupKey = { tv: TextView, hidden: String, main: String, onMainClick: () -> Unit, onHiddenClick: () -> Unit ->
            val spannable = SpannableString(main)
            spannable.setSpan(DoubleLetterSpan(hidden, main), 0, main.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            tv.text = spannable
            tv.textSize = currentKeyTextSize

            tv.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> showKeyPreview(v, main)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dismissKeyPreview()
                }
                false
            }

            var lastClick = 0L
            tv.setOnClickListener {
                performKeyFeedback()
                val time = System.currentTimeMillis()
                if (time - lastClick < 400) {
                    currentInputConnection?.deleteSurroundingText(1, 0)
                    onHiddenClick()
                } else {
                    onMainClick()
                }
                lastClick = time
            }
        }

        setupKey(view.findViewById(R.id.btn_sad), "ض", "ص", 
            { currentInputConnection?.commitText("ص", 1) }, 
            { currentInputConnection?.commitText("ض", 1) }
        )
        setupKey(view.findViewById(R.id.btn_e), "غ", "ع", 
            { currentInputConnection?.commitText("ع", 1) }, 
            { currentInputConnection?.commitText("غ", 1) }
        )
        setupKey(view.findViewById(R.id.btn_ta), "ظ", "ط", 
            { currentInputConnection?.commitText("ط", 1) }, 
            { currentInputConnection?.commitText("ظ", 1) }
        )
        setupKey(view.findViewById(R.id.btn_ze), "ژ", "ز", 
            { currentInputConnection?.commitText("ز", 1) }, 
            { currentInputConnection?.commitText("ژ", 1) }
        )
        setupKey(view.findViewById(R.id.btn_dal), "ذ", "د", 
            { currentInputConnection?.commitText("د", 1) }, 
            { currentInputConnection?.commitText("ذ", 1) }
        )
    }

    class DoubleLetterSpan(private val hiddenLetter: String, private val mainLetter: String) : ReplacementSpan() {
        override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
            return paint.measureText(mainLetter).toInt()
        }

        override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
            val mainWidth = paint.measureText(mainLetter)
            canvas.drawText(mainLetter, x, y.toFloat(), paint)

            val hiddenPaint = Paint(paint)
            hiddenPaint.textSize = paint.textSize * 0.45f
            hiddenPaint.color = Color.parseColor("#B0BEC5") 
            val hiddenWidth = hiddenPaint.measureText(hiddenLetter)
            val hiddenX = x + (mainWidth - hiddenWidth) / 2 
            val hiddenY = y - paint.textSize * 0.8f 
            canvas.drawText(hiddenLetter, hiddenX, hiddenY, hiddenPaint)
        }
    }

    private fun bindSymbols(view: View, pageBtn: TextView) {
        val pageData = symbolPages[currentSymbolPage]
        val symbolIds = listOf(
            R.id.sym_1, R.id.sym_2, R.id.sym_3, R.id.sym_4, R.id.sym_5, R.id.sym_6, R.id.sym_7, R.id.sym_8, R.id.sym_9, R.id.sym_10,
            R.id.sym_11, R.id.sym_12, R.id.sym_13, R.id.sym_14, R.id.sym_15, R.id.sym_16, R.id.sym_17, R.id.sym_18, R.id.sym_19, R.id.sym_20,
            R.id.sym_21, R.id.sym_22, R.id.sym_23, R.id.sym_24, R.id.sym_25, R.id.sym_26, R.id.sym_27, R.id.sym_28, R.id.sym_29, R.id.sym_30,
            R.id.sym_31, R.id.sym_32, R.id.sym_33, R.id.sym_34, R.id.sym_35, R.id.sym_36, R.id.sym_37, R.id.sym_38, R.id.sym_39, R.id.sym_40
        )

        for (i in symbolIds.indices) {
            val tv = view.findViewById<TextView>(symbolIds[i])
            if (tv != null) {
                if (i < pageData.size) {
                    val item = pageData[i]
                    if (item.symbol.isNotEmpty()) {
                        val fullText = if (item.meaning.isNotEmpty()) "${item.symbol}\n${item.meaning}" else item.symbol
                        val spannable = SpannableString(fullText)
                        if (item.meaning.isNotEmpty()) {
                            spannable.setSpan(android.text.style.RelativeSizeSpan(if (currentSymbolPage == 4) 1.5f else 1.3f), 0, item.symbol.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            spannable.setSpan(android.text.style.RelativeSizeSpan(if (currentSymbolPage == 4) 0.6f else 0.5f), item.symbol.length, fullText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        tv.text = spannable
                        tv.textSize = currentKeyTextSize
                        tv.visibility = View.VISIBLE
                        tv.setOnClickListener { 
                            performKeyFeedback()
                            currentInputConnection?.commitText(item.symbol, 1) 
                        }
                    } else {
                        tv.text = ""
                        tv.visibility = View.INVISIBLE
                        tv.setOnClickListener(null)
                    }
                } else {
                    tv.text = ""
                    tv.visibility = View.INVISIBLE
                    tv.setOnClickListener(null)
                }
            }
        }
        pageBtn.text = "${currentSymbolPage + 1}/6"
    }

    private fun updateSuggestions(suggestions: List<TextView>) {
        val text = currentInputConnection?.getTextBeforeCursor(20, 0)?.toString() ?: ""
        val words = text.split(" ", "\n", ".", "،", "؟", "!")
        val lastWord = words.lastOrNull() ?: ""
        val previousWord = if (words.size > 1) words[words.size - 2].trim() else ""

        val currentSuggestions = mutableListOf<String>()

        if (lastWord.isEmpty()) {
            if (previousWord.isNotEmpty() && nextWordPredictions.containsKey(previousWord)) {
                currentSuggestions.addAll(nextWordPredictions[previousWord]!!)
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
                        showToast("کلمه «$word» ذخیره شد")
                        currentInputConnection?.commitText(" ", 1)
                    } else {
                        currentInputConnection?.deleteSurroundingText(lastWord.length, 0)
                        currentInputConnection?.commitText("$word ", 1)
                        saveWordToMemory(word)
                    }
                    updateSuggestions(suggestions)
                }

                textView.setOnLongClickListener {
                    if (learnedWords.contains(word)) {
                        learnedWords.remove(word)
                        sharedPrefs.edit().putStringSet("learned_words", learnedWords).apply()
                        showToast("کلمه «$word» از حافظه حذف شد")
                        updateSuggestions(suggestions)
                    } else {
                        showToast("این کلمه ذخیره نشده است")
                    }
                    true
                }
            } else {
                textView.text = ""
                textView.setTextColor(Color.parseColor("#ECEFF1"))
                textView.setOnClickListener(null)
                textView.setOnLongClickListener(null)
            }
        }
    }

    private fun saveWordToMemory(word: String) {
        if (word.isNotBlank() && !learnedWords.contains(word)) {
            learnedWords.add(word.trim())
            sharedPrefs.edit().putStringSet("learned_words", learnedWords).apply()
        }
    }

    private fun setupContinuousDeletion(btn: TextView, suggestions: List<TextView>) {
        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null

        btn.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    performKeyFeedback()
                    deleteCharacter()
                    
                    runnable = object : Runnable {
                        override fun run() {
                            deleteCharacter()
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

    private fun deleteCharacter() {
        val ic = currentInputConnection ?: return
        val sel = ic.getSelectedText(0)
        if (sel.isNullOrEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ic.deleteSurroundingTextInCodePoints(1, 0)
            } else {
                val text = ic.getTextBeforeCursor(2, 0)?.toString() ?: ""
                if (text.length == 2 && Character.isSurrogatePair(text[0], text[1])) {
                    ic.deleteSurroundingText(2, 0)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            }
        } else {
            ic.commitText("", 1)
        }
    }

    private fun setupLongPressPopups(view: View) {
        view.findViewById<TextView>(R.id.btn_alef)?.setOnLongClickListener {
            performKeyFeedback()
            showPopup(it, listOf(Pair("آ", "آ"), Pair("أ", "أ"), Pair("إ", "إ"), Pair("ٱ", "ٱ"), Pair("ا", "ا")))
            true
        }
        view.findViewById<TextView>(R.id.btn_te)?.setOnLongClickListener {
            performKeyFeedback()
            showPopup(it, listOf(Pair("ت", "ت"), Pair("-5-", kashida.repeat(5)), Pair("-10-", kashida.repeat(10)), Pair("-20-", kashida.repeat(20))))
            true
        }
        view.findViewById<TextView>(R.id.btn_ye)?.setOnLongClickListener {
            performKeyFeedback()
            showPopup(it, listOf(Pair("ي", "ي"), Pair("ى", "ى"), Pair("ئ", "ئ"), Pair("ی", "ی")))
            true
        }
        view.findViewById<TextView>(R.id.btn_kaf)?.setOnLongClickListener {
            performKeyFeedback()
            showPopup(it, listOf(Pair("ك", "ك"), Pair("ک", "ک")))
            true
        }
        view.findViewById<TextView>(R.id.btn_he)?.setOnLongClickListener {
            performKeyFeedback()
            showPopup(it, listOf(Pair("ة", "ة"), Pair("ه", "ه"), Pair("ۀ", "ۀ")))
            true
        }
        
        view.findViewById<TextView>(R.id.btn_sad)?.setOnLongClickListener {
            performKeyFeedback()
            showPopup(it, listOf(Pair("ض", "ض"), Pair("ص", "ص")))
            true
        }
        view.findViewById<TextView>(R.id.btn_e)?.setOnLongClickListener {
            performKeyFeedback()
            showPopup(it, listOf(Pair("غ", "غ"), Pair("ع", "ع")))
            true
        }
        view.findViewById<TextView>(R.id.btn_ta)?.setOnLongClickListener {
            performKeyFeedback()
            showPopup(it, listOf(Pair("ظ", "ظ"), Pair("ط", "ط")))
            true
        }
        view.findViewById<TextView>(R.id.btn_ze)?.setOnLongClickListener {
            performKeyFeedback()
            showPopup(it, listOf(Pair("ژ", "ژ"), Pair("ز", "ز")))
            true
        }
        view.findViewById<TextView>(R.id.btn_dal)?.setOnLongClickListener {
            performKeyFeedback()
            showPopup(it, listOf(Pair("ذ", "ذ"), Pair("د", "د")))
            true
        }
    }

    private fun showPopup(anchorView: View, items: List<Pair<String, String>>) {
        val context = anchorView.context
        val popupView = layoutInflater.inflate(R.layout.key_popup, null)
        val container = popupView.findViewById<LinearLayout>(R.id.popup_container)
        
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false 
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true

        for (item in items) {
            val tv = TextView(context)
            tv.text = item.first 
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
            tv.setBackgroundColor(Color.parseColor("#546E7A")) 

            tv.setOnClickListener {
                currentInputConnection?.commitText(item.second, 1) 
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
        closeTv.setBackgroundColor(Color.parseColor("#546E7A"))
        closeTv.setOnClickListener { popupWindow.dismiss() }
        container.addView(closeTv)

        popupWindow.showAsDropDown(anchorView, -100, -220)
    }

    private fun setupNavigation(view: View, pageBtn: TextView, suggestions: List<TextView>) {
        view.findViewById<TextView>(R.id.btn_symbols)?.setOnClickListener { 
            lettersContainer?.visibility = View.GONE
            symbolsContainer?.visibility = View.VISIBLE
            currentSymbolPage = 0
            bindSymbols(view, pageBtn) 
        }
        view.findViewById<TextView>(R.id.btn_lang_back)?.setOnClickListener { 
            symbolsContainer?.visibility = View.GONE
            lettersContainer?.visibility = View.VISIBLE 
        }
        pageBtn.setOnClickListener { 
            currentSymbolPage++
            if (currentSymbolPage >= symbolPages.size) currentSymbolPage = 0
            bindSymbols(view, pageBtn) 
        }

        view.findViewById<TextView>(R.id.btn_dot)?.setOnClickListener { 
            performKeyFeedback()
            currentInputConnection?.commitText(".", 1) 
        }
        view.findViewById<TextView>(R.id.btn_dot)?.setOnLongClickListener {
            performKeyFeedback()
            lettersContainer?.visibility = View.GONE
            diacriticsContainer?.visibility = View.VISIBLE
            true
        }
        view.findViewById<TextView>(R.id.btn_close_diacritics)?.setOnClickListener {
            diacriticsContainer?.visibility = View.GONE
            lettersContainer?.visibility = View.VISIBLE
        }
        
        val diacriticsMap = mapOf(
            R.id.diacritic_fatha to "َ", R.id.diacritic_kasra to "ِ", R.id.diacritic_damma to "ُ", 
            R.id.diacritic_shadda to "ّ", R.id.diacritic_sukun to "ْ", R.id.diacritic_fathatan to "ً", 
            R.id.diacritic_kasratan to "ٍ", R.id.diacritic_dammatan to "ٌ", R.id.diacritic_alef_kh to "ٰ"
        )
        for ((id, symbol) in diacriticsMap) {
            view.findViewById<TextView>(id)?.setOnClickListener {
                performKeyFeedback()
                currentInputConnection?.commitText(symbol, 1)
                diacriticsContainer?.visibility = View.GONE
                lettersContainer?.visibility = View.VISIBLE
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

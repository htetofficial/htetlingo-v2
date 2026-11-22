package com.htetlingo.app

import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import org.json.JSONObject

data class PhoneCheck(
    val valid: Boolean,
    val normalized: String,
    val operator: String,
    val network: String
)

object MyanmarUtils {
    private val myDigits = mapOf(
        '၀' to '0', '၁' to '1', '၂' to '2', '၃' to '3', '၄' to '4',
        '၅' to '5', '၆' to '6', '၇' to '7', '၈' to '8', '၉' to '9'
    )

    fun checkPhone(raw: String): PhoneCheck {
        var n = raw.map { myDigits[it] ?: it }.joinToString("")
            .replace(Regex("[\\s\\-()]"), "")
        if (n.startsWith("+959")) n = "09" + n.drop(4)
        else if (n.startsWith("959")) n = "09" + n.drop(3)
        else if (n.startsWith("+95")) n = "0" + n.drop(3)

        if (!Regex("^09\\d{7,9}$").matches(n)) {
            return PhoneCheck(false, n, "Unknown", "Invalid format")
        }
        return when {
            Regex("^09(9[4-8])\\d{7}$").matches(n) -> PhoneCheck(true, n, "Ooredoo", "GSM / 4G")
            Regex("^09(7[4-9])\\d{7}$").matches(n) -> PhoneCheck(true, n, "ATOM (Telenor)", "GSM / 4G")
            Regex("^09(6[5-9])\\d{7}$").matches(n) -> PhoneCheck(true, n, "Mytel", "GSM / 4G")
            Regex("^09(3[0-6])\\d{6,7}$").matches(n) -> PhoneCheck(true, n, "MEC", "CDMA / GSM")
            else -> PhoneCheck(true, n, "MPT", "GSM / 4G")
        }
    }

    fun todayMyanmarDate(): Triple<Int, String, String> {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val wd = cal.get(Calendar.DAY_OF_WEEK) - 1
        val meYear = y - if (m < 4 || (m == 4 && d < 17)) 639 else 638
        val months = listOf(
            "တန်ခူး (Tagu)", "ကဆုန် (Kason)", "နယုန် (Nayon)", "ဝါဆို (Waso)",
            "ဝါခေါင် (Wagaung)", "တော်သလင်း (Tawthalin)", "သီတင်းကျွတ် (Thadingyut)",
            "တန်ဆောင်မုန်း (Tazaungmon)", "နတ်တော် (Nadaw)", "ပြာသို (Pyatho)",
            "တပို့တွဲ (Tabodwe)", "တပေါင်း (Tabaung)"
        )
        val weekdays = listOf(
            "တနင်္ဂနွေ (Sunday)", "တနင်္လာ (Monday)", "အင်္ဂါ (Tuesday)",
            "ဗုဒ္ဓဟူး (Wednesday)", "ကြာသပတေး (Thursday)", "သောကြာ (Friday)", "စနေ (Saturday)"
        )
        return Triple(meYear, months[(m + 8) % 12], weekdays[wd.coerceIn(0, 6)])
    }

    val offlinePhrasebook: Map<String, Map<String, String>> = mapOf(
        "hello" to mapOf("en" to "Hello", "my" to "မင်္ဂလာပါ", "ja" to "こんにちは", "zh" to "你好", "ko" to "안녕하세요", "th" to "สวัสดี", "fr" to "Bonjour", "de" to "Hallo"),
        "thank you" to mapOf("en" to "Thank you", "my" to "ကျေးဇူးတင်ပါတယ်", "ja" to "ありがとうございます", "zh" to "谢谢", "ko" to "감사합니다", "th" to "ขอบคุณ", "fr" to "Merci", "de" to "Danke"),
        "good morning" to mapOf("en" to "Good morning", "my" to "မင်္ဂလာနံနက်ခင်းပါ", "ja" to "おはようございます", "zh" to "早上好", "ko" to "좋은 아침이에요", "th" to "สวัสดีตอนเช้า", "fr" to "Bonjour", "de" to "Guten Morgen"),
        "how are you" to mapOf("en" to "How are you?", "my" to "နေကောင်းလား", "ja" to "お元気ですか？", "zh" to "你好吗？", "ko" to "잘 지내세요?", "th" to "สบายดีไหม", "fr" to "Comment allez-vous ?", "de" to "Wie geht es Ihnen?"),
        "water" to mapOf("en" to "Water", "my" to "ရေ", "ja" to "水", "zh" to "水", "ko" to "물", "th" to "น้ำ", "fr" to "Eau", "de" to "Wasser")
    )

    fun translateSync(text: String, source: String, target: String): Pair<String, String> {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return "" to "Idle"
        val key = cleaned.lowercase().trimEnd('?', '.', '!')
        offlinePhrasebook[key]?.get(target)?.let { return it to "Offline Phrasebook" }

        return runCatching {
            val conn = (URL("https://htetlingo.pages.dev/api/translate").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
            }
            val payload = JSONObject().apply {
                put("text", cleaned)
                put("source", source)
                put("target", target)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            val resp = conn.inputStream.bufferedReader().readText()
            val translated = JSONObject(resp).optString("translatedText", "")
            if (translated.isNotEmpty()) translated to "Cloudflare Edge"
            else "No translation returned" to "Online"
        }.getOrElse {
            "Offline mode: connect to internet for full sentences, or tap quick phrases." to "Offline"
        }
    }
}

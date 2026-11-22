import Foundation

enum TranslationService {
    static let phrasebook: [String: [String: String]] = [
        "hello": ["en": "Hello", "my": "မင်္ဂလာပါ", "ja": "こんにちは", "zh": "你好", "ko": "안녕하세요", "th": "สวัสดี", "fr": "Bonjour", "de": "Hallo"],
        "thank you": ["en": "Thank you", "my": "ကျေးဇူးတင်ပါတယ်", "ja": "ありがとうございます", "zh": "谢谢", "ko": "감사합니다", "th": "ขอบคุณ", "fr": "Merci", "de": "Danke"],
        "good morning": ["en": "Good morning", "my": "မင်္ဂလာနံနက်ခင်းပါ", "ja": "おはようございます", "zh": "早上好", "ko": "좋은 아침이에요", "th": "สวัสดีตอนเช้า", "fr": "Bonjour", "de": "Guten Morgen"],
        "how are you": ["en": "How are you?", "my": "နေကောင်းလား", "ja": "お元気ですか？", "zh": "你好吗？", "ko": "잘 지내세요?", "th": "สบายดีไหม", "fr": "Comment allez-vous ?", "de": "Wie geht es Ihnen?"],
        "water": ["en": "Water", "my": "ရေ", "ja": "水", "zh": "水", "ko": "물", "th": "น้ำ", "fr": "Eau", "de": "Wasser"]
    ]

    static func translate(text: String, source: String, target: String) async -> (result: String, provider: String) {
        let cleaned = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleaned.isEmpty else { return ("", "idle") }

        let key = cleaned.lowercased().trimmingCharacters(in: CharacterSet(charactersIn: "?.!"))
        if let hit = phrasebook[key]?[target] {
            return (hit, "Offline Phrasebook")
        }

        guard let url = URL(string: "https://htetlingo.pages.dev/api/translate") else {
            return ("Offline mode", "offline")
        }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.timeoutInterval = 10
        let body: [String: String] = ["text": cleaned, "source": source, "target": target]
        req.httpBody = try? JSONSerialization.data(withJSONObject: body)

        do {
            let (data, response) = try await URLSession.shared.data(for: req)
            if let http = response as? HTTPURLResponse, http.statusCode == 200,
               let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let translated = json["translatedText"] as? String, !translated.isEmpty {
                return (translated, "Cloudflare Edge")
            }
        } catch {}

        return ("Offline: connect to internet for full-sentence translation.", "Offline")
    }
}

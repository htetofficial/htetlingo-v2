import Foundation

struct VocabCard: Identifiable, Hashable {
    let id: String
    let language: String
    let bcp47: String
    let word: String
    let reading: String
    let meaningEn: String
    let meaningMy: String
    let example: String
}

struct GrammarItem: Identifiable {
    let id: String
    let language: String
    let title: String
    let formula: String
    let explanationMy: String
    let example: String
}

struct QuizItem: Identifiable {
    let id: String
    let prompt: String
    let options: [String]
    let answer: String
    let explanation: String
}

enum SampleData {
    static let vocab: [VocabCard] = [
        VocabCard(id: "en-1", language: "English", bcp47: "en-US", word: "Resilience", reading: "/rɪˈzɪl.jəns/", meaningEn: "Capacity to recover quickly from difficulties", meaningMy: "အခက်အခဲများမှ လျင်မြန်စွာ ပြန်လည်နာလန်ထူနိုင်စွမ်း", example: "Language learning builds patience and resilience."),
        VocabCard(id: "ja-1", language: "Japanese", bcp47: "ja-JP", word: "言葉", reading: "ことば (kotoba)", meaningEn: "Word; language", meaningMy: "စကားလုံး၊ ဘာသာစကား", example: "新しい言葉を毎日学びます。"),
        VocabCard(id: "zh-1", language: "Chinese", bcp47: "zh-CN", word: "学习", reading: "xuéxí", meaningEn: "To study; to learn", meaningMy: "သင်ယူလေ့လာသည်", example: "我每天学习中文。"),
        VocabCard(id: "ko-1", language: "Korean", bcp47: "ko-KR", word: "언어", reading: "eoneo", meaningEn: "Language", meaningMy: "ဘာသာစကား", example: "새로운 언어를 배우는 것은 즐겁습니다."),
        VocabCard(id: "my-1", language: "Burmese", bcp47: "my-MM", word: "မင်္ဂလာပါ", reading: "ming-ga-la-ba", meaningEn: "Hello", meaningMy: "နှုတ်ခွန်းဆက်စကား", example: "မင်္ဂလာပါ၊ နေကောင်းလား။")
    ]

    static let grammar: [GrammarItem] = [
        GrammarItem(
            id: "g1",
            language: "English",
            title: "Present Perfect vs. Past Simple",
            formula: "Subject + have/has + V3",
            explanationMy: "အတိတ်မှာ ပြီးသွားပေမယ့် လက်ရှိအချိန်နဲ့ ဆက်စပ်နေတဲ့ အတွေ့အကြုံတွေအတွက် Present Perfect ကို သုံးပါတယ်။",
            example: "I have studied Japanese for two years."
        ),
        GrammarItem(
            id: "g2",
            language: "Japanese",
            title: "Topic は (wa) vs. Subject が (ga)",
            formula: "[Topic] は [Comment] です",
            explanationMy: "は (wa) ကို စကားဝိုင်းရဲ့ အဓိကအကြောင်းအရာအတွက် သုံးပြီး が (ga) ကို အချက်အလက်အသစ်ကို အထူးပြုဖို့ သုံးပါတယ်။",
            example: "私はミャンマー人です。"
        )
    ]

    static let quizzes: [QuizItem] = [
        QuizItem(
            id: "q1",
            prompt: "Choose the Burmese meaning for \"Resilience\":",
            options: ["ကြံ့ခိုင်စွာ ပြန်လည်နာလန်ထူနိုင်စွမ်း", "အမြန်နှုန်း", "တိတ်ဆိတ်ငြိမ်သက်မှု"],
            answer: "ကြံ့ခိုင်စွာ ပြန်လည်နာလန်ထူနိုင်စွမ်း",
            explanation: "Resilience means recovering quickly from difficulties."
        ),
        QuizItem(
            id: "q2",
            prompt: "What does 言葉 (kotoba) mean?",
            options: ["Word / Language", "Mountain", "Water"],
            answer: "Word / Language",
            explanation: "言葉 (ことば) means word or language in Japanese."
        )
    ]
}

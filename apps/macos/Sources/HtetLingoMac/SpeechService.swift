import AVFoundation

@MainActor
final class SpeechService {
    static let shared = SpeechService()
    private let synthesizer = AVSpeechSynthesizer()

    func speak(_ text: String, languageCode: String = "en-US", rate: Float = AVSpeechUtteranceDefaultSpeechRate) {
        synthesizer.stopSpeaking(at: .immediate)
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: languageCode)
        utterance.rate = rate
        synthesizer.speak(utterance)
    }
}

import SwiftUI
import Observation

enum NavTab: String, CaseIterable, Identifiable {
    case dictionary = "Dictionary"
    case vocabulary = "Vocabulary"
    case grammar = "Grammar"
    case quiz = "Quiz"
    case progress = "Progress & Sync"

    var id: String { rawValue }
    var icon: String {
        switch self {
        case .dictionary: return "book.closed.fill"
        case .vocabulary: return "rectangle.stack.fill"
        case .grammar: return "text.book.closed.fill"
        case .quiz: return "checkmark.seal.fill"
        case .progress: return "person.crop.circle.fill"
        }
    }
}

@MainActor
@Observable
final class AppModel {
    let store = DictionaryStore()
    var selectedTab: NavTab? = .dictionary
    var xp: Int = 30
    var query: String = "" {
        didSet { refreshSearch() }
    }
    var results: [DictionaryEntry] = []
    var selectedWord: DictionaryEntry?
    var cardIndex: Int = 0
    var flipped: Bool = false
    var quizIndex: Int = 0
    var quizSelection: String?

    init() {
        refreshSearch()
    }

    func refreshSearch() {
        results = store.search(prefix: query)
        selectedWord = results.first
    }
}

@main
struct HtetLingoMacApp: App {
    let model = AppModel()

    var body: some Scene {
        WindowGroup("HtetLingo") {
            ContentView(model: model)
                .frame(minWidth: 920, minHeight: 620)
        }
    }
}

struct ContentView: View {
    @Bindable var model: AppModel

    var body: some View {
        NavigationSplitView {
            List(NavTab.allCases, selection: $model.selectedTab) { tab in
                Label(tab.rawValue, systemImage: tab.icon)
                    .tag(tab)
            }
            .navigationTitle("HtetLingo")
            .safeAreaInset(edge: .bottom) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Daily XP: \(model.xp)")
                        .font(.headline)
                    Text(model.store.isConnected ? "SQLite Offline: \(model.store.totalWordCount()) words" : "SQLite not loaded")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(.thinMaterial)
            }
        } detail: {
            switch model.selectedTab ?? .dictionary {
            case .dictionary:
                dictionaryView
            case .vocabulary:
                vocabularyView
            case .grammar:
                grammarView
            case .quiz:
                quizView
            case .progress:
                progressView
            }
        }
    }

    private var dictionaryView: some View {
        HSplitView {
            VStack(spacing: 8) {
                TextField("Search 111,232 Ornagai EN–MY words…", text: $model.query)
                    .textFieldStyle(.roundedBorder)
                    .padding([.top, .horizontal])

                List(model.results, selection: $model.selectedWord) { entry in
                    Text(entry.word)
                        .tag(entry)
                }
            }
            .frame(minWidth: 240, maxWidth: 320)

            ScrollView {
                if let entry = model.selectedWord {
                    VStack(alignment: .leading, spacing: 14) {
                        HStack {
                            Text(entry.word)
                                .font(.largeTitle.bold())
                            Spacer()
                            Button {
                                SpeechService.shared.speak(entry.word, languageCode: "en-US")
                            } label: {
                                Label("Pronounce", systemImage: "speaker.wave.2.fill")
                            }
                            .buttonStyle(.borderedProminent)
                        }
                        Divider()
                        Text(entry.plainText)
                            .font(.body)
                            .textSelection(.enabled)
                    }
                    .padding(24)
                } else {
                    ContentUnavailableView("Select a Word", systemImage: "book.closed")
                }
            }
        }
        .navigationTitle("Ornagai Dictionary")
    }

    private var vocabularyView: some View {
        let card = SampleData.vocab[model.cardIndex % SampleData.vocab.count]
        return VStack(spacing: 20) {
            Text("Vocabulary Flashcards (SM-2)")
                .font(.title.bold())
            VStack(spacing: 12) {
                Text(card.language)
                    .font(.caption.bold())
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(.teal.opacity(0.15), in: Capsule())
                Text(card.word)
                    .font(.system(size: 42, weight: .bold))
                Text(card.reading)
                    .foregroundStyle(.secondary)
                Button {
                    SpeechService.shared.speak(card.word, languageCode: card.bcp47)
                } label: {
                    Label("Listen", systemImage: "speaker.wave.2.fill")
                }
                if model.flipped {
                    Divider()
                    Text(card.meaningEn).font(.headline)
                    Text(card.meaningMy).foregroundStyle(.teal).font(.title3)
                    Text(card.example).font(.callout).foregroundStyle(.secondary)
                } else {
                    Text("Click Reveal to check meaning")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(30)
            .frame(maxWidth: 520)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18))

            HStack(spacing: 12) {
                Button(model.flipped ? "Hide" : "Reveal") { model.flipped.toggle() }
                    .buttonStyle(.bordered)
                Button("Good (+10 XP)") {
                    model.xp += 10
                    model.flipped = false
                    model.cardIndex += 1
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding()
    }

    private var grammarView: some View {
        List(SampleData.grammar) { item in
            VStack(alignment: .leading, spacing: 8) {
                Text("\(item.language) · \(item.title)").font(.headline)
                Text(item.formula)
                    .font(.system(.callout, design: .monospaced))
                    .padding(8)
                    .background(.quaternary, in: RoundedRectangle(cornerRadius: 8))
                Text(item.explanationMy)
                Text("Example: \(item.example)").foregroundStyle(.secondary)
            }
            .padding(.vertical, 8)
        }
        .navigationTitle("Grammar Lessons")
    }

    private var quizView: some View {
        let q = SampleData.quizzes[model.quizIndex % SampleData.quizzes.count]
        return VStack(alignment: .leading, spacing: 16) {
            Text("Quiz & Exercises").font(.title.bold())
            Text(q.prompt).font(.title3)
            ForEach(q.options, id: \.self) { opt in
                Button {
                    model.quizSelection = opt
                    if opt == q.answer { model.xp += 15 }
                } label: {
                    HStack {
                        Text(opt)
                        Spacer()
                        if model.quizSelection == opt {
                            Image(systemName: opt == q.answer ? "checkmark.circle.fill" : "xmark.circle.fill")
                                .foregroundStyle(opt == q.answer ? .green : .red)
                        }
                    }
                    .padding(10)
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
            if model.quizSelection != nil {
                Text(q.explanation).foregroundStyle(.secondary)
                Button("Next Question") {
                    model.quizSelection = nil
                    model.quizIndex += 1
                }
                .buttonStyle(.borderedProminent)
            }
            Spacer()
        }
        .padding(28)
    }

    private var progressView: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Learning Progress & Cloud Sync").font(.title.bold())
            Text("Total XP: \(model.xp)").font(.title2)
            Text("Supabase Endpoint: https://tqwvzvbnintvijiessgm.supabase.co").font(.callout).foregroundStyle(.secondary)
            Text("Monetization: RevenueCat (Free · Premium · Lifetime · Language Packs)").font(.callout).foregroundStyle(.secondary)
            Spacer()
        }
        .padding(28)
    }
}

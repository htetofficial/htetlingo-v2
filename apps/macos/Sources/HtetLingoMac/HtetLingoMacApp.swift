import SwiftUI
import Observation

enum NavTab: String, CaseIterable, Identifiable {
    case dictionary = "Dictionary"
    case vocabulary = "Speed Vocabulary"
    case grammar = "Grammar"
    case quiz = "Infinite Quiz"
    case pronunciation = "Pronunciation"
    case translate = "Translate"
    case myanmar = "Myanmar Hub"
    case progress = "Progress & VIP"

    var id: String { rawValue }
    var icon: String {
        switch self {
        case .dictionary: return "book.closed.fill"
        case .vocabulary: return "bolt.fill"
        case .grammar: return "text.book.closed.fill"
        case .quiz: return "checkmark.seal.fill"
        case .pronunciation: return "waveform"
        case .translate: return "globe"
        case .myanmar: return "sparkles"
        case .progress: return "person.crop.circle.fill"
        }
    }
    var tint: Color {
        switch self {
        case .dictionary: return .teal
        case .vocabulary: return .orange
        case .grammar: return .indigo
        case .quiz: return .green
        case .pronunciation: return .pink
        case .translate: return .blue
        case .myanmar: return .purple
        case .progress: return .mint
        }
    }
}

enum DictMode: String, CaseIterable, Identifiable {
    case enMy = "EN–MY (111k)"
    case enZh = "EN–ZH (27k)"
    case enEn = "EN–EN (49k)"
    var id: String { rawValue }
}

enum QuizMode: String, CaseIterable, Identifiable {
    case enMy = "EN → MY"
    case enZh = "EN → ZH"
    case listening = "Listening"
    var id: String { rawValue }
}

struct GeneratedQuizQuestion: Identifiable {
    let id: String
    let prompt: String
    let audioWord: String?
    let options: [String]
    let answer: String
    let explanation: String
}

@MainActor
@Observable
final class AppModel {
    let store = DictionaryStore()
    var selectedTab: NavTab? = .dictionary

    // Progress & VIP
    var xp: Int = UserDefaults.standard.integer(forKey: "hl_xp") == 0 ? 35 : UserDefaults.standard.integer(forKey: "hl_xp")
    var studiedCount: Int = UserDefaults.standard.integer(forKey: "hl_studied")
    var savedWords: Set<String> = Set(UserDefaults.standard.stringArray(forKey: "hl_saved") ?? ["resilience", "language"])
    var accountEmail: String = UserDefaults.standard.string(forKey: "hl_email") ?? "htetmyanmar321@icloud.com"

    var isFounderVip: Bool {
        accountEmail.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() == "htetmyanmar321@icloud.com"
    }

    // Dictionary
    var dictMode: DictMode = .enMy { didSet { refreshDictionary() } }
    var dictQuery: String = "" { didSet { refreshDictionary() } }
    var enMyResults: [DictionaryEntry] = []
    var selectedEnMy: DictionaryEntry?
    var enZhResults: [EnZhEntry] = []
    var selectedEnZh: EnZhEntry?
    var enEnResults: [EnEnEntry] = []
    var selectedEnEn: EnEnEntry?

    // Speed Vocabulary (12,000+ DB words)
    var vocabFilter: String = "ALL" { didSet { refreshVocab() } }
    var vocabSearch: String = "" { didSet { refreshVocab() } }
    var vocabItems: [UnifiedVocabItem] = []
    var vocabIndex: Int = 0
    var speedRevealAlways: Bool = true
    var vocabFlipped: Bool = false
    var autoSpeak: Bool = true

    // Infinite Quiz
    var quizMode: QuizMode = .enMy
    var quizQuestions: [GeneratedQuizQuestion] = []
    var quizStep: Int = 0
    var quizSelected: String?
    var quizScore: Int = 0
    var quizFinished: Bool = false

    // Pronunciation
    var pronLangCode: String = "en-US"
    var pronText: String = "Learn languages, understand words, connect cultures."
    var pronRate: Float = 0.48

    // Translate
    var transSource: String = "en"
    var transTarget: String = "my"
    var transInput: String = "Hello"
    var transOutput: String = "မင်္ဂလာပါ"
    var transProvider: String = "Offline Phrasebook"
    var transLoading: Bool = false

    // Myanmar Hub
    var nameQuery: String = "" { didSet { refreshNames() } }
    var myanmarNames: [MyanmarNamePair] = []
    var phoneInput: String = "09791234567"
    var calendarDate: Date = Date()

    // Cached SQLite stats
    let totalEnMy: Int
    let totalEnZh: Int
    let totalEnEn: Int
    let totalVocab: Int
    let totalNames: Int

    init() {
        self.totalEnMy = store.tableCount("entries")
        self.totalEnZh = store.tableCount("ecdict_en_zh")
        self.totalEnEn = store.tableCount("en_en_entries")
        self.totalVocab = store.tableCount("unified_vocab")
        self.totalNames = store.tableCount("myanmar_names")
        refreshDictionary()
        refreshVocab()
        refreshNames()
        generateQuiz(mode: .enMy)
    }

    func gainXp(_ amount: Int) {
        xp += amount
        UserDefaults.standard.set(xp, forKey: "hl_xp")
    }

    func toggleSave(_ word: String) {
        if savedWords.contains(word) { savedWords.remove(word) }
        else { savedWords.insert(word) }
        UserDefaults.standard.set(Array(savedWords), forKey: "hl_saved")
    }

    func saveEmail(_ email: String) {
        accountEmail = email
        UserDefaults.standard.set(email, forKey: "hl_email")
    }

    func refreshDictionary() {
        switch dictMode {
        case .enMy:
            enMyResults = store.searchEnMy(prefix: dictQuery)
            selectedEnMy = enMyResults.first
        case .enZh:
            enZhResults = store.searchEnZh(prefix: dictQuery)
            selectedEnZh = enZhResults.first
        case .enEn:
            enEnResults = store.searchEnEn(prefix: dictQuery)
            selectedEnEn = enEnResults.first
        }
    }

    func refreshVocab() {
        vocabItems = store.fetchUnifiedVocab(filter: vocabFilter, search: vocabSearch, limit: 300)
        vocabIndex = 0
        vocabFlipped = false
    }

    func advanceVocab(xpGain: Int) {
        gainXp(xpGain)
        studiedCount += 1
        UserDefaults.standard.set(studiedCount, forKey: "hl_studied")
        guard !vocabItems.isEmpty else { return }
        vocabIndex = (vocabIndex + 1) % vocabItems.count
        vocabFlipped = false
        if autoSpeak {
            SpeechService.shared.speak(vocabItems[vocabIndex].word, languageCode: "en-US")
        }
    }

    func jumpRandomVocab() {
        guard !vocabItems.isEmpty else { return }
        vocabIndex = Int.random(in: 0..<vocabItems.count)
        vocabFlipped = false
        if autoSpeak {
            SpeechService.shared.speak(vocabItems[vocabIndex].word, languageCode: "en-US")
        }
    }

    func generateQuiz(mode: QuizMode) {
        quizMode = mode
        let pool = store.randomVocabSample(count: 45)
        guard pool.count >= 12 else { return }
        var built: [GeneratedQuizQuestion] = []
        for i in 0..<10 {
            let item = pool[i]
            let distractors = pool.filter { $0.id != item.id }.shuffled().prefix(3)
            switch mode {
            case .enMy:
                let opts = ([item.myDef] + distractors.map(\.myDef)).shuffled()
                built.append(GeneratedQuizQuestion(
                    id: "my-\(i)-\(item.id)",
                    prompt: "Choose the Burmese meaning of \"\(item.word)\" \(item.ipa.isEmpty ? "" : "/\(item.ipa)/"):",
                    audioWord: nil,
                    options: opts,
                    answer: item.myDef,
                    explanation: "\(item.word) → 🇲🇲 \(item.myDef) · 🇨🇳 \(item.zhDef)"
                ))
            case .enZh:
                let opts = ([item.zhDef] + distractors.map(\.zhDef)).shuffled()
                built.append(GeneratedQuizQuestion(
                    id: "zh-\(i)-\(item.id)",
                    prompt: "Choose the Chinese meaning of \"\(item.word)\":",
                    audioWord: nil,
                    options: opts,
                    answer: item.zhDef,
                    explanation: "\(item.word) → 🇨🇳 \(item.zhDef) · 🇲🇲 \(item.myDef)"
                ))
            case .listening:
                let opts = ([item.word] + distractors.map(\.word)).shuffled()
                built.append(GeneratedQuizQuestion(
                    id: "ls-\(i)-\(item.id)",
                    prompt: "Listen and choose the exact English word you heard:",
                    audioWord: item.word,
                    options: opts,
                    answer: item.word,
                    explanation: "\(item.word) → 🇲🇲 \(item.myDef)"
                ))
            }
        }
        quizQuestions = built
        quizStep = 0
        quizSelected = nil
        quizScore = 0
        quizFinished = false
    }

    func refreshNames() {
        myanmarNames = store.searchMyanmarNames(query: nameQuery, limit: 60)
    }

    func runTranslation() {
        transLoading = true
        let text = transInput
        let src = transSource
        let dst = transTarget
        Task {
            let res = await TranslationService.translate(text: text, source: src, target: dst)
            self.transOutput = res.result
            self.transProvider = res.provider
            self.transLoading = false
            self.gainXp(3)
        }
    }
}

@main
struct HtetLingoMacApp: App {
    let model = AppModel()

    var body: some Scene {
        WindowGroup("HtetLingo") {
            ContentView(model: model)
                .frame(minWidth: 1020, minHeight: 680)
        }
        .windowStyle(.titleBar)
        .windowToolbarStyle(.unified(showsTitle: true))
        .commands {
            CommandMenu("Navigate") {
                ForEach(Array(NavTab.allCases.enumerated()), id: \.element.id) { idx, tab in
                    Button(tab.rawValue) {
                        model.selectedTab = tab
                    }
                    .keyboardShortcut(KeyEquivalent(Character("\(idx + 1)")), modifiers: .command)
                }
            }
        }
    }
}

struct ContentView: View {
    @Bindable var model: AppModel

    var body: some View {
        NavigationSplitView {
            List(selection: $model.selectedTab) {
                Section("Learn") {
                    sidebarRow(.dictionary)
                    sidebarRow(.vocabulary)
                    sidebarRow(.grammar)
                    sidebarRow(.quiz)
                }
                Section("Practice & Reference") {
                    sidebarRow(.pronunciation)
                    sidebarRow(.translate)
                    sidebarRow(.myanmar)
                }
                Section("Account") {
                    sidebarRow(.progress)
                }
            }
            .listStyle(.sidebar)
            .navigationSplitViewColumnWidth(min: 205, ideal: 225, max: 260)
            .safeAreaInset(edge: .bottom) {
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Image(systemName: model.isFounderVip ? "crown.fill" : "flame.fill")
                            .foregroundStyle(model.isFounderVip ? .yellow : .orange)
                        Text(model.isFounderVip ? "Founder VIP · Lifetime" : "Daily XP: \(model.xp)")
                            .font(.subheadline.weight(.semibold))
                    }
                    Text("\(model.totalEnMy + model.totalEnZh + model.totalEnEn) offline entries ready")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(.ultraThinMaterial)
                .overlay(alignment: .top) { Divider() }
            }
        } detail: {
            Group {
                switch model.selectedTab ?? .dictionary {
                case .dictionary: DictionaryDetailView(model: model)
                case .vocabulary: SpeedVocabView(model: model)
                case .grammar: GrammarDetailView(model: model)
                case .quiz: InfiniteQuizView(model: model)
                case .pronunciation: PronunciationStudioView(model: model)
                case .translate: TranslateStudioView(model: model)
                case .myanmar: MyanmarHubView(model: model)
                case .progress: ProgressVipView(model: model)
                }
            }
        }
    }

    private func sidebarRow(_ tab: NavTab) -> some View {
        Label {
            Text(tab.rawValue)
        } icon: {
            Image(systemName: tab.icon)
                .foregroundStyle(tab.tint)
        }
        .tag(tab)
    }
}

// MARK: - 1. Multilingual Dictionary View (Apple HIG Split View)
struct DictionaryDetailView: View {
    @Bindable var model: AppModel

    var body: some View {
        HSplitView {
            List {
                switch model.dictMode {
                case .enMy:
                    ForEach(model.enMyResults) { entry in
                        HStack {
                            Text(entry.word)
                            Spacer()
                            if model.savedWords.contains(entry.word) {
                                Image(systemName: "bookmark.fill")
                                    .font(.caption)
                                    .foregroundStyle(.teal)
                            }
                        }
                        .contentShape(Rectangle())
                        .padding(.vertical, 2)
                        .listRowBackground(model.selectedEnMy?.id == entry.id ? Color.accentColor.opacity(0.18) : Color.clear)
                        .onTapGesture {
                            model.selectedEnMy = entry
                            model.gainXp(1)
                        }
                    }
                case .enZh:
                    ForEach(model.enZhResults) { entry in
                        HStack {
                            Text(entry.word)
                            Spacer()
                            if let firstTag = entry.tag.split(separator: " ").first {
                                Text(String(firstTag).uppercased())
                                    .font(.caption2.bold())
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(.orange.opacity(0.15), in: Capsule())
                            }
                        }
                        .contentShape(Rectangle())
                        .padding(.vertical, 2)
                        .listRowBackground(model.selectedEnZh?.id == entry.id ? Color.accentColor.opacity(0.18) : Color.clear)
                        .onTapGesture {
                            model.selectedEnZh = entry
                            model.gainXp(1)
                        }
                    }
                case .enEn:
                    ForEach(model.enEnResults) { entry in
                        Text(entry.word)
                            .contentShape(Rectangle())
                            .padding(.vertical, 2)
                            .listRowBackground(model.selectedEnEn?.id == entry.id ? Color.accentColor.opacity(0.18) : Color.clear)
                            .onTapGesture {
                                model.selectedEnEn = entry
                                model.gainXp(1)
                            }
                    }
                }
            }
            .listStyle(.inset)
            .frame(minWidth: 230, idealWidth: 260, maxWidth: 320)

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    switch model.dictMode {
                    case .enMy:
                        if let e = model.selectedEnMy {
                            wordHeader(word: e.word, subtitle: "Ornagai English → Burmese (Offline)")
                            Divider()
                            Text(e.plainText)
                                .font(.body)
                                .lineSpacing(6)
                                .textSelection(.enabled)
                        } else {
                            ContentUnavailableView("Search Ornagai Dictionary", systemImage: "book.closed", description: Text("111,232 English–Burmese headwords available offline."))
                        }
                    case .enZh:
                        if let e = model.selectedEnZh {
                            wordHeader(word: e.word, subtitle: e.phonetic.isEmpty ? "ECDICT English → Chinese" : "/\(e.phonetic)/ · ECDICT EN–ZH")
                            if !e.tag.isEmpty {
                                HStack {
                                    ForEach(e.tag.split(separator: " ").map(String.init), id: \.self) { t in
                                        Text(t.uppercased())
                                            .font(.caption.bold())
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 3)
                                            .background(.orange.opacity(0.16), in: Capsule())
                                    }
                                }
                            }
                            Divider()
                            VStack(alignment: .leading, spacing: 8) {
                                ForEach(e.translation.components(separatedBy: "; "), id: \.self) { line in
                                    Text(line).font(.title3).textSelection(.enabled)
                                }
                            }
                        } else {
                            ContentUnavailableView("Search ECDICT", systemImage: "character.book.closed")
                        }
                    case .enEn:
                        if let e = model.selectedEnEn {
                            wordHeader(word: e.word, subtitle: "English → English Dictionary (49,537 words)")
                            Divider()
                            VStack(alignment: .leading, spacing: 10) {
                                ForEach(Array(e.definitions.enumerated()), id: \.offset) { idx, def in
                                    HStack(alignment: .top, spacing: 8) {
                                        Text("\(idx + 1).").font(.headline).foregroundStyle(.secondary)
                                        Text(def).font(.body).textSelection(.enabled)
                                    }
                                }
                            }
                        } else {
                            ContentUnavailableView("Search English–English", systemImage: "text.book.closed")
                        }
                    }
                }
                .padding(28)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .background(Color(nsColor: .textBackgroundColor))
        }
        .navigationTitle("Dictionary")
        .searchable(text: $model.dictQuery, placement: .toolbar, prompt: "Search 188,630+ offline headwords…")
        .toolbar {
            ToolbarItem(placement: .principal) {
                Picker("Dictionary", selection: $model.dictMode) {
                    ForEach(DictMode.allCases) { m in
                        Text(m.rawValue).tag(m)
                    }
                }
                .pickerStyle(.segmented)
                .frame(width: 310)
            }
        }
    }

    private func wordHeader(word: String, subtitle: String) -> some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 4) {
                Text(word)
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Button {
                SpeechService.shared.speak(word, languageCode: "en-US")
            } label: {
                Label("Listen", systemImage: "speaker.wave.2.fill")
            }
            .buttonStyle(.borderedProminent)
            .keyboardShortcut("p", modifiers: .command)

            Button {
                model.toggleSave(word)
            } label: {
                Image(systemName: model.savedWords.contains(word) ? "bookmark.fill" : "bookmark")
            }
            .buttonStyle(.bordered)
            .keyboardShortcut("b", modifiers: .command)
        }
    }
}

// MARK: - 2. Speed Vocabulary (12,000+ Auto-Generated Words)
struct SpeedVocabView: View {
    @Bindable var model: AppModel
    private let filters = ["ALL", "IELTS", "TOEFL", "CET4", "CET6", "GRE", "A2", "B1", "B2", "C1"]

    var body: some View {
        VStack(spacing: 18) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(filters, id: \.self) { f in
                        Button(f) { model.vocabFilter = f }
                            .buttonStyle(.bordered)
                            .tint(model.vocabFilter == f ? .accentColor : .secondary)
                    }
                }
                .padding(.horizontal, 24)
            }
            .padding(.top, 12)

            if !model.vocabItems.isEmpty {
                let item = model.vocabItems[model.vocabIndex % model.vocabItems.count]
                VStack(spacing: 16) {
                    HStack {
                        Text(item.level)
                            .font(.caption.bold())
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(.teal.opacity(0.16), in: Capsule())
                        if !item.tag.isEmpty {
                            Text(item.tag.uppercased())
                                .font(.caption.bold())
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(.orange.opacity(0.16), in: Capsule())
                        }
                        Spacer()
                        Text("#\((model.vocabIndex % model.vocabItems.count) + 1) of \(model.totalVocab) DB words")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }

                    Text(item.word)
                        .font(.system(size: 46, weight: .bold, design: .rounded))
                    if !item.ipa.isEmpty {
                        Text("/\(item.ipa)/")
                            .font(.title3)
                            .foregroundStyle(.secondary)
                    }

                    Button {
                        SpeechService.shared.speak(item.word, languageCode: "en-US")
                    } label: {
                        Label("Pronounce", systemImage: "speaker.wave.2.fill")
                    }
                    .buttonStyle(.bordered)

                    if model.speedRevealAlways || model.vocabFlipped {
                        Divider()
                        VStack(alignment: .leading, spacing: 10) {
                            Label(item.myDef, systemImage: "m.circle.fill")
                                .font(.title3.weight(.semibold))
                                .foregroundStyle(.teal)
                            Label(item.zhDef, systemImage: "c.circle.fill")
                                .font(.body)
                            if !item.enDef.isEmpty {
                                Label(item.enDef, systemImage: "e.circle.fill")
                                    .font(.callout)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    } else {
                        Button("Reveal Meanings") { model.vocabFlipped = true }
                            .buttonStyle(.borderedProminent)
                    }
                }
                .padding(28)
                .frame(maxWidth: 660)
                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20))
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.primary.opacity(0.08)))

                HStack(spacing: 14) {
                    Button { model.advanceVocab(xpGain: 4) } label: {
                        Label("Again", systemImage: "arrow.counterclockwise")
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.large)

                    Button { model.advanceVocab(xpGain: 8) } label: {
                        Label("Got It (+8 XP)", systemImage: "checkmark.circle.fill")
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)

                    Button { model.jumpRandomVocab() } label: {
                        Label("Random", systemImage: "shuffle")
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.large)
                }
            } else {
                ContentUnavailableView("No Matching Words", systemImage: "bolt.slash")
            }
            Spacer()
        }
        .navigationTitle("Speed Vocabulary")
        .searchable(text: $model.vocabSearch, placement: .toolbar, prompt: "Filter 12,000+ words…")
        .toolbar {
            ToolbarItemGroup {
                Toggle("Speed Mode", isOn: $model.speedRevealAlways)
                Toggle("Auto-Speak", isOn: $model.autoSpeak)
            }
        }
    }
}

// MARK: - 3. Grammar Studio
struct GrammarDetailView: View {
    @Bindable var model: AppModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                ForEach(SampleData.grammar) { item in
                    GroupBox {
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                Text(item.language)
                                    .font(.caption.bold())
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 3)
                                    .background(.indigo.opacity(0.16), in: Capsule())
                                Text(item.title).font(.headline)
                                Spacer()
                                Button {
                                    SpeechService.shared.speak(item.example, languageCode: item.language == "Japanese" ? "ja-JP" : "en-US")
                                } label: {
                                    Image(systemName: "speaker.wave.2.fill")
                                }
                                .buttonStyle(.borderless)
                            }
                            Text(item.formula)
                                .font(.system(.callout, design: .monospaced))
                                .padding(10)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 8))
                            Text(item.explanationMy)
                            Text("Example: \(item.example)")
                                .font(.callout)
                                .foregroundStyle(.secondary)
                        }
                        .padding(6)
                    }
                }
            }
            .padding(24)
        }
        .navigationTitle("Grammar Studio")
    }
}

// MARK: - 4. Infinite Auto-Generated Quiz
struct InfiniteQuizView: View {
    @Bindable var model: AppModel

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            if model.quizFinished {
                VStack(spacing: 16) {
                    Image(systemName: "checkmark.seal.fill")
                        .font(.system(size: 56))
                        .foregroundStyle(.green)
                    Text("Score: \(model.quizScore) / \(model.quizQuestions.count)")
                        .font(.largeTitle.bold())
                    Text("You earned +\(model.quizScore * 15) XP!")
                        .foregroundStyle(.secondary)
                    Button {
                        model.generateQuiz(mode: model.quizMode)
                    } label: {
                        Label("Generate Fresh 10-Word Quiz", systemImage: "sparkles")
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if model.quizStep < model.quizQuestions.count {
                let q = model.quizQuestions[model.quizStep]
                ProgressView(value: Double(model.quizStep), total: Double(model.quizQuestions.count))
                Text("Question \(model.quizStep + 1) of \(model.quizQuestions.count)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(q.prompt)
                    .font(.title2.bold())

                if let audio = q.audioWord {
                    Button {
                        SpeechService.shared.speak(audio, languageCode: "en-US")
                    } label: {
                        Label("Play Audio Prompt", systemImage: "speaker.wave.2.fill")
                    }
                    .buttonStyle(.borderedProminent)
                }

                VStack(spacing: 10) {
                    ForEach(q.options, id: \.self) { opt in
                        let chosen = model.quizSelected == opt
                        let correct = opt == q.answer
                        Button {
                            guard model.quizSelected == nil else { return }
                            model.quizSelected = opt
                            if correct {
                                model.quizScore += 1
                                model.gainXp(15)
                            }
                        } label: {
                            HStack {
                                Text(opt).multilineTextAlignment(.leading)
                                Spacer()
                                if model.quizSelected != nil && correct {
                                    Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
                                } else if chosen && !correct {
                                    Image(systemName: "xmark.circle.fill").foregroundStyle(.red)
                                }
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                    }
                }

                if model.quizSelected != nil {
                    GroupBox {
                        HStack {
                            Text(q.explanation).font(.callout)
                            Spacer()
                            Button(model.quizStep + 1 >= model.quizQuestions.count ? "Finish" : "Next") {
                                if model.quizStep + 1 >= model.quizQuestions.count {
                                    model.quizFinished = true
                                } else {
                                    model.quizStep += 1
                                    model.quizSelected = nil
                                }
                            }
                            .buttonStyle(.borderedProminent)
                        }
                    }
                }
                Spacer()
            }
        }
        .padding(28)
        .navigationTitle("Infinite Quiz")
        .toolbar {
            ToolbarItem(placement: .principal) {
                Picker("Quiz Mode", selection: Binding(
                    get: { model.quizMode },
                    set: { model.generateQuiz(mode: $0) }
                )) {
                    ForEach(QuizMode.allCases) { m in
                        Text(m.rawValue).tag(m)
                    }
                }
                .pickerStyle(.segmented)
                .frame(width: 280)
            }
            ToolbarItem {
                Button {
                    model.generateQuiz(mode: model.quizMode)
                } label: {
                    Label("New Set", systemImage: "arrow.clockwise")
                }
            }
        }
    }
}

// MARK: - 5. Pronunciation Studio
struct PronunciationStudioView: View {
    @Bindable var model: AppModel
    private let langs: [(name: String, code: String, sample: String)] = [
        ("🇬🇧 English", "en-US", "Learn languages, understand words, connect cultures."),
        ("🇯🇵 Japanese", "ja-JP", "新しい言葉を毎日学びます。"),
        ("🇨🇳 Chinese", "zh-CN", "我每天学习中文和英语。"),
        ("🇰🇷 Korean", "ko-KR", "안녕하세요, 만나서 반갑습니다."),
        ("🇹🇭 Thai", "th-TH", "สวัสดีครับ ยินดีที่ได้รู้จัก"),
        ("🇫🇷 French", "fr-FR", "Bonjour, je veux apprendre le français."),
        ("🇩🇪 German", "de-DE", "Sprache verbindet Menschen weltweit.")
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Picker("Voice Language", selection: $model.pronLangCode) {
                ForEach(langs, id: \.code) { l in
                    Text(l.name).tag(l.code)
                }
            }
            .pickerStyle(.segmented)
            .onChange(of: model.pronLangCode) { _, newVal in
                if let match = langs.first(where: { $0.code == newVal }) {
                    model.pronText = match.sample
                }
            }

            GroupBox("Practice Text") {
                TextEditor(text: $model.pronText)
                    .font(.title3)
                    .frame(height: 120)
            }

            HStack(spacing: 16) {
                Button {
                    SpeechService.shared.speak(model.pronText, languageCode: model.pronLangCode, rate: model.pronRate)
                    model.gainXp(5)
                } label: {
                    Label("Speak Now (AVSpeechSynthesizer)", systemImage: "waveform.circle.fill")
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)

                VStack(alignment: .leading) {
                    Text("Speed: \(String(format: "%.2f", model.pronRate))")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Slider(value: $model.pronRate, in: 0.30...0.65)
                        .frame(width: 200)
                }
            }
            Spacer()
        }
        .padding(28)
        .navigationTitle("Pronunciation Studio")
    }
}

// MARK: - 6. Live Translation Studio
struct TranslateStudioView: View {
    @Bindable var model: AppModel
    private let codes: [(String, String)] = [
        ("en", "🇬🇧 English"), ("my", "🇲🇲 Burmese"), ("ja", "🇯🇵 Japanese"),
        ("zh", "🇨🇳 Chinese"), ("ko", "🇰🇷 Korean"), ("th", "🇹🇭 Thai"),
        ("fr", "🇫🇷 French"), ("de", "🇩🇪 German")
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack {
                Picker("From", selection: $model.transSource) {
                    ForEach(codes, id: \.0) { c, n in Text(n).tag(c) }
                }
                Button {
                    let s = model.transSource
                    model.transSource = model.transTarget
                    model.transTarget = s
                } label: {
                    Image(systemName: "arrow.left.arrow.right")
                }
                Picker("To", selection: $model.transTarget) {
                    ForEach(codes, id: \.0) { c, n in Text(n).tag(c) }
                }
                Spacer()
                Text(model.transProvider)
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(.blue.opacity(0.15), in: Capsule())
            }

            HStack(spacing: 16) {
                GroupBox("Source") {
                    TextEditor(text: $model.transInput)
                        .font(.title3)
                        .frame(minHeight: 180)
                }
                GroupBox("Translation") {
                    ScrollView {
                        Text(model.transOutput)
                            .font(.title3)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .textSelection(.enabled)
                            .padding(6)
                    }
                    .frame(minHeight: 180)
                }
            }

            HStack {
                Button {
                    model.runTranslation()
                } label: {
                    Label(model.transLoading ? "Translating…" : "Translate", systemImage: "globe")
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .disabled(model.transLoading)

                Button {
                    SpeechService.shared.speak(model.transOutput, languageCode: model.transTarget)
                } label: {
                    Label("Speak Translation", systemImage: "speaker.wave.2")
                }
                .buttonStyle(.bordered)
                .controlSize(.large)
            }
            Spacer()
        }
        .padding(28)
        .navigationTitle("Multilingual Translation")
    }
}

// MARK: - 7. Myanmar Culture & Reference Hub
struct MyanmarHubView: View {
    @Bindable var model: AppModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HStack(alignment: .top, spacing: 16) {
                    GroupBox("Myanmar Calendar (mmcal)") {
                        let info = MyanmarUtilities.myanmarDateSummary(for: model.calendarDate)
                        VStack(alignment: .leading, spacing: 8) {
                            DatePicker("Date", selection: $model.calendarDate, displayedComponents: .date)
                            Divider()
                            Text("မြန်မာသက္ကရာဇ် \(info.year) ခုနှစ်")
                                .font(.title2.bold())
                            Text(info.month).foregroundStyle(.teal).font(.headline)
                            Text(info.weekday).foregroundStyle(.secondary)
                        }
                        .padding(6)
                    }

                    GroupBox("Myanmar Phone Checker") {
                        let check = MyanmarUtilities.checkPhone(model.phoneInput)
                        VStack(alignment: .leading, spacing: 8) {
                            TextField("09791234567", text: $model.phoneInput)
                                .textFieldStyle(.roundedBorder)
                            Divider()
                            HStack {
                                Text(check.operatorName).font(.title2.bold())
                                Spacer()
                                Text(check.valid ? "Valid" : "Invalid")
                                    .font(.caption.bold())
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 3)
                                    .background((check.valid ? Color.green : Color.red).opacity(0.18), in: Capsule())
                            }
                            Text("\(check.normalized) · \(check.network)")
                                .foregroundStyle(.secondary)
                        }
                        .padding(6)
                    }
                }

                GroupBox("Myanmar Names Transliteration (\(model.totalNames) offline pairs)") {
                    VStack(alignment: .leading, spacing: 10) {
                        TextField("Search English or Burmese name (e.g., Htet, Aung, Su, အောင်)…", text: $model.nameQuery)
                            .textFieldStyle(.roundedBorder)
                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 180))], spacing: 10) {
                            ForEach(model.myanmarNames) { pair in
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(pair.en).font(.headline)
                                    Text(pair.mm).foregroundStyle(.teal)
                                }
                                .padding(10)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color.primary.opacity(0.04), in: RoundedRectangle(cornerRadius: 10))
                            }
                        }
                    }
                    .padding(6)
                }
            }
            .padding(24)
        }
        .navigationTitle("Myanmar Culture Hub")
    }
}

// MARK: - 8. Progress, Offline Stats & Founder VIP
struct ProgressVipView: View {
    @Bindable var model: AppModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HStack(spacing: 20) {
                    GroupBox("Daily Goal") {
                        HStack(spacing: 20) {
                            Gauge(value: min(Double(model.xp), 100.0), in: 0...100) {
                                Text("XP")
                            } currentValueLabel: {
                                Text("\(model.xp)")
                            }
                            .gaugeStyle(.accessoryCircular)
                            .scaleEffect(1.4)
                            .padding(10)

                            VStack(alignment: .leading, spacing: 4) {
                                Text("\(model.xp) Total XP").font(.title2.bold())
                                Text("Studied Vocabulary: \(model.studiedCount) cards").foregroundStyle(.secondary)
                                Text("Saved Bookmarks: \(model.savedWords.count) words").foregroundStyle(.secondary)
                            }
                        }
                        .padding(8)
                    }

                    GroupBox("Account & Founder VIP") {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Image(systemName: model.isFounderVip ? "crown.fill" : "person.fill")
                                    .foregroundStyle(model.isFounderVip ? .yellow : .secondary)
                                Text(model.isFounderVip ? "Founder VIP · Lifetime Free Unlocked" : "Free Tier")
                                    .font(.headline)
                            }
                            TextField("Account Email", text: Binding(
                                get: { model.accountEmail },
                                set: { model.saveEmail($0) }
                            ))
                            .textFieldStyle(.roundedBorder)
                            Text("All Premium features & Language Packs are permanently unlocked free for htetmyanmar321@icloud.com.")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        .padding(8)
                    }
                }

                GroupBox("Bundled Offline SQLite Database") {
                    VStack(alignment: .leading, spacing: 8) {
                        statRow("Ornagai English–Myanmar", "\(model.totalEnMy) headwords")
                        statRow("English–English Dictionary", "\(model.totalEnEn) headwords")
                        statRow("ECDICT English–Chinese", "\(model.totalEnZh) core words")
                        statRow("Auto-Generated Unified Vocabulary", "\(model.totalVocab) trilingual words")
                        statRow("Myanmar Names EN ↔ MY", "\(model.totalNames) pairs")
                    }
                    .padding(6)
                }
            }
            .padding(24)
        }
        .navigationTitle("Progress & VIP Account")
    }

    private func statRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
            Spacer()
            Text(value).font(.headline).foregroundStyle(.teal)
        }
    }
}

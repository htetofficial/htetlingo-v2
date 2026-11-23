import SwiftUI
import Combine

struct CefrPlacementQuestion: Identifiable {
    let id: String
    let level: String
    let item: UnifiedVocabItem
    let options: [String]
}

struct CefrStudioView: View {
    @Bindable var model: AppModel



    private let levels = ["A1", "A2", "B1", "B2", "C1", "C2"]

    func colorFor(_ level: String) -> Color {
        switch level {
        case "A1": return .green
        case "A2": return .teal
        case "B1": return .blue
        case "B2": return .indigo
        case "C1": return .orange
        default: return .pink
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Picker("CEFR Tool", selection: $model.cefrSubTab) {
                    Text("A1–C2 Placement Test").tag(0)
                    Text("Text CEFR Grader").tag(1)
                    Text("🎓 Official CEFR Exam (30 Qs)").tag(2)
                }
                .pickerStyle(.segmented)
                .frame(maxWidth: 560)

                switch model.cefrSubTab {
                case 0:
                    placementView
                case 1:
                    graderView
                default:
                    authenticExamView
                }
            }
            .padding(24)
        }
        .navigationTitle("CEFR Studio & Diagnostic Exam")
        .onAppear {
            if model.examQuestions.isEmpty {
                model.examQuestions = model.store.fetchCefrExamQuestions(count: 30)
            }
        }
    }

    // MARK: - Official CEFR Examination
    private var authenticExamView: some View {
        VStack(alignment: .leading, spacing: 16) {
            if !model.examSubmitted {
                // Header with Timer & Progress
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Official CEFR Diagnostic Examination")
                            .font(.title2.bold())
                        Text("Question \(model.examIndex + 1) of \(model.examQuestions.count) · 25-Minute Timed Session")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()

                    // Countdown Timer Pill
                    let mins = model.examSecondsRemaining / 60
                    let secs = model.examSecondsRemaining % 60
                    HStack(spacing: 6) {
                        Image(systemName: "timer")
                        Text(String(format: "%02d:%02d", mins, secs))
                            .font(.system(.title3, design: .monospaced).bold())
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(model.examSecondsRemaining < 300 ? Color.red.opacity(0.18) : Color.indigo.opacity(0.16), in: Capsule())
                    .foregroundStyle(model.examSecondsRemaining < 300 ? .red : .indigo)
                }

                Divider()

                if let currentQ = model.examQuestions.indices.contains(model.examIndex) ? model.examQuestions[model.examIndex] : nil {
                    // Question Card
                    VStack(alignment: .leading, spacing: 14) {
                        HStack {
                            Text(currentQ.cefrLevel)
                                .font(.caption.bold())
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(colorFor(currentQ.cefrLevel).opacity(0.18), in: Capsule())
                                .foregroundStyle(colorFor(currentQ.cefrLevel))

                            Text(currentQ.category)
                                .font(.caption.bold())
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(Color.secondary.opacity(0.12), in: Capsule())

                            Spacer()

                            Button {
                                if model.examFlagged.contains(model.examIndex) {
                                    model.examFlagged.remove(model.examIndex)
                                } else {
                                    model.examFlagged.insert(model.examIndex)
                                }
                            } label: {
                                Label("Flag for Review", systemImage: model.examFlagged.contains(model.examIndex) ? "flag.fill" : "flag")
                            }
                            .buttonStyle(.borderless)
                            .foregroundStyle(model.examFlagged.contains(model.examIndex) ? .orange : .secondary)
                        }

                        // Audio button if applicable
                        if currentQ.category.contains("Listening") || !currentQ.audioText.isEmpty {
                            HStack {
                                Button {
                                    SpeechService.shared.speak(currentQ.audioText, languageCode: "en-US")
                                } label: {
                                    Label("Play 🇺🇸 US Audio", systemImage: "speaker.wave.2.fill")
                                }
                                .buttonStyle(.bordered)

                                Button {
                                    SpeechService.shared.speak(currentQ.audioText, languageCode: "en-GB")
                                } label: {
                                    Label("Play 🇬🇧 UK Audio", systemImage: "speaker.wave.2.fill")
                                }
                                .buttonStyle(.bordered)
                            }
                        }

                        Text(currentQ.question)
                            .font(.title3.weight(.medium))
                            .padding(.vertical, 4)

                        // 4 Multiple Choice Options
                        VStack(spacing: 8) {
                            ForEach(0..<currentQ.options.count, id: \.self) { optIdx in
                                let isSelected = model.examAnswers[model.examIndex] == optIdx
                                Button {
                                    model.examAnswers[model.examIndex] = optIdx
                                } label: {
                                    HStack {
                                        Text(["A", "B", "C", "D"][optIdx])
                                            .font(.caption.bold())
                                            .frame(width: 26, height: 26)
                                            .background(isSelected ? Color.indigo : Color.secondary.opacity(0.15), in: Circle())
                                            .foregroundStyle(isSelected ? .white : .primary)

                                        Text(currentQ.options[optIdx])
                                            .font(.body)
                                            .multilineTextAlignment(.leading)

                                        Spacer()

                                        if isSelected {
                                            Image(systemName: "checkmark.circle.fill")
                                                .foregroundStyle(.indigo)
                                        }
                                    }
                                    .padding(12)
                                    .frame(maxWidth: .infinity)
                                    .background(isSelected ? Color.indigo.opacity(0.1) : Color.primary.opacity(0.03), in: RoundedRectangle(cornerRadius: 10))
                                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(isSelected ? Color.indigo : Color.clear, lineWidth: 1.5))
                                }
                                .buttonStyle(.borderless)
                            }
                        }

                        // Navigation Buttons
                        HStack {
                            Button("Previous") {
                                if model.examIndex > 0 { model.examIndex -= 1 }
                            }
                            .disabled(model.examIndex == 0)

                            Spacer()

                            if model.examIndex < model.examQuestions.count - 1 {
                                Button("Next Question") {
                                    model.examIndex += 1
                                }
                                .buttonStyle(.borderedProminent)
                            } else {
                                Button("Submit Exam (အပြီးသတ်တင်မည်)") {
                                    model.examSubmitted = true
                                    model.gainXp(100)
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(.green)
                            }
                        }
                        .padding(.top, 10)
                    }
                    .padding(22)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))

                    // Question Navigator Grid (1..30)
                    GroupBox("Question Navigator (Answered: \(model.examAnswers.count)/\(model.examQuestions.count))") {
                        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 10), spacing: 6) {
                            ForEach(0..<model.examQuestions.count, id: \.self) { idx in
                                let isAnswered = model.examAnswers[idx] != nil
                                let isFlagged = model.examFlagged.contains(idx)
                                let isCurrent = model.examIndex == idx

                                Button {
                                    model.examIndex = idx
                                } label: {
                                    Text("\(idx + 1)")
                                        .font(.caption.bold())
                                        .frame(maxWidth: .infinity, minHeight: 30)
                                        .background(
                                            isFlagged ? Color.orange :
                                            (isAnswered ? Color.indigo : Color.primary.opacity(0.08)),
                                            in: RoundedRectangle(cornerRadius: 6)
                                        )
                                        .foregroundStyle(isAnswered || isFlagged ? .white : .primary)
                                        .overlay(RoundedRectangle(cornerRadius: 6).stroke(isCurrent ? Color.teal : Color.clear, lineWidth: 2))
                                }
                                .buttonStyle(.borderless)
                            }
                        }
                        .padding(6)
                    }
                }
            } else {
                // Official Result & Certificate
                let totalQ = model.examQuestions.count
                let correctCount = model.examQuestions.indices.filter { model.examAnswers[$0] == model.examQuestions[$0].correctIndex }.count
                let pct = Int((Double(correctCount) / Double(max(totalQ, 1))) * 100)
                let band: String = {
                    switch pct {
                    case 90...: return "C2"
                    case 80...: return "C1"
                    case 65...: return "B2"
                    case 50...: return "B1"
                    case 35...: return "A2"
                    default: return "A1"
                    }
                }()

                let bandDesc: String = {
                    switch band {
                    case "C2": return "အဆင့်အမြင့်ဆုံး အင်္ဂလိပ်စာ အရည်အချင်း။ မူရင်းနိုင်ငံသားကဲ့သို့ မည်သည့် ရှုပ်ထွေးသော သဒ္ဒါ၊ စာပေနှင့် အသံကိုမဆို တိကျစွာ သုံးစွဲနားလည်နိုင်ပါသည်။"
                    case "C1": return "အဆင့်မြင့် အင်္ဂလိပ်စာ ကျွမ်းကျင်မှု။ အလုပ်ခွင်၊ တက္ကသိုလ်စာတမ်းများနှင့် ပညာရပ်ဆိုင်ရာ ဆွေးနွေးမှုများကို လွယ်ကူလျင်မြန်စွာ ဆောင်ရွက်နိုင်ပါသည်။"
                    case "B2": return "အလယ်အလတ်အထက် ကျွမ်းကျင်မှု။ အကြောင်းအရာမျိုးစုံကို အဆင်ပြေပြေ ရေးသားပြောဆိုနိုင်ပြီး မူရင်းစကားပြောသူများနှင့် သွက်လက်စွာ ဆက်သွယ်နိုင်ပါသည်။"
                    case "B1": return "အခြေခံအလယ်အလတ် အဆင့်။ နေ့စဉ်ဘဝ၊ ခရီးသွားလုပ်ငန်းနှင့် မိမိစိတ်ဝင်စားသော အကြောင်းအရာများကို အဆင်ပြေစွာ ဆွေးနွေးနိုင်ပါသည်။"
                    case "A2": return "အခြေခံအဆင့်။ နေ့စဉ်သုံး ရိုးရှင်းသော ဝါကျများနှင့် မေးခွန်းများကို နားလည်နိုင်ပါသည်။"
                    default: return "စတင်လေ့လာသူအဆင့်။ ရိုးရှင်းသော စကားလုံးများနှင့် အခြေခံနှုတ်ခွန်းဆက် စကားများကို လေ့လာသင်ယူဆဲ ဖြစ်ပါသည်။"
                    }
                }()

                // Certificate Box
                VStack(spacing: 16) {
                    Image(systemName: "rosette")
                        .font(.system(size: 64))
                        .foregroundStyle(Color.yellow)

                    Text("OFFICIAL CEFR LEVEL CERTIFICATE")
                        .font(.title2.bold())
                        .foregroundStyle(Color.yellow)

                    Text("Awarded to Learner: ထက်")
                        .font(.headline)
                        .foregroundStyle(.secondary)

                    VStack(spacing: 6) {
                        Text(band)
                            .font(.system(size: 60, weight: .heavy))
                            .foregroundStyle(colorFor(band))
                        Text("Score: \(pct)% (\(correctCount) / \(totalQ) Correct)")
                            .font(.headline)
                    }
                    .padding(20)
                    .frame(maxWidth: 360)
                    .background(Color.primary.opacity(0.04), in: RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.yellow.opacity(0.5), lineWidth: 1.5))

                    Text(bandDesc)
                        .font(.callout)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)

                    HStack(spacing: 14) {
                        Button(model.showExamReview ? "Hide Full Answer Review" : "Review All Answers") {
                            model.showExamReview.toggle()
                        }
                        .buttonStyle(.bordered)

                        Button("Retake Examination") {
                            model.examQuestions = model.store.fetchCefrExamQuestions(count: 30)
                            model.examIndex = 0
                            model.examAnswers = [:]
                            model.examFlagged = []
                            model.examSecondsRemaining = 1500
                            model.examSubmitted = false
                            model.showExamReview = false
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(32)
                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20))

                // Answer Review Details
                if model.showExamReview {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Question by Question Review")
                            .font(.title3.bold())

                        ForEach(0..<model.examQuestions.count, id: \.self) { qIdx in
                            let q = model.examQuestions[qIdx]
                            let userAns = model.examAnswers[qIdx]
                            let isCorrect = userAns == q.correctIndex

                            GroupBox {
                                VStack(alignment: .leading, spacing: 8) {
                                    HStack {
                                        Text("Q\(qIdx + 1) · \(q.cefrLevel)")
                                            .font(.caption.bold())
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 3)
                                            .background(colorFor(q.cefrLevel).opacity(0.18), in: Capsule())
                                        Spacer()
                                        Text(isCorrect ? "✓ Correct" : "✗ Incorrect")
                                            .font(.caption.bold())
                                            .foregroundStyle(isCorrect ? .green : .red)
                                    }

                                    Text(q.question).font(.headline)

                                    HStack {
                                        Text("Your choice: \(userAns != nil ? q.options[userAns!] : "None")")
                                            .foregroundStyle(isCorrect ? .green : .red)
                                        Spacer()
                                        if !isCorrect {
                                            Text("Correct: \(q.options[q.correctIndex])")
                                                .foregroundStyle(.green)
                                                .bold()
                                        }
                                    }
                                    .font(.subheadline)

                                    Text(q.explanation)
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                                .padding(6)
                            }
                        }
                    }
                }
            }
        }
    }

    private var placementView: some View {
        Group {
            if model.cefrFinished {
                let total = model.cefrCorrectByLevel.values.reduce(0, +)
                let est: String = {
                    switch total {
                    case 11...: return "C2 (Proficiency)"
                    case 9...10: return "C1 (Advanced)"
                    case 7...8: return "B2 (Upper Intermediate)"
                    case 5...6: return "B1 (Intermediate)"
                    case 3...4: return "A2 (Elementary)"
                    default: return "A1 (Beginner)"
                    }
                }()
                VStack(spacing: 14) {
                    Image(systemName: "rosette")
                        .font(.system(size: 56))
                        .foregroundStyle(colorFor(String(est.prefix(2))))
                    Text("Estimated CEFR English Level")
                        .font(.headline)
                        .foregroundStyle(.secondary)
                    Text(est)
                        .font(.largeTitle.bold())
                        .foregroundStyle(colorFor(String(est.prefix(2))))
                    Text("Score: \(total) / \(model.cefrQuestions.count)")
                    HStack(spacing: 8) {
                        ForEach(levels, id: \.self) { lvl in
                            Text("\(lvl): \(model.cefrCorrectByLevel[lvl] ?? 0)/2")
                                .font(.caption.bold())
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(colorFor(lvl).opacity(0.16), in: Capsule())
                        }
                    }
                    Button("Retake CEFR Placement Test") {
                        model.startCefrTest()
                    }
                    .buttonStyle(.borderedProminent)
                }
                .frame(maxWidth: .infinity)
                .padding(28)
                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18))
            } else if model.cefrStep < model.cefrQuestions.count {
                let q = model.cefrQuestions[model.cefrStep]
                VStack(alignment: .leading, spacing: 14) {
                    ProgressView(value: Double(model.cefrStep), total: Double(model.cefrQuestions.count))
                    HStack {
                        Text("CEFR \(q.level)")
                            .font(.caption.bold())
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(colorFor(q.level).opacity(0.18), in: Capsule())
                            .foregroundStyle(colorFor(q.level))
                        Spacer()
                    }
                    Text("Choose the meaning of \"\(q.item.word)\":")
                        .font(.title2.bold())
                    ForEach(q.options, id: \.self) { opt in
                        let isPick = model.cefrChosen == opt
                        let isRight = opt == q.item.myDef
                        Button {
                            guard model.cefrChosen == nil else { return }
                            model.cefrChosen = opt
                            if isRight {
                                model.cefrCorrectByLevel[q.level, default: 0] += 1
                                model.gainXp(20)
                            }
                        } label: {
                            HStack {
                                Text(opt).multilineTextAlignment(.leading)
                                Spacer()
                                if model.cefrChosen != nil && isRight {
                                    Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
                                } else if isPick && !isRight {
                                    Image(systemName: "xmark.circle.fill").foregroundStyle(.red)
                                }
                            }
                            .padding(10)
                            .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                    }
                    if model.cefrChosen != nil {
                        Button(model.cefrStep + 1 >= model.cefrQuestions.count ? "View My CEFR Level" : "Next Question") {
                            if model.cefrStep + 1 >= model.cefrQuestions.count {
                                model.cefrFinished = true
                            } else {
                                model.cefrStep += 1
                                model.cefrChosen = nil
                            }
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
                .padding(22)
                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18))
            }
        }
    }

    private var graderView: some View {
        let tokens = model.graderInput
            .components(separatedBy: CharacterSet.letters.inverted)
            .map { $0.lowercased() }
            .filter { $0.count >= 2 }
        let matched = model.store.lookupExactVocab(tokens)
        var counts: [String: Int] = ["A1": 0, "A2": 0, "B1": 0, "B2": 0, "C1": 0, "C2": 0]
        for v in matched.values { counts[v.level, default: 0] += 1 }
        let total = max(matched.count, 1)
        let score = Double(
            (counts["A1"] ?? 0) * 1 + (counts["A2"] ?? 0) * 2 + (counts["B1"] ?? 0) * 3 +
            (counts["B2"] ?? 0) * 4 + (counts["C1"] ?? 0) * 5 + (counts["C2"] ?? 0) * 6
        ) / Double(total)
        let band: String = {
            if score >= 5.1 { return "C2" }
            if score >= 4.2 { return "C1" }
            if score >= 3.3 { return "B2" }
            if score >= 2.4 { return "B1" }
            if score >= 1.6 { return "A2" }
            return "A1"
        }()
        let advanced = matched.values.filter { ["B2", "C1", "C2"].contains($0.level) }
            .sorted { $0.level > $1.level }

        return VStack(alignment: .leading, spacing: 16) {
            GroupBox("Paste English Text / Paragraph") {
                TextEditor(text: $model.graderInput)
                    .font(.body)
                    .frame(height: 110)
            }

            GroupBox("CEFR Analysis (\(matched.count) words profiled)") {
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text("Estimated Passage Level: CEFR \(band)")
                            .font(.title2.bold())
                            .foregroundStyle(colorFor(band))
                        Spacer()
                        Text("CEFR-J · Octanove · Maximax67")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    ForEach(levels, id: \.self) { lvl in
                        let c = counts[lvl] ?? 0
                        HStack {
                            Text(lvl).font(.caption.bold()).foregroundStyle(colorFor(lvl)).frame(width: 28, alignment: .leading)
                            ProgressView(value: Double(c), total: Double(total))
                                .tint(colorFor(lvl))
                            Text("\(c)").font(.caption).frame(width: 28, alignment: .trailing)
                        }
                    }
                }
                .padding(6)
            }

            if !advanced.isEmpty {
                GroupBox("Key Advanced Words (B2–C2)") {
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(advanced) { item in
                            HStack {
                                Text(item.word).font(.headline)
                                Text(item.level)
                                    .font(.caption2.bold())
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(colorFor(item.level).opacity(0.18), in: Capsule())
                                Text("🇲🇲 \(item.myDef)").foregroundStyle(.teal).lineLimit(1)
                                Spacer()
                                Button("🇺🇸") { SpeechService.shared.speak(item.word, languageCode: "en-US") }.buttonStyle(.borderless)
                                Button("🇬🇧") { SpeechService.shared.speak(item.word, languageCode: "en-GB") }.buttonStyle(.borderless)
                            }
                            Divider()
                        }
                    }
                    .padding(6)
                }
            }
        }
    }
}

import SwiftUI

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
                    Text("A1–C2 Level Placement Test").tag(0)
                    Text("Text CEFR Vocabulary Grader").tag(1)
                }
                .pickerStyle(.segmented)
                .frame(maxWidth: 440)

                if model.cefrSubTab == 0 {
                    placementView
                } else {
                    graderView
                }
            }
            .padding(24)
        }
        .navigationTitle("CEFR Test & Text Grader")
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
                        Text("Question \(model.cefrStep + 1) of \(model.cefrQuestions.count)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Text("Choose the Burmese meaning of \"\(q.item.word)\":")
                        .font(.title2.bold())
                    HStack(spacing: 10) {
                        Button("🇺🇸 US Voice") { SpeechService.shared.speak(q.item.word, languageCode: "en-US") }
                            .buttonStyle(.bordered)
                        Button("🇬🇧 UK Voice") { SpeechService.shared.speak(q.item.word, languageCode: "en-GB") }
                            .buttonStyle(.bordered)
                    }
                    ForEach(q.options, id: \.self) { opt in
                        let isRight = opt == q.item.myDef
                        let isPick = model.cefrChosen == opt
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

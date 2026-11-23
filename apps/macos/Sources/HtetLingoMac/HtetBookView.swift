import SwiftUI

struct HtetBookMacView: View {
    @Bindable var model: AppModel

    var body: some View {
        VStack(spacing: 0) {
            Picker("Book Section", selection: $model.bookSubTab) {
                Text("🌸 Foundation A1 (ထက်)").tag(0)
                Text("49 Lessons").tag(1)
                Text("24 Vocab Units").tag(2)
                Text("10 Reading Lab").tag(3)
                Text("9 Writing Workshop").tag(4)
                Text("97 Irregular Verbs").tag(5)
                Text("16-Week Plan").tag(6)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            Divider()

            switch model.bookSubTab {
            case 0: foundationChaptersView
            case 1: lessonsSplitView
            case 2: vocabUnitsView
            case 3: readingsView
            case 4: writingsView
            case 5: irregularVerbsView
            default: studyPlanView
            }
        }
        .navigationTitle("Htet Bilingual Books (Foundation A1 + Mastery © 2026 ထက်)")
    }

    private var foundationChaptersView: some View {
        HSplitView {
            List(model.foundationChapters) { ch in
                VStack(alignment: .leading, spacing: 4) {
                    Text(ch.title)
                        .font(.body.bold())
                        .lineLimit(2)
                    Text("© ထက် · Foundation A1")
                        .font(.caption2)
                        .foregroundStyle(.pink)
                }
                .padding(.vertical, 4)
                .contentShape(Rectangle())
                .listRowBackground(model.selectedGrammarChapter?.id == ch.id ? Color.pink.opacity(0.18) : Color.clear)
                .onTapGesture {
                    model.selectedGrammarChapter = ch
                    model.gainXp(2)
                }
            }
            .listStyle(.inset)
            .frame(minWidth: 280, idealWidth: 320, maxWidth: 380)

            ScrollView {
                if let ch = model.selectedGrammarChapter ?? model.foundationChapters.first {
                    VStack(alignment: .leading, spacing: 16) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(ch.title).font(.title2.bold())
                                Text(ch.source).font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Button {
                                SpeechService.shared.speak(String(ch.content.prefix(250)), languageCode: "en-US")
                            } label: {
                                Label("Read Aloud 🇺🇸", systemImage: "speaker.wave.2.fill")
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(.pink)
                        }

                        Divider()

                        Text(ch.content)
                            .font(.system(.body, design: .serif))
                            .lineSpacing(6)
                            .textSelection(.enabled)
                    }
                    .padding(24)
                } else {
                    Text("Select a Foundation English (A1) chapter to read.")
                        .foregroundStyle(.secondary)
                        .padding(24)
                }
            }
        }
    }

    private var lessonsSplitView: some View {
        HSplitView {
            List(model.bookLessons) { l in
                HStack {
                    Text(String(format: "%02d", l.id))
                        .font(.caption.monospacedDigit().bold())
                        .foregroundStyle(.secondary)
                    Text(l.title).lineLimit(1)
                    Spacer()
                    Text(l.level)
                        .font(.caption2.bold())
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(.teal.opacity(0.16), in: Capsule())
                }
                .contentShape(Rectangle())
                .listRowBackground(model.selectedBookLesson?.id == l.id ? Color.accentColor.opacity(0.18) : Color.clear)
                .onTapGesture {
                    model.selectedBookLesson = l
                    model.bookShowAnswer = false
                    model.gainXp(2)
                }
            }
            .listStyle(.inset)
            .frame(minWidth: 260, idealWidth: 300, maxWidth: 360)

            ScrollView {
                if let l = model.selectedBookLesson ?? model.bookLessons.first {
                    VStack(alignment: .leading, spacing: 16) {
                        HStack {
                            Text("Lesson \(String(format: "%02d", l.id)) · \(l.title)")
                                .font(.title.bold())
                            Spacer()
                            Text(l.level)
                                .font(.caption.bold())
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(.teal.opacity(0.18), in: Capsule())
                        }
                        Text(l.explanation).lineSpacing(5).textSelection(.enabled)
                        GroupBox("Grammar Patterns") {
                            Text(l.patterns).font(.system(.callout, design: .monospaced)).frame(maxWidth: .infinity, alignment: .leading).padding(4)
                        }
                        GroupBox("Examples with Myanmar Translation") {
                            Text(l.examples).foregroundStyle(.teal).frame(maxWidth: .infinity, alignment: .leading).padding(4)
                        }
                        GroupBox("Practice & Answer Key") {
                            VStack(alignment: .leading, spacing: 10) {
                                Text(l.practice)
                                Button(model.bookShowAnswer ? "Hide Answers" : "Check Answers") {
                                    model.bookShowAnswer.toggle()
                                }
                                .buttonStyle(.bordered)
                                if model.bookShowAnswer {
                                    Text(l.answers).foregroundStyle(.green).fontWeight(.semibold)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(4)
                        }
                    }
                    .padding(24)
                }
            }
        }
    }

    private var vocabUnitsView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                ForEach(model.bookVocabUnits) { u in
                    GroupBox("Unit \(String(format: "%02d", u.id)) · \(u.title) (\(u.level))") {
                        VStack(alignment: .leading, spacing: 10) {
                            LazyVGrid(columns: [GridItem(.adaptive(minimum: 210))], spacing: 8) {
                                ForEach(Array(u.words.enumerated()), id: \.offset) { _, pair in
                                    HStack {
                                        VStack(alignment: .leading) {
                                            Text(pair.en).font(.headline)
                                            Text(pair.my).font(.caption).foregroundStyle(.teal)
                                        }
                                        Spacer()
                                        Button("🇺🇸") { SpeechService.shared.speak(pair.en, languageCode: "en-US") }.buttonStyle(.borderless)
                                        Button("🇬🇧") { SpeechService.shared.speak(pair.en, languageCode: "en-GB") }.buttonStyle(.borderless)
                                    }
                                    .padding(8)
                                    .background(Color.primary.opacity(0.04), in: RoundedRectangle(cornerRadius: 8))
                                }
                            }
                            Text("Collocations: \(u.collocations)").font(.callout).foregroundStyle(.secondary)
                            Text(u.examples).font(.callout)
                        }
                        .padding(6)
                    }
                }
            }
            .padding(24)
        }
    }

    private var readingsView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                ForEach(model.bookReadings) { r in
                    GroupBox("Reading \(String(format: "%02d", r.id)) · \(r.title) (\(r.level))") {
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                Button("🇺🇸 Read US") { SpeechService.shared.speak(r.passage, languageCode: "en-US", rate: 0.46) }.buttonStyle(.borderedProminent)
                                Button("🇬🇧 Read UK") { SpeechService.shared.speak(r.passage, languageCode: "en-GB", rate: 0.46) }.buttonStyle(.bordered)
                            }
                            Text(r.passage).lineSpacing(5).textSelection(.enabled)
                            Text("Grammar Notes: \(r.grammarNotes)").font(.callout).foregroundStyle(.teal)
                            Divider()
                            Text("Questions:\n\(r.questions)").font(.callout)
                            Text("Suggested Answers:\n\(r.answers)").font(.callout).foregroundStyle(.green)
                        }
                        .padding(6)
                    }
                }
            }
            .padding(24)
        }
    }

    private var writingsView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                ForEach(model.bookWritings) { w in
                    GroupBox("Writing \(String(format: "%02d", w.id)) · \(w.title) (\(w.level))") {
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                Text("Task: \(w.task)").font(.headline)
                                Spacer()
                                Button("🔊 Listen Model") { SpeechService.shared.speak(w.modelAnswer, languageCode: "en-GB", rate: 0.47) }.buttonStyle(.bordered)
                            }
                            Text(w.modelAnswer)
                                .padding(12)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 10))
                                .textSelection(.enabled)
                            Text("Why It Works: \(w.whyItWorks)").font(.callout).foregroundStyle(.teal)
                            Text("Checklist: \(w.checklist)").font(.caption).foregroundStyle(.secondary)
                        }
                        .padding(6)
                    }
                }
            }
            .padding(24)
        }
    }

    private var irregularVerbsView: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 260))], spacing: 10) {
                ForEach(model.bookIrregularVerbs) { v in
                    HStack {
                        VStack(alignment: .leading) {
                            Text("\(v.v1) → \(v.v2) → \(v.v3)").font(.headline)
                            Text(v.my).font(.caption).foregroundStyle(.teal)
                        }
                        Spacer()
                        Button("🇺🇸") { SpeechService.shared.speak("\(v.v1), \(v.v2), \(v.v3)", languageCode: "en-US") }.buttonStyle(.borderless)
                    }
                    .padding(10)
                    .background(Color.primary.opacity(0.04), in: RoundedRectangle(cornerRadius: 10))
                }
            }
            .padding(24)
        }
    }

    private var studyPlanView: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 260))], spacing: 12) {
                ForEach(model.bookStudyPlan) { wk in
                    GroupBox(wk.week) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Grammar: \(wk.grammar)").font(.headline).foregroundStyle(.teal)
                            Text("Vocabulary: \(wk.vocabulary)").font(.callout)
                            Text("Skills: \(wk.skills)").font(.caption).foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(4)
                    }
                }
            }
            .padding(24)
        }
    }
}

import SwiftUI

struct UltimateAcademyView: View {
    @Bindable var model: AppModel
    private let topics = ["ALL", "Grammar Exam", "Articles", "Do and Make", "Prepositions", "Phrasal", "Irregular", "Negative"]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Picker("Section", selection: $model.academySubTab) {
                    Text("Grammar Exams & Exercises (710)").tag(0)
                    Text("🐝 Spelling Bee & Audio").tag(1)
                    Text("💻 Developer Interview English").tag(2)
                    Text("📖 EN Grammar Book (19)").tag(3)
                }
                .pickerStyle(.segmented)

                switch model.academySubTab {
                case 0: exercisesView
                case 1: spellingBeeView
                case 2: devInterviewView
                default: grammarBookView
                }
            }
            .padding(24)
        }
        .navigationTitle("Exams, Spelling Bee & Dev English")
    }

    private var exercisesView: some View {
        VStack(alignment: .leading, spacing: 14) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(topics, id: \.self) { t in
                        Button(t) { model.selectExerciseTopic(t) }
                            .buttonStyle(.bordered)
                            .tint(model.exerciseTopic == t ? .accentColor : .secondary)
                    }
                }
            }

            if model.exerciseStep < model.grammarExercises.count {
                let ex = model.grammarExercises[model.exerciseStep]
                GroupBox {
                    VStack(alignment: .leading, spacing: 14) {
                        HStack {
                            Text(ex.topic)
                                .font(.caption.bold())
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(.indigo.opacity(0.16), in: Capsule())
                            Spacer()
                            Text("Exercise \(model.exerciseStep + 1) / \(model.grammarExercises.count)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }

                        Text(ex.question)
                            .font(.title2.bold())

                        HStack(spacing: 10) {
                            Button("🇺🇸 US Voice") {
                                SpeechService.shared.speak(ex.question.replacingOccurrences(of: "_____", with: ex.answer), languageCode: "en-US")
                            }
                            .buttonStyle(.bordered)
                            Button("🇬🇧 UK Voice") {
                                SpeechService.shared.speak(ex.question.replacingOccurrences(of: "_____", with: ex.answer), languageCode: "en-GB")
                            }
                            .buttonStyle(.bordered)
                        }

                        ForEach(ex.options, id: \.self) { opt in
                            let isRight = opt.lowercased() == ex.answer.lowercased()
                            let isPick = model.exerciseChosen == opt
                            Button {
                                guard model.exerciseChosen == nil else { return }
                                model.exerciseChosen = opt
                                if isRight { model.gainXp(15) }
                            } label: {
                                HStack {
                                    Text(opt)
                                    Spacer()
                                    if model.exerciseChosen != nil && isRight {
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

                        if model.exerciseChosen != nil {
                            Text(ex.explanation)
                                .font(.callout)
                                .foregroundStyle(.secondary)
                            Button(model.exerciseStep + 1 >= model.grammarExercises.count ? "Load Fresh Set" : "Next Exercise") {
                                if model.exerciseStep + 1 >= model.grammarExercises.count {
                                    model.selectExerciseTopic(model.exerciseTopic)
                                } else {
                                    model.exerciseStep += 1
                                    model.exerciseChosen = nil
                                }
                            }
                            .buttonStyle(.borderedProminent)
                        }
                    }
                    .padding(10)
                }
            }
        }
    }

    private var spellingBeeView: some View {
        VStack(alignment: .leading, spacing: 16) {
            if !model.beePuzzles.isEmpty {
                let p = model.beePuzzles[model.beeIndex % model.beePuzzles.count]
                GroupBox("🐝 7-Letter Honeycomb Spelling Bee (Center Letter Required)") {
                    VStack(spacing: 14) {
                        HStack {
                            Text("Found: \(model.beeFound.count) / \(p.answers.count) words")
                                .font(.headline)
                                .foregroundStyle(.orange)
                            Spacer()
                            Button("Next Honeycomb →") {
                                model.beeIndex += 1
                                model.beeTyped = ""
                                model.beeFound = []
                                model.beeFeedback = "Form 4+ letter words using center letter \(model.beePuzzles[model.beeIndex % model.beePuzzles.count].center)!"
                            }
                            .buttonStyle(.bordered)
                        }

                        Text(model.beeTyped.isEmpty ? "—" : model.beeTyped)
                            .font(.system(size: 32, weight: .heavy, design: .rounded))
                        Text(model.beeFeedback)
                            .font(.callout)
                            .foregroundStyle(.secondary)

                        HStack(spacing: 10) {
                            ForEach(p.outer, id: \.self) { ch in
                                Button(ch) { model.beeTyped += ch }
                                    .buttonStyle(.bordered)
                                    .controlSize(.large)
                            }
                            Button("★ \(p.center)") { model.beeTyped += p.center }
                                .buttonStyle(.borderedProminent)
                                .tint(.orange)
                                .controlSize(.large)
                        }

                        HStack(spacing: 10) {
                            Button("Delete") {
                                if !model.beeTyped.isEmpty { model.beeTyped.removeLast() }
                            }
                            Button("Clear") { model.beeTyped = "" }
                            Button("Submit Word") {
                                let w = model.beeTyped.lowercased()
                                if w.count < 4 {
                                    model.beeFeedback = "Too short (need 4+ letters)"
                                } else if !w.contains(p.center.lowercased()) {
                                    model.beeFeedback = "Must include center letter \(p.center)!"
                                } else if model.beeFound.contains(w) {
                                    model.beeFeedback = "Already found!"
                                } else if p.answers.contains(w) {
                                    model.beeFound.insert(w)
                                    model.beeFeedback = "Awesome! +12 XP for '\(w)'"
                                    SpeechService.shared.speak(w, languageCode: "en-US")
                                    model.gainXp(12)
                                    model.beeTyped = ""
                                } else {
                                    model.beeFeedback = "Not in puzzle list — try another!"
                                }
                            }
                            .buttonStyle(.borderedProminent)
                        }

                        if !model.beeFound.isEmpty {
                            Divider()
                            Text("Discovered: \(model.beeFound.sorted().joined(separator: ", "))")
                                .font(.callout)
                                .foregroundStyle(.teal)
                        }
                    }
                    .padding(10)
                }
            }
        }
    }

    private var devInterviewView: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(model.devInterviews) { item in
                GroupBox {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text(item.category)
                                .font(.caption.bold())
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(.blue.opacity(0.16), in: Capsule())
                            Text(item.title).font(.headline)
                            Spacer()
                            Button("🇺🇸") { SpeechService.shared.speak(item.title, languageCode: "en-US") }
                                .buttonStyle(.borderless)
                            Button("🇬🇧") { SpeechService.shared.speak(item.title, languageCode: "en-GB") }
                                .buttonStyle(.borderless)
                        }
                        Text(item.content)
                            .font(.callout)
                            .foregroundStyle(.secondary)
                            .textSelection(.enabled)
                    }
                    .padding(6)
                }
            }
        }
    }

    private var grammarBookView: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(model.grammarChapters) { ch in
                GroupBox(ch.title) {
                    Text(ch.content)
                        .font(.callout)
                        .textSelection(.enabled)
                        .padding(6)
                }
            }
        }
    }
}

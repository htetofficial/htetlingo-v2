import SwiftUI

struct MacQuestNode: Identifiable {
    let id: String
    let title: String
    let subtitle: String
    let icon: String
    let stars: Int
    let isCompleted: Bool
    let isCurrent: Bool
    let isLocked: Bool
}

struct MacQuestUnit: Identifiable {
    let id: Int
    let number: Int
    let title: String
    let subtitle: String
    let badge: String
    let color: Color
    let nodes: [MacQuestNode]
}

struct QuestPathMacView: View {
    @Bindable var model: AppModel

    private let bubblePool = ["I", "have", "a", "cup", "of", "coffee", "every", "morning", "tea", "water"]
    private let correctSentence = ["I", "have", "a", "cup", "of", "coffee", "every", "morning"]

    private let units: [MacQuestUnit] = [
        MacQuestUnit(
            id: 1,
            number: 1,
            title: "Unit 1: First Words & Warm Greetings",
            subtitle: "စတင်မိတ်ဆက်ခြင်းနှင့် နေ့စဉ် နှုတ်ခွန်းဆက် စကားများ",
            badge: "🌸 Basics",
            color: .pink,
            nodes: [
                MacQuestNode(id: "u1_1", title: "Hello & Welcome", subtitle: "မင်္ဂလာပါနှင့် နှုတ်ခွန်းဆက်", icon: "👋", stars: 3, isCompleted: true, isCurrent: false, isLocked: false),
                MacQuestNode(id: "u1_2", title: "Personal Pronouns", subtitle: "I, You, We, They ဝါကျဖွဲ့ထုံး", icon: "✨", stars: 3, isCompleted: true, isCurrent: false, isLocked: false),
                MacQuestNode(id: "u1_3", title: "Verb to Be (am/is/are)", subtitle: "Be ကြိယာ ရှင်းလင်းချက်", icon: "⭐", stars: 2, isCompleted: false, isCurrent: true, isLocked: false),
                MacQuestNode(id: "u1_4", title: "Unit 1 Milestone Chest", subtitle: "ရတနာသေတ္တာ စိန်ဆုလာဘ်", icon: "🎁", stars: 0, isCompleted: false, isCurrent: false, isLocked: true)
            ]
        ),
        MacQuestUnit(
            id: 2,
            number: 2,
            title: "Unit 2: Cafe, Food & Daily Talks",
            subtitle: "ကော်ဖီဆိုင်၊ စားသောက်ဆိုင်နှင့် နေ့စဉ်လူနေမှု",
            badge: "☕ Daily Life",
            color: .purple,
            nodes: [
                MacQuestNode(id: "u2_1", title: "Ordering Coffee", subtitle: "ကော်ဖီနှင့် အစားအသောက် မှာယူခြင်း", icon: "☕", stars: 0, isCompleted: false, isCurrent: false, isLocked: true),
                MacQuestNode(id: "u2_2", title: "Present Continuous", subtitle: "လုပ်ဆောင်ဆဲ လုပ်ငန်းများ (-ing)", icon: "⏳", stars: 0, isCompleted: false, isCurrent: false, isLocked: true),
                MacQuestNode(id: "u2_3", title: "Countable & Uncountable", subtitle: "Much vs Many ပမာဏပြများ", icon: "🍰", stars: 0, isCompleted: false, isCurrent: false, isLocked: true),
                MacQuestNode(id: "u2_4", title: "Unit 2 Milestone Chest", subtitle: "စိန်ပွင့်ဆုလာဘ်", icon: "🎁", stars: 0, isCompleted: false, isCurrent: false, isLocked: true)
            ]
        ),
        MacQuestUnit(
            id: 3,
            number: 3,
            title: "Unit 3: Travel, Directions & Flights",
            subtitle: "လေဆိပ်၊ ခရီးသွားလုပ်ငန်းနှင့် လမ်းညွှန်စကား",
            badge: "✈️ Adventure",
            color: .orange,
            nodes: [
                MacQuestNode(id: "u3_1", title: "At the Airport", subtitle: "လေဆိပ် Check-in နှင့် စကားပြော", icon: "🛫", stars: 0, isCompleted: false, isCurrent: false, isLocked: true),
                MacQuestNode(id: "u3_2", title: "Past Simple Verbs", subtitle: "အတိတ်ကာလ ကြိယာပုံစံများ", icon: "🕰️", stars: 0, isCompleted: false, isCurrent: false, isLocked: true),
                MacQuestNode(id: "u3_3", title: "Asking Directions", subtitle: "ဘယ်ဘက်၊ ညာဘက် လမ်းညွှန်စကား", icon: "🧭", stars: 0, isCompleted: false, isCurrent: false, isLocked: true),
                MacQuestNode(id: "u3_4", title: "Unit 3 Milestone Chest", subtitle: "ရတနာသေတ္တာ", icon: "🎁", stars: 0, isCompleted: false, isCurrent: false, isLocked: true)
            ]
        ),
        MacQuestUnit(
            id: 4,
            number: 4,
            title: "Unit 4: Author's Grammar & Fluency",
            subtitle: "ထက် ၏ A1–C2 သဒ္ဒါစည်းမျဉ်းများနှင့် ဝါကျဖွဲ့ထုံး",
            badge: "📖 Master Grammar",
            color: .mint,
            nodes: [
                MacQuestNode(id: "u4_1", title: "Present Perfect", subtitle: "Have/Has + V3 ချိတ်ဆက်ပုံ", icon: "💎", stars: 0, isCompleted: false, isCurrent: false, isLocked: true),
                MacQuestNode(id: "u4_2", title: "Conditionals (If Clause)", subtitle: "အခြေအနေပြ ဝါကျများ", icon: "🔮", stars: 0, isCompleted: false, isCurrent: false, isLocked: true),
                MacQuestNode(id: "u4_3", title: "Passive Voice Mastery", subtitle: "Subject အာရုံပြောင်း ဝါကျများ", icon: "⚡", stars: 0, isCompleted: false, isCurrent: false, isLocked: true),
                MacQuestNode(id: "u4_4", title: "Crown Champion Node", subtitle: "ဂုဏ်ထူးဆောင် သရဖူစိန်", icon: "👑", stars: 0, isCompleted: false, isCurrent: false, isLocked: true)
            ]
        )
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // FLOATING TOP BAR (Frosted Liquid Glass: Streak, Gems, Hearts)
                HStack(spacing: 20) {
                    // Mascot Brand
                    HStack(spacing: 8) {
                        Text("🌸")
                            .font(.title2)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("HtetLingo Quest")
                                .font(.headline.bold())
                                .foregroundStyle(LinearGradient(colors: [.pink, .purple], startPoint: .leading, endPoint: .trailing))
                            Text("Interactive Bloom Journey")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                    }

                    Spacer()

                    // Streak Pill
                    HStack(spacing: 6) {
                        Text("🔥")
                        Text("7 Days Streak")
                            .font(.subheadline.bold())
                            .foregroundStyle(.orange)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 7)
                    .background(Color.orange.opacity(0.14), in: Capsule())

                    // Gems Pill
                    HStack(spacing: 6) {
                        Text("💎")
                        Text("420 Gems")
                            .font(.subheadline.bold())
                            .foregroundStyle(.blue)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 7)
                    .background(Color.blue.opacity(0.14), in: Capsule())

                    // Hearts Pill
                    HStack(spacing: 6) {
                        Text("❤️")
                        Text("5 / 5")
                            .font(.subheadline.bold())
                            .foregroundStyle(.pink)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 7)
                    .background(Color.pink.opacity(0.14), in: Capsule())
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 14)
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 20))
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.4), lineWidth: 1))
                .shadow(color: .pink.opacity(0.15), radius: 10, y: 4)

                // QUEST PATH (S-Curved Stepping-Stone Nodes)
                VStack(spacing: 36) {
                    ForEach(units) { unit in
                        VStack(spacing: 18) {
                            // Unit Header Box
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(unit.badge)
                                        .font(.caption.bold())
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 3)
                                        .background(unit.color.opacity(0.16), in: Capsule())
                                        .foregroundStyle(unit.color)

                                    Text(unit.title)
                                        .font(.title3.bold())

                                    Text(unit.subtitle)
                                        .font(.subheadline)
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Image(systemName: "sparkles")
                                    .font(.title)
                                    .foregroundStyle(unit.color)
                            }
                            .padding(18)
                            .background(unit.color.opacity(0.08), in: RoundedRectangle(cornerRadius: 18))
                            .overlay(RoundedRectangle(cornerRadius: 18).stroke(unit.color.opacity(0.25), lineWidth: 1))

                            // Stepping-Stone Nodes (S-Curve wave)
                            ForEach(Array(unit.nodes.enumerated()), id: \.element.id) { index, node in
                                let offsets: [CGFloat] = [0, 50, 90, 50, 0, -50, -90, -50]
                                let xOffset = offsets[index % offsets.count]

                                HStack {
                                    Spacer()
                                    VStack(spacing: 6) {
                                        Button {
                                            if !node.isLocked {
                                                model.questShowExerciseSheet = true
                                                model.questSelectedBubbles = []
                                                model.questCheckStatus = nil
                                                model.questShowComplete = false
                                            }
                                        } label: {
                                            ZStack {
                                                Circle()
                                                    .fill(
                                                        node.isCompleted ? Color.yellow.opacity(0.9) :
                                                        (node.isCurrent ? unit.color : Color.secondary.opacity(0.2))
                                                    )
                                                    .frame(width: 72, height: 72)
                                                    .overlay(
                                                        Circle().stroke(Color.white, lineWidth: node.isCurrent ? 3.5 : 2)
                                                    )
                                                    .shadow(color: (node.isCurrent ? unit.color : Color.clear).opacity(0.4), radius: 8, y: 4)

                                                Text(node.isLocked ? "🔒" : node.icon)
                                                    .font(.system(size: 28))
                                            }
                                        }
                                        .buttonStyle(.plain)

                                        if node.isCompleted {
                                            HStack(spacing: 2) {
                                                ForEach(0..<3) { s in
                                                    Text(s < node.stars ? "⭐" : "☆")
                                                        .font(.caption2)
                                                }
                                            }
                                        } else if node.isCurrent {
                                            Text("START")
                                                .font(.caption2.bold())
                                                .padding(.horizontal, 8)
                                                .padding(.vertical, 2)
                                                .background(unit.color, in: Capsule())
                                                .foregroundStyle(.white)
                                        }
                                    }
                                    .offset(x: xOffset)
                                    Spacer()
                                }
                            }
                        }
                    }
                }
            }
            .padding(24)
        }
        .navigationTitle("🌸 Quest Learning Path")
        .sheet(isPresented: $model.questShowExerciseSheet) {
            interactiveExerciseSheet
        }
    }

    // INTERACTIVE EXERCISE MODAL SHEET
    private var interactiveExerciseSheet: some View {
        VStack(spacing: 20) {
            // Header Progress
            HStack {
                Button("Exit") { model.questShowExerciseSheet = false }
                    .buttonStyle(.borderless)

                ProgressView(value: model.questShowComplete ? 1.0 : 0.6)
                    .tint(.pink)

                HStack(spacing: 4) {
                    Text("❤️")
                    Text("5")
                        .font(.headline.bold())
                        .foregroundStyle(.pink)
                }
            }
            .padding(.horizontal, 24)
            .padding(.top, 20)

            Divider()

            if model.questShowComplete {
                // Celebration
                VStack(spacing: 16) {
                    Spacer()
                    Text("🌸✨🎉")
                        .font(.system(size: 64))
                    Text("သင်ခန်းစာ အောင်မြင်စွာ ပြီးဆုံးပါပြီ!")
                        .font(.title.bold())
                        .foregroundStyle(.pink)
                    Text("Lesson Completed · You earned +25 XP & +10 Gems!")
                        .foregroundStyle(.secondary)

                    HStack(spacing: 16) {
                        VStack {
                            Text("⚡ +25 XP").font(.headline.bold()).foregroundStyle(.orange)
                        }
                        .padding()
                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))

                        VStack {
                            Text("💎 +10 Gems").font(.headline.bold()).foregroundStyle(.blue)
                        }
                        .padding()
                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
                    }

                    Spacer()

                    Button("Continue (ဆက်လက်လေ့လာမည်)") {
                        model.gainXp(25)
                        model.questShowExerciseSheet = false
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.pink)
                    .controlSize(.large)
                }
                .padding(32)
            } else {
                VStack(alignment: .leading, spacing: 20) {
                    Text("အောက်ပါ စကားလုံးများကို စီစဉ်ပြီး ဝါကျတည်ဆောက်ပါ:")
                        .font(.headline)

                    HStack {
                        Button {
                            SpeechService.shared.speak("I have a cup of coffee every morning.", languageCode: "en-US")
                        } label: {
                            Label("Listen 🇺🇸", systemImage: "speaker.wave.2.fill")
                        }
                        .buttonStyle(.bordered)
                        Text("ကျေးဇူးပြု၍ ကော်ဖီတစ်ခွက်ပေးပါ")
                            .font(.title3.bold())
                            .foregroundStyle(.secondary)
                    }

                    // Selected Words Sentence Area
                    GroupBox {
                        HStack(spacing: 8) {
                            if model.questSelectedBubbles.isEmpty {
                                Text("စကားလုံးများကို အောက်မှ နှိပ်ပါ…")
                                    .foregroundStyle(.secondary)
                                    .padding(.vertical, 8)
                            } else {
                                ForEach(Array(model.questSelectedBubbles.enumerated()), id: \.offset) { idx, word in
                                    Button {
                                        model.questSelectedBubbles.remove(at: idx)
                                    } label: {
                                        Text(word)
                                            .font(.body.bold())
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .background(Color.pink, in: Capsule())
                                            .foregroundStyle(.white)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            Spacer()
                        }
                        .frame(minHeight: 48)
                    }

                    Divider()

                    // Word Pool Area (Candy bubbles)
                    Text("Tap to build:")
                        .font(.caption.bold())
                        .foregroundStyle(.secondary)

                    FlowLayout(spacing: 10) {
                        ForEach(bubblePool, id: \.self) { word in
                            let isUsed = model.questSelectedBubbles.contains(word)
                            Button {
                                guard !isUsed else { return }
                                model.questSelectedBubbles.append(word)
                                SpeechService.shared.speak(word, languageCode: "en-US")
                            } label: {
                                Text(word)
                                    .font(.body.weight(.semibold))
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .background(isUsed ? Color.secondary.opacity(0.12) : Color.purple.opacity(0.15), in: Capsule())
                                    .foregroundStyle(isUsed ? Color.secondary.opacity(0.4) : Color.primary)
                                    .overlay(Capsule().stroke(isUsed ? Color.clear : Color.purple.opacity(0.3), lineWidth: 1))
                            }
                            .buttonStyle(.plain)
                            .disabled(isUsed)
                        }
                    }

                    Spacer()

                    // Check Answer Footer
                    HStack {
                        if let status = model.questCheckStatus {
                            Text(status ? "🌸 အံ့ဩဖွယ် မှန်ကန်ပါတယ်! (Excellent!)" : "❤️ မမှန်သေးပါ၊ ပြန်လည်စမ်းသပ်ကြည့်ပါ")
                                .font(.headline.bold())
                                .foregroundStyle(status ? .green : .pink)
                        }
                        Spacer()
                        Button(model.questCheckStatus == nil ? "Check (စစ်ဆေးမည်)" : "Continue (ရှေ့ဆက်မည်)") {
                            if model.questCheckStatus == nil {
                                model.questCheckStatus = model.questSelectedBubbles == correctSentence
                            } else {
                                model.questShowComplete = true
                            }
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(model.questCheckStatus == true ? .green : .pink)
                        .controlSize(.large)
                    }
                    .padding(16)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 14))
                }
                .padding(24)
            }
        }
        .frame(minWidth: 540, minHeight: 480)
    }
}

// FlowLayout helper for SwiftUI
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? 0
        var height: CGFloat = 0
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > width && x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
        }
        height = y + rowHeight
        return CGSize(width: width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX && x > bounds.minX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: .unspecified)
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
        }
    }
}

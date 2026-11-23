import { useState } from 'react';
import { Sparkles, Flame, Gem, Heart, Volume2, CheckCircle2, AlertCircle, Award, ChevronRight, Lock } from 'lucide-react';

interface QuestNode {
  id: string;
  title: string;
  subtitle: string;
  icon: string;
  stars: number;
  isCompleted: boolean;
  isCurrent: boolean;
  isLocked: boolean;
}

interface QuestUnit {
  id: number;
  number: number;
  title: string;
  subtitle: string;
  badge: string;
  color: string;
  nodes: QuestNode[];
}

const QUEST_UNITS: QuestUnit[] = [
  {
    id: 1,
    number: 1,
    title: 'Unit 1: First Words & Warm Greetings',
    subtitle: 'စတင်မိတ်ဆက်ခြင်းနှင့် နေ့စဉ် နှုတ်ခွန်းဆက် စကားများ',
    badge: '🌸 Basics',
    color: '#ec4899',
    nodes: [
      { id: 'u1_1', title: 'Hello & Welcome', subtitle: 'မင်္ဂလာပါနှင့် နှုတ်ခွန်းဆက်', icon: '👋', stars: 3, isCompleted: true, isCurrent: false, isLocked: false },
      { id: 'u1_2', title: 'Personal Pronouns', subtitle: 'I, You, We, They ဝါကျဖွဲ့ထုံး', icon: '✨', stars: 3, isCompleted: true, isCurrent: false, isLocked: false },
      { id: 'u1_3', title: 'Verb to Be (am/is/are)', subtitle: 'Be ကြိယာ ရှင်းလင်းချက်', icon: '⭐', stars: 2, isCompleted: false, isCurrent: true, isLocked: false },
      { id: 'u1_4', title: 'Unit 1 Milestone Chest', subtitle: 'ရတနာသေတ္တာ စိန်ဆုလာဘ်', icon: '🎁', stars: 0, isCompleted: false, isCurrent: false, isLocked: true },
    ],
  },
  {
    id: 2,
    number: 2,
    title: 'Unit 2: Cafe, Food & Daily Talks',
    subtitle: 'ကော်ဖီဆိုင်၊ စားသောက်ဆိုင်နှင့် နေ့စဉ်လူနေမှု',
    badge: '☕ Daily Life',
    color: '#a855f7',
    nodes: [
      { id: 'u2_1', title: 'Ordering Coffee', subtitle: 'ကော်ဖီနှင့် အစားအသောက် မှာယူခြင်း', icon: '☕', stars: 0, isCompleted: false, isCurrent: false, isLocked: true },
      { id: 'u2_2', title: 'Present Continuous', subtitle: 'လုပ်ဆောင်ဆဲ လုပ်ငန်းများ (-ing)', icon: '⏳', stars: 0, isCompleted: false, isCurrent: false, isLocked: true },
      { id: 'u2_3', title: 'Countable & Uncountable', subtitle: 'Much vs Many ပမာဏပြများ', icon: '🍰', stars: 0, isCompleted: false, isCurrent: false, isLocked: true },
      { id: 'u2_4', title: 'Unit 2 Milestone Chest', subtitle: 'စိန်ပွင့်ဆုလာဘ်', icon: '🎁', stars: 0, isCompleted: false, isCurrent: false, isLocked: true },
    ],
  },
  {
    id: 3,
    number: 3,
    title: 'Unit 3: Travel, Directions & Flights',
    subtitle: 'လေဆိပ်၊ ခရီးသွားလုပ်ငန်းနှင့် လမ်းညွှန်စကား',
    badge: '✈️ Adventure',
    color: '#f97316',
    nodes: [
      { id: 'u3_1', title: 'At the Airport', subtitle: 'လေဆိပ် Check-in နှင့် စကားပြော', icon: '🛫', stars: 0, isCompleted: false, isCurrent: false, isLocked: true },
      { id: 'u3_2', title: 'Past Simple Verbs', subtitle: 'အတိတ်ကာလ ကြိယာပုံစံများ', icon: '🕰️', stars: 0, isCompleted: false, isCurrent: false, isLocked: true },
      { id: 'u3_3', title: 'Asking Directions', subtitle: 'ဘယ်ဘက်၊ ညာဘက် လမ်းညွှန်စကား', icon: '🧭', stars: 0, isCompleted: false, isCurrent: false, isLocked: true },
      { id: 'u3_4', title: 'Unit 3 Milestone Chest', subtitle: 'ရတနာသေတ္တာ', icon: '🎁', stars: 0, isCompleted: false, isCurrent: false, isLocked: true },
    ],
  },
  {
    id: 4,
    number: 4,
    title: "Unit 4: Author's Grammar & Fluency",
    subtitle: 'ထက် ၏ A1–C2 သဒ္ဒါစည်းမျဉ်းများနှင့် ဝါကျဖွဲ့ထုံး',
    badge: '📖 Master Grammar',
    color: '#14b8a6',
    nodes: [
      { id: 'u4_1', title: 'Present Perfect', subtitle: 'Have/Has + V3 ချိတ်ဆက်ပုံ', icon: '💎', stars: 0, isCompleted: false, isCurrent: false, isLocked: true },
      { id: 'u4_2', title: 'Conditionals (If Clause)', subtitle: 'အခြေအနေပြ ဝါကျများ', icon: '🔮', stars: 0, isCompleted: false, isCurrent: false, isLocked: true },
      { id: 'u4_3', title: 'Passive Voice Mastery', subtitle: 'Subject အာရုံပြောင်း ဝါကျများ', icon: '⚡', stars: 0, isCompleted: false, isCurrent: false, isLocked: true },
      { id: 'u4_4', title: 'Crown Champion Node', subtitle: 'ဂုဏ်ထူးဆောင် သရဖူစိန်', icon: '👑', stars: 0, isCompleted: false, isCurrent: false, isLocked: true },
    ],
  },
];

interface QuestPathViewProps {
  onGainXp: (amount: number) => void;
}

export function QuestPathView({ onGainXp }: QuestPathViewProps) {
  const [hearts, setHearts] = useState(5);
  const [gems, setGems] = useState(420);
  const [streak] = useState(7);
  const [activeLesson, setActiveLesson] = useState<QuestNode | null>(null);

  // Exercise State inside Lesson Modal
  const wordPool = ['I', 'have', 'a', 'cup', 'of', 'coffee', 'every', 'morning', 'tea', 'water'];
  const correctSentence = ['I', 'have', 'a', 'cup', 'of', 'coffee', 'every', 'morning'];
  const [selectedWords, setSelectedWords] = useState<string[]>([]);
  const [checkStatus, setCheckStatus] = useState<'idle' | 'correct' | 'wrong'>('idle');
  const [isCompleted, setIsCompleted] = useState(false);

  const speak = (text: string) => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const u = new SpeechSynthesisUtterance(text);
      u.lang = 'en-US';
      u.rate = 0.9;
      window.speechSynthesis.speak(u);
    }
  };

  const handleStartNode = (node: QuestNode) => {
    if (node.isLocked) return;
    setActiveLesson(node);
    setSelectedWords([]);
    setCheckStatus('idle');
    setIsCompleted(false);
  };

  const handleToggleWord = (word: string, fromSelected: boolean, index?: number) => {
    if (fromSelected && index !== undefined) {
      const next = [...selectedWords];
      next.splice(index, 1);
      setSelectedWords(next);
    } else {
      if (!selectedWords.includes(word)) {
        setSelectedWords([...selectedWords, word]);
        speak(word);
      }
    }
    setCheckStatus('idle');
  };

  const handleCheck = () => {
    const isCorrect = selectedWords.join(' ') === correctSentence.join(' ');
    if (isCorrect) {
      setCheckStatus('correct');
      speak('Excellent! ' + correctSentence.join(' '));
    } else {
      setCheckStatus('wrong');
      setHearts((h) => Math.max(1, h - 1));
    }
  };

  const handleFinishLesson = () => {
    onGainXp(25);
    setGems((g) => g + 10);
    setIsCompleted(true);
  };

  return (
    <div className="quest-container">
      {/* FLOATING FROSTED GLASS STATS BAR */}
      <header className="quest-glass-bar">
        <div className="mascot-badge">
          <span className="mascot-bloom">🌸</span>
          <div>
            <div className="mascot-title">HtetLingo Quest</div>
            <div className="mascot-sub">Interactive Bloom Journey</div>
          </div>
        </div>

        <div className="quest-stats">
          <div className="quest-stat-pill streak">
            <Flame size={16} className="text-orange-500 fill-orange-500" />
            <span>{streak} Days</span>
          </div>

          <div className="quest-stat-pill gems">
            <Gem size={16} className="text-sky-500 fill-sky-500" />
            <span>{gems}</span>
          </div>

          <div className="quest-stat-pill hearts">
            <Heart size={16} className="text-pink-500 fill-pink-500" />
            <span>{hearts} / 5</span>
          </div>
        </div>
      </header>

      {/* S-CURVE QUEST JOURNEY MAP */}
      <div className="quest-map">
        {QUEST_UNITS.map((unit) => (
          <section key={unit.id} className="quest-unit-section">
            {/* Unit Header Card */}
            <div className="quest-unit-card" style={{ borderColor: `${unit.color}40` }}>
              <div className="quest-unit-info">
                <span className="quest-unit-badge" style={{ background: `${unit.color}20`, color: unit.color }}>
                  {unit.badge}
                </span>
                <h3>{unit.title}</h3>
                <p>{unit.subtitle}</p>
              </div>
              <Sparkles size={28} style={{ color: unit.color }} />
            </div>

            {/* Stepping-Stone Nodes */}
            <div className="quest-nodes-flow">
              {unit.nodes.map((node, i) => {
                const offsets = [0, 45, 80, 45, 0, -45, -80, -45];
                const xOffset = offsets[i % offsets.length];

                return (
                  <div
                    key={node.id}
                    className="quest-node-wrapper"
                    style={{ transform: `translateX(${xOffset}px)` }}
                  >
                    <button
                      className={`quest-node-btn ${node.isCompleted ? 'completed' : ''} ${
                        node.isCurrent ? 'current' : ''
                      } ${node.isLocked ? 'locked' : ''}`}
                      style={{
                        background: node.isCompleted
                          ? 'linear-gradient(135deg, #fbbf24, #f59e0b)'
                          : node.isCurrent
                          ? `linear-gradient(135deg, ${unit.color}, #f43f5e)`
                          : 'var(--surface-2)',
                      }}
                      onClick={() => handleStartNode(node)}
                      disabled={node.isLocked}
                    >
                      {node.isLocked ? <Lock size={24} /> : <span className="node-icon">{node.icon}</span>}
                    </button>

                    {node.isCompleted && (
                      <div className="node-stars">
                        {'★'.repeat(node.stars)}{'☆'.repeat(3 - node.stars)}
                      </div>
                    )}

                    {node.isCurrent && (
                      <div className="node-start-badge" style={{ background: unit.color }}>
                        START
                      </div>
                    )}

                    <div className="node-label">
                      <strong>{node.title}</strong>
                      <small>{node.subtitle}</small>
                    </div>
                  </div>
                );
              })}
            </div>
          </section>
        ))}
      </div>

      {/* FULL-SCREEN LESSON MODAL */}
      {activeLesson && (
        <div className="quest-modal-backdrop">
          <div className="quest-modal-card">
            {/* Modal Header */}
            <div className="quest-modal-header">
              <button
                className="quest-modal-exit"
                onClick={() => setActiveLesson(null)}
              >
                ✕
              </button>
              <div className="quest-progress-track">
                <div
                  className="quest-progress-fill"
                  style={{ width: isCompleted ? '100%' : checkStatus === 'correct' ? '85%' : '45%' }}
                />
              </div>
              <div className="quest-modal-hearts">
                <Heart size={18} className="fill-pink-500 text-pink-500" />
                <span>{hearts}</span>
              </div>
            </div>

            {/* Modal Body */}
            {isCompleted ? (
              <div className="quest-celebration-pane">
                <div className="celebration-sparkle">🌸✨🎉</div>
                <h2>သင်ခန်းစာ အောင်မြင်စွာ ပြီးဆုံးပါပြီ!</h2>
                <p>Lesson Completed · You mastered this stepping stone!</p>

                <div className="reward-cards-row">
                  <div className="reward-chip xp">
                    <Award size={24} className="text-amber-500" />
                    <strong>+25 XP</strong>
                    <small>Daily Experience</small>
                  </div>
                  <div className="reward-chip gem">
                    <Gem size={24} className="text-sky-500" />
                    <strong>+10 Gems</strong>
                    <small>Crystal Bonus</small>
                  </div>
                </div>

                <button
                  className="quest-primary-btn"
                  onClick={() => setActiveLesson(null)}
                >
                  Continue (ရှေ့သို့ ဆက်လေ့လာမည်)
                  <ChevronRight size={18} />
                </button>
              </div>
            ) : (
              <div className="quest-exercise-pane">
                <div className="exercise-prompt-box">
                  <span className="exercise-tag">🌸 Sentence Builder</span>
                  <h4>အောက်ပါ မြန်မာဝါကျနှင့် ကိုက်ညီသော အင်္ဂလိပ်စာလုံးများကို စီစဉ်ပါ:</h4>
                  <div className="prompt-bubble">
                    <button
                      className="audio-btn"
                      onClick={() => speak('I have a cup of coffee every morning')}
                      title="Listen audio"
                    >
                      <Volume2 size={20} />
                    </button>
                    <span className="target-my">"ကျနော် မနက်တိုင်း ကော်ဖီတစ်ခွက် သောက်တယ်"</span>
                  </div>
                </div>

                {/* Selected Words Area */}
                <div className="sentence-assembly-zone">
                  {selectedWords.length === 0 ? (
                    <span className="assembly-placeholder">အောက်မှ စကားလုံးများကို နှိပ်ပြီး စီစဉ်ပါ…</span>
                  ) : (
                    selectedWords.map((word, idx) => (
                      <button
                        key={`${word}-${idx}`}
                        className="candy-bubble selected"
                        onClick={() => handleToggleWord(word, true, idx)}
                      >
                        {word}
                      </button>
                    ))
                  )}
                </div>

                {/* Word Pool Area */}
                <div className="word-pool-label">ရွေးချယ်ရန် စကားလုံးများ (Tap word to build):</div>
                <div className="word-pool-zone">
                  {wordPool.map((word, idx) => {
                    const isUsed = selectedWords.includes(word);
                    return (
                      <button
                        key={`${word}-${idx}`}
                        className={`candy-bubble pool ${isUsed ? 'used' : ''}`}
                        onClick={() => handleToggleWord(word, false)}
                        disabled={isUsed}
                      >
                        {word}
                      </button>
                    );
                  })}
                </div>

                {/* Feedback & Actions */}
                <div className="quest-action-bar">
                  {checkStatus === 'correct' && (
                    <div className="status-feedback correct">
                      <CheckCircle2 size={22} />
                      <div>
                        <strong>အံ့ဩဖွယ် မှန်ကန်ပါတယ်! (Excellent!)</strong>
                        <p>I have a cup of coffee every morning.</p>
                      </div>
                    </div>
                  )}

                  {checkStatus === 'wrong' && (
                    <div className="status-feedback wrong">
                      <AlertCircle size={22} />
                      <div>
                        <strong>မမှန်သေးပါ၊ ပြန်လည်စစ်ဆေးကြည့်ပါ</strong>
                        <p>သဲလွန်စ: I have a cup of coffee...</p>
                      </div>
                    </div>
                  )}

                  <div className="action-buttons-right">
                    {checkStatus === 'correct' ? (
                      <button className="quest-primary-btn success" onClick={handleFinishLesson}>
                        Continue (ဆက်လက်လေ့လာမည်)
                        <ChevronRight size={18} />
                      </button>
                    ) : (
                      <button
                        className="quest-primary-btn"
                        onClick={handleCheck}
                        disabled={selectedWords.length === 0}
                      >
                        Check Answer (စစ်ဆေးမည်)
                      </button>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

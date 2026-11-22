# HtetLingo v2

> **Learn Languages. Understand Words. Connect Cultures.**  
> Multilingual Learning, Dictionary, and Translation Platform for **English, Burmese, Japanese, Chinese, Korean, Thai, French, and German**.

## Live Deployments
- **Web App & iPad PWA (Cloudflare Pages):** https://htetlingo.pages.dev
- **Landing Page, Docs & Static Demo (GitHub Pages):** https://htetofficial.github.io/htetlingo-v2/

## Architecture (Monorepo)

| Directory | Platform / Purpose | Tech Stack |
| --- | --- | --- |
| `apps/web` | Actual HtetLingo Web App + iPad PWA | React 19, TypeScript, Vite, Service Worker, IndexedDB, Cloudflare Pages Functions |
| `apps/site` | Landing Page, Documentation, Static Demo, Project Intro | React 19, TypeScript, Vite, GitHub Actions → GitHub Pages |
| `apps/macos` | Mac Native App | Swift 6, SwiftUI, Observation, SQLite3 FTS5, AVSpeechSynthesizer |
| `apps/android` | Android Native App | Kotlin 2.1, Jetpack Compose (Material 3), SQLite, Android TextToSpeech, RevenueCat SDK |
| `packages/core` | Shared Domain Models, SM-2 SRS Engine & Multilingual Decks | TypeScript |
| `packages/data-pipeline` | Ornagai StarDict Parser (111,232 headwords, 249,103 examples) | Python 3 → SQLite FTS5 + 27 JSON shards |
| `packages/supabase` | Cloud Sync Database Schema & Row Level Security (RLS) | PostgreSQL / Supabase |

## Features
- **Vocabulary** with SM-2 Spaced Repetition flashcards across 8 languages
- **Grammar** lessons with Burmese explanations, formulas, and common error fixes
- **Quiz & Exercises** (multiple choice, grammar, listening comprehension)
- **Pronunciation & Listening** (Web Speech API / AVSpeechSynthesizer / Android TextToSpeech)
- **Dictionary** powered by Ornagai English–Myanmar (111,232 headwords)
- **Translation** via Cloudflare Pages Function `/api/translate` + offline phrasebook fallback
- **Offline Learning** via IndexedDB shards (Web/iPad PWA) and SQLite FTS5 (Mac/Android)
- **Learning Progress & User Accounts** backed by Supabase Auth + RLS
- **Monetization** via RevenueCat (Free, Premium, Lifetime, Optional Language & Learning Packs)

## Quick Start

```bash
# Install workspace dependencies
pnpm install

# Build Web App + GitHub Pages site
pnpm build:all

# Run Web App locally
pnpm dev:web

# Build Mac Native app
cd apps/macos && swift build

# Rebuild Ornagai SQLite & JSON shards from StarDict zip
python3 packages/data-pipeline/build_ornagai.py /path/to/ornagai-en-my-dict.zip
```

## Security
Never commit `.env.local` or secret keys (`sb_secret_*`, `cfat_*`, `ghp_*`). Only public/publishable keys belong in client apps with Supabase Row Level Security (RLS) enabled.

-- HtetLingo Supabase Schema with Row Level Security (RLS)
-- Project: https://tqwvzvbnintvijiessgm.supabase.co
-- Run this in Supabase SQL Editor to initialize cloud sync tables.

-- 1. User Profiles
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  native_language text not null default 'my',
  target_languages text[] not null default array['en', 'ja'],
  daily_goal_xp int not null default 50,
  total_xp int not null default 0,
  streak_days int not null default 0,
  last_active_date date,
  subscription_tier text not null default 'free' check (subscription_tier in ('free', 'premium', 'lifetime')),
  unlocked_packs text[] not null default array[]::text[],
  updated_at timestamptz not null default now()
);

-- 2. Saved / Bookmarked Dictionary Words
create table if not exists public.saved_words (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  word text not null,
  language_pair text not null default 'en-my',
  notes text,
  created_at timestamptz not null default now(),
  unique(user_id, word, language_pair)
);

-- 3. Spaced Repetition (SM-2) Vocabulary Cards
create table if not exists public.srs_cards (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  card_key text not null,
  language_code text not null,
  ease_factor real not null default 2.5,
  interval_days int not null default 1,
  repetitions int not null default 0,
  next_review_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(user_id, card_key)
);

-- 4. Learning Progress & Quiz Attempts
create table if not exists public.quiz_attempts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  language_code text not null,
  module text not null check (module in ('vocabulary', 'grammar', 'listening', 'translation')),
  score int not null,
  total int not null,
  xp_earned int not null default 0,
  completed_at timestamptz not null default now()
);

-- Enable Row Level Security on every table
alter table public.profiles enable row level security;
alter table public.saved_words enable row level security;
alter table public.srs_cards enable row level security;
alter table public.quiz_attempts enable row level security;

-- RLS Policies: Users can only read/write their own rows
create policy "Users manage own profile"
  on public.profiles for all
  using (auth.uid() = id)
  with check (auth.uid() = id);

create policy "Users manage own saved_words"
  on public.saved_words for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "Users manage own srs_cards"
  on public.srs_cards for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "Users manage own quiz_attempts"
  on public.quiz_attempts for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- Auto-create profile row on new user sign-up (grants Founder VIP Lifetime + all packs to htetmyanmar321@icloud.com)
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  is_vip boolean := lower(coalesce(new.email, '')) = 'htetmyanmar321@icloud.com';
begin
  insert into public.profiles (id, display_name, subscription_tier, unlocked_packs)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'display_name', split_part(new.email, '@', 1)),
    case when is_vip then 'lifetime' else 'free' end,
    case when is_vip then array['pack_jlpt_n5_n3', 'pack_ielts_my', 'pack_hsk_1_4', 'pack_topik_kr'] else array[]::text[] end
  )
  on conflict (id) do update
    set subscription_tier = case when is_vip then 'lifetime' else public.profiles.subscription_tier end,
        unlocked_packs = case when is_vip then array['pack_jlpt_n5_n3', 'pack_ielts_my', 'pack_hsk_1_4', 'pack_topik_kr'] else public.profiles.unlocked_packs end;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

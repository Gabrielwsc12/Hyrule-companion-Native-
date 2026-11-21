-- Hyrule Companion v1.12
-- Execute no SQL Editor do seu projeto Supabase.
create table if not exists public.hc_user_data (
  user_id uuid primary key references auth.users(id) on delete cascade,
  payload jsonb not null,
  updated_at timestamptz not null default now()
);

alter table public.hc_user_data enable row level security;
grant select, insert, update on table public.hc_user_data to authenticated;

-- Cada conta pode ler apenas sua própria jornada.
drop policy if exists "Users can read their own Hyrule Companion data" on public.hc_user_data;
drop policy if exists "Users can insert their own Hyrule Companion data" on public.hc_user_data;
drop policy if exists "Users can update their own Hyrule Companion data" on public.hc_user_data;

create policy "Users can read their own Hyrule Companion data"
  on public.hc_user_data for select
  to authenticated
  using (auth.uid() = user_id);

-- Cada conta pode inserir apenas sua própria jornada.
create policy "Users can insert their own Hyrule Companion data"
  on public.hc_user_data for insert
  to authenticated
  with check (auth.uid() = user_id);

-- Cada conta pode atualizar apenas sua própria jornada.
create policy "Users can update their own Hyrule Companion data"
  on public.hc_user_data for update
  to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- Nenhuma policy de leitura pública ou acesso anônimo é criada.

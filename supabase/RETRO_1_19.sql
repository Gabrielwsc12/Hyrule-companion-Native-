begin;

create table if not exists public.hc_retro_snapshots (
    user_id uuid primary key references auth.users(id) on delete cascade,
    handle text not null check (handle ~ '^[A-Za-z0-9_-]{1,32}$'),
    retro_id text not null unique check (retro_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    games jsonb not null check (jsonb_typeof(games) = 'array' and pg_column_size(games) <= 4096),
    synced_at timestamptz not null default now()
);

create table if not exists public.hc_retro_attempts (
    user_id uuid primary key references auth.users(id) on delete cascade,
    attempted_at timestamptz not null default '-infinity',
    challenge text,
    expires_at timestamptz
);

alter table public.hc_retro_snapshots enable row level security;
alter table public.hc_retro_attempts enable row level security;
revoke all on public.hc_retro_snapshots, public.hc_retro_attempts from public, anon, authenticated;

create or replace function public.hc_retro_begin(p_user uuid) returns boolean
language sql security definer set search_path = '' as $$
    with attempt as (
        insert into public.hc_retro_attempts as previous(user_id, attempted_at) values (p_user, now())
        on conflict (user_id) do update set attempted_at = excluded.attempted_at
        where previous.attempted_at < now() - interval '1 minute'
        returning 1
    )
    select exists(select 1 from attempt);
$$;

create or replace function public.hc_retro_challenge() returns text
language sql security definer set search_path = '' as $$
    insert into public.hc_retro_attempts as previous(user_id, challenge, expires_at)
    values (auth.uid(), 'HC-' || gen_random_uuid(), now() + interval '30 minutes')
    on conflict (user_id) do update set
        challenge = case when previous.expires_at > now() then previous.challenge else excluded.challenge end,
        expires_at = case when previous.expires_at > now() then previous.expires_at else excluded.expires_at end
    returning challenge;
$$;

create or replace function public.hc_retro_link_status(p_user uuid) returns jsonb
language sql stable security definer set search_path = '' as $$
    select jsonb_build_object('handle', s.handle, 'retro_id', s.retro_id,
        'challenge', case when a.expires_at > now() then a.challenge end)
    from public.hc_retro_attempts a left join public.hc_retro_snapshots s on s.user_id = a.user_id
    where a.user_id = p_user;
$$;

create or replace function public.hc_retro_save(p_user uuid, p_handle text, p_retro_id text, p_games jsonb, p_challenge text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare result public.hc_retro_snapshots;
begin
    perform 1 from public.hc_retro_attempts where user_id = p_user for update;
    if not exists(select 1 from public.hc_retro_snapshots where user_id = p_user and retro_id = p_retro_id) then
        update public.hc_retro_attempts set challenge = null, expires_at = null
        where user_id = p_user and challenge = p_challenge and expires_at > now();
        if not found then raise exception 'verification'; end if;
    end if;
    insert into public.hc_retro_snapshots(user_id, handle, retro_id, games, synced_at)
    values(p_user, p_handle, p_retro_id, p_games, now())
    on conflict (user_id) do update set handle = excluded.handle, retro_id = excluded.retro_id,
        games = excluded.games, synced_at = excluded.synced_at
    returning * into result;
    if (select coalesce(sum((g->>'earned')::int), 0) from jsonb_array_elements(p_games) g) > 50 then
        insert into public.hc_store_wallets(user_id) values(p_user) on conflict do nothing;
        insert into public.hc_store_inventory(user_id, item) values(p_user, 'ra_trophy') on conflict do nothing;
    end if;
    return to_jsonb(result) - 'user_id' - 'retro_id';
end
$$;

create or replace function public.hc_retro_summary(p_user uuid) returns jsonb
language sql stable security definer set search_path = '' as $$
    select to_jsonb(s) - 'user_id' - 'retro_id' from public.hc_retro_snapshots s
    where s.user_id = p_user and (auth.uid() = p_user or exists (
        select 1 from public.hc_community_profiles p where p.user_id = p_user and p.discoverable
    ));
$$;

revoke all on function public.hc_retro_begin(uuid), public.hc_retro_save(uuid,text,text,jsonb,text),
    public.hc_retro_link_status(uuid), public.hc_retro_summary(uuid), public.hc_retro_challenge()
from public, anon, authenticated;
grant execute on function public.hc_retro_begin(uuid), public.hc_retro_save(uuid,text,text,jsonb,text),
    public.hc_retro_link_status(uuid) to service_role;
grant execute on function public.hc_retro_summary(uuid), public.hc_retro_challenge() to authenticated;
commit;

begin;

create table public.hc_community_profiles (
    user_id uuid primary key references auth.users(id) on delete cascade,
    handle text not null unique check (handle ~ '^[a-z0-9_]{3,24}$'),
    favorite_game text check (favorite_game ~ '^[a-z0-9_-]{1,80}$'),
    discoverable boolean not null default false,
    share_journey boolean not null default false,
    journey jsonb not null default '{}'::jsonb
        check (
            jsonb_typeof(journey) = 'object' and pg_column_size(journey) <= 12000
            and not jsonb_path_exists(journey, 'strict $.* ? (@.type() != "string" || (@ != "Em andamento" && @ != "Concluído" && @ != "100%"))')
        ),
    check ((discoverable and share_journey) or journey = '{}'::jsonb)
);

create table public.hc_community_follows (
    follower_id uuid not null references public.hc_community_profiles(user_id) on delete cascade,
    followed_id uuid not null references public.hc_community_profiles(user_id) on delete cascade,
    primary key (follower_id, followed_id),
    check (follower_id <> followed_id)
);

create index hc_community_follows_followed_idx on public.hc_community_follows (followed_id);

alter table public.hc_community_profiles enable row level security;
alter table public.hc_community_follows enable row level security;

revoke all on public.hc_community_profiles, public.hc_community_follows from public, anon, authenticated;
grant select, insert, update on public.hc_community_profiles to authenticated;
grant select, insert, delete on public.hc_community_follows to authenticated;

create policy "Learn about discoverable profiles or your own"
on public.hc_community_profiles for select to authenticated
using (discoverable or user_id = (select auth.uid()));

create policy "Create your own profile"
on public.hc_community_profiles for insert to authenticated
with check (user_id = (select auth.uid()));

create policy "Update your own profile"
on public.hc_community_profiles for update to authenticated
using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

create policy "Read your own follows"
on public.hc_community_follows for select to authenticated
using (follower_id = (select auth.uid()));

create policy "Follow discoverable profiles from your own profile"
on public.hc_community_follows for insert to authenticated
with check (
    follower_id = (select auth.uid())
    and exists (select 1 from public.hc_community_profiles p where p.user_id = followed_id and p.discoverable)
);

create policy "Unfollow from your own account"
on public.hc_community_follows for delete to authenticated
using (follower_id = (select auth.uid()));

commit;

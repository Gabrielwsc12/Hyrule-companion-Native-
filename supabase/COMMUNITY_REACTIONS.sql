begin;

create table public.hc_community_applause (
    profile_id uuid not null references public.hc_community_profiles(user_id) on delete cascade,
    game_id text not null check (game_id ~ '^[a-z0-9_-]{1,80}$'),
    reactor_id uuid not null references auth.users(id) on delete cascade,
    primary key (profile_id, game_id, reactor_id),
    check (profile_id <> reactor_id)
);

create index hc_community_applause_reactor_idx on public.hc_community_applause (reactor_id);
alter table public.hc_community_applause enable row level security;
revoke all on public.hc_community_applause from public, anon, authenticated;

create function public.hc_community_reaction_counts(target_profile uuid)
returns table(game_id text, applause_count bigint, applauded boolean)
language sql stable security definer set search_path = ''
as $$
    select r.game_id, count(*), bool_or(r.reactor_id = (select auth.uid()))
    from public.hc_community_applause r
    join public.hc_community_profiles p on p.user_id = r.profile_id
    where auth.uid() is not null and p.user_id = target_profile
        and p.discoverable and p.share_journey
        and p.journey ->> r.game_id in ('Concluído', '100%')
    group by r.game_id;
$$;

create function public.hc_community_set_applause(target_profile uuid, target_game text, is_applauded boolean)
returns void language plpgsql security definer set search_path = ''
as $$
declare
    actor uuid := auth.uid();
begin
    if actor is null or actor = target_profile then
        raise exception 'Reação indisponível' using errcode = '42501';
    end if;

    if is_applauded then
        -- Serialize additions with changes to sharing or to the published journey.
        perform 1 from public.hc_community_profiles p
        where p.user_id = target_profile and p.discoverable and p.share_journey
            and p.journey ->> target_game in ('Concluído', '100%')
        for share;
        if not found then
            raise exception 'Conquista indisponível' using errcode = '42501';
        end if;
        insert into public.hc_community_applause values (target_profile, target_game, actor)
        on conflict do nothing;
    else
        delete from public.hc_community_applause
        where profile_id = target_profile and game_id = target_game and reactor_id = actor;
    end if;
end;
$$;

create function public.hc_community_clean_applause()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
    delete from public.hc_community_applause
    where profile_id = new.user_id and (
        not new.discoverable or not new.share_journey
        or coalesce(new.journey ->> game_id, '') not in ('Concluído', '100%')
    );
    return new;
end;
$$;

create trigger hc_community_clean_applause
after update of discoverable, share_journey, journey on public.hc_community_profiles
for each row execute function public.hc_community_clean_applause();

revoke execute on function public.hc_community_reaction_counts(uuid),
    public.hc_community_set_applause(uuid, text, boolean), public.hc_community_clean_applause()
from public, anon, authenticated;
grant execute on function public.hc_community_reaction_counts(uuid),
    public.hc_community_set_applause(uuid, text, boolean) to authenticated;

commit;

begin;

alter table public.hc_community_profiles drop constraint hc_community_banner_format;
alter table public.hc_community_profiles add constraint hc_community_banner_format check (
    length(banner_image) <= 2400030
    and banner_image ~ '^data:image/(jpeg|png|gif);base64,[A-Za-z0-9+/]+=*$'
);

create table public.hc_presence_preferences (
    user_id uuid primary key references auth.users(id) on delete cascade,
    hidden boolean not null default false
);

create table public.hc_presence_devices (
    user_id uuid not null references auth.users(id) on delete cascade,
    device_id uuid not null,
    expires_at timestamptz not null,
    primary key (user_id, device_id)
);

alter table public.hc_presence_preferences enable row level security;
alter table public.hc_presence_devices enable row level security;
revoke all on public.hc_presence_preferences, public.hc_presence_devices from public, anon, authenticated;

create function public.hc_presence_heartbeat(device uuid, active boolean)
returns boolean language plpgsql security definer set search_path = '' as $$
declare actor uuid := auth.uid();
begin
    if actor is null then raise exception 'Conta necessária' using errcode = '42501'; end if;
    delete from public.hc_presence_devices
        where user_id = actor and (expires_at <= now() or (not active and device_id = device));
    if active then
        insert into public.hc_presence_devices values (actor, device, now() + interval '75 seconds')
        on conflict (user_id, device_id) do update set expires_at = excluded.expires_at;
    end if;
    return coalesce((select hidden from public.hc_presence_preferences where user_id = actor), false);
end;
$$;

create function public.hc_presence_set_hidden(value boolean)
returns void language plpgsql security definer set search_path = '' as $$
declare actor uuid := auth.uid();
begin
    if actor is null then raise exception 'Conta necessária' using errcode = '42501'; end if;
    insert into public.hc_presence_preferences values (actor, value)
    on conflict (user_id) do update set hidden = excluded.hidden;
end;
$$;

create function public.hc_presence_online(target uuid)
returns boolean language sql stable security definer set search_path = '' as $$
    select auth.uid() is not null
        and exists (select 1 from public.hc_community_profiles where user_id = target and discoverable)
        and not exists (select 1 from public.hc_presence_preferences where user_id = target and hidden)
        and exists (select 1 from public.hc_presence_devices where user_id = target and expires_at > now());
$$;

revoke execute on function public.hc_presence_heartbeat(uuid, boolean),
    public.hc_presence_set_hidden(boolean), public.hc_presence_online(uuid) from public, anon, authenticated;
grant execute on function public.hc_presence_heartbeat(uuid, boolean),
    public.hc_presence_set_hidden(boolean), public.hc_presence_online(uuid) to authenticated;
commit;

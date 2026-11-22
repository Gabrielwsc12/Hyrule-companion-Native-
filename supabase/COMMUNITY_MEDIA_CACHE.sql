begin;

alter table public.hc_community_profiles
    add column if not exists media_revision uuid not null default gen_random_uuid();

create or replace function public.hc_track_community_media_revision()
returns trigger language plpgsql set search_path = '' as $$
begin
    if tg_op = 'INSERT' then
        new.media_revision := gen_random_uuid();
    elsif new.avatar_image is distinct from old.avatar_image
        or new.banner_image is distinct from old.banner_image then
        new.media_revision := gen_random_uuid();
    else
        new.media_revision := old.media_revision;
    end if;
    return new;
end;
$$;

drop trigger if exists hc_community_media_revision on public.hc_community_profiles;
create trigger hc_community_media_revision
before insert or update of avatar_image, banner_image, media_revision on public.hc_community_profiles
for each row execute function public.hc_track_community_media_revision();

create or replace function public.hc_read_community_profile(target uuid, known_media uuid)
returns jsonb language sql stable security invoker set search_path = '' as $$
    select jsonb_build_object(
        'user_id', p.user_id, 'handle', p.handle, 'favorite_game', p.favorite_game,
        'discoverable', p.discoverable, 'share_journey', p.share_journey, 'journey', p.journey,
        'media_revision', p.media_revision
    ) || case when p.media_revision = known_media then '{}'::jsonb
        else jsonb_build_object('avatar_image', p.avatar_image, 'banner_image', p.banner_image) end
    from public.hc_community_profiles p
    where p.user_id = target;
$$;

revoke execute on function public.hc_track_community_media_revision() from public, anon, authenticated;
revoke execute on function public.hc_read_community_profile(uuid, uuid) from public, anon, authenticated;
grant execute on function public.hc_read_community_profile(uuid, uuid) to authenticated;

commit;

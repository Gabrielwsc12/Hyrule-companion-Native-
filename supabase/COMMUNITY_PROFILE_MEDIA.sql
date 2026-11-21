begin;

alter table public.hc_community_profiles
    add column if not exists avatar_image text,
    add column if not exists banner_image text;

alter table public.hc_community_profiles
    add constraint hc_community_avatar_format check (
        length(avatar_image) <= 2400030
        and avatar_image ~ '^data:image/(jpeg|png|webp|gif);base64,[A-Za-z0-9+/]+=*$'
    ),
    add constraint hc_community_banner_format check (
        length(banner_image) <= 1333360
        and banner_image ~ '^data:image/(jpeg|png);base64,[A-Za-z0-9+/]+=*$'
    );

commit;

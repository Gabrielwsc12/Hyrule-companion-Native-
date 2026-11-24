begin;

create table if not exists public.hc_adventure_posts (
    id bigint generated always as identity primary key,
    author uuid not null references public.hc_community_profiles(user_id) on delete cascade,
    body text not null check (char_length(body) <= 1600),
    has_image boolean not null,
    created_at timestamptz not null default now(),
    check (btrim(body) <> '' or has_image)
);
create index if not exists hc_adventure_posts_author on public.hc_adventure_posts(author, id desc);

create table if not exists public.hc_adventure_images (
    post_id bigint primary key references public.hc_adventure_posts(id) on delete cascade,
    image text not null check (char_length(image) <= 400023 and image ~ '^data:image/jpeg;base64,[A-Za-z0-9+/]+={0,2}$')
);

create table if not exists public.hc_adventure_comments (
    id bigint generated always as identity primary key,
    post_id bigint not null references public.hc_adventure_posts(id) on delete cascade,
    author uuid not null references public.hc_community_profiles(user_id) on delete cascade,
    body text not null check (char_length(btrim(body)) between 1 and 600),
    created_at timestamptz not null default now()
);
create index if not exists hc_adventure_comments_post on public.hc_adventure_comments(post_id, id desc);
create index if not exists hc_adventure_comments_author on public.hc_adventure_comments(author, id desc);

create table if not exists public.hc_adventure_applause (
    post_id bigint not null references public.hc_adventure_posts(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    primary key (post_id, user_id)
);

alter table public.hc_adventure_posts enable row level security;
alter table public.hc_adventure_images enable row level security;
alter table public.hc_adventure_comments enable row level security;
alter table public.hc_adventure_applause enable row level security;
revoke all on public.hc_adventure_posts, public.hc_adventure_images, public.hc_adventure_comments, public.hc_adventure_applause
from public, anon, authenticated;

create or replace function public.hc_adventure_visible(p_post bigint) returns boolean
language sql security definer stable set search_path = '' as $$
    select exists(
        select 1 from public.hc_adventure_posts a join public.hc_community_profiles p on p.user_id = a.author
        where a.id = p_post and (p.discoverable or a.author = auth.uid())
    );
$$;

create or replace function public.hc_adventure_feed(p_following boolean, p_before bigint default null) returns jsonb
language sql security definer stable set search_path = '' as $$
    select coalesce(jsonb_agg(to_jsonb(item) order by item.id desc), '[]'::jsonb)
    from (
        select a.id, p.handle, a.body, a.has_image, a.created_at, a.author = auth.uid() as mine,
            (select count(*) from public.hc_adventure_applause l where l.post_id = a.id) as applause,
            exists(select 1 from public.hc_adventure_applause l where l.post_id = a.id and l.user_id = auth.uid()) as applauded,
            (select count(*) from public.hc_adventure_comments c join public.hc_community_profiles cp on cp.user_id = c.author
                where c.post_id = a.id and (cp.discoverable or c.author = auth.uid())) as comments
        from public.hc_adventure_posts a join public.hc_community_profiles p on p.user_id = a.author
        where auth.uid() is not null and (p.discoverable or a.author = auth.uid())
        and (p_before is null or a.id < p_before)
        and (not p_following or exists (
            select 1 from public.hc_community_follows f where f.follower_id = auth.uid() and f.followed_id = a.author
        ))
        order by a.id desc limit 12
    ) item;
$$;

create or replace function public.hc_adventure_image(p_post bigint) returns text
language sql security definer stable set search_path = '' as $$
    select image from public.hc_adventure_images where post_id = p_post and auth.uid() is not null
        and public.hc_adventure_visible(p_post);
$$;

create or replace function public.hc_adventure_publish(p_body text, p_image text default null) returns bigint
language plpgsql security definer set search_path = '' as $$
declare actor uuid := auth.uid(); item bigint;
begin
    perform 1 from public.hc_community_profiles where user_id = actor and discoverable for update;
    if not found then raise exception 'profile_required'; end if;
    if exists(select 1 from public.hc_adventure_posts where author = actor and created_at > now() - interval '15 seconds') then
        raise exception 'post_rate';
    end if;
    insert into public.hc_adventure_posts(author, body, has_image)
    values (actor, btrim(coalesce(p_body, '')), p_image is not null) returning id into item;
    if p_image is not null then
        if char_length(p_image) > 400023 then raise exception 'invalid_image'; end if;
        if substring(decode(split_part(p_image, ',', 2),'base64') for 3) <> decode('ffd8ff','hex') then
            raise exception 'invalid_image';
        end if;
        insert into public.hc_adventure_images(post_id, image) values (item, p_image);
    end if;
    return item;
end
$$;

create or replace function public.hc_adventure_delete(p_post bigint) returns void
language sql security definer set search_path = '' as $$
    delete from public.hc_adventure_posts where id = p_post and author = auth.uid();
$$;

create or replace function public.hc_adventure_clap(p_post bigint, p_applaud boolean) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare actor uuid := auth.uid();
begin
    if actor is null or not public.hc_adventure_visible(p_post) then raise exception 'post_unavailable'; end if;
    if p_applaud then
        insert into public.hc_adventure_applause(post_id, user_id) values(p_post, actor) on conflict do nothing;
    else
        delete from public.hc_adventure_applause where post_id = p_post and user_id = actor;
    end if;
    return jsonb_build_object('count', (select count(*) from public.hc_adventure_applause where post_id = p_post));
end
$$;

create or replace function public.hc_adventure_comments(p_post bigint, p_before bigint default null) returns jsonb
language sql security definer stable set search_path = '' as $$
    select coalesce(jsonb_agg(to_jsonb(item) order by item.id desc), '[]'::jsonb)
    from (
        select c.id, p.handle, c.body, c.created_at, c.author = auth.uid() as mine
        from public.hc_adventure_comments c join public.hc_community_profiles p on p.user_id = c.author
        where auth.uid() is not null and c.post_id = p_post and (p.discoverable or c.author = auth.uid())
          and (p_before is null or c.id < p_before) and public.hc_adventure_visible(p_post)
        order by c.id desc limit 30
    ) item;
$$;

create or replace function public.hc_adventure_comment(p_post bigint, p_body text) returns void
language plpgsql security definer set search_path = '' as $$
declare actor uuid := auth.uid();
begin
    perform 1 from public.hc_community_profiles where user_id = actor and discoverable for update;
    if not found then raise exception 'profile_required'; end if;
    if not public.hc_adventure_visible(p_post) then raise exception 'post_unavailable'; end if;
    if exists(select 1 from public.hc_adventure_comments where author = actor and created_at > now() - interval '4 seconds') then
        raise exception 'comment_rate';
    end if;
    insert into public.hc_adventure_comments(post_id, author, body) values (p_post, actor, btrim(p_body));
end
$$;

create or replace function public.hc_adventure_delete_comment(p_comment bigint) returns void
language sql security definer set search_path = '' as $$
    delete from public.hc_adventure_comments where id = p_comment and author = auth.uid();
$$;

revoke all on function public.hc_adventure_visible(bigint) from public, anon, authenticated;
revoke all on function public.hc_adventure_feed(boolean,bigint), public.hc_adventure_image(bigint), public.hc_adventure_publish(text,text),
    public.hc_adventure_delete(bigint), public.hc_adventure_clap(bigint,boolean), public.hc_adventure_comments(bigint,bigint),
    public.hc_adventure_comment(bigint,text), public.hc_adventure_delete_comment(bigint)
from public, anon, authenticated;
grant execute on function public.hc_adventure_feed(boolean,bigint), public.hc_adventure_image(bigint), public.hc_adventure_publish(text,text),
    public.hc_adventure_delete(bigint), public.hc_adventure_clap(bigint,boolean), public.hc_adventure_comments(bigint,bigint),
    public.hc_adventure_comment(bigint,text), public.hc_adventure_delete_comment(bigint)
to authenticated;
commit;

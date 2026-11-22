begin;

create table if not exists public.hc_store_wallets (
    user_id uuid primary key references auth.users(id) on delete cascade,
    balance bigint not null default 0 check (balance >= 0),
    last_cast_at timestamptz
);
create table if not exists public.hc_store_inventory (
    user_id uuid not null references public.hc_store_wallets(user_id) on delete cascade,
    item text not null,
    equipped boolean not null default false check (item <> 'theme' or not equipped),
    primary key (user_id, item)
);
alter table public.hc_store_inventory drop constraint if exists hc_store_inventory_item_check;
alter table public.hc_store_inventory add constraint hc_store_inventory_item_check
    check (item in ('sword','gold','hat','theme','beta','chester'));

create table if not exists public.hc_store_creator (
    singleton boolean primary key default true check (singleton),
    user_id uuid not null references auth.users(id) on delete cascade
);
alter table public.hc_store_creator enable row level security;
revoke all on public.hc_store_creator from public, anon, authenticated;

create table if not exists public.hc_store_casts (
    user_id uuid not null references public.hc_store_wallets(user_id) on delete cascade,
    id uuid not null,
    created_at timestamptz not null,
    ready_at timestamptz not null,
    rarity text not null,
    species text not null,
    wait_ms integer not null,
    reward integer not null,
    claimed boolean not null default false,
    primary key (user_id, id)
);
create index if not exists hc_store_casts_user_created_idx on public.hc_store_casts(user_id, created_at);
create table if not exists public.hc_store_flags (
    singleton boolean primary key default true check (singleton),
    beta_on_sale boolean not null default true
);
insert into public.hc_store_flags(singleton) values (true) on conflict do nothing;

alter table public.hc_store_wallets enable row level security;
alter table public.hc_store_inventory enable row level security;
alter table public.hc_store_casts enable row level security;
alter table public.hc_store_flags enable row level security;
revoke all on public.hc_store_wallets, public.hc_store_inventory, public.hc_store_casts, public.hc_store_flags
from public, anon, authenticated;

create or replace function public.hc_store_state()
returns jsonb language sql stable security definer set search_path = '' as $$
    select jsonb_build_object(
        'balance', coalesce((select balance from public.hc_store_wallets where user_id = auth.uid()), 0),
        'owned', coalesce(jsonb_agg(item order by item), '[]'::jsonb),
        'equipped', coalesce(jsonb_agg(item order by item) filter (where equipped), '[]'::jsonb),
        'beta_available', (select beta_on_sale from public.hc_store_flags where singleton),
        'creator', exists(select 1 from public.hc_store_creator where user_id = auth.uid())
    )
    from public.hc_store_inventory where user_id = auth.uid();
$$;

create or replace function public.hc_store_buy(p_item text)
returns void language plpgsql security definer set search_path = '' as $$
declare
    owner_id uuid := auth.uid();
    price integer := case p_item when 'sword' then 700 when 'gold' then 10000 when 'hat' then 200
        when 'theme' then 70 when 'beta' then 20 end;
    beta_live boolean;
begin
    if owner_id is null or price is null then raise exception 'Item indisponível.'; end if;
    insert into public.hc_store_wallets(user_id) values (owner_id) on conflict do nothing;
    perform 1 from public.hc_store_wallets where user_id = owner_id for update;
    if exists(select 1 from public.hc_store_inventory where user_id = owner_id and item = p_item) then
        return;
    end if;
    if p_item = 'beta' then
        select beta_on_sale into beta_live from public.hc_store_flags where singleton for share;
        if beta_live is distinct from true then raise exception 'O período de venda do emblema terminou.'; end if;
    end if;
    if not exists(select 1 from public.hc_store_creator where user_id = owner_id) then
        update public.hc_store_wallets set balance = balance - price where user_id = owner_id and balance >= price;
        if not found then raise exception 'Rúpias insuficientes.'; end if;
    end if;
    insert into public.hc_store_inventory(user_id, item) values (owner_id, p_item);
end;
$$;

create or replace function public.hc_store_equip(p_item text, p_equipped boolean)
returns void language plpgsql security definer set search_path = '' as $$
begin
    update public.hc_store_inventory set equipped = p_equipped
    where user_id = auth.uid() and item = p_item and item <> 'theme';
    if not found then raise exception 'Compre o cosmético antes de equipá-lo.'; end if;
end;
$$;

create or replace function public.hc_store_public_cosmetics(p_profile uuid)
returns jsonb language sql stable security definer set search_path = '' as $$
    select coalesce(jsonb_agg(items.item order by items.item), '[]'::jsonb)
    from public.hc_community_profiles p
    cross join lateral (
        select i.item from public.hc_store_inventory i where i.user_id = p.user_id and i.equipped
        union all
        select 'creator' from public.hc_store_creator c where c.user_id = p.user_id
    ) items
    where p.user_id = p_profile and auth.uid() is not null
        and (p.discoverable or p.user_id = auth.uid());
$$;

create or replace function public.hc_store_cast(p_cast uuid)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    owner_id uuid := auth.uid();
    t timestamptz := clock_timestamp();
    recent timestamptz;
    attempt public.hc_store_casts%rowtype;
    roll double precision := random();
    fish text[];
    fish_min integer;
    fish_max integer;
begin
    if owner_id is null or p_cast is null then raise exception 'Sessão inválida.'; end if;
    insert into public.hc_store_wallets(user_id) values (owner_id) on conflict do nothing;
    select last_cast_at into recent from public.hc_store_wallets where user_id = owner_id for update;
    select * into attempt from public.hc_store_casts where user_id = owner_id and id = p_cast;
    if not found then
        if recent > t - interval '2 seconds' then raise exception 'Espere um instante para lançar de novo.'; end if;
        delete from public.hc_store_casts where user_id = owner_id and created_at < t - interval '1 day';
        -- A nova isca substitui outros lançamentos abertos, preservando recibos de capturas já creditadas.
        update public.hc_store_casts set ready_at = '-infinity' where user_id = owner_id and not claimed and ready_at > '-infinity';
        if roll < .70 then
            attempt.rarity := 'COMMON'; fish_min := 10; fish_max := 25;
            fish := array['Hyrule Bass','Staminoka Bass','Chillfin Trout','Mighty Porgy','Armored Porgy'];
        elsif roll < .98 then
            attempt.rarity := 'UNCOMMON'; fish_min := 45; fish_max := 70;
            fish := array['Sizzlefin Trout','Voltfin Trout','Mighty Carp','Armored Carp','Glowing Cave Fish'];
        else
            attempt.rarity := 'RARE'; fish_min := 60; fish_max := 90;
            fish := array['Hearty Bass','Hearty Salmon','Stealthfin Trout','Sanke Carp','Ancient Arowana'];
        end if;
        attempt.species := fish[1 + floor(random()*array_length(fish, 1))::integer];
        attempt.wait_ms := 2400 + floor(random()*1801)::integer;
        attempt.reward := fish_min + floor(random()*(fish_max-fish_min+1))::integer;
        insert into public.hc_store_casts(user_id,id,created_at,ready_at,rarity,species,wait_ms,reward)
            values(owner_id,p_cast,t,t + (attempt.wait_ms + 5000)*interval '1 millisecond',
                attempt.rarity,attempt.species,attempt.wait_ms,attempt.reward);
        update public.hc_store_wallets set last_cast_at = t where user_id = owner_id;
    end if;
    return jsonb_build_object('rarity',attempt.rarity,'species',attempt.species,'wait_ms',attempt.wait_ms);
end;
$$;

create or replace function public.hc_store_claim(p_cast uuid)
returns integer language plpgsql security definer set search_path = '' as $$
declare
    owner_id uuid := auth.uid();
    attempt public.hc_store_casts%rowtype;
begin
    perform 1 from public.hc_store_wallets where user_id = owner_id for update;
    select * into attempt from public.hc_store_casts where user_id = owner_id and id = p_cast;
    if not found then raise exception 'Lance a isca novamente.'; end if;
    if attempt.claimed then return attempt.reward; end if;
    if attempt.ready_at = '-infinity' or clock_timestamp() < attempt.ready_at
        or clock_timestamp() > attempt.created_at + interval '3 minutes' then
        raise exception 'Esse lançamento não está mais disponível. Lance a isca novamente.';
    end if;
    update public.hc_store_wallets set balance = balance + attempt.reward where user_id = owner_id;
    update public.hc_store_casts set claimed = true where user_id = owner_id and id = p_cast;
    return attempt.reward;
end;
$$;

create or replace function public.hc_store_redeem(p_keys text[])
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    owner_id uuid := auth.uid();
    unlocked boolean;
begin
    if owner_id is null or p_keys is distinct from array['UP','UP','DOWN','DOWN','LEFT','RIGHT','LEFT','RIGHT','B','A'] then
        raise exception 'Sequência inválida.';
    end if;
    insert into public.hc_store_wallets(user_id) values(owner_id) on conflict do nothing;
    perform 1 from public.hc_store_wallets where user_id = owner_id for update;
    insert into public.hc_store_inventory(user_id, item) values(owner_id, 'chester') on conflict do nothing;
    unlocked := found;
    if unlocked then
        update public.hc_store_wallets set balance = balance + 10000 where user_id = owner_id;
    end if;
    return jsonb_build_object('unlocked', unlocked, 'wallet', public.hc_store_state());
end;
$$;

revoke execute on function public.hc_store_state(), public.hc_store_buy(text),
    public.hc_store_equip(text,boolean), public.hc_store_public_cosmetics(uuid),
    public.hc_store_cast(uuid), public.hc_store_claim(uuid), public.hc_store_redeem(text[]) from public, anon, authenticated;
grant execute on function public.hc_store_state(), public.hc_store_buy(text),
    public.hc_store_equip(text,boolean), public.hc_store_public_cosmetics(uuid),
    public.hc_store_cast(uuid), public.hc_store_claim(uuid), public.hc_store_redeem(text[]) to authenticated;

commit;

begin;

alter table public.hc_store_inventory drop constraint if exists hc_store_inventory_item_check;
alter table public.hc_store_inventory add constraint hc_store_inventory_item_check
    check (item in ('sword','gold','hat','theme','beta','chester','flame','shadow','fish_badge','ra_trophy'));

create table if not exists public.hc_fish_catalog (
    species text primary key
);
insert into public.hc_fish_catalog values
    ('Hyrule Bass'),('Staminoka Bass'),('Chillfin Trout'),('Mighty Porgy'),('Armored Porgy'),
    ('Sizzlefin Trout'),('Voltfin Trout'),('Mighty Carp'),('Armored Carp'),('Glowing Cave Fish'),
    ('Hearty Bass'),('Hearty Salmon'),('Stealthfin Trout'),('Sanke Carp'),('Ancient Arowana'),
    ('Hylian Loach'),('Ordon Catfish'),('Reekfish') on conflict do nothing;
create table if not exists public.hc_aquarium (
    user_id uuid not null references public.hc_store_wallets(user_id) on delete cascade,
    species text not null references public.hc_fish_catalog(species),
    primary key (user_id,species)
);
alter table public.hc_fish_catalog enable row level security;
alter table public.hc_aquarium enable row level security;
revoke all on public.hc_fish_catalog, public.hc_aquarium from public,anon,authenticated;
insert into public.hc_aquarium(user_id,species)
    select distinct c.user_id,c.species from public.hc_store_casts c
    join public.hc_fish_catalog f on f.species=c.species where c.claimed on conflict do nothing;


create or replace function public.hc_store_state()
returns jsonb language sql stable security definer set search_path = '' as $$
    select jsonb_build_object(
        'balance', coalesce((select balance from public.hc_store_wallets where user_id = auth.uid()), 0),
        'owned', coalesce(jsonb_agg(item order by item), '[]'::jsonb),
        'equipped', coalesce(jsonb_agg(item order by item) filter (where equipped), '[]'::jsonb),
        'beta_available', (select beta_on_sale from public.hc_store_flags where singleton),
        'fish', coalesce((select jsonb_agg(species order by species) from public.hc_aquarium where user_id = auth.uid()), '[]'::jsonb),
        'creator', exists(select 1 from public.hc_store_creator where user_id = auth.uid())
    )
    from public.hc_store_inventory where user_id = auth.uid();
$$;

create or replace function public.hc_store_buy(p_item text)
returns void language plpgsql security definer set search_path = '' as $$
declare
    owner_id uuid := auth.uid();
    price integer := case p_item when 'sword' then 700 when 'gold' then 10000 when 'hat' then 200
        when 'flame' then 500 when 'shadow' then 350 when 'theme' then 70 when 'beta' then 20 end;
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

create or replace function public.hc_store_cast_v119(p_cast uuid)
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
        if roll < .695 then
            attempt.rarity := 'COMMON'; fish_min := 10; fish_max := 25;
            fish := array['Hyrule Bass','Staminoka Bass','Chillfin Trout','Mighty Porgy','Armored Porgy'];
        elsif roll < .975 then
            attempt.rarity := 'UNCOMMON'; fish_min := 45; fish_max := 70;
            fish := array['Sizzlefin Trout','Voltfin Trout','Mighty Carp','Armored Carp','Glowing Cave Fish'];
        elsif roll < .995 then
            attempt.rarity := 'RARE'; fish_min := 60; fish_max := 90;
            fish := array['Hearty Bass','Hearty Salmon','Stealthfin Trout','Sanke Carp','Ancient Arowana'];
        else
            attempt.rarity := 'LEGENDARY'; fish_min := 250; fish_max := 500;
            fish := array['Hylian Loach','Ordon Catfish','Reekfish'];
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
    insert into public.hc_aquarium(user_id,species)
        select owner_id,species from public.hc_fish_catalog where species=attempt.species
        on conflict do nothing;
    if not exists(select 1 from public.hc_fish_catalog f where not exists (
        select 1 from public.hc_aquarium a where a.user_id=owner_id and a.species=f.species)) then
        insert into public.hc_store_inventory(user_id,item) values(owner_id,'fish_badge') on conflict do nothing;
    end if;
    return attempt.reward;
end;
$$;

revoke execute on function public.hc_store_cast_v119(uuid) from public,anon,authenticated;
grant execute on function public.hc_store_cast_v119(uuid) to authenticated;

commit;

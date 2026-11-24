begin;

-- An account can wear one username effect.
update public.hc_store_inventory as shadow set equipped = false
where item = 'shadow' and equipped and exists (
    select 1 from public.hc_store_inventory as flame
    where flame.user_id = shadow.user_id and flame.item = 'flame' and flame.equipped
);

create unique index if not exists hc_one_name_effect
    on public.hc_store_inventory(user_id) where equipped and item in ('flame', 'shadow');

create or replace function public.hc_store_equip(p_item text, p_equipped boolean)
returns void language plpgsql security definer set search_path = '' as $$
declare owner_id uuid := auth.uid();
begin
    perform 1 from public.hc_store_wallets where user_id = owner_id for update;
    if p_equipped and p_item in ('flame', 'shadow') then
        update public.hc_store_inventory set equipped = false
        where user_id = owner_id and item in ('flame', 'shadow') and item <> p_item and equipped;
    end if;
    update public.hc_store_inventory set equipped = p_equipped
    where user_id = owner_id and item = p_item and item <> 'theme';
    if not found then raise exception 'Compre o cosmético antes de equipá-lo.'; end if;
end;
$$;

commit;

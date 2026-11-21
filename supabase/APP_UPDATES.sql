-- Run ONCE in the SQL Editor of the EXISTING Hyrule Companion Supabase project.
-- Adds only release metadata. Does not touch hc_user_data, users or existing RLS.
create table if not exists public.hc_app_releases (
    version_code integer primary key check (version_code > 0),
    version_name text not null check (length(version_name) between 1 and 64),
    notes text not null default '',
    download_url text not null check (download_url ~ '^https://[^[:space:]]+$'),
    mandatory boolean not null default false,
    active boolean not null default true,
    published_at timestamptz not null default now()
);

alter table public.hc_app_releases enable row level security;

-- Only allow anonymous / authenticated users to read published versions.
revoke all on public.hc_app_releases from anon, authenticated;
grant select on public.hc_app_releases to anon, authenticated;
drop policy if exists "Read public app releases" on public.hc_app_releases;
create policy "Read public app releases"
    on public.hc_app_releases for select
    to anon, authenticated
    using (active = true);

-- No client-side INSERT/UPDATE/DELETE policy is created. Only project admins
-- (SQL Editor) or a trusted backend/CI with a Supabase secret key may publish.

-- EXAMPLE ONLY: replace with your REAL, HTTPS-hosted APK link and a higher version_code.
-- Do NOT execute this example before your APK is publicly available:
-- insert into public.hc_app_releases
--     (version_code, version_name, notes, download_url, mandatory, active)
-- values (11608, '1.16.8-native', 'Novos recursos e melhorias.',
--         'https://github.com/OWNER/REPO/releases/download/v1.16.8-native/HyruleCompanion-v1.16.8-native.apk',
--         false, true)
-- on conflict (version_code) do update set
--     version_name = excluded.version_name,
--     notes = excluded.notes,
--     download_url = excluded.download_url,
--     mandatory = excluded.mandatory,
--     active = excluded.active;

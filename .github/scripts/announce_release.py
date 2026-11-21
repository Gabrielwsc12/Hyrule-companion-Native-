"""Publish public release metadata to the existing Supabase project from CI.

HC_SUPABASE_SECRET_KEY is a GitHub Actions secret. Do NOT commit it or use in Android.
"""
import json
import os
import urllib.error
import urllib.request
from urllib.parse import urlparse


def main():
    env = os.environ
    key = env["HC_SUPABASE_SECRET_KEY"]
    base = env["HC_SUPABASE_URL"].rstrip("/")
    url = env["HC_APK_URL"]
    code = int(env["HC_VERSION_CODE"])
    name = env["HC_VERSION_NAME"]
    notes = env.get("HC_RELEASE_NOTES", "Atualização com melhorias e correções.")
    if not key.startswith(("sb_secret_", "eyJ")):
        raise ValueError("Configure HC_SUPABASE_SECRET_KEY as the Supabase secret key")
    if urlparse(base).scheme != "https" or urlparse(url).scheme != "https":
        raise ValueError("All URLs must use HTTPS")
    if code <= 0 or not name or urlparse(url).hostname is None:
        raise ValueError("Invalid release metadata")
    data = {
        "version_code": code,
        "version_name": name,
        "download_url": url,
        "notes": notes,
        "mandatory": False,
        "active": True,
    }
    request = urllib.request.Request(
        base + "/rest/v1/hc_app_releases?on_conflict=version_code",
        data=json.dumps(data).encode("utf-8"),
        method="POST",
        headers={
            "apikey": key,  # New sb_secret_ keys must NOT be used as Bearer tokens.
            "Content-Type": "application/json",
            "Prefer": "resolution=merge-duplicates,return=minimal",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=25) as response:
            if response.status not in (200, 201, 204):
                raise RuntimeError("Unexpected Supabase status: " + str(response.status))
    except urllib.error.HTTPError as exc:
        # Never print headers or API keys.
        raise RuntimeError(f"Supabase publication failed (HTTP {exc.code}). Is APP_UPDATES.sql installed?") from None
    print(f"Release metadata published: {name} ({code})")


if __name__ == "__main__":
    main()

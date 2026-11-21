"""Offline test: release announcement must use only the secret apikey header."""
import importlib.util
import json
import os
from pathlib import Path
from unittest.mock import patch

script = Path(__file__).resolve().parents[1] / '.github/scripts/announce_release.py'
spec = importlib.util.spec_from_file_location('announce_release', script)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

class FakeResponse:
    status = 201
    def __enter__(self): return self
    def __exit__(self, *args): return False

captured = []
def urlopen(req, timeout):
    captured.append(req)
    return FakeResponse()

env = {
    'HC_SUPABASE_SECRET_KEY': 'sb_secret_mock_1234',
    'HC_SUPABASE_URL': 'https://example.supabase.co',
    'HC_APK_URL': 'https://github.com/owner/repo/releases/download/v1.16.8/app.apk',
    'HC_VERSION_CODE': '11608',
    'HC_VERSION_NAME': '1.16.8-native',
    'HC_RELEASE_NOTES': 'Conteúdo novo!',
}
with patch.dict(os.environ, env), patch.object(mod.urllib.request, 'urlopen', urlopen):
    mod.main()
assert len(captured) == 1
req = captured[0]
assert req.get_method() == 'POST'
assert req.get_header('Apikey') == 'sb_secret_mock_1234'
assert req.get_header('Authorization') is None
body = json.loads(req.data)
assert body['version_code'] == 11608 and body['version_name'] == '1.16.8-native'
assert body['download_url'].startswith('https://')
print('OK — release publication payload and secret handling validated offline')

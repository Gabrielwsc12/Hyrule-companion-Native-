"""Dependency-free structural validation; not an Android compilation test."""
from pathlib import Path
from xml.etree import ElementTree as ET
import re

root = Path(__file__).resolve().parents[1]
app = root / 'app'
gradle = (app / 'build.gradle.kts').read_text()
manifest = ET.parse(app / 'src/main/AndroidManifest.xml').getroot()
code = int(re.search(r'versionCode\s*=\s*(\d+)', gradle).group(1))
name = re.search(r'versionName\s*=\s*"([^"]+)"', gradle).group(1)
assert re.fullmatch(r'\d+\.\d+\.\d+-native', name) and code >= 11607
assert 'androidx.work:work-runtime-ktx' in gradle
assert 'androidx.lifecycle:lifecycle-runtime-compose' in gradle
assert any(p.attrib.get('{http://schemas.android.com/apk/res/android}name') == 'android.permission.POST_NOTIFICATIONS'
           for p in manifest.findall('uses-permission'))
package = app / 'src/main/java/com/hyrulecompanion/app'
for f in ('updates/AppUpdateRepository.kt','updates/AppUpdateScheduler.kt','ui/App.kt','ui/ProfileSettings.kt','HyruleViewModel.kt'):
    assert (package / f).is_file(), f
assert 'hc_app_releases' in (root / 'supabase/APP_UPDATES.sql').read_text()
assert 'using (active = true)' in (root / 'supabase/APP_UPDATES.sql').read_text()
assert 'HC_SUPABASE_SECRET_KEY' not in ''.join(p.read_text() for p in package.rglob('*.kt'))
assert 'Release Hyrule Companion APK' in (root / '.github/workflows/release-apk.yml').read_text()
print(f'OK — update sources/config/manifest/sql/CI present; version {name} ({code})')

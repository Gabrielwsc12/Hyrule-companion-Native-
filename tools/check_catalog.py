"""Static compatibility checks that require only Python 3, not Android SDK."""
from pathlib import Path
import json,re
root=Path(__file__).resolve().parents[1]
data=json.loads((root/'app/src/main/assets/hyrule_content.json').read_text())
assert len(data['games'])==62, 'Original catalog must retain 62 games'
assert len(data['guideData'])==58, 'Original guide list must retain 58 guides'
assert len(data['atlasMaps'])==12, 'Original atlas must retain 12 maps'
assert len(data['dungeonData'])==5, 'Original dungeons must retain 5 entries'
assert len(data['timelineRoutes'])==5, 'Original timeline must retain all branches'
assert all(len(x['locations'])>0 for x in data['atlasMaps'])
assert set(data['totkLayerLocations'])=={'surface','sky','depths'}
ids={x['id'] for x in data['games']}
assert len(ids)==len(data['games'])
assert all(g['game'] in ids for g in data['guideData'])
assert all(d['game'] in ids for d in data['dungeonData'])
assert all(m['gameId'] in ids for m in data['atlasMaps'])
assert all(e[1] in ids for r in data['timelineRoutes'].values() for e in r['entries'])
assert set(data['dungeonReference'])=={d['id'] for d in data['dungeonData']}
assert set(data['dungeonEnemies'])=={d['id'] for d in data['dungeonData']}
for g in data['guideData']:
    assert isinstance(g['steps'],list)
    assert all(isinstance(s,str) for s in g['steps'])
for m in data['atlasMaps']:
    assert all(0<=point[3]<=100 and 0<=point[4]<=100 for point in m['locations'])
assert (root/'app/src/main/res/raw/hyrule_intro.mp3').exists()
for dir in ['mdpi','hdpi','xhdpi','xxhdpi','xxxhdpi']:
    assert (root/f'app/src/main/res/mipmap-{dir}/ic_launcher.png').exists()
code='\n'.join(p.read_text() for p in (root/'app/src/main/java').rglob('*.kt'))
assert 'android.webkit.WebView' not in code and 'WebView(' not in code
assert 'hc_user_data' in code and 'hc-last-cloud-sync' in code
assert 'schemaVersion' in code and '13' in code
print('OK — 62 games, 58 guides, 12 maps, 5 dungeons, 5 timeline branches; key references valid.')

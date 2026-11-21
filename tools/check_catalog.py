"""Static compatibility checks that require only Python 3, not Android SDK."""
from pathlib import Path
import json,re
root=Path(__file__).resolve().parents[1]
data=json.loads((root/'app/src/main/assets/hyrule_content.json').read_text())
assert len(data['games'])==62, 'Original catalog must retain 62 games'
assert len(data['guideData'])==58, 'Original guide list must retain 58 guides'
assert len(data['atlasMaps']) >= 15
assert len(data['dungeonData']) >= 9
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
assets = root / 'app/src/main/assets'
previews = json.loads((assets / 'atlas/maps.json').read_text())
layer_keys = {f'totk-{name}' for name in data['totkLayerLocations']}
for m in data['atlasMaps']:
    layers = m.get('layers', [{'key': m['id'], 'locations': m['locations']}])
    for layer in layers:
        points = layer['locations']
        assert points and all(0 <= p[3] <= 100 and 0 <= p[4] <= 100 for p in points)
        if m['id'] != 'totk':
            assert layer['key'] not in layer_keys
            layer_keys.add(layer['key'])
        if 'layers' in m:
            pins = previews[layer['key']]['points']
            assert len(points) == len(pins)
            for point, pin in zip(points, pins):
                assert point[0] == pin[2]
                assert abs(point[3] / 100 - pin[0]) < 1e-6 and abs(point[4] / 100 - pin[1]) < 1e-6
assert set(previews) == layer_keys
for key, preview in previews.items():
    assert (assets / f'atlas/{key}.webp').exists()
    assert preview['aspect'] > 0
    assert all(0 <= point[0] <= 1 and 0 <= point[1] <= 1 for point in preview['points'])
for dungeon in data['dungeonData']:
    dungeon_id = dungeon['id']
    assert len(dungeon['rooms']) == len(data['dungeonReference'][dungeon_id]['rooms'])
    assert len(dungeon['rooms']) == len(data['dungeonEnemies'][dungeon_id])
assert (root/'app/src/main/res/raw/hyrule_intro.mp3').exists()
for dir in ['mdpi','hdpi','xhdpi','xxhdpi','xxxhdpi']:
    assert (root/f'app/src/main/res/mipmap-{dir}/ic_launcher.png').exists()
code='\n'.join(p.read_text() for p in (root/'app/src/main/java').rglob('*.kt'))
assert 'android.webkit.WebView' not in code and 'WebView(' not in code
assert 'hc_user_data' in code and 'hc-last-cloud-sync' in code
assert 'schemaVersion' in code and '13' in code
print(f"OK — 62 games, 58 guides, {len(data['atlasMaps'])} mapped games, {len(layer_keys)} map views, {len(data['dungeonData'])} dungeons; key references valid.")

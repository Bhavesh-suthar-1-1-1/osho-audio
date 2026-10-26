import json, re, os
from collections import defaultdict
from urllib.parse import quote

d = json.load(open('/home/hatch/workspace/osho-app/item_metadata.json'))
ID = 'osho-audio-discourses-collection'
series = {}  # (lang, folder) -> {title, tracks: [{title, url, size}]}

def clean_series(folder):
    # strip leading number "100-" and trailing " - Osho World"
    s = re.sub(r'^\d+-', '', folder)
    s = re.sub(r'\s*[-–]\s*Osho World\s*$', '', s)
    s = re.sub(r'\s+', ' ', s).strip()
    return s

def clean_track(fname):
    s = os.path.splitext(os.path.basename(fname))[0]
    s = re.sub(r'[_]+', ' ', s)
    s = re.sub(r'\s+', ' ', s).strip()
    return s

for f in d.get('files', []):
    name = f['name']
    if not name.lower().endswith('.mp3'):
        continue
    parts = name.split('/')
    if len(parts) < 4 or parts[0] != 'OSHO_WORLD':
        continue
    lang, folder = parts[1], parts[2]
    key = (lang, folder)
    if key not in series:
        series[key] = {'language': lang, 'folder': folder, 'title': clean_series(folder), 'tracks': []}
    series[key]['tracks'].append({
        'title': clean_track(name),
        'url': f'https://archive.org/download/{ID}/' + quote(name, safe='/'),
        'size': int(f.get('size', 0)),
    })

# sort tracks naturally by title
def natkey(t):
    return [int(c) if c.isdigit() else c.lower() for c in re.split(r'(\d+)', t['title'])]
for s in series.values():
    s['tracks'].sort(key=natkey)
    s['count'] = len(s['tracks'])

# person keyword map (high-confidence only)
PERSONS = [
    ('Ashtavakra', ['maha geeta', 'ashtavakra']),
    ('Kabir', ['kabir', 'kabira', 'sadho']),
    ('Mahavira', ['mahaveer', 'mahavir', 'jin ']),
    ('Krishna', ['krishna', 'geeta']),
    ('Nanak', ['nanak', 'omkar']),
    ('Shiva', ['shiv ']),
    ('Buddha', ['dhammo', 'buddha']),
    ('Lao Tzu', ['tao ']),
    ('Rahim', ['rahiman', 'rahim']),
    ('Shankaracharya', ['bhaj govindam', 'bhaj']),
]
def person_of(title):
    t = ' ' + title.lower() + ' '
    for person, kws in PERSONS:
        if any(k in t for k in kws):
            return person
    return None

catalog = {'languages': {}, 'persons': defaultdict(list), 'series': []}
for (lang, folder), s in sorted(series.items()):
    s2 = {'id': f"{lang[:2].lower()}-{re.sub(r'[^a-z0-9]+', '-', s['title'].lower()).strip('-')}",
          'language': lang, 'title': s['title'], 'count': s['count'],
          'person': person_of(s['title']), 'tracks': s['tracks']}
    catalog['series'].append(s2)
    catalog['languages'].setdefault(lang, {'series': 0, 'tracks': 0})
    catalog['languages'][lang]['series'] += 1
    catalog['languages'][lang]['tracks'] += s['count']
    if s2['person']:
        catalog['persons'][s2['person']].append({'title': s['title'], 'count': s['count']})
catalog['persons'] = dict(catalog['persons'])
catalog['series'].sort(key=lambda x: (x['language'], x['title'].lower()))

json.dump(catalog, open('/home/hatch/workspace/osho-app/catalog.json', 'w'), ensure_ascii=False, indent=1)
print('languages:', catalog['languages'])
print('total series:', len(catalog['series']))
print('persons:')
for p, ss in sorted(catalog['persons'].items()):
    print(f"  {p}: {len(ss)} series, {sum(x['count'] for x in ss)} tracks -> {[x['title'] for x in ss][:8]}")
print('Maha Geeta (Ashtavakra):', [(s['title'], s['count']) for s in catalog['series'] if 'maha geeta' in s['title'].lower()])
print('Tao Upanishad:', [(s['title'], s['count']) for s in catalog['series'] if 'tao upanishad' in s['title'].lower()])
print('English series sample:', [(s['title'], s['count']) for s in catalog['series'] if s['language']=='English'][:25])
print('Hindi series count:', sum(1 for s in catalog['series'] if s['language']=='Hindi'))
print('catalog.json bytes:', os.path.getsize('/home/hatch/workspace/osho-app/catalog.json'))

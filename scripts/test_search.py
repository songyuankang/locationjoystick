import urllib.request
import urllib.parse
import json

queries = ['北京天安门广场', '郑州', '上海市']

# Nominatim public mirrors
mirrors = [
    'https://nominatim.openstreetmap.org/search',
    'https://nominatim.kumi.systems/search',
    'https://nominatim.openstreetmap.de/search',
]

for q in queries:
    encoded = urllib.parse.quote(q)
    for base in mirrors:
        url = f"{base}?q={encoded}&format=json&limit=5&accept-language=zh-CN,zh"
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
            with urllib.request.urlopen(req, timeout=4) as resp:
                data = json.loads(resp.read().decode('utf-8'))
                print(f"SUCCESS [{q}] count={len(data)} via {base}")
                for item in data[:2]:
                    print(f"  * {item.get('display_name')} ({item.get('lat')}, {item.get('lon')})")
                break
        except Exception as e:
            print(f"FAILED [{q}] via {base}: {e}")

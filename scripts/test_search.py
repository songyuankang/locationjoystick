import urllib.request
import urllib.parse
import json

key = 'cfc8fa33f036d939984b414aaa1400bd'
queries = ['郑州', '南乐县泰和新城', '北京天安门广场', '上海市']

for q in queries:
    encoded = urllib.parse.quote(q)
    # 1. Amap InputTips API
    url1 = f"https://restapi.amap.com/v3/assistant/inputtips?keywords={encoded}&key={key}"
    try:
        req = urllib.request.Request(url1, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            status = data.get('status')
            info = data.get('info')
            tips = data.get('tips', [])
            print(f"InputTips [{q}] -> Status: {status}, Info: {info}, Count: {len(tips)}")
            if status == '1' and tips:
                for t in tips[:2]:
                    print(f"  * {t.get('name')} | {t.get('district')} | {t.get('location')}")
    except Exception as e:
        print('InputTips ERROR:', e)

    # 2. Amap Place Text Search API
    url2 = f"https://restapi.amap.com/v3/place/text?keywords={encoded}&key={key}"
    try:
        req = urllib.request.Request(url2, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            status = data.get('status')
            info = data.get('info')
            pois = data.get('pois', [])
            print(f"PlaceText [{q}] -> Status: {status}, Info: {info}, Count: {len(pois)}")
            if status == '1' and pois:
                for p in pois[:2]:
                    print(f"  * {p.get('name')} | {p.get('pname')}{p.get('cityname')}{p.get('adname')} | {p.get('location')}")
    except Exception as e:
        print('PlaceText ERROR:', e)

import json

filepath = 'app/src/main/assets/mall_graph.json'
with open(filepath, 'r', encoding='utf-8') as f:
    data = json.load(f)

for shop in data.get('shops', []):
    if shop.get('shopId') == 6 and shop.get('name') == 'Zara':
        shop['name'] = 'Zara Kids'
    if shop.get('name') == 'Breshka':
        shop['name'] = 'Bershka'

with open(filepath, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=2)

print("mall_graph.json updated successfully!")

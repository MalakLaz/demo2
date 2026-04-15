import json
import math
from queue import PriorityQueue

with open('app/src/main/assets/mall_graph.json', 'r') as f:
    data = json.load(f)

nodes = {n['id']: n for n in data['nodes']}
edges = data.get('edges', [])
shops = data.get('shops', [])

adj = {n: [] for n in nodes.keys()}
for e in edges:
    adj[e['from']].append(e['to'])
    adj[e['to']].append(e['from'])

def heuristic(a, b):
    na, nb = nodes[a], nodes[b]
    return math.hypot(na['x'] - nb['x'], na['y'] - nb['y'])

def find_path(start, end):
    open_set = PriorityQueue()
    open_set.put((0, start))
    came_from = {}
    g_score = {n: float('inf') for n in nodes}
    g_score[start] = 0
    
    visited = set()

    while not open_set.empty():
        _, current = open_set.get()
        if current == end:
            path = []
            while current in came_from:
                path.append(current)
                current = came_from[current]
            path.append(current)
            return path[::-1]
        
        if current in visited: continue
        visited.add(current)
        
        for neighbor in adj[current]:
            if neighbor in visited: continue
            tentative_g = g_score[current] + heuristic(current, neighbor)
            if tentative_g < g_score[neighbor]:
                came_from[neighbor] = current
                g_score[neighbor] = tentative_g
                f_score = tentative_g + heuristic(neighbor, end)
                open_set.put((f_score, neighbor))
    return None

oxxo_point = 119
zara_point = 110
path = find_path(oxxo_point, zara_point)
print("Path from OXXO (119) to ZARA (110):", path)

if path:
    for p in path:
        shop = next((s['name'] for s in shops if s['pointId'] == p), "Hallway Node")
        print(f"Node {p} at ({nodes[p]['x']}, {nodes[p]['y']}) -> {shop}")

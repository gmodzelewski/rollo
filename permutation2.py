from itertools import combinations
from collections import defaultdict

def find_combo_chains_unique():
    digits = range(1, 10)
    combos = [c for c in combinations(digits, 3) if sum(c) == 14]
    
    graph = defaultdict(list)
    for a in combos:
        for b in combos:
            if a != b and 1 <= len(set(a) & set(b)) <= 2:
                graph[a].append(b)
    
    def dfs(current, path, used):
        if len(path) == 4:
            if len(used) <= 9:  # All numbers unique across chain
                return [path[:]]
            return []
        
        chains = []
        for nxt in graph[current]:
            new_used = used | set(nxt)
            if len(new_used) <= 9:  # Prune early
                path.append(nxt)
                chains.extend(dfs(nxt, path, new_used))
                path.pop()
        return chains
    
    all_chains = []
    for start in combos:
        chains = dfs(start, [start], set(start))
        all_chains.extend(chains)
    
    return combos, all_chains

combos, chains = find_combo_chains_unique()
print(f"Valid combos ({len(combos)}): {combos}")
print(f"\nChains of 4 with unique numbers: {len(chains)}")
if chains:
    for chain in chains[:5]:
        print("  " + " -> ".join(str(list(c)) for c in chain))
else:
    print("No valid chains found. Try shorter length (e.g., 3).") [code_file:1]

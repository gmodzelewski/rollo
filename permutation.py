from itertools import combinations  # [web:4][web:14][web:16][web:17][web:27]

def combos_sum_14():
    digits = range(1, 10)
    valid_combos = []

    # combinations of length 3, digits are unique and order doesn't matter
    for c in combinations(digits, 3):
        if sum(c) == 14:
            valid_combos.append(c)

    return valid_combos

if __name__ == "__main__":
    results = combos_sum_14()
    print(f"Found {len(results)} combinations:")
    for combo in results:
        print(combo)

#!/usr/bin/env python3
"""
Resolve missing zip codes from zipcode_kosong.csv using Wikipedia + Nominatim (free).

Usage:
    python3 resolve_zipcode.py                # all rows
    python3 resolve_zipcode.py --limit 5      # first 5 unresolved rows

Output:
    zipcode_hasil.csv  — same format + zipcode_hasil column
    progress.json      — resume file (delete to restart)

Rate limits respected:
    Wikipedia:  300ms between requests
    Nominatim: 1100ms between requests
"""

import csv
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
INPUT_FILE = os.path.join(SCRIPT_DIR, "zipcode_kosong.csv")
OUTPUT_FILE = os.path.join(SCRIPT_DIR, "zipcode_hasil.csv")
PROGRESS_FILE = os.path.join(SCRIPT_DIR, "progress.json")

USER_AGENT = "ManajemenDistrik/1.0 (educational project)"
HEADERS = {
    "User-Agent": USER_AGENT,
    "Accept": "application/json",
}

WIKI_API_URL = "https://id.wikipedia.org/w/api.php"
NOMINATIM_SEARCH_URL = "https://nominatim.openstreetmap.org/search"
NOMINATIM_DETAILS_URL = "https://nominatim.openstreetmap.org/details"


def http_get_json(url, params):
    qs = urllib.parse.urlencode(params)
    full_url = f"{url}?{qs}"
    req = urllib.request.Request(full_url, headers=HEADERS)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"    [HTTP ERROR] {e}", file=sys.stderr)
        return None


def levenshtein_similarity(a, b):
    a, b = a.lower().strip(), b.lower().strip()
    if a == b:
        return 100
    la, lb = len(a), len(b)
    if la == 0 or lb == 0:
        return 0
    dp = list(range(lb + 1))
    for i in range(1, la + 1):
        prev = dp[0]
        dp[0] = i
        for j in range(1, lb + 1):
            temp = dp[j]
            if a[i - 1] == b[j - 1]:
                dp[j] = prev
            else:
                dp[j] = 1 + min(prev, dp[j], dp[j - 1])
            prev = temp
    return int((1 - dp[lb] / max(la, lb)) * 100)


def clean_name(name):
    """Strip common prefixes like Desa, Kelurahan, etc."""
    return re.sub(r"^(Desa|Kelurahan|Kel\.?|Ds\.?)\s+", "", name, flags=re.IGNORECASE).strip()


def strip_wiki_markup(text):
    text = re.sub(r"<ref[^>]*>.*?</ref>", "", text, flags=re.DOTALL)
    text = re.sub(r"<ref[^/]*/>", "", text)
    text = re.sub(r"<[^>]+>", "", text)
    text = re.sub(r"\[\[([^|\]]*\|)?([^\]]*)\]\]", r"\2", text)
    text = re.sub(r"\{\{[^}]*\}\}", "", text)
    return text.strip()


def extract_infobox_field(wikitext, field_name):
    """Extract field from wiki infobox. field_name can be regex pattern."""
    # Match value on the SAME LINE as the field (not greedy across lines)
    pattern = rf"\|\s*{field_name}\s*=\s*([^\n]*)"
    m = re.search(pattern, wikitext, re.IGNORECASE)
    if m:
        val = strip_wiki_markup(m.group(1).strip())
        if val:
            return val
    return None


def search_wikipedia(name, district_name="", state_name=""):
    """Search Wikipedia for the sub-district, return zip code if found in infobox."""
    bare_name = clean_name(name)

    # Try multiple search queries for better coverage
    all_results = []
    for query in [f"{bare_name} {district_name}", bare_name, f"{name} {district_name}"]:
        data = http_get_json(WIKI_API_URL, {
            "action": "query",
            "list": "search",
            "srsearch": query,
            "srlimit": "5",
            "format": "json",
        })
        if data and "query" in data:
            for r in data["query"].get("search", []):
                if not any(x["pageid"] == r["pageid"] for x in all_results):
                    all_results.append(r)
        time.sleep(0.3)

    results = all_results
    if not results:
        return None

    # Pick best match — compare title against both original and bare name
    def title_sim(r):
        t = r["title"]
        # Extract first part before comma (e.g. "Pematang Kapau, Kulim, Pekanbaru" -> "Pematang Kapau")
        t_base = t.split(",")[0].strip()
        return max(
            levenshtein_similarity(t, name),
            levenshtein_similarity(t, bare_name),
            levenshtein_similarity(t_base, name),
            levenshtein_similarity(t_base, bare_name),
        )

    best = max(results, key=title_sim)
    title = best["title"]
    sim = title_sim(best)

    if sim < 40:
        return None

    # Fetch wikitext
    parse_data = http_get_json(WIKI_API_URL, {
        "action": "parse",
        "page": title,
        "prop": "wikitext",
        "format": "json",
    })
    if not parse_data or "parse" not in parse_data:
        return None

    wikitext = parse_data["parse"].get("wikitext", {}).get("*", "")

    # Try multiple kode_pos field name variants (with space or underscore)
    zipcode = None
    for field in [r"kode[_ ]pos", r"kodepos", r"postal[_ ]code", r"postcode"]:
        zipcode = extract_infobox_field(wikitext, field)
        if zipcode:
            break

    if zipcode:
        zipcode = re.sub(r"[^\d]", "", zipcode)
        if len(zipcode) == 5:
            return {"zip": zipcode, "source": "Wikipedia", "similarity": sim}

    # Wikipedia found the page but no zip — try Nominatim for just the postcode
    # using the Wikipedia-confirmed name for better Nominatim matching
    wiki_name = extract_infobox_field(wikitext, "nama") or bare_name
    wiki_kecamatan = extract_infobox_field(wikitext, "kecamatan") or district_name

    time.sleep(1.1)
    nom_zip = nominatim_postcode_only(wiki_name, wiki_kecamatan, state_name)
    if nom_zip:
        return {"zip": nom_zip, "source": "Wikipedia+Nominatim", "similarity": sim}

    return None


def nominatim_postcode_only(name, district_name="", state_name=""):
    """Query Nominatim just for postcode, return 5-digit zip string or None."""
    bare = clean_name(name)
    parts = [bare]
    if district_name:
        parts.append(clean_name(district_name))
    if state_name:
        parts.append(state_name)
    parts.append("Indonesia")
    query = ", ".join(parts)

    data = http_get_json(NOMINATIM_SEARCH_URL, {
        "q": query,
        "format": "json",
        "addressdetails": "1",
        "limit": "3",
        "countrycodes": "id",
    })
    if not data or not isinstance(data, list) or len(data) == 0:
        # Retry with just bare name + Indonesia
        data = http_get_json(NOMINATIM_SEARCH_URL, {
            "q": f"{bare}, Indonesia",
            "format": "json",
            "addressdetails": "1",
            "limit": "3",
            "countrycodes": "id",
        })
        if not data or not isinstance(data, list) or len(data) == 0:
            return None

    best = max(data, key=lambda r: levenshtein_similarity(r.get("display_name", ""), name))
    place_id = best.get("place_id")
    if not place_id:
        return None

    # Check address postcode first (no extra API call needed)
    addr = best.get("address", {})
    postcode = addr.get("postcode", "")
    postcode = re.sub(r"[^\d]", "", postcode)
    if len(postcode) == 5:
        return postcode

    time.sleep(1.1)

    # Fetch details for calculated_postcode
    details = http_get_json(NOMINATIM_DETAILS_URL, {
        "place_id": str(place_id),
        "format": "json",
    })
    if not details:
        return None

    postcode = details.get("calculated_postcode") or ""
    postcode = re.sub(r"[^\d]", "", postcode)
    if len(postcode) == 5:
        return postcode

    return None


def search_nominatim(name, district_name="", state_name="", province_name=""):
    """Search Nominatim for the sub-district, return zip code from details API."""
    bare = clean_name(name)

    # Try multiple query strategies
    queries = [
        ", ".join(filter(None, [bare, clean_name(district_name), state_name, "Indonesia"])),
        ", ".join(filter(None, [bare, state_name, "Indonesia"])),
        f"{bare}, Indonesia",
    ]

    for query in queries:
        data = http_get_json(NOMINATIM_SEARCH_URL, {
            "q": query,
            "format": "json",
            "addressdetails": "1",
            "limit": "3",
            "countrycodes": "id",
        })
        if data and isinstance(data, list) and len(data) > 0:
            break
        time.sleep(1.1)
    else:
        return None

    best = max(data, key=lambda r: levenshtein_similarity(
        r.get("display_name", "").split(",")[0], bare))
    place_id = best.get("place_id")
    if not place_id:
        return None

    display = best.get("display_name", "")
    sim = levenshtein_similarity(display.split(",")[0], bare)

    # Check address postcode first
    addr = best.get("address", {})
    postcode = addr.get("postcode", "")
    postcode = re.sub(r"[^\d]", "", postcode)
    if len(postcode) == 5:
        return {"zip": postcode, "source": "Nominatim", "similarity": sim}

    time.sleep(1.1)

    details = http_get_json(NOMINATIM_DETAILS_URL, {
        "place_id": str(place_id),
        "format": "json",
    })
    if not details:
        return None

    postcode = details.get("calculated_postcode") or ""
    postcode = re.sub(r"[^\d]", "", postcode)
    if len(postcode) == 5:
        return {"zip": postcode, "source": "Nominatim", "similarity": sim}

    return None


def resolve_zipcode(row):
    """Try Wikipedia first, then Nominatim fallback."""
    name = row["SubDistrictName"].strip().strip('"')
    district = row["DistrictName"].strip().strip('"')
    state = row["StateName"].strip().strip('"')
    province = row["ProvinceName"].strip().strip('"')

    # 1. Wikipedia (+ Nominatim postcode fallback inside)
    result = search_wikipedia(name, district, state)
    if result:
        return result

    time.sleep(0.3)

    # 2. Pure Nominatim
    result = search_nominatim(name, district, state, province)
    if result:
        return result

    return {"zip": "", "source": "NOT_FOUND", "similarity": 0}


def load_progress():
    if os.path.exists(PROGRESS_FILE):
        with open(PROGRESS_FILE, "r") as f:
            return json.load(f)
    return {}


def save_progress(progress):
    with open(PROGRESS_FILE, "w") as f:
        json.dump(progress, f)


def main():
    limit = None
    if len(sys.argv) > 1 and sys.argv[1] == "--limit":
        limit = int(sys.argv[2])

    # Load input
    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        rows = list(reader)

    total = len(rows)
    print(f"Total rows: {total}")
    if limit:
        print(f"Limit: {limit} rows")

    # Load progress
    progress = load_progress()
    resolved = len(progress)
    print(f"Already resolved: {resolved}/{total}")

    # Resolve missing zip codes
    processed = 0
    for i, row in enumerate(rows):
        sub_id = row["SubDistrictID"].strip().strip('"')
        if sub_id in progress:
            continue
        if limit and processed >= limit:
            break
        processed += 1

        name = row["SubDistrictName"].strip().strip('"')
        district = row["DistrictName"].strip().strip('"')
        pct = (i + 1) / total * 100
        print(f"[{i+1}/{total} {pct:.1f}%] {sub_id} — {name}, {district} ... ", end="", flush=True)

        result = resolve_zipcode(row)
        progress[sub_id] = result
        print(f"-> {result['zip'] or '—'} ({result['source']})")

        # Save progress every 10 rows
        if len(progress) % 10 == 0:
            save_progress(progress)

    save_progress(progress)

    # Write output CSV
    out_fields = list(fieldnames) + ["zipcode_hasil", "zipcode_source", "zipcode_similarity"]
    with open(OUTPUT_FILE, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=out_fields, quoting=csv.QUOTE_ALL)
        writer.writeheader()
        found = 0
        for row in rows:
            sub_id = row["SubDistrictID"].strip().strip('"')
            result = progress.get(sub_id, {"zip": "", "source": "NOT_FOUND", "similarity": 0})
            row["zipcode_hasil"] = result["zip"]
            row["zipcode_source"] = result["source"]
            row["zipcode_similarity"] = result["similarity"]
            if result["zip"]:
                found += 1
            writer.writerow(row)

    not_found = total - found
    print(f"\nDone! Output: {OUTPUT_FILE}")
    print(f"  Found:     {found}/{total} ({found/total*100:.1f}%)")
    print(f"  Not found: {not_found}/{total} ({not_found/total*100:.1f}%)")


if __name__ == "__main__":
    main()

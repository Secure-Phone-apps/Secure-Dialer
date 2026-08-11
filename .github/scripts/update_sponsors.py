#!/usr/bin/env python3
import os
import re
import sys
import json
import urllib.request

GRAPHQL_URL = "https://api.github.com/graphql"
WALL_OF_HONOR_PATH = "wiki/Wall-of-Honor.md"

def fetch_sponsors(token, owner_repo):
    parts = owner_repo.split('/')
    owner = parts[0] if len(parts) > 0 else "Secure-Phone-apps"

    query = """
    query($login: String!) {
      user(login: $login) {
        sponsorshipsAsMaintainer(first: 100, activeOnly: true) {
          nodes {
            sponsorEntity {
              ... on User {
                login
                name
                avatarUrl
                url
              }
              ... on Organization {
                login
                name
                avatarUrl
                url
              }
            }
            tier {
              monthlyPriceInDollars
              name
            }
          }
        }
      }
      organization(login: $login) {
        sponsorshipsAsMaintainer(first: 100, activeOnly: true) {
          nodes {
            sponsorEntity {
              ... on User {
                login
                name
                avatarUrl
                url
              }
              ... on Organization {
                login
                name
                avatarUrl
                url
              }
            }
            tier {
              monthlyPriceInDollars
              name
            }
          }
        }
      }
    }
    """

    req = urllib.request.Request(
        GRAPHQL_URL,
        data=json.dumps({"query": query, "variables": {"login": owner}}).encode('utf-8'),
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "User-Agent": "Secure-Dialer-Sponsors-Sync"
        }
    )

    try:
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            if "errors" in data and not ("data" in data and data["data"]):
                print(f"GraphQL Errors: {data['errors']}")
                return []
            
            res_data = data.get("data", {})
            user_spons = res_data.get("user", {}) or {}
            org_spons = res_data.get("organization", {}) or {}

            nodes = (
                user_spons.get("sponsorshipsAsMaintainer", {}).get("nodes", []) or
                org_spons.get("sponsorshipsAsMaintainer", {}).get("nodes", []) or
                []
            )
            return nodes
    except Exception as e:
        print(f"Failed to query GitHub GraphQL API: {e}")
        return []

def format_tier_content(sponsors, min_dollars, max_dollars, tier_name, fallback_msg):
    tier_sponsors = []
    for item in sponsors:
        tier = item.get("tier") or {}
        price = tier.get("monthlyPriceInDollars", 0)
        
        if max_dollars is None:
            if price >= min_dollars:
                tier_sponsors.append(item)
        else:
            if min_dollars <= price <= max_dollars:
                tier_sponsors.append(item)

    if not tier_sponsors:
        return f"\n{fallback_msg}\n"

    output = ["\n<p align=\"left\">"]
    for item in tier_sponsors:
        entity = item.get("sponsorEntity", {}) or {}
        login = entity.get("login", "anonymous")
        name = entity.get("name") or login
        avatar = entity.get("avatarUrl", "https://github.com/ghost.png")
        url = entity.get("url", f"https://github.com/{login}")

        if min_dollars >= 200:
            # Visionary Leader
            card = f'  <a href="{url}" target="_blank" title="{name} (@{login}) - Visionary Leader">\n' \
                   f'    <img src="{avatar}" width="80" height="80" style="border-radius: 50%; margin: 8px;" alt="{name}" />\n' \
                   f'  </a>'
        elif min_dollars >= 50:
            # Community Champion
            card = f'  <a href="{url}" target="_blank" title="{name} (@{login}) - Community Champion">\n' \
                   f'    <img src="{avatar}" width="64" height="64" style="border-radius: 50%; margin: 6px;" alt="{name}" />\n' \
                   f'  </a>'
        elif min_dollars >= 10:
            # Proud Sponsor
            card = f'  <a href="{url}" target="_blank" title="{name} (@{login}) - Proud Sponsor">\n' \
                   f'    <img src="{avatar}" width="50" height="50" style="border-radius: 50%; margin: 5px;" alt="{name}" />\n' \
                   f'  </a>'
        else:
            # Well Wisher
            card = f'  <a href="{url}" target="_blank" title="{name} (@{login}) - Well Wisher">\n' \
                   f'    <img src="{avatar}" width="40" height="40" style="border-radius: 50%; margin: 4px;" alt="{name}" />\n' \
                   f'  </a>'
        output.append(card)
    output.append("</p>\n")
    return "\n".join(output)

def update_wall_of_honor():
    token = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN")
    repo = os.environ.get("GITHUB_REPOSITORY", "Secure-Phone-apps/Secure-Dialer")

    sponsors = []
    if token:
        sponsors = fetch_sponsors(token, repo)
    else:
        print("No GH_TOKEN found. Skipping API fetch and preserving fallback blocks.")

    if not os.path.exists(WALL_OF_HONOR_PATH):
        print(f"Error: {WALL_OF_HONOR_PATH} not found.")
        sys.exit(1)

    with open(WALL_OF_HONOR_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    tiers = [
        ("SPONSORS_VISIONARY", 200, None, "Visionary Leader", "*Be the first Visionary Leader to support Secure Dialer! [Become a Sponsor](https://github.com/sponsors/Secure-Phone-apps)*"),
        ("SPONSORS_CHAMPION", 50, 199, "Community Champion", "*Be the first Community Champion to support Secure Dialer! [Become a Sponsor](https://github.com/sponsors/Secure-Phone-apps)*"),
        ("SPONSORS_PROUD", 10, 49, "Proud Sponsor", "*Be the first Proud Sponsor to support Secure Dialer! [Become a Sponsor](https://github.com/sponsors/Secure-Phone-apps)*"),
        ("SPONSORS_WELL_WISHER", 2, 9, "Well Wisher", "*Be the first Well Wisher to support Secure Dialer! [Become a Sponsor](https://github.com/sponsors/Secure-Phone-apps)*")
    ]

    for marker, min_val, max_val, name, fallback in tiers:
        formatted = format_tier_content(sponsors, min_val, max_val, name, fallback)
        pattern = re.compile(rf"<!-- {marker}:START -->.*?<!-- {marker}:END -->", re.DOTALL)
        replacement = f"<!-- {marker}:START -->{formatted}<!-- {marker}:END -->"
        content = pattern.sub(replacement, content)

    with open(WALL_OF_HONOR_PATH, "w", encoding="utf-8") as f:
        f.write(content)

    print("Successfully updated Wall-of-Honor.md with latest sponsors data!")

if __name__ == "__main__":
    update_wall_of_honor()

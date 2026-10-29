#!/usr/bin/env python3
"""Assign Keycloak realm roles to test users via Admin REST API."""
import json
import urllib.request
import urllib.parse

KEYCLOAK_URL = "http://localhost:8484"
REALM = "disaster-relief-realm"
ADMIN_USER = "admin"
ADMIN_PASS = "admin"

# --- Get admin token ---
token_data = urllib.parse.urlencode({
    "client_id": "admin-cli",
    "username": ADMIN_USER,
    "password": ADMIN_PASS,
    "grant_type": "password",
}).encode()

req = urllib.request.Request(
    f"{KEYCLOAK_URL}/realms/master/protocol/openid-connect/token",
    data=token_data,
    method="POST",
)
with urllib.request.urlopen(req) as r:
    TOKEN = json.load(r)["access_token"]

BASE = f"{KEYCLOAK_URL}/admin/realms/{REALM}"
HEADERS = {"Authorization": f"Bearer {TOKEN}", "Content-Type": "application/json"}


def api(method, path, payload=None):
    url = BASE + path
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(url, data=data, headers=HEADERS, method=method)
    try:
        with urllib.request.urlopen(req) as r:
            body = r.read()
            return r.status, json.loads(body) if body else {}
    except urllib.error.HTTPError as e:
        return e.code, {}


# --- Role map: name -> (id, name) ---
status, roles = api("GET", "/roles")
role_map = {r["name"]: r for r in roles}
print("Available roles:", list(role_map.keys()))

# --- User assignments: username -> list of role names ---
USER_ROLES = {
    "admin":       ["ADMIN", "COORDINATOR"],
    "bhavesh10":   ["ADMIN", "COORDINATOR"],
    "coordinator": ["ADMIN", "COORDINATOR"],
    "donor":       ["DONOR"],
    "driver":      ["DRIVER"],
    "receiver":    ["RECEIVER"],
}

# --- Get users ---
status, users = api("GET", "/users?max=50")
user_map = {u["username"]: u["id"] for u in users}
print("Users found:", list(user_map.keys()))

# --- Assign roles ---
for username, role_names in USER_ROLES.items():
    uid = user_map.get(username)
    if not uid:
        print(f"  SKIP: user '{username}' not found")
        continue

    payload = [
        {"id": role_map[rn]["id"], "name": rn}
        for rn in role_names
        if rn in role_map
    ]
    if not payload:
        print(f"  SKIP: no matching roles for '{username}'")
        continue

    st, _ = api("POST", f"/users/{uid}/role-mappings/realm", payload)
    print(f"  {username} -> {[r['name'] for r in payload]} | HTTP {st}")

# --- Verify ---
print("\n=== Verification ===")
for username, uid in user_map.items():
    st, assigned = api("GET", f"/users/{uid}/role-mappings/realm")
    names = [r["name"] for r in assigned if not r["name"].startswith("default-")]
    print(f"  {username}: {names}")

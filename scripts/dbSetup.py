import json
import os
import sys
import urllib.error
import urllib.request
import base64

DATABASES = [
    "users",
    "members",
    "events",
    "reports",
    "app_settings",
    "emergency_token",
    "library",
    "changelogs",
    "help_center",
]

APP_SETTINGS_DB = "app_settings"

DEFAULT_SETTINGS = {
    "_id": "general",
    "maxMembers": 10,
    "scoreCategories": {
        "": "all",
        "all": "Alle Kategorien",
        "Alle Kategorien": "all",
    },
    "feedbackCategories": {
       "_functionality": "Funktionalität",
       "_ui": "UI/Design",
       "_general": "Allgemein",
       "_other": "Sonstiges",
       "Funktionalität": "_functionality",
       "UI/Design": "_ui",
       "Allgemein": "_general",
       "Sonstiges": "_other"
    },
    "appVersion": "v1.0",
    "helpCenterCategories": [],
}

COUCHDB_URL = os.environ.get("COUCHDB_URL", "http://127.0.0.1:5984").rstrip("/")
COUCHDB_USERNAME = os.environ["COUCHDB_USERNAME"]
COUCHDB_PASSWORD = os.environ["COUCHDB_PASSWORD"]

def request(method, path, body=None):
    url = f"{COUCHDB_URL}/{path}"

    credentials = f"{COUCHDB_USERNAME}:{COUCHDB_PASSWORD}"
    encoded_credentials = base64.b64encode(
        credentials.encode("utf-8")
    ).decode("ascii")

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Basic {encoded_credentials}",
    }

    data = None if body is None else json.dumps(body).encode("utf-8")

    request = urllib.request.Request(
        url,
        data=data,
        headers=headers,
        method=method,
    )

    try:
        with urllib.request.urlopen(request) as response:
            response_body = response.read()

            if not response_body:
                return response.status, None

            return response.status, json.loads(response_body)

    except urllib.error.HTTPError as e:
        response_body = e.read()

        try:
            body = json.loads(response_body)
        except json.JSONDecodeError:
            body = None

        return e.code, body

    except urllib.error.URLError as e:
        print(f"ERROR: Could not connect to CouchDB: {e}", file=sys.stderr)
        sys.exit(1)

def create_databases():
    print("Creating databases...")

    for database in DATABASES:
        status, _ = request("PUT", database)

        if status in (201, 202):
            print(f"  [OK] Created {database}")

        elif status == 412:
            print(f"  [OK] {database} already exists")

        else:
            print(
                f"  [ERROR] Failed to create {database} "
                f"(HTTP {status})",
                file=sys.stderr,
            )
            sys.exit(1)


def initialize_default_settings():
    print("Initializing default settings...")

    status, existing = request(
        "GET",
        f"{APP_SETTINGS_DB}/general",
    )

    if status == 200:
        document_id = existing["_id"]
        revision = existing["_rev"]

        delete_status, _ = request(
            "DELETE",
            f"{APP_SETTINGS_DB}/{document_id}?rev={revision}",
        )

        if delete_status not in (200, 202):
            print(
                f"  [ERROR] Failed to delete existing "
                f"default settings document (HTTP {delete_status})",
                file=sys.stderr,
            )
            sys.exit(1)

        print("  [OK] Removed existing default settings")

    elif status != 404:
        print(
            f"  [ERROR] Failed to check default settings "
            f"document (HTTP {status})",
            file=sys.stderr,
        )
        sys.exit(1)

    status, response = request(
        "POST",
        APP_SETTINGS_DB,
        DEFAULT_SETTINGS,
    )

    if status not in (201, 202):
        print(
            f"  [ERROR] Failed to insert default settings "
            f"document (HTTP {status})",
            file=sys.stderr,
        )
        sys.exit(1)

    print("  [OK] Inserted default settings")


def main():
    print("GVW Office database initialization")
    print(f"CouchDB: {COUCHDB_URL}")
    print()

    create_databases()
    print()
    initialize_default_settings()

    print()
    print("Database initialization completed successfully.")


if __name__ == "__main__":
    main()

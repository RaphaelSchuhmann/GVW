import base64
import json
import os
import sys
import urllib.error
import urllib.request
import bcrypt
import uuid


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
    "feedbacks",
    "bug_reports",
]

APP_SETTINGS_DB = "app_settings"
USERS_DB = "users"

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
        "Sonstiges": "_other",
    },
    "appVersion": "v1.0",
    "helpCenterCategories": [],
}

COUCHDB_URL = os.environ.get("COUCHDB_URL", "http://127.0.0.1:5984").rstrip("/")

COUCHDB_USERNAME = os.environ["COUCHDB_USERNAME"]
COUCHDB_PASSWORD = os.environ["COUCHDB_PASSWORD"]

ADMIN_EMAIL = os.environ.get("GVW_ADMIN_EMAIL")
ADMIN_NAME = os.environ.get("GVW_ADMIN_NAME")
ADMIN_PASSWORD = os.environ.get("GVW_ADMIN_PASSWORD")
ADMIN_PHONE = os.environ.get("GVW_ADMIN_PHONE", "")
ADMIN_ADDRESS = os.environ.get("GVW_ADMIN_ADDRESS", "")

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

    req = urllib.request.Request(
        url,
        data=data,
        headers=headers,
        method=method,
    )

    try:
        with urllib.request.urlopen(req) as response:
            response_body = response.read()

            if not response_body:
                return response.status, None

            return response.status, json.loads(response_body)

    except urllib.error.HTTPError as e:
        response_body = e.read()

        try:
            response_body = json.loads(response_body)
        except json.JSONDecodeError:
            response_body = None

        return e.code, response_body

    except urllib.error.URLError as e:
        print(
            f"ERROR: Could not connect to CouchDB: {e}",
            file=sys.stderr,
        )
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

def create_admin_user():
    print("Creating default admin user...")

    if not ADMIN_PASSWORD:
        print(
            "  [ERROR] GVW_ADMIN_PASSWORD environment variable "
            "is not set.",
            file=sys.stderr,
        )
        sys.exit(1)

    # Check whether an admin user already exists.
    status, result = request(
        "POST",
        f"{USERS_DB}/_find",
        {
            "selector": {
                "role": "ADMIN",
            },
            "limit": 1,
        },
    )

    if status != 200:
        print(
            f"  [ERROR] Failed to check for existing admin user "
            f"(HTTP {status})",
            file=sys.stderr,
        )
        sys.exit(1)

    if result.get("docs"):
        print("  [OK] Admin user already exists")
        return

    password_hash = bcrypt.hashpw(
        ADMIN_PASSWORD.encode("utf-8"),
        bcrypt.gensalt(),
    ).decode("utf-8")

    user = {
        "_id": str(uuid.uuid4()),
        "email": ADMIN_EMAIL,
        "name": ADMIN_NAME,
        "password": password_hash,
        "phone": ADMIN_PHONE,
        "address": ADMIN_ADDRESS,
        "changePassword": False,
        "firstLogin": True,
        "userId": str(uuid.uuid4()),
        "role": "ADMIN",
        "memberId": "1",
        "failedLoginAttempts": 0,
    }

    status, result = request(
        "POST",
        USERS_DB,
        user,
    )

    if status not in (201, 202):
        print(
            f"  [ERROR] Failed to create admin user "
            f"(HTTP {status})",
            file=sys.stderr,
        )
        sys.exit(1)

    print(f"  [OK] Created admin user: {ADMIN_EMAIL}")
    print("  [OK] firstLogin is set to true")


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

    status, _ = request(
        "POST",
        APP_SETTINGS_DB,
        DEFAULT_SETTINGS,
    )

    if status not in (201, 202):
        print(
            f"  [ERROR] Failed to insert default settings "
            f"(HTTP {status})",
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

    create_admin_user()
    print()

    initialize_default_settings()

    print()
    print("Database initialization completed successfully.")


if __name__ == "__main__":
    main()

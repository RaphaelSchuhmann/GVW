import subprocess
import pandas as pd
import json
import io
import os


def extract_access_direct(db_filename, output_dir):
    # Force the absolute path.
    base_dir = os.path.dirname(os.path.abspath(__file__))
    abs_db_path = os.path.join(base_dir, db_filename)

    if not os.path.exists(abs_db_path):
        print(f"CRITICAL: Python cannot find the file at {abs_db_path}")
        return

    tables = [
        "Abgaenge", "Ansprechpartner", "Ereignisse", "Funktionen",
        "Kathegorieen", "Konfiguration", "Mitgliederdaten", "Noten",
        "Noten_Liedart", "OBE_Online", "OversoA0", "OversoAN",
        "Programm", "Sitzungen", "SitzungenArt", "SitzungTOP",
        "Vereinsgruppen"
    ]

    output_path = os.path.join(base_dir, output_dir)
    os.makedirs(output_path, exist_ok=True)

    for table in tables:
        print(f"Extracting table: {table}...", end=" ")

        try:
            # Get raw bytes from mdb-export.
            result = subprocess.run(
                ['mdb-export', abs_db_path, table],
                capture_output=True,
                check=True
            )

            # mdb-export outputs UTF-8.
            csv_data = result.stdout.decode('utf-8')

            # Keep everything as strings.
            # Empty fields remain empty strings instead of becoming NaN.
            df = pd.read_csv(
                io.StringIO(csv_data),
                dtype=str,
                keep_default_na=False,
                na_filter=False
            )

            records = df.to_dict(orient='records')

            table_json_path = os.path.join(
                output_path,
                f"{table}.json"
            )

            with open(table_json_path, 'w', encoding='utf-8') as f:
                json.dump(
                    records,
                    f,
                    indent=4,
                    ensure_ascii=False
                )

            print(f"Done -> {table_json_path}")

        except subprocess.CalledProcessError as e:
            error = e.stderr.decode('utf-8', errors='replace')
            print(f"FAILED. Error: {error.strip()}")

        except UnicodeDecodeError as e:
            print(f"FAILED. UTF-8 decoding error: {e}")

        except Exception as e:
            print(f"FAILED. {type(e).__name__}: {e}")

    print(f"\nExtraction complete.")
    print(f"JSON files have been written to: {output_path}")


extract_access_direct(
    "Weppersdorf_v09.mdv",
    "output"
)

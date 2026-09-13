import fs from "node:fs/promises";

const typeMap = {
   "Weihnachtslieder": "weihnachtslieder",
   "Weltliche Lieder": "weltliche_lieder",
   "Trauerlieder": "trauerlieder",
   "Kirchenlieder": "kirchenlieder",
   "Glückwunschlieder": "glückwunschlieder",
   "Gemischter Chor": "gemischter_chor",
   "Sonstiges": "sonstiges"
};

const token = "PLACEHOLDER";

const entries = JSON.parse(await fs.readFile("../output/Noten.json", "utf8"));

const headers = new Headers();
headers.append("Authorization", `Bearer ${token}`);

for (const entry of entries) {
    let title = entry.Titel;
    let artist = entry.Komponist;

    if (entry.Alias) {
        title = title + " - " + entry.Alias;
    }

    if (entry.Texter) {
        artist = artist + " - " + entry.Texter;
    }

    const data = {
        scoreId: entry.ID,
        title: title,
        artist: artist || "N/A",
        type: typeMap[entry.Art] ?? typeMap["Sonstiges"], // -> This needs to be mapped to actual types
        voices: ["t1"],
        voiceCount: 1
    }

    const formData = new FormData();

    formData.append("scoreData", new Blob([JSON.stringify(data)], {
        type: "application/json"
    }));

    await fetch("https://office.weppersdorf.de/api/library/new", {
            method: "POST",
            headers: headers,
            body: formData
        });
}
import { eventsFilterState, eventsStore } from "../stores/events.svelte";
import { membersFilterState, membersStore } from "../stores/members.svelte";
import { libraryFilterState, libraryStore } from "../stores/library.svelte";
import { statusMap, typeMap } from "../services/eventsService.svelte";
import { apiGetMembers } from "../api/apiMembers.svelte";
import { apiGetEvents } from "../api/apiEvents.svelte";
import { apiGetScores } from "../api/apiLibrary.svelte";
import { appSettings } from "../stores/appSettings.svelte";
import { filterRoleMap } from "../services/userService.svelte";
import { userManagerFilterState, userManagerStore } from "../stores/userManager.svelte";
import { apiGetUsersAD } from "../api/apiUser.svelte";
import { reportTypeFilterMap } from "../services/reportService.svelte.js";
import { apiDeepSearchReport, apiGetReports } from "../api/apiReports.svelte.js";
import { reportDeepSearchStore, reportsFilterState, reportsStore } from "../stores/report.svelte.js";

/**
 * Filter registry containing both filter and search configurations
 * @type {Object<string, Object>}
 */
export const filterRegistry = {
    members: {
        fetch: apiGetMembers,
        store: membersStore,
        filterState: membersFilterState,
        fuse: {
            keys: ["name", "surname", "email", "address"],
            threshold: 0.3
        },
        config: {
            search: {
                placeholder: "Mitglied suchen...",
                deepSearch: false,
            }
        }
    },
    events: {
        fetch: apiGetEvents,
        store: eventsStore,
        filterState: eventsFilterState,
        get optionMap() { return typeMap },
        tabMap: statusMap,
        config: {
            dropdown: {
                options: ["Alle Typen", "Proben", "Meeting", "Konzerte", "Sonstiges"]
            },
            tab: {
                contents: ["Bevorstehend", "Abgeschlossen"]
            },
            search: {
                deepSearch: false,
            }
        }
    },
    library: {
        fetch: apiGetScores,
        store: libraryStore,
        filterState: libraryFilterState,
        get optionMap() { return appSettings.scoreCategories; },
        fuse: {
            keys: ["title", "artist", "voices"],
            threshold: 0.3
        },
        config: {
            search: {
                placeholder: "Noten durchsuchen...",
                deepSearch: false,
            },
            dropdown: {
                customDefault: "Alle Kategorien"
            },
        }
    },
    userManager: {
        fetch: apiGetUsersAD,
        store: userManagerStore,
        filterState: userManagerFilterState,
        get optionMap(){ return filterRoleMap; },
        fuse: {
            keys: ["name", "email"],
            threshold: 0.3
        },
        config: {
            search: {
                placeholder: "Benutzer durchsuchen...",
                deepSearch: false,
            },
            dropdown: {
                customDefault: "Alle Rollen"
            },
        }
    },
    reports: {
        fetch: apiGetReports,
        store: reportsStore,
        filterState: reportsFilterState,
        get optionMap() { return reportTypeFilterMap; },
        fuse: {
            keys: ["title", "author", "description"],
            threshold: 0.3
        },
        config: {
            search: {
                placeholder: "Berichte durchsuchen...",
                deepSearch: true,
                deepSearchStore: reportDeepSearchStore,
                deepSearchFetch: apiDeepSearchReport,
            },
            dropdown: {
                customDefault: "Alle Typen"
            },
        }
    },
    reportEditor: {
        fetch: apiGetReports,
        store: reportsStore,
        filterState: reportsFilterState,
        fuse: {
            keys: ["title", "author", "description"],
            threshold: 0.3
        },
        config: {
            search: {
                placeholder: "Berichte durchsuchen...",
                deepSearch: false
            }
        }
    }
}
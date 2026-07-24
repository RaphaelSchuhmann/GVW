<script>
    import Dropdown from "./Dropdown.svelte";
    import { filterRegistry } from "../lib/filterRegistry.svelte";
    import { addToast } from "../stores/toasts.svelte";

    let {
        options = [],
        page = "",
        textWrap = true,
        customDefault = "",
        ...restProps
    } = $props();

    const validPages = ["events", "reports", "library", "userManager"];
    const activePage = $derived(validPages.includes(page) ? page : "none");

    const DEFAULT_FILTER_ENTRY = {
        optionMap: {},
        config: { dropdown: { options: null, customDefault: null } },
        filterState: {
            update: () => {}
        }
    };

    let regEntry = filterRegistry[activePage] || DEFAULT_FILTER_ENTRY;

    const usedOptions = $derived(regEntry.config.dropdown.options ?? options);
    const usedDefault = $derived.by(() => {
        if (regEntry.config.dropdown.customDefault) {
            return regEntry.config.dropdown.customDefault;
        }

        if (customDefault) {
            return customDefault;
        }

        return "Alle Typen";
    });

    let isInitialized = $state(false);

    /**
     * Updates the local regEntry from the global registry
     */
    function updateConfig() {
        if (filterRegistry[activePage]) {
            regEntry = filterRegistry[activePage];
        }
    }

    /**
     * Updates the filter state
     */
    function filter(selected) {
        const rawSelected = $state.snapshot(selected);

        const { optionMap, filterState } = regEntry;

        if (!(rawSelected in optionMap)) {
            addToast({
                title: "Unerwarteter Fehler",
                subTitle: `Der Filter "${rawSelected}" konnte nicht zugeordnet werden.`,
                type: "error"
            });
            return;
        }

        const filterFor = optionMap[rawSelected];
        Object.assign(filterState, { dropdown: filterFor });
    }

    $effect(() => {
        if (isInitialized) return;
        if (!(usedDefault in regEntry.optionMap)) return;
        filter(usedDefault);
        isInitialized = true;
    });

    $effect(() => {
        const intervalId = setInterval(updateConfig, 5000);
        return () => clearInterval(intervalId);
    });
</script>

<div class="flex w-full items-center gap-2" {...restProps}>
    <span class="material-symbols-rounded text-gv-dark-text text-icon-dt-2">
        tune
    </span>
    <Dropdown
        options={usedOptions}
        onChange={filter}
        selected={usedDefault}
        {textWrap}
    />
</div>
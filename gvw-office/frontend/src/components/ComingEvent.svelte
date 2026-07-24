<script>
    import { marginMap } from "../lib/dynamicStyles";
    import Chip from "./Chip.svelte";
    import { typeMap } from "../services/eventsService.svelte.js";

    let {
        title = "",
        time = "",
        location = "",
        type = "other",
        margin = "",
        isMobile = false,
        ...restProps
    } = $props();

    const VALID_EVENT_TYPES = ["all", "practice", "meeting", "concert", "other"];

    const displayType = $derived.by(() => {
        const raw = String(type ?? "").trim();

        const canonical = VALID_EVENT_TYPES.includes(raw)
            ? raw
            : (typeMap[raw] ?? "other");

        if (!VALID_EVENT_TYPES.includes(canonical)) {
            return typeMap["other"];
        }

        return typeMap[canonical];
    });
</script>

<div
    class={`flex ${isMobile ? "flex-col items-start" : "items-center"} w-full ${marginMap[margin]}`}
    {...restProps}
>
    <div class="flex flex-col items-start justify-around">
        <p class="text-dt-5 text-gv-dark-text text-nowrap truncate">{title}</p>
        <p class="text-dt-6 text-gv-light-text">
            {#if time.includes(' - ')}
                {time.split(' - ')[0]} - <span class="whitespace-nowrap">{time.split(' - ')[1]}</span>
            {:else}
                {time}
            {/if}
        </p>
        <p class="text-dt-6 text-gv-light-text">{location}</p>
    </div>
    <div class={`${isMobile ? "mt-2" : "ml-auto"}`}>
        <Chip text={displayType} />
    </div>
</div>
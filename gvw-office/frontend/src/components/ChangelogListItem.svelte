<script>
    import { slide } from "svelte/transition";
    import { cubicOut } from "svelte/easing";
    import { user } from "../stores/user.svelte.js";
    import { formatISODateString } from "../services/dateTimeUtils.js";
    import { deleteChangelog, formatChangelog } from "../services/changelogService.svelte.js";
    import Spinner from "./Spinner.svelte";
    import SanitizedHTML from "./SanitizedHTML.svelte";

    let {
        data,
        expanded = $bindable(false),
        onToggle,
        ...restProps
    } = $props();

    let isDeleting = $state(false);

    async function removeChangelog() {
        if (!data.id) return;
        isDeleting = true;

        try {
            await deleteChangelog(data.id);
        } finally {
            isDeleting = false;
        }
    }

    function handleOnToggle(event) {
        const id = event.currentTarget.dataset.id;
        onToggle(id);
    }
</script>

<div class="w-full flex flex-col gap-2 p-4 items-start justify-start rounded-2 shadow-md bg-white">
    {#if isDeleting}
        <div class="flex justify-center items-center w-full">
            <Spinner />
        </div>
    {:else}
        <div class="flex w-full items-center">
            <button
                type="button"
                class="flex flex-1 items-center cursor-pointer text-left gap-2"
                data-id={data.id}
                onclick={handleOnToggle}
            >
                <span class="text-dt-5 text-gv-dark-text font-semibold text-nowrap truncate">{data.title}</span>
                <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text ml-auto">
                    { expanded ? "arrow_drop_up" : "arrow_drop_down" }
                </span>
            </button>

            {#if user.role === "admin"}
                <button
                    aria-label="Changelog entfernen"
                    title="Changelog entfernen"
                    type="button"
                    onclick={async (e) => { e.stopPropagation(); await removeChangelog(); }}
                    class="flex items-center justify-center p-2 cursor-pointer hover:bg-gv-hover-effect rounded-2 hover:text-gv-delete-hover duration-100"
                >
                    <span class="material-symbols-rounded text-icon-dt-5">delete</span>
                </button>
            {/if}
        </div>

        <p class="text-dt-7 text-gv-light-text font-semibold">{formatISODateString(data.timestamp) + " - " + data.version}</p>

        {#if expanded}
            <div transition:slide={{ duration: 200, easing: cubicOut }}
                 class="w-full max-h-[30vh] overflow-y-auto mt-2">
                <div class="text-gv-dark-text text-dt-7 pl-1">
                    <SanitizedHTML htmlContent={formatChangelog(data.content)} />
                </div>
            </div>
        {/if}
    {/if}
</div>
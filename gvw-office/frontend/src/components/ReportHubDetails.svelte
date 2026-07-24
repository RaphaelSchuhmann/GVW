<script>
    import CollapsableViewer from "./CollapsableViewer.svelte";
    import { removeMillisecondsFromTimeStamp } from "../services/dateTimeUtils";

    let {
        userEmail = "",
        timestamp = "",
        appVersion = "",
        route = "",
        os = "",
        browser = "",
        viewport = "",
        ...restProps
    } = $props();
</script>

<CollapsableViewer title="Details" expanded={true}>
    <div class="w-full flex flex-col items-start justify-start gap-2">
        {#if userEmail}
            <div class="flex items-center justify-between w-full">
                <p class="text-dt-6 text-gv-dark-text">User E-Mail</p>
                <p class="text-dt-6 text-gv-dark-text">
                    {userEmail}
                </p>
            </div>
        {/if}

        {#if timestamp}
            <div class="flex items-center justify-between w-full">
                <p class="text-dt-6 text-gv-dark-text">Timestamp</p>
                <p class="text-dt-6 text-gv-dark-text">
                    {removeMillisecondsFromTimeStamp(timestamp)}
                </p>
            </div>
        {/if}

        {#if appVersion}
            <div class="flex items-center justify-between w-full">
                <p class="text-dt-6 text-gv-dark-text">App Version</p>
                <p class="text-dt-6 text-gv-dark-text">
                    {appVersion}
                </p>
            </div>
        {/if}

        {#if route}
            <div class="flex items-center justify-between w-full">
                <p class="text-dt-6 text-gv-dark-text">Page</p>
                <p class="text-dt-6 text-gv-dark-text">
                    {route}
                </p>
            </div>
        {/if}

        {#if os}
            <div class="flex items-center justify-between w-full">
                <p class="text-dt-6 text-gv-dark-text">OS</p>
                <p class="text-dt-6 text-gv-dark-text">
                    {os}
                </p>
            </div>
        {/if}

        {#if browser}
            <div class="flex items-center justify-between w-full">
                <p class="text-dt-6 text-gv-dark-text">Browser</p>
                <p class="text-dt-6 text-gv-dark-text">
                    {browser}
                </p>
            </div>
        {/if}

        {#if viewport}
            <div class="flex items-center justify-between w-full">
                <p class="text-dt-6 text-gv-dark-text">Viewport</p>
                <p class="text-dt-6 text-gv-dark-text">
                    {viewport}
                </p>
            </div>
        {/if}
    </div>
</CollapsableViewer>

<script>
    import {isAppReady} from "../stores/appLoading.svelte.js";
    import WaveLoadingAnimation from "./WaveLoadingAnimation.svelte";

    let {
        loading = false,
        title = "GVW Office",
        subTitle = "Daten werden geladen...",
        children
    } = $props();

    let isLoading = $derived(!isAppReady() || Boolean(loading));
    let showLoader = $state(false);

    $effect(() => {
        if (!isLoading) {
            showLoader = false;
            return;
        }

        const timeout = setTimeout(() => {
            showLoader = true;
        }, 200);

        return () => clearTimeout(timeout);
    })
</script>

{#if showLoader}
    <div class="w-dvw h-dvh flex justify-center items-center bg-white z-9999999 absolute top-0 left-0">
        {#if showLoader}
            <WaveLoadingAnimation title={title} subTitle={subTitle}/>
        {/if}
    </div>
{:else}
    {@render children()}
{/if}
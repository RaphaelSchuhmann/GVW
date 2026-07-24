<script>
    import { isAppReady } from "../stores/appLoading.svelte.js";
    import WaveLoadingAnimation from "./WaveLoadingAnimation.svelte";

    let {
        loading = false,
        title = "GVW Office",
        subTitle = "Daten werden geladen...",
        children
    } = $props();

    let isLoading = $derived(!isAppReady() || Boolean(loading));
</script>

{#if isLoading}
    <div class="w-dvw h-dvh flex justify-center items-center bg-white z-9999999 absolute top-0 left-0">
        <WaveLoadingAnimation title="GVW Office" subTitle="Daten werden geladen..." />
    </div>
{:else}
    {@render children()}
{/if}
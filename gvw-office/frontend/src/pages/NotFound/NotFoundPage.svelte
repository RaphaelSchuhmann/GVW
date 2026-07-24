<script>
    import { viewport } from "../../stores/viewport.svelte";

    import NotFoundDesktop from "./NotFoundDesktop.svelte";
    import NotFoundMobile from "./NotFoundMobile.svelte";
</script>

{#if viewport.width < 800}
    <NotFoundMobile />
{:else}
    <NotFoundDesktop />
{/if}
<script>
    import { viewport } from "../../stores/viewport.svelte";
    import LoginDesktop from "./LoginDesktop.svelte";
    import LoginMobile from "./LoginMobile.svelte";
</script>

{#if viewport.isMobile}
    <LoginMobile/>
{:else}
    <LoginDesktop/>
{/if}
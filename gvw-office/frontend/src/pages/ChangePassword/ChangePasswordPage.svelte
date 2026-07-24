<script>
    import { viewport } from "../../stores/viewport.svelte";
    import ChangePasswordDesktop from "./ChangePasswordDesktop.svelte";
    import ChangePasswordMobile from "./ChangePasswordMobile.svelte";

    let message = $state("");

    $effect(() => {
        const hash = window.location.hash;
        const queryString = hash.split("?")[1];
        if (!queryString) return;

        const params = new URLSearchParams(queryString);
        let firstLogin = params.get("firstLogin") === "true";

        if (firstLogin) {
            message = "Erstmaliger Login - Bitte ändern Sie Ihr Passwort";
        } else {
            message = "Passwort vergessen - Bitte ändern Sie Ihr Passwort";
        }
    });
</script>

{#if viewport.isMobile}
    <ChangePasswordMobile message={message}/>
{:else}
    <ChangePasswordDesktop message={message}/>
{/if}
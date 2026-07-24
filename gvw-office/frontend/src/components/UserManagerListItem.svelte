<script>
    import { roleMapI2D } from "../services/userService.svelte";
    import Chip from "./Chip.svelte";
    import { push } from "svelte-spa-router";

    let {
        id = "",
        name = "",
        role = "",
        email = "",
        isOrphan = false,
        isMobile = false,
        ...restProps
    } = $props();
</script>

{#if isMobile && isOrphan}
    <button class="w-full p-3 pl-4 flex items-center justify-start gap-4 border-b-2 border-gv-border"
            onclick={async () => await push(`/admin/userManagement/details?id=${id}&editing=false`)}>
        <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">verified_off</span>
        <div class="flex flex-col item-start justify-around">
            <p class="text-dt-6 text-gv-dark-text text-left">{name}</p>
            <div class="flex items-center justify-start gap-2">
                <span class="material-symbols-rounded min-[1300px]:text-icon-dt-6 text-icon-dt-7 text-gv-dark-turquoise">
                    mail
                </span>
                <p class="min-[1300px]:text-dt-7 text-dt-8 text-gv-dark-turquoise">{email}</p>
            </div>
        </div>
    </button>
{:else}
    <div class="w-full p-3 pl-4 flex items-center justify-start gap-4 border-b-2 border-gv-border">
        <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">{isOrphan ? "verified_off" : "verified"}</span>
        <div class="flex flex-col item-start justify-around">
            <p class="text-dt-6 text-gv-dark-text">{name}</p>
            <div class="flex items-center justify-start gap-2">
                <span class="material-symbols-rounded min-[1300px]:text-icon-dt-6 text-icon-dt-7 text-gv-dark-turquoise">
                    mail
                </span>
                <p class="min-[1300px]:text-dt-7 text-dt-8 text-gv-dark-turquoise">{email}</p>
            </div>
        </div>

        {#if !isMobile}
            <div class="flex ml-auto items-center gap-2">
                <Chip text={roleMapI2D[role] ?? role ?? "—"}/>
                {#if isOrphan}
                    <button class="flex items-center justify-center p-2 cursor-pointer hover:bg-gv-hover-effect rounded-2"
                            onclick={async () => await push(`/admin/userManagement/details?id=${id}&editing=false`)}>
                        <span class="material-symbols-rounded text-icon-dt-5">manage_accounts</span>
                    </button>
                {/if}
            </div>
        {/if}
    </div>
{/if}
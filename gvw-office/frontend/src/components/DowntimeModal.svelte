<script>
    import { viewport } from "../stores/viewport.svelte.js";
    import { heightMap, widthMap } from "../lib/dynamicStyles.js";
    import { getTodayDeployments } from "../services/deploymentService.svelte.js";
    import ModalHeader from "./ModalHeader.svelte";

    let times = $state([]);
    let visible = $derived(times.length > 0);

    $effect(() => {
        getTodayDeployments().then((deployments) => {
            times = deployments;
        });
    });

    /**
     * Logic: Side effect for body scroll
     * This replaces the manual logic in show/hide and onDestroy.
     */
    $effect(() => {
        if (visible) {
            document.body.style.overflow = "hidden";
        } else {
            document.body.style.overflow = "";
        }

        // Cleanup: Ensures scroll is restored if component is unmounted
        return () => {
            document.body.style.overflow = "";
        };
    });

    export function hide() {
        times = [];
    }
</script>

{#if visible}
    <div
        class="z-999 top-0 left-0 w-dvw h-dvh flex bg-gv-overlay border border-gv-toast-warning
               {viewport.isMobile ? 'fixed items-end' : 'fixed items-center justify-center'}"
    >
        <div
            class="bg-white flex flex-col p-5 overflow-hidden max-h-[90vh]
                   {viewport.isMobile ? 'w-full h-8/9 rounded-t-1' : `${widthMap["1/3"]} ${heightMap["auto"]} rounded-1`}"
        >
            <div class="flex w-full items-center gap-2">
                <span class="material-symbols-rounded text-gv-toast-warning text-icon-dt-4">warning</span>
                <ModalHeader title="Geplante Wartungen" hideSubTitle={true} onclick={hide} />
            </div>

            <div class="w-full flex-1 min-h-0 flex flex-col overflow-y-scroll overflow-x-hidden mt-2 p-0.5 gap-2">
                <p class="font-medium text-gv-dark-text text-dt-4">Geplante Wartungen heute:</p>
                <div class="flex flex-col item-start justify-start gap-1 w-full">
                    {#each times as time, i (i)}
                        <p class="font-medium text-gv-dark-text text-dt-4">{time}</p>
                    {/each}
                </div>
                <p class="text-dt-5 text-gv-dark-text">Innerhalb dieser Zeiten wird GVW-Office nicht erreichbar sein.</p>
            </div>
        </div>
    </div>
{/if}
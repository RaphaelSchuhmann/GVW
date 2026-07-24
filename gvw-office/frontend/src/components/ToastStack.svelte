<script>
    import Toast from "./Toast.svelte";
    import { toasts, removeToast } from "../stores/toasts.svelte";

    let {
        isMobile = false,
        ...restProps
    } = $props();
</script>

{#if !isMobile}
    <div class="absolute z-9999 top-5 right-5 flex flex-col items-center min-[1200px]:w-1/4 min-[1000px]:w-2/6 w-3/6 h-auto max-h-3/4">
        {#each toasts as toast (toast.id)}
            <Toast id={toast.id} type={toast.type} title={toast.title} subTitle={toast.subTitle} onClose={removeToast}
                   marginTop="5" />
        {/each}
    </div>
{:else}
    <div class="absolute z-9999 bottom-0 right-0 flex flex-col-reverse items-center w-full h-auto max-h-1/3 p-2">
        {#each toasts as toast (toast.id)}
            <Toast id={toast.id} type={toast.type} title={toast.title} subTitle={toast.subTitle} onClose={removeToast}
                   marginTop="5" isMobile={isMobile}/>
        {/each}
    </div>
{/if}
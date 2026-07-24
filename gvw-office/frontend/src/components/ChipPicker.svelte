<script>
    import { viewport } from "../stores/viewport.svelte.js";

    let {
        title,
        options,
        selectedOptions = $bindable([]),
        useLock = false,
        lockTooltip = "",
        onChange = () => {
        },
        disabled = false,
        ...restProps
    } = $props();

    /**
     * Toggles the selection of a certain option.
     * @param {Event} event
     */
    function toggleChip(event) {
        const option = event.currentTarget.dataset.option;
        if (!option) return;

        if (selectedOptions.includes(option)) {
            selectedOptions = selectedOptions.filter(item => item !== option);
            onChange?.(`remove:${option}`);
        } else {
            selectedOptions.push(option);
            onChange?.(`add:${option}`);
        }
    }

    let mobileIsTooltipOpen = $state(false);

    function handleReset() {
        const removed = [...selectedOptions];
        selectedOptions = [];
        removed.forEach((option) => onChange?.(`remove:${option}`));
    }

    function toggleMobileTooltip() { mobileIsTooltipOpen = !mobileIsTooltipOpen; }
</script>

<div class="w-full flex flex-col items-start justify-start gap-2">
    <div class="flex items-center justify-start gap-2">
        <p class="text-dt-6 font-medium text-gv-dark-text">{title}</p>
        {#if useLock && selectedOptions.length > 0 && !disabled}
            {#if viewport.isMobile}
                <div class="relative inline-block">
                    {#if mobileIsTooltipOpen}
                        <div
                            class="fixed bottom-[27%] left-0 right-0 -translate-y-1/2 mx-4 rounded-2 p-2 drop-shadow-[0_0_2px_rgba(0,0,0,0.2)] bg-gv-input-bg border border-gv-separator text-gv-dark-text z-[10000]">
                            {lockTooltip}
                        </div>
                    {/if}

                    <button onclick={toggleMobileTooltip}>
                        <span class="material-symbols-rounded text-icon-dt-6 text-gv-toast-error block cursor-help">
                            lock
                        </span>
                    </button>
                </div>
            {:else}
                <div class="group relative inline-block">
                    <div
                        class="absolute top-0 right-0 -translate-y-full translate-x-full rounded-2 p-2 drop-shadow-[0_0_2px_rgba(0,0,0,0.2)] bg-gv-input-bg border border-gv-separator hidden! group-hover:block! text-nowrap text-gv-dark-text z-10000">
                        {lockTooltip}
                    </div>

                    <span class="material-symbols-rounded text-icon-dt-6 text-gv-toast-error block cursor-help">
                        lock
                    </span>
                </div>
            {/if}
        {/if}
    </div>
    <div class="flex items-start justify-start gap-2 flex-wrap w-full">
        {#each options as option, i (i)}
            <button
                class="group {option.length < 4 ? 'pl-4 pr-4 pt-2 pb-2' : 'p-2'} flex items-center justify-center gap-2 rounded-1 border-2 cursor-pointer disabled:cursor-not-allowed
                       {selectedOptions.includes(option)
                            ? 'bg-gv-secondary border-gv-border-bar text-gv-dark-turquoise hover:bg-gv-semi-transparent-red hover:border-gv-toast-error hover:text-gv-delete'
                            : 'border-gv-border hover:bg-gv-hover-effect'
                       }"
                disabled={disabled}
                data-option={option}
                onclick={toggleChip}
            >
                {#if selectedOptions.includes(option)}
                    <span
                        class="material-symbols-rounded {viewport.isMobile ? 'text-icon-dt-7' : 'text-icon-dt-6'} group-hover:hidden!">
                        check
                    </span>

                    <span
                        class="material-symbols-rounded hidden! {viewport.isMobile ? 'text-icon-dt-7' : 'text-icon-dt-6'} group-hover:block!">
                        close
                    </span>
                {/if}

                <span class="text-dt-6 max-[470px]:text-dt-7 text-nowrap truncate">
                    {option}
                </span>
            </button>
        {/each}
    </div>
    {#if selectedOptions.length > 0 && !disabled}
        <button
            class="p-2 bg-gv-semi-transparent-red rounded-2 flex items-center justify-center gap-2 text-gv-delete hover:bg-gv-semi-transparent-red-hover cursor-pointer"
            onclick={handleReset}
        >
            <span class="material-symbols-rounded text-icon-dt-6">
                {useLock ? "lock_open" : "close"}
            </span>
            <span class="text-dt-6 max-[470px]:text-dt-7 text-nowrap truncate">
                Zurücksetzen
            </span>
        </button>
    {/if}
</div>
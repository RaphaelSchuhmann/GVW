<script>
    import ModalHeader from "./ModalHeader.svelte";
    import { widthMap, heightMap } from "../lib/dynamicStyles";

    /**
     * @typedef {Object} Props
     * @property {string} [title=""] - The main title text displayed at the top of the component
     * @property {string} [subTitle=""] - The subtitle or description text displayed below the main title
     * @property {boolean} [hideSubTitle=false] - Toggles the visibility of the subtitle layout element
     * @property {string} [height="auto"] - The height css class configuration (e.g., 'auto', 'full', 'h-96')
     * @property {string} [width="1/3"] - The horizontal scaling utility width configuration (e.g., '1/3', '1/2', 'full')
     * @property {boolean} [isMobile=false] - Flag specifying if the layout should adjust for mobile screen viewports
     * @property {Function} [extraFunction=( )=>{}] - An optional secondary action callback triggered by a specific layout interaction
     * @property {boolean} [extraFunctionOnClose=true] - Dictates whether the extraFunction lifecycle hook should execute on close events
     * @property {Function} [onCancel=( )=>{}] - The primary cancellation or modal dismissal callback function
     * @property {import('svelte').Snippet} children - The Svelte snippet placeholder representing children elements injected into the slot
     */

    /** @type {Props} */
    let {
        title = "",
        subTitle = "",
        hideSubTitle = false,
        height = "auto",
        width = "1/3",
        isMobile = false,
        extraFunction = () => {},
        extraFunctionOnClose = true,
        onCancel = () => {},
        children
    } = $props();

    let visible = $state(false);

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

    /**
     * Methods exposed to the parent via bind:this
     */
    export function showModal() {
        visible = true;
        if (!extraFunctionOnClose) extraFunction();
    }

    export function hideModal() {
        visible = false;
        if (extraFunctionOnClose) extraFunction();
    }

    function closeModal() {
        hideModal();
        onCancel();
    }
</script>

{#if visible}
    <div
        class="z-999 top-0 left-0 w-dvw h-dvh flex bg-gv-overlay
               {isMobile ? 'fixed items-end' : 'fixed items-center justify-center'}"
    >
        <div
            class="bg-white flex flex-col p-5 overflow-hidden max-h-[90vh]
                   {isMobile ? 'w-full h-8/9 rounded-t-1' : `${widthMap[width]} ${heightMap[height]} rounded-1`}"
        >
            <ModalHeader {title} {subTitle} {hideSubTitle} onclick={closeModal} />

            <div class="w-full flex-1 min-h-0 flex flex-col overflow-y-scroll overflow-x-hidden mt-2 p-0.5">
                {#if children}
                    {@render children()}
                {/if}
            </div>
        </div>
    </div>
{/if}
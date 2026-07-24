<script>
    import Button from "../Button.svelte";
    import SearchBarButton from "./SearchBarButton.svelte";
    import { viewport } from "../../stores/viewport.svelte.js";

    let {
        onClickAddCategory = () => {},
        onClickSearchBar = () => {},
        ...restProps
    } = $props();
</script>

<div class="w-dvw flex items-center p-4 justify-start gap-10 bg-gv-bg-bar border-b border-gv-border-bar">
    <div class="flex items-center justify-start gap-2">
        <span class="material-symbols-rounded min-[1000px]:text-icon-dt-2 max-[1000px]:text-icon-dt-3 text-gv-dark-text">menu_book</span>
        <p class="text-gv-dark-text min-[1000px]:text-dt-3 max-[1000px]:text-dt-4">Hilfe-Center</p>
    </div>
    <SearchBarButton isHeader={true} onclick={onClickSearchBar}/>
    {#if viewport.width > 900}
        <div class="flex items-center ml-auto justify-center">
            <Button onclick={onClickAddCategory}>
                <span class="material-symbols-rounded min-[1000px]:text-icon-dt-6 max-[1000px]:text-icon-dt-7">splitscreen_add</span>
                <span class="min-[1000px]:text-dt-5 max-[1000px]:text-dt-6 ml-2">Kategorie hinzufügen</span>
            </Button>
        </div>
    {/if}
</div>
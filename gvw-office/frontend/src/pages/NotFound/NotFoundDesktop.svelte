<script>
    import bgImage from "../../assets/AbstractMusicNotesVector.svg";
    import { push } from "svelte-spa-router";
</script>

<main class="bg-gv-bg-bar w-dvw h-dvh grid grid-cols-1 grid-rows-1 place-items-center overflow-hidden">
    {#if bgImage}
        <img
            src={bgImage}
            alt=""
            class="col-start-1 row-start-1 pointer-events-none blur-sm"
        />
    {/if}

    <div
        class="col-start-1 row-start-1 z-10 max-w-1/4 max-[1500px]:max-w-1/2 text-center flex flex-col items-center gap-6 p-5 pt-10 pb-10 bg-[rgba(243,243,243,0.3)] border border-gv-border rounded-1 backdrop-blur-[154px]">
        <div class="flex flex-col items-center">
            <p class="text-gv-dark-text text-[80px] font-bold leading-none">404</p>
            <p class="text-gv-dark-text text-dt-2">Seite nicht gefunden</p>
        </div>
        <p class="text-gv-dark-text text-dt-3">Die Seite die Sie suchen existiert nicht mehr</p>
        <button
            class="flex items-center justify-center gap-2 rounded-full bg-gv-primary cursor-pointer hover:bg-gv-primary-hover p-2 pl-4 pr-6"
            onclick={async () => await push("/dashboard")}>
            <span class="material-symbols-rounded text-icon-dt-4 text-white">
                arrow_back
            </span>

            <span class="text-dt-4 text-white">
                Zurück zum Dashboard
            </span>
        </button>
    </div>
</main>
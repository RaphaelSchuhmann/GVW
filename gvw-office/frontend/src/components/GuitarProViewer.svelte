<script>
    import * as alphaTab from "@coderline/alphatab";
    import Dropdown from "./Dropdown.svelte";
    import Spinner from "./Spinner.svelte";
    import { triggerFileDownload } from "../services/utils.js";

    let { blob = null, filename } = $props();

    let container = $state();
    let scrollContainer = $state();
    let api = $state(null);

    let isPlaying = $state(false);
    let soundFontLoaded = $state(false);
    let isFileLoaded = $state(false);

    let isReady = $derived(soundFontLoaded && isFileLoaded);

    let tracks = $state([]);
    let selectedTrackIndex = $state(0);

    $effect(() => {
        if (!container || !scrollContainer) return;

        const instance = new alphaTab.AlphaTabApi(container, {
            core: {
                fontDirectory: "/alphatab/font/"
            },
            player: {
                enablePlayer: true,
                enableCursor: true,
                enableAnimatedBeatCursor: true,
                scrollElement: scrollContainer,
                scrollMode: "Continuous",
                soundFont: "/alphatab/soundfont/sonivox.sf2"
            }
        });

        instance.soundFontLoaded.on(() => {
            soundFontLoaded = true;
        });

        instance.playerReady.on(() => {
            soundFontLoaded = true;
        });

        instance.playerStateChanged.on((e) => {
            isPlaying = e.state === alphaTab.synth.PlayerState.Playing;
        });

        instance.scoreLoaded.on((score) => {
            tracks = score.tracks || [];
            if (tracks.length > 0) {
                selectedTrackIndex = 0;
            }
        });

        instance.renderFinished.on(() => {
            isFileLoaded = true;
        });

        api = instance;

        return () => {
            instance.destroy();
            api = null;
        };
    });

    $effect(() => {
        if (api && blob) {
            loadBlob(blob);
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => {
            window.removeEventListener("keydown", handleKeyDown);
        };
    });

    function handleKeyDown(e) {
        if (e.key === " ") {
            togglePlayPause();
        }
    }

    async function loadBlob(fileBlob) {
        isFileLoaded = false;

        try {
            const arrayBuffer = await fileBlob.arrayBuffer();
            const data = new Uint8Array(arrayBuffer);
            api.load(data);

            if (!soundFontLoaded) {
                fetch("/alphatab/soundfont/sonivox.sf2")
                    .then((res) => {
                        if (!res.ok) throw new Error(`HTTP error ${res.status}`);
                        return res.arrayBuffer();
                    })
                    .then((sfData) => {
                        api.loadSoundFont(new Uint8Array(sfData));
                    })
                    .catch((err) => {
                        console.error("[alphaTab] Error fetching SoundFont file:", err);
                    });
            }
        } catch (err) {
            console.error("[alphaTab] Error reading file blob:", err);
        }
    }

    function togglePlayPause() {
        if (api) {
            api.playPause();
        }
    }

    function handleTrackChange(val) {
        const index = tracks.findIndex(t => t.name === val);
        if (index !== -1) {
            selectedTrackIndex = index;
            if (api && tracks[index]) {
                api.renderTracks([tracks[index]]);
            }
        }
    }
</script>

<div class="flex flex-col gap-4 w-full">
    <div class="relative z-50 flex items-center w-full">
        <div class="flex items-center gap-3 w-full">
            <button
                onclick={togglePlayPause}
                disabled={!isReady}
                class="flex rounded-2 justify-center items-center p-2 bg-gv-primary hover:bg-gv-primary-hover disabled:opacity-50 text-white cursor-pointer disabled:cursor-not-allowed"
            >
                <span class="material-symbols-rounded text-icon-dt-4">{isPlaying ? 'pause' : 'play_arrow'}</span>
            </button>

            {#if tracks.length > 0}
                <Dropdown
                    title=""
                    selected={tracks[selectedTrackIndex]?.name ?? ''}
                    options={tracks.map(t => t.name)}
                    onChange={handleTrackChange}
                    fillWidth={false}
                />
            {/if}
        </div>

        {#if filename.includes(".")}
            <button
                class="flex items-center justify-center p-2 cursor-pointer hover:bg-gv-hover-effect rounded-2 ml-auto"
                onclick={() => triggerFileDownload(blob, filename)}
            >
                <span class="material-symbols-rounded text-icon-dt-5">download</span>
            </button>
        {/if}

        {#if !isFileLoaded || !soundFontLoaded}
            <div class="flex items-center w-1/20">
                <Spinner simple={true} width="full" />
            </div>
        {/if}
    </div>

    <div
        bind:this={scrollContainer}
        class="alphatab-wrapper relative z-0 w-full h-150 border border-slate-200 rounded-lg overflow-auto shadow-inner bg-white"
    >
        <div bind:this={container} class="w-full"></div>
    </div>
</div>

<style>
    /* Measure background highlight */
    :global(.alphatab-wrapper .at-cursor-bar) {
        background: rgba(99, 102, 241, 0.12) !important;
    }

    /* Beat cursor line */
    :global(.alphatab-wrapper .at-cursor-beat) {
        background: #6366f1 !important;
        border-left: 2px solid #6366f1 !important;
    }
</style>
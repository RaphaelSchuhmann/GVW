<script>
    import Card from "./Card.svelte";
    import { triggerFileDownload } from "../services/utils.js";
    import Slider from "./Slider.svelte";

    let {
        filename = "",
        blob
    } = $props();

    const downloadable = $state(filename.includes("."));
    let url = null;

    let audio = null;
    let duration = $state(0);
    let currentTime = $state(0);

    let isPlaying = $state(false);

    $effect(() => {
        if (!blob) return;

        url = URL.createObjectURL(blob);
        audio = new Audio(url);

        audio.addEventListener("loadedmetadata", () => {
            duration = audio.duration;
        });

        audio.addEventListener("timeupdate", () => {
            currentTime = audio.currentTime;
        });

        audio.addEventListener("ended", () => {
            isPlaying = false;
            currentTime = 0;
        });

        window.addEventListener("keydown", handleKeyDown);

        return () => {
            audio.pause();
            URL.revokeObjectURL(url);
            window.removeEventListener("keydown", handleKeyDown);
        };
    });

    function handleKeyDown(e) {
        if (e.key === " ") {
            togglePlay();
        } else if (e.key === "ArrowRight") {
            forward();
        } else if (e.key === "ArrowLeft") {
            rewind();
        }
    }

    function setCurrentPlaybackPosition(val) {
        audio.currentTime = val;
    }

    function formatTime(totalSeconds) {
        const minutes = Math.floor(totalSeconds / 60);
        const seconds = Math.floor(totalSeconds % 60);

        return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
    }

    async function togglePlay() {
        if (isPlaying) {
            audio.pause();
            isPlaying = false;
        } else {
            await audio.play();
            isPlaying = true;
        }
    }

    function forward() {
        setCurrentPlaybackPosition(Math.min(audio.currentTime + 10, duration));
    }

    function rewind() {
        setCurrentPlaybackPosition(Math.max(audio.currentTime - 10, 0));
    }
</script>

<Card>
    <div class="flex flex-col items-start justify-start gap-2 w-full">
        <div class="flex items-center justify-start w-full">
            <p class="text-gv-dark-text text-dt-4 max-[1000px]:text-dt-5 font-medium w-full text-nowrap truncate">{filename}</p>
            {#if downloadable}
                <button
                    class="flex items-center justify-center p-2 cursor-pointer hover:bg-gv-hover-effect rounded-2"
                    onclick={() => triggerFileDownload(blob, filename)}
                >
                    <span class="material-symbols-rounded text-icon-dt-5">download</span>
                </button>
            {/if}
        </div>
        <div class="flex items-center justify-start w-full">
            <button class="flex items-center justify-center p-2 cursor-pointer hover:bg-gv-primary-hover rounded-2 bg-gv-primary text-white"
                    tabindex="-1"
                    onclick={(event) => {
                        togglePlay();
                        event.currentTarget.blur();
                    }}>
                <span class="material-symbols-rounded text-icon-dt-4">{isPlaying ? "pause" : "play_arrow"}</span>
            </button>
            <div class="flex items-center gap-2 ml-auto">
                <button
                    class="flex items-center justify-center p-2 cursor-pointer hover:bg-gv-hover-effect rounded-2"
                    onclick={rewind}
                >
                    <span class="material-symbols-rounded text-icon-dt-5">replay_10</span>
                </button>

                <button
                    class="flex items-center justify-center p-2 cursor-pointer hover:bg-gv-hover-effect rounded-2"
                    onclick={forward}
                >
                    <span class="material-symbols-rounded text-icon-dt-5">forward_10</span>
                </button>
            </div>
        </div>
        <div class="flex flex-col items-start w-full gap-2">
            <Slider bind:value={currentTime} max={duration} onChange={setCurrentPlaybackPosition} />
            <div class="flex items-center w-full">
                <p class="text-gv-dark-text font-medium text-dt-6">{formatTime(currentTime)}</p>
                <p class="text-gv-dark-text font-medium text-dt-6 ml-auto">{formatTime(duration)}</p>
            </div>
        </div>
    </div>
</Card>
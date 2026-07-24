<script>
    import ProgressBarVisualizer from "./ProgressBarVisualizer.svelte";

    let {
        voice = "",
        voiceMembers = 0,
        totalMembers = 1,
        isMobile = false,
        ...restProps
    } = $props();
</script>

<div class="flex flex-col items-center justify-around w-full mt-10">
    <div class="flex items-center w-full mb-5">
        <p class={`text-gv-dark-text ${isMobile ? "text-dt-6" : "text-dt-5"} font-semibold`}>{voice}</p>
        <p class={`text-gv-light-text ${isMobile ? "text-dt-6" : "text-dt-5"} ml-auto`}>{`${voiceMembers}. Mitglieder`}</p>
    </div>
    <ProgressBarVisualizer value={voiceMembers} max={totalMembers}/>
</div>
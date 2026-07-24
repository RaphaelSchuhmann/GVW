<script>
    import { marginMap } from "../lib/dynamicStyles";
    import { tick } from "svelte";

    let {
        selected = $bindable(""),
        marginTop = "",
        onChange = () => {
        }
    } = $props();

    let open = $state(false);
    let timepickerRef = null;
    let hoursRef = null;
    let minuteRef = null;

    let isAutoScrolling = $state(false);

    const hours = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, "0"));
    const minutes = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, "0"));

    let activeHour = $state("00");
    let activeMinute = $state("00");

    $effect(() => {
        const handleClickOutside = (event) => {
            if (timepickerRef && !timepickerRef.contains(event.target)) {
                open = false;
            }
        };

        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    });

    function updateSelection() {
        selected = `${activeHour}:${activeMinute}`;
        onChange(selected);
    }

    function handleScroll(e, type) {
        if (isAutoScrolling) return;

        const itemHeight = 48;
        const scrollTop = e.target.scrollTop;
        const index = Math.round(scrollTop / itemHeight);

        if (type === "h" && hours[index] && hours[index] !== activeHour) {
            activeHour = hours[index];
            updateSelection();
        } else if (type === "m" && minutes[index] && minutes[index] !== activeMinute) {
            activeMinute = minutes[index];
            updateSelection();
        }
    }

    function handleWheel(e) {
        e.preventDefault();

        const itemHeight = 48;

        const direction = e.deltaY > 0 ? 1 : -1;

        e.currentTarget.scrollBy({
            top: direction * itemHeight,
            behavior: "smooth"
        });
    }

    function scrollToItem(array, item, ref) {
        if (!array || array.length === 0 || !ref) return;

        const index = array.indexOf(item);
        if (index === -1) return;

        const container = ref === "hours" ? hoursRef : minuteRef;
        if (!container) return;

        isAutoScrolling = true;

        requestAnimationFrame(() => {
            const itemHeight = 48;
            container.scrollTo({
                top: index * itemHeight,
                behavior: "smooth"
            });

            setTimeout(() => {
                isAutoScrolling = false;
            }, 600);
        });
    }

    async function toggleOpen() {
        open = !open;

        if (open) {
            if (selected) {
                const [h, m] = selected.split(":");
                activeHour = h;
                activeMinute = m;
            } else {
                activeHour = String(new Date().getHours()).padStart(2, "0");
                activeMinute = String(new Date().getMinutes()).padStart(2, "0");
                updateSelection();
            }

            await tick();

            scrollToItem(hours, activeHour, "hours");
            scrollToItem(minutes, activeMinute, "minutes");
        }
    }

    function selectMinute(e) {
        const minute = e.currentTarget.dataset.minute;
        activeMinute = minute;
        updateSelection();
        scrollToItem(minutes, minute, "minutes");
    }

    function selectHour(e) {
        const hour = e.currentTarget.dataset.hour;
        activeHour = hour;
        updateSelection();
        scrollToItem(hours, hour, "hours");
    }

    function handleHourScroll(e) { handleScroll(e, 'h'); }

    function handleMinuteScroll(e) { handleScroll(e, "m"); }
</script>

<div class="relative w-full {marginMap[marginTop]}" bind:this={timepickerRef}>
    <div
        class="flex items-center w-full bg-gv-input-bg border-gv-primary rounded-1 {open ? 'border' : ''} gap-1">
        <input type="text" class="w-full p-2 pl-3 pr-3 rounded-l-1 text-gv-dark-text outline-gv-primary text-dt-6"
               placeholder="--:--"
               value={selected}
               readonly>
        <button
            class="p-1.5 rounded-2 h-full aspect-square mr-1 flex items-center justify-center cursor-pointer hover:bg-gv-hover-effect"
            onclick={toggleOpen}>
            <span class="material-symbols-rounded text-icon-dt-6 text-gv-light-text">schedule</span>
        </button>
    </div>

    {#if open}
        <div
            class="absolute bottom-full flex flex-col items-center rounded-1 w-full bg-gv-input-bg border border-gv-primary p-4 gap-4 mb-1">
            <span class="text-gv-dark-text text-dt-6 w-full text-center">Uhrzeit auswählen</span>

            <div class="relative flex items-center justify-center w-full h-40">
                <div
                    class="absolute pointer-events-none w-3/4 h-12 border border-gv-input-placeholder rounded-2 flex items-center justify-center">
                    <span class="text-dt-4 text-gv-input-placeholder">:</span>
                </div>

                <div class="flex w-3/4 h-full">
                    <div class="flex-1 overflow-y-auto no-scrollbar scroll-container py-14"
                         onscroll={handleHourScroll}
                         onwheel={handleWheel}
                         bind:this={hoursRef}>
                        {#each hours as hour, i (i)}
                            <button class="snap-item h-12 w-full shrink-0 flex items-center justify-center text-dt-4
                                          {activeHour === hour ? 'text-gv-dark-text font-bold' : 'text-gv-light-text'}"
                                    onclick={selectHour}>
                                {hour}
                            </button>
                        {/each}
                    </div>

                    <div class="w-4"></div>

                    <div class="flex-1 overflow-y-auto no-scrollbar scroll-container py-14"
                         onscroll={handleMinuteScroll}
                         onwheel={handleWheel}
                         bind:this={minuteRef}>
                        {#each minutes as minute, i (i)}
                            <button class="snap-item h-12 w-full shrink-0 flex items-center justify-center text-dt-4
                                          {activeMinute === minute ? 'text-gv-dark-text font-bold' : 'text-gv-light-text'}"
                                    data-minute={minute}
                                    onclick={selectMinute}>
                                {minute}
                            </button>
                        {/each}
                    </div>
                </div>
            </div>
        </div>
    {/if}
</div>

<style>
    /* Hide scrollbars but keep functionality */
    .no-scrollbar::-webkit-scrollbar {
        display: none;
    }

    .no-scrollbar {
        -ms-overflow-style: none;
        scrollbar-width: none;
    }

    .scroll-container {
        scroll-snap-type: y mandatory;
    }

    .snap-item {
        scroll-snap-align: center;
    }
</style>

<script>
    import { marginMap } from "../lib/dynamicStyles";
    import { tick } from "svelte";
    import { getRoundedTime } from "../services/dateTimeUtils.js";

    let {
        title = "Uhrzeit",
        selected = $bindable(""),
        marginTop = "",
        onChange = () => {}
    } = $props();

    let open = $state(false);
    let timepickerRef = $state(null);
    let hoursRef = $state(null);
    let minuteRef = $state(null);
    let popupStyle = $state("");

    let isAutoScrolling = $state(false);

    const hours = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, "0"));
    const minutes = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, "0"));

    let activeHour = $state("00");
    let activeMinute = $state("00");

    $effect(() => {
        const handleClickOutside = (event) => {
            if (
                timepickerRef &&
                !timepickerRef.contains(event.target) &&
                !event.target.closest('.timepicker-portal-popup')
            ) {
                open = false;
            }
        };

        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    });

    function calculatePortalPosition() {
        if (!timepickerRef) return;
        const rect = timepickerRef.getBoundingClientRect();

        // Exact height offset of the time picker popup (~240px) + margin
        const popupHeight = 240;
        const top = rect.top + window.scrollY - popupHeight - 4;
        const left = rect.left + window.scrollX;
        const width = rect.width;

        popupStyle = `position: absolute; top: ${top}px; left: ${left}px; width: ${width}px; z-index: 99999;`;
    }

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
        if (!array || array.length === 0) return;

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
            calculatePortalPosition();

            if (selected) {
                const [h, m] = selected.split(":");
                activeHour = h;
                activeMinute = m;
            } else {
                const { hours, minutes } = getRoundedTime();
                activeHour = String(hours).padStart(2, "0");
                activeMinute = String(minutes).padStart(2, "0");
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

    function handleHourScroll(e) { handleScroll(e, "h"); }
    function handleMinuteScroll(e) { handleScroll(e, "m"); }

    function portal(node) {
        document.body.appendChild(node);
        return {
            destroy() {
                if (node.parentNode) {
                    node.parentNode.removeChild(node);
                }
            }
        };
    }
</script>

<div class="flex flex-col w-full">
    {#if title}
        <p class="text-dt-6 font-medium mb-2 text-gv-dark-text">{title}</p>
    {/if}
    <div class="relative w-full {marginMap[marginTop]}" bind:this={timepickerRef}>
        <div
            class="flex items-center w-full bg-gv-input-bg border-gv-primary rounded-1 {open ? 'border' : ''} gap-1">
            <input type="text" class="w-full p-2 pl-3 pr-3 rounded-l-1 text-gv-dark-text outline-gv-primary text-dt-6"
                   placeholder="--:--"
                   value={selected}
                   readonly>
            <button
                type="button"
                aria-label={open ? "Close time picker" : "Open time picker"}
                class="p-1.5 rounded-2 h-full aspect-square mr-1 flex items-center justify-center cursor-pointer hover:bg-gv-hover-effect"
                onclick={toggleOpen}>
                <span class="material-symbols-rounded text-icon-dt-6 text-gv-light-text">schedule</span>
            </button>
        </div>

        {#if open}
            <div
                use:portal
                style={popupStyle}
                class="timepicker-portal-popup flex flex-col items-center rounded-1 bg-gv-input-bg border border-gv-primary p-4 gap-4 overflow-hidden">
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
                                <button type="button"
                                        class="snap-item h-12 w-full shrink-0 flex items-center justify-center text-dt-4
                                               {activeHour === hour ? 'text-gv-dark-text font-bold' : 'text-gv-light-text'}"
                                        data-hour={hour}
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
                                <button type="button"
                                        class="snap-item h-12 w-full shrink-0 flex items-center justify-center text-dt-4
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
</div>

<style>
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
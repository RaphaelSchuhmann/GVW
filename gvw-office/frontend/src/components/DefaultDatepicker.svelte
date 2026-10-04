<script>
    import { marginMap } from "../lib/dynamicStyles";
    import {
        daysInMonth,
        firstWeekdayOfMonth,
        currentYear,
        currentMonth,
        isToday,
        germanDateToISO,
        isISOString,
        formatISODateString
    } from "../services/dateTimeUtils.js";
    import Dropdown from "./Dropdown.svelte";

    let {
        selected = $bindable(""),
        marginTop = "",
        onChange = () => {},
        disabled = $bindable(false),
        title = "",
    } = $props();

    let open = $state(false);
    let datepickerRef = $state(null);
    let popupStyle = $state("");

    let usedMonth = $state(currentMonth);
    let usedYear = $state(currentYear);
    const todayDay = new Date().getDate();
    let selectedDay = $state(todayDay);

    const monthOptions = [
        "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember"
    ];

    function generateYears(center, range = 101) {
        return Array.from({ length: range * 2 + 1 }, (_, i) => String(center - range + i));
    }
    const yearOptions = generateYears(currentYear, 101);

    function parseSelected() {
        if (!selected) return null;

        let parts;
        if (isISOString(selected)) {
            parts = formatISODateString(selected).split('.').map(Number);
        } else if (selected.includes('.')) {
            parts = selected.split('.').map(Number);
        }

        return parts ? { day: parts[0], month: parts[1] - 1, year: parts[2] } : null;
    }

    $effect(() => {
        const current = parseSelected();
        if (current) {
            usedMonth = current.month;
            usedYear = current.year;
            selectedDay = current.day;
        }
    });

    let calendar = $derived(buildCalendar(usedYear, usedMonth));

    $effect(() => {
        const handleClickOutside = (event) => {
            if (datepickerRef && !datepickerRef.contains(event.target) && !event.target.closest('.datepicker-portal-popup')) {
                open = false;
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    });

    $effect(() => {
        const maxDays = daysInMonth(usedYear, usedMonth);
        if (selectedDay > maxDays) {
            selectedDay = maxDays;
        }
    });

    function buildCalendar(year, month) {
        const days = daysInMonth(year, month);
        const startDay = firstWeekdayOfMonth(year, month);
        const calendarGrid = [];
        let week = [];

        for (let i = 0; i < startDay; i++) week.push(null);
        for (let day = 1; day <= days; day++) {
            week.push(day);
            if (week.length === 7) {
                calendarGrid.push(week);
                week = [];
            }
        }
        if (week.length > 0) {
            while (week.length < 7) week.push(null);
            calendarGrid.push(week);
        }
        return calendarGrid;
    }

    function calculatePortalPosition() {
        if (!datepickerRef) return;
        const rect = datepickerRef.getBoundingClientRect();

        // Match popup height offset dynamically
        const popupHeight = 350;
        const top = rect.top + window.scrollY - popupHeight - 4;
        const left = rect.left + window.scrollX;
        const width = rect.width;

        popupStyle = `position: absolute; top: ${top}px; left: ${left}px; width: ${width}px; z-index: 99999;`;
    }

    function toggleDatepicker() {
        if (disabled) return;
        open = !open;
        if (open) {
            calculatePortalPosition();
            const current = parseSelected();
            if (current) {
                usedMonth = current.month;
                usedYear = current.year;
                selectedDay = current.day;
            }
        }
    }

    function itemClicked(event) {
        const day = Number(event.currentTarget.dataset.day);

        selectedDay = day;
        const displayStr = `${String(day).padStart(2, '0')}.${String(usedMonth + 1).padStart(2, '0')}.${usedYear}`;
        selected = displayStr;
        onChange(germanDateToISO(displayStr));
        open = false;
    }

    function next() {
        if (usedMonth === 11) {
            if (!yearOptions.includes(String(usedYear + 1))) return;
            usedMonth = 0;
            usedYear++;
        } else {
            usedMonth++;
        }
    }

    function back() {
        if (usedMonth === 0) {
            if (!yearOptions.includes(String(usedYear - 1))) return;
            usedMonth = 11;
            usedYear--;
        } else {
            usedMonth--;
        }
    }

    function isSelected(day) {
        const current = parseSelected();
        return current &&
            current.day === day &&
            current.month === usedMonth &&
            current.year === usedYear;
    }

    function updateUsedYear(val) { usedYear = Number(val); }
    function updateUsedMonth(val) { usedMonth = monthOptions.indexOf(val); }

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

<div class="flex w-full flex-col">
    {#if title}
        <p class="text-dt-6 font-medium mb-2 {disabled ? 'text-gv-light-text' : 'text-gv-dark-text'}">{title}</p>
    {/if}
    <div class="relative w-full {marginMap[marginTop]}" bind:this={datepickerRef}>
        <div class="flex items-center w-full bg-gv-input-bg {open ? 'border border-gv-primary' : ''} rounded-1 gap-1">
            <input
                type="text"
                class="w-full p-2 pl-3 pr-3 rounded-l-1 {disabled ? 'text-gv-light-text' : 'text-gv-dark-text'} outline-gv-primary text-dt-6"
                placeholder="DD.MM.YYYY"
                value={isISOString(selected) ? formatISODateString(selected) : selected}
                readonly
            >
            <button
                type="button"
                aria-label={open ? "Close date picker" : "Open date picker"}
                class="p-1.5 rounded-2 h-full aspect-square mr-1 flex items-center justify-center cursor-pointer hover:bg-gv-hover-effect"
                onclick={toggleDatepicker}
                disabled={disabled}>
                <span class="material-symbols-rounded text-icon-dt-6 text-gv-light-text">calendar_month</span>
            </button>
        </div>

        {#if open && !disabled}
            <div
                use:portal
                style={popupStyle}
                class="datepicker-portal-popup flex flex-col rounded-1 bg-gv-input-bg border border-gv-primary p-2 pt-4 gap-2 overflow-hidden"
            >
                <div class="w-full items-center flex flex-col">
                    {@render calendarGrid()}
                </div>

                <div class="flex items-center w-full justify-between gap-1">
                    <button
                        type="button"
                        aria-label="Previous month"
                        class="flex items-center justify-center p-2 rounded-2 cursor-pointer hover:bg-gv-hover-effect"
                        onclick={back}>
                        <span class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">arrow_left</span>
                    </button>

                    <div class="flex items-center w-full gap-2">
                        <Dropdown
                            bgWhite={true}
                            padding="2"
                            options={monthOptions}
                            selected={monthOptions[usedMonth]}
                            onChange={updateUsedMonth}
                            disableMinWidth={true}
                            displayTop={true}
                        />
                        <Dropdown
                            bgWhite={true}
                            padding="2"
                            options={yearOptions}
                            selected={String(usedYear)}
                            onChange={updateUsedYear}
                            disableMinWidth={true}
                            displayTop={true}
                        />
                    </div>

                    <button
                        type="button"
                        aria-label="Next month"
                        class="flex items-center justify-center p-2 rounded-2 cursor-pointer hover:bg-gv-hover-effect"
                        onclick={next}>
                        <span class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">arrow_right</span>
                    </button>
                </div>
            </div>
        {/if}
    </div>
</div>

{#snippet calendarGrid()}
    <table class="w-full border-collapse">
        <thead>
        <tr class="text-gv-light-text text-dt-8">
            <th>Mo</th><th>Di</th><th>Mi</th><th>Do</th><th>Fr</th><th>Sa</th><th>So</th>
        </tr>
        </thead>
        <tbody>
        {#each calendar as week, i (i)}
            <tr>
                {#each week as day, j (j)}
                    <td class="text-center p-1">
                        {#if day}
                            <button
                                type="button"
                                class="w-8 h-8 md:w-10 md:h-10 rounded-full text-dt-8 cursor-pointer transition-colors"
                                class:bg-gv-dark-turquoise={isSelected(day)}
                                class:text-white={isSelected(day) || isToday(day, usedMonth, usedYear)}
                                class:bg-gv-primary={isToday(day, usedMonth, usedYear) && !isSelected(day)}
                                class:text-gv-light-text={!isSelected(day) && !isToday(day, usedMonth, usedYear)}
                                class:hover:bg-gv-hover-effect={!isSelected(day)}
                                data-day={day}
                                onclick={itemClicked}
                            >
                                {day}
                            </button>
                        {:else}
                            <div class="w-8 h-8 md:w-10 md:h-10"></div>
                        {/if}
                    </td>
                {/each}
            </tr>
        {/each}
        </tbody>
    </table>
{/snippet}
<script>
    import { getTimeFromISODateString, ISOToGermanDate, isToday } from "../services/dateTimeUtils.js";

    let {
        id,
        type = "info",
        title = "Title",
        subTitle = "SubTitle",
        timestamp,
        isMobile = false,
        ...restProps
    } = $props();

    const validTypes = ["info", "success", "warning", "error"];
    if (!validTypes.includes(type)) {
        type = "info";
    }

    const NOTIFICATION_STYLE_MAP = {
        info: { icon: "info", iconColor: "text-gv-toast-info" },
        success: { icon: "check_circle", iconColor: "text-gv-toast-success" },
        warning: { icon: "warning", iconColor: "text-gv-toast-warning" },
        error: { icon: "cancel", iconColor: "text-gv-toast-error" }
    };

    const styles = $derived(NOTIFICATION_STYLE_MAP[type] || NOTIFICATION_STYLE_MAP.info);

    function isTimestampToday() {
        if (!timestamp) return false;

        const date = new Date(timestamp);

        const day = date.getDate();
        const month = date.getMonth();
        const year = date.getFullYear();

        return isToday(day, month, year);
    }
</script>

{#if title}
    <div
        class={`flex bg-white ${subTitle ? "items-start" : "items-center"} justify-start gap-2 w-full rounded-2 border border-gv-border p-2`}
        {...restProps}
    >
        <span class={`material-symbols-rounded ${!isMobile ? "text-icon-dt-5" : "text-icon-dt-4"} ${styles.iconColor}`}>
            {styles.icon}
        </span>

        <div class={`flex flex-col ${subTitle ? "items-start" : "items-center"} justify-around pr-2 gap-1 overflow-hidden`}>
            <div class="flex items-center w-full gap-2">
                <p class={`${!isMobile ? "text-dt-5" : "text-dt-6"} font-semibold w-full text-nowrap truncate`}>{title}</p>
                <p class="text-gv-light-text font-medium ml-auto text-dt-7 text-nowrap">{isTimestampToday() ? getTimeFromISODateString(timestamp) : `${ISOToGermanDate(timestamp)} - ${getTimeFromISODateString(timestamp)}`}</p>
            </div>
            {#if subTitle}
                <div class="flex items-start w-full">
                    <p class={`${!isMobile ? "text-dt-6 line-clamp-5" : "text-dt-7 line-clamp-3"}`}>{subTitle}</p>
                </div>
            {/if}
        </div>
    </div>
{/if}
<script>
    let {
        id,
        type = "info",
        title = "Title",
        subTitle = "SubTitle",
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
</script>

{#if title}
    <div
        class={`flex bg-white ${subTitle ? "items-start" : "items-center"} justify-start gap-2 w-full rounded-2 border border-gv-border p-2`}
        {...restProps}
    >
        <span class={`material-symbols-rounded ${!isMobile ? "text-icon-dt-4" : "text-icon-dt-3"} ${styles.iconColor}`}>
            {styles.icon}
        </span>

        <div class={`flex flex-col ${subTitle ? "items-start" : "items-center"} justify-around`}>
            <p class={`${!isMobile ? "text-dt-5" : "text-dt-6"} font-semibold`}>{title}</p>
            {#if subTitle}
                <p class={`${!isMobile ? "text-dt-6 line-clamp-5" : "text-dt-7 line-clamp-3"}`}>{subTitle}</p>
            {/if}
        </div>
    </div>
{/if}
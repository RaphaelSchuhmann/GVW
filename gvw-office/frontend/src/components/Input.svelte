<script>
    import { marginMap } from "../lib/dynamicStyles";

    let {
        value = $bindable(""),
        title = "Title",
        type = "text",
        placeholder = "",
        marginTop = "",
        readonly = false,
        disabled = false,
        width = "w-full",
        onChange = () => {},
        onblur = undefined, // Forwarding onblur
        showTitle = true,
        ...restProps
    } = $props();

    let inputEl = null;
    let passwordVisible = $state(false);

    const isOriginallyPassword = type === "password";

    let currentType = $state(type);

    const validTypes = ["text", "email", "password"];
    if (!validTypes.includes(type)) {
        currentType = "text";
    }

    let icon = passwordVisible ? "visibility_off" : "visibility";

    /**
     * Toggles password visibility
     */
    function togglePasswordView() {
        passwordVisible = !passwordVisible;
        currentType = passwordVisible ? "text" : "password";
    }

    /**
     * Focuses the input element (Exposed via bind:this)
     */
    export function focus() {
        inputEl?.focus();
    }
</script>

<div class={`flex flex-col gap-1 items-start ${width} ${marginMap[marginTop]}`}>
    {#if showTitle}
        <p class="text-dt-6 font-medium">{title}</p>
    {/if}

    {#if !isOriginallyPassword}
        <input
            bind:value
            bind:this={inputEl}
            type={currentType}
            {placeholder}
            {readonly}
            {disabled}
            {onblur}
            onchange={onChange}
            class="rounded-1 w-full p-2 pl-3 pr-3 bg-gv-input-bg text-black outline-gv-primary text-dt-6 max-[470px]:text-dt-7"
            {...restProps}
        />
    {:else}
        <div class={`flex items-stretch ${width}`}>
            <input
                bind:value
                bind:this={inputEl}
                type={currentType}
                {placeholder}
                {readonly}
                {disabled}
                {onblur}
                onchange={onChange}
                class="rounded-tl-1 rounded-bl-1 w-full p-2 pl-3 pr-3 bg-gv-input-bg text-black outline-gv-primary text-dt-6 max-[470px]:text-dt-7"
                {...restProps}
            />
            <button
                type="button"
                class="flex items-center justify-center bg-gv-input-bg px-3 rounded-tr-1 rounded-br-1 cursor-pointer"
                aria-label={passwordVisible ? "Hide password" : "Show password"}
                aria-pressed={passwordVisible}
                onclick={togglePasswordView}
            >
                <span class="material-symbols-rounded text-icon-dt-3 max-[470px]:text-icon-dt-5">
                    {icon}
                </span>
            </button>
        </div>
    {/if}
</div>
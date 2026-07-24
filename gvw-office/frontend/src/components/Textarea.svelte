<script>
    import { marginMap } from "../lib/dynamicStyles";

    let {
        value = $bindable(""),
        title = "Title",
        placeholder = "",
        marginTop = "",
        readonly = false,
        disabled = false,
        width = "w-full",
        height = "h-auto",
        resizable = false,
        onChange = () => {},
        onblur = undefined,
        ...restProps
    } = $props();

    let inputEl = null;

    /**
     * Focuses the input element (Exposed via bind:this)
     */
    export function focus() {
        inputEl?.focus();
    }
</script>

<div class={`flex flex-col items-start ${width} ${marginMap[marginTop]}`}>
    <p class="text-dt-6 font-medium">{title}</p>
    <textarea
        bind:value
        bind:this={inputEl}
        {onblur}
        {placeholder}
        {readonly}
        {disabled}
        onchange={onChange}
        class={`rounded-1 w-full ${height} p-2 pl-3 pr-3 bg-gv-input-bg text-black outline-gv-primary mt-1 text-dt-6 ${resizable ? "resize" : "resize-none"}`}
        {...restProps}
    ></textarea>
</div>
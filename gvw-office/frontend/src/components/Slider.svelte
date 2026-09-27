<script>
    let {
        max,
        value = $bindable(),
        onChange = () => {},
        ...restProps
    } = $props();

    function handleInput(event) {
        value = Number(event.currentTarget.value);
        onChange(value);
    }
</script>

<div class="relative w-full h-3" {...restProps}>
    <div class="absolute inset-0 rounded-full bg-gv-secondary"></div>

    <div
        class="absolute left-0 top-0 h-full rounded-full bg-gv-primary pointer-events-none"
        {...{ style: `width: ${Math.min((value / max) * 100, 100)}%` }}
    ></div>

    <input
        type="range"
        min="0"
        {max}
        {value}
        oninput={handleInput}
        class="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
    />
</div>
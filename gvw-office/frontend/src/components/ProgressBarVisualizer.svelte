<script>
    let {
        value = 0,
        max = 1,
        ...restProps
    } = $props()
</script>

<div class="w-full h-4 rounded-full bg-gv-secondary overflow-hidden">
    <div class="h-full bg-gv-primary rounded-tl-full rounded-bl-full transition-all duration-300 {`width: ${Math.min((value / max) * 100, 100)}%`}"></div>
</div>
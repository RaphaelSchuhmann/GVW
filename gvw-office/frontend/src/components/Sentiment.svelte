<script>
    let {
        selected = $bindable(0), // 0 means nothing is selected
        numberOfStars = 5,
        title = "",
        ...restProps
    } = $props();

    let stars = $state(Array.from({ length: numberOfStars }).fill("unselected"));

    let savedStates = Array.from({ length: numberOfStars }).fill("unselected");

    const baseStyles = "cursor-pointer text-icon-dt-4"

    const styles = {
        hoverSelect: "text-gv-sentiment-selected/50 material-symbols-rounded",
        hoverDeselect: "text-gv-dark-text/50 material-symbols-rounded",
        selected: "text-gv-sentiment-selected material-symbols-rounded-filled",
        unselected: "text-gv-dark-text material-symbols-rounded",
    };

    function updateSelected(e) {
        const index = Number(e.currentTarget.dataset.index);
        for (let i = 0; i < numberOfStars; i++) {
            savedStates[i] = i <= index ? "selected" : "unselected";
        }
        stars = [...savedStates];
        selected = index + 1;
    }

    function handleHover(e) {
        const index = Number(e.currentTarget.dataset.index);
        for (let i = 0; i < numberOfStars; i++) {
            stars[i] = i <= index ? "hoverSelect" : "hoverDeselect";
        }
    }

    function resetHover() {
        stars = [...savedStates];
    }
</script>

<div class="w-full flex flex-col items-start gap-1">
    <p id="sentiment-label" class="text-gv-dark-text text-dt-6 font-medium">{title}</p>

    <div
        class="w-full flex items-center justify-start gap-2"
        role="group"
        aria-labelledby="sentiment-label"
        onmouseleave={resetHover}
    >
        {#each stars as state, i (i)}
            <button
                type="button"
                data-index={i}
                onclick={updateSelected}
                onmouseenter={handleHover}
                onfocus={handleHover}
                onblur={resetHover}
                aria-label="{i + 1} von {numberOfStars} Sternen"
                aria-pressed={i < selected}
            >
                <span class="{baseStyles} {styles[state]}">
                    star
                </span>
            </button>
        {/each}
    </div>
</div>
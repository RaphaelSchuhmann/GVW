export function createContextMenu() {
    const clamp = (value, min, max) => Math.max(min, Math.min(value, max))

    let state = $state({
        open: false,
        x: 0,
        y: 0,
        activeId: null
    });

    function openFromEvent(event, id, width = 200, height = 150) {
        event.preventDefault();
        event.stopPropagation();
        state.activeId = id;

        requestAnimationFrame(() => {
            const maxX = Math.max(0, window.innerWidth - width);
            const maxY = Math.max(0, window.innerHeight - height);
            state.x = clamp(event.clientX, 0, maxX);
            state.y = clamp(event.clientY, 0, maxY);
            state.open = true;
        });
    }

    function openFromButton(event, id, width = 170, height = 150) {
        event.preventDefault();
        event.stopPropagation();
        state.activeId = id;

        const rect = event.currentTarget.getBoundingClientRect();
        state.open = true;

        requestAnimationFrame(() => {
            const maxX = Math.max(0, window.innerWidth - width);
            const maxY = Math.max(0, window.innerHeight - height);
            state.x = clamp(rect.left - width, 0, maxX);
            state.y = clamp(rect.bottom, 0, maxY);
            state.open = true;
        });
    }

    return {
        get data() { return state; },
        openFromEvent,
        openFromButton
    };
}
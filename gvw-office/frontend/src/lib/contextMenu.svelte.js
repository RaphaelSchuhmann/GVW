/**
 * Creates a reactive context menu controller.
 *
 * Manages the visibility, position, and active target of a context menu.
 * The returned controller can open the menu either at a pointer event
 * position or relative to a triggering button element.
 *
 * @returns {Object} Context menu controller.
 * @returns {Object} returns.data - Reactive context menu state.
 * @returns {boolean} returns.data.open - Whether the context menu is visible.
 * @returns {number} returns.data.x - Horizontal menu position.
 * @returns {number} returns.data.y - Vertical menu position.
 * @returns {string|null} returns.data.activeId - Identifier of the active target.
 * @returns {Function} returns.openFromEvent - Opens the menu at a pointer event position.
 * @returns {Function} returns.openFromButton - Opens the menu relative to a button element.
 */
export function createContextMenu() {
    /**
     * Restricts a value to a given range.
     *
     * @param {number} value - The value to clamp.
     * @param {number} min - Minimum allowed value.
     * @param {number} max - Maximum allowed value.
     *
     * @returns {number} The clamped value.
     */
    const clamp = (value, min, max) => Math.max(min, Math.min(value, max))

    /**
     * Reactive context menu state.
     *
     * @type {{
     *   open: boolean,
     *   x: number,
     *   y: number,
     *   activeId: string|null
     * }}
     */
    let state = $state({
        open: false,
        x: 0,
        y: 0,
        activeId: null
    });

    /**
     * Opens the context menu at the position of a pointer event.
     *
     * Prevents the default browser context menu and adjusts the position
     * to keep the menu within the visible viewport boundaries.
     *
     * @param {MouseEvent} event - The triggering mouse event.
     * @param {string} id - Identifier of the item the menu belongs to.
     * @param {number} [width=200] - Expected menu width.
     * @param {number} [height=150] - Expected menu height.
     *
     * @returns {void}
     */
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

    /**
     * Opens the context menu relative to a button element.
     *
     * Positions the menu below and to the left of the triggering button
     * while keeping it inside the visible viewport.
     *
     * @param {MouseEvent} event - The triggering button event.
     * @param {string} id - Identifier of the item the menu belongs to.
     * @param {number} [width=170] - Expected menu width.
     * @param {number} [height=150] - Expected menu height.
     *
     * @returns {void}
     */
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
let _width = $state(globalThis.window === undefined ? 1024 : window.innerWidth);

// Create a manual listener that updates our state
// This bypasses the Svelte binding logic and talks to the browser directly
if (globalThis.window !== undefined) {
    window.addEventListener('resize', () => {
        _width = window.innerWidth;
    });
}

export const viewport = {
    get width() {
        return _width;
    },
    get isMobile() {
        return _width < 768;
    }
};
/**
 * Global reactive state for the current user data.
 */
export const user = $state({
    name: "",
    email: "",
    role: "",
    phone: "",
    address: "",
    loaded: false,
    lastFetched: 0,
});

/**
 * Global reactive state for the current ui states the user set.
 */
export const uiStates = $state({
    sidebarMinimized: false,
});
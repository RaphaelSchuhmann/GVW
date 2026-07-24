import { getValue } from "../services/store";

/**
 * Svelte store for authentication data
 */
export const auth = $state({
    token: getValue("authToken"),
});
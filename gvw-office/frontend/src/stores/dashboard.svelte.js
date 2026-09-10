/**
 * Svelte store for dashboard data
 */
export const dashboardStore = $state({
    members: [],
    totalEvents: 0,
    upcomingEvents: [],
    upcomingBirthdays: [],
    totalScores: 0
});
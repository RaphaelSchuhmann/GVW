/**
 * Svelte store for dashboard data
 */
export const adminDashboardStore = $state({
    reportHub: {
        feedbackCount: 0,
        bugReportCount: 0,
        averageSentiment: 0,
        mostUsedHash: "/"
    },
    userManagement: {
        userCount: 0,
        orphanedUserCount: 0,
    }
});
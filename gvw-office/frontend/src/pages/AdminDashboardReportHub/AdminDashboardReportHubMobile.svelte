<script>
    import {
        deleteBugReport,
        deleteFeedback,
        getAllBugReports,
        getAllFeedbacks,
        getItemDetails,
    } from "../../services/reportHubService.svelte";
    import { bugReportStore, feedbackStore } from "../../stores/reportHub.svelte";
    import { appSettings } from "../../stores/appSettings.svelte";
    import { severityMap } from "../../services/reportHubService.svelte.js";

    import ToastStack from "../../components/ToastStack.svelte";
    import MobileSidebar from "../../components/MobileSidebar.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import HorizontalNavBar from "../../components/AdminHorizontalNavBar.svelte";
    import TabBar from "../../components/TabBar.svelte";
    import Card from "../../components/Card.svelte";
    import ReportHubListItem from "../../components/ReportHubListItem.svelte";
    import Chip from "../../components/Chip.svelte";
    import CollapsableViewer from "../../components/CollapsableViewer.svelte";
    import Modal from "../../components/Modal.svelte";
    import ReportHubDetails from "../../components/ReportHubDetails.svelte";

    let sidebarOpen = $state(false);

    /** @type {import("../../components/Modal.svelte").default} */
    let detailsModal = null;

    let selectedView = $state("Feedback");

    let feedbackItemDetails = $state({
        title: "",
        category: "",
        message: "",
        timestamp: "",
        route: "",
        sentiment: 0,
        userEmail: "",
        appVersion: "",
    });

    let bugReportItemDetails = $state({
        title: "",
        severity: "",
        stepsToReproduce: "",
        userEmail: "",
        timestamp: "",
        route: "",
        os: "",
        browser: "",
        viewport: "",
        appVersion: "",
    });

    async function selectItem(id) {
        const type = selectedView.toLowerCase().replaceAll(" ", "_");

        const details = await getItemDetails(id, type);
        if (!details) return;

        if (type === "feedback") {
            feedbackItemDetails = details;
        } else {
            bugReportItemDetails = details;
        }
    }

    function openSidebar() { sidebarOpen = true; }
</script>

<ToastStack isMobile={true} />

<Modal bind:this={detailsModal} isMobile={true} 
       title={`${selectedView === "Feedback" ? "Feedback" : "Bug Report"} Details`} hideSubTitle={true}>
    {#if selectedView === "Feedback" && feedbackItemDetails.title}
        <div class="w-full h-full flex flex-col items-start justify-start gap-4">
            <p class="text-dt-4 font-semibold line-clamp-2 truncate">
                {feedbackItemDetails.title}
            </p>
            <div class="flex items-center justify-start gap-4">
                <Chip
                    text={appSettings.feedbackCategories[feedbackItemDetails.category] || appSettings.feedbackCategories["_other"]}
                />
                <div class="flex gap-1 items-center">
                    <p class="text-dt-5 font-semibold">
                        {feedbackItemDetails.sentiment}
                    </p>
                    <span class="material-symbols-rounded-filled text-icon-dt-3 text-gv-sentiment-selected">
                        star
                    </span>
                </div>
            </div>

            <CollapsableViewer title="Nachricht" expanded={true}>
                <p>{feedbackItemDetails.message}</p>
            </CollapsableViewer>

            <ReportHubDetails 
                userEmail={feedbackItemDetails.userEmail}
                timestamp={feedbackItemDetails.timestamp}
                appVersion={feedbackItemDetails.appVersion}
                route={feedbackItemDetails.route}
            />
        </div>
    {:else if selectedView === "Bug Reports" && bugReportItemDetails.title}
        <div class="w-full h-full flex flex-col items-start justify-start gap-4">
            <p class="text-dt-4 font-semibold line-clamp-2 truncate">
                {bugReportItemDetails.title}
            </p>

            <Chip
                text={severityMap[bugReportItemDetails.severity]}
            />

            <CollapsableViewer title="Nachricht" expanded={true}>
                <p>{bugReportItemDetails.stepsToReproduce}</p>
            </CollapsableViewer>

            <ReportHubDetails
                userEmail={bugReportItemDetails.userEmail}
                timestamp={bugReportItemDetails.timestamp}
                appVersion={bugReportItemDetails.appVersion}
                route={bugReportItemDetails.route}
                os={bugReportItemDetails.os}
                browser={bugReportItemDetails.browser}
                viewport={bugReportItemDetails.viewport}
            />
        </div>
    {/if}
</Modal>

<MobileSidebar currentPage="adminDashboard" bind:isOpen={sidebarOpen}/>

<main class="flex overflow-hidden h-screen">
    <div class="flex-1 min-h-0 overflow-y-auto">
        <div class="flex flex-col w-full flex-1 overflow-hidden p-7 min-h-0 h-full">
            <div class="w-full flex items-center justify-start">
                <button class="flex items-center justify-center" onclick={openSidebar}>
                    <span class="material-symbols-rounded text-icon-dt-4 text-gv-dark-text">
                        menu
                    </span>
                </button>
            </div>
            <div class="mt-5">
                <HorizontalNavBar currentPage="reportHub" />
            </div>
            <PageHeader
                title="Berichte Hub"
                subTitle=""
                showSlot={false}
                hideSubTitle={true}
                marginTop="5"
            />

            <TabBar
                contents={["Feedback", "Bug Reports"]}
                selected={selectedView}
                marginTop="5"
                onChange={async (val) => {
                    selectedView = val;
                    if (selectedView === "Feedback") {
                        await getAllFeedbacks();
                    } else {
                        await getAllBugReports();
                    }
                }}
            />

            {#if selectedView === "Feedback"}
                <div class="w-full h-full flex gap-4 mt-5">
                    <div class="h-full w-full">
                        <Card fillHeight={true} padding="0">
                            {#each feedbackStore.data as feedback (feedback.id)}
                                <ReportHubListItem
                                    id={feedback.id}
                                    title={feedback.title}
                                    chipText={appSettings.feedbackCategories[feedback.category] || appSettings.feedbackCategories["other"]}
                                    asyncDeleteFunction={deleteFeedback}
                                    onclick={async (id) => {
                                        await selectItem(id); 
                                        detailsModal.showModal();
                                    }}
                                />
                            {/each}
                        </Card>
                    </div>
                </div>
            {:else}
                <div class="w-full h-full flex gap-4 mt-5">
                    <div class="h-full w-full">
                        <Card fillHeight={true} padding="0">
                            {#each bugReportStore.data as bugReport (bugReport.id)}
                                <ReportHubListItem
                                    id={bugReport.id}
                                    title={bugReport.title}
                                    chipText={severityMap[bugReport.severity]}
                                    asyncDeleteFunction={deleteBugReport}
                                    onclick={async (id) => {
                                        await selectItem(id); 
                                        detailsModal.showModal();
                                    }}
                                />
                            {/each}
                        </Card>
                    </div>
                </div>
            {/if}
        </div>
    </div>
</main>

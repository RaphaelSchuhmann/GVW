<script>
    import {adminDashboardStore} from "../../stores/adminDashboard.svelte.js";
    import {push} from "svelte-spa-router";

    import ToastStack from "../../components/ToastStack.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import MobileSidebar from "../../components/MobileSidebar.svelte";
    import Card from "../../components/Card.svelte";
    import Button from "../../components/Button.svelte";
    import HorizontalNavBar from "../../components/AdminHorizontalNavBar.svelte";
    import Chip from "../../components/Chip.svelte";

    let sidebarOpen = $state(false);

    function openSidebar() { sidebarOpen = true; }
    async function routeToReportHub() { await push("/admin/reportHub"); }
    async function routeToUserManagement() { await push("/admin/userManagement"); }
    async function routeToDeployments() { await push("/admin/deployments"); }
</script>

<ToastStack isMobile={true} />

<MobileSidebar currentPage="adminDashboard" bind:isOpen={sidebarOpen} />

<main class="flex overflow-hidden">
    <div class="flex-1 min-h-0 overflow-y-auto">
        <div class="flex flex-col w-full flex-1 overflow-hidden p-7 min-h-0">
            <div class="w-full flex items-center justify-start">
                <button class="flex items-center justify-center" onclick={openSidebar}>
                    <span class="material-symbols-rounded text-icon-dt-4 text-gv-dark-text">menu</span>
                </button>
            </div>
            <div class="mt-5">
                <HorizontalNavBar currentPage="overview"/>
            </div>
            <PageHeader title="Admin Dashboard" subTitle=""
                        showSlot={false} marginTop="5" hideSubTitle={true} />
            <div class="flex flex-col w-full gap-4 mt-10">
                <Card>
                    <div class="w-full flex items-center justify-start p-1">
                        <p class="text-gv-dark-text text-dt-4">Deployments</p>
                    </div>
                    <div class="flex flex-col w-full h-full gap-2">
                        {#each adminDashboardStore.deployments as deployment, index (deployment.id)}
                            <Card>
                                <div class="flex flex-col w-full items-start justify-start gap-2">
                                    <p class="text-gv-dark-text text-dt-5">{deployment.title}</p>
                                    <Chip text={deployment.type} colorType={deployment.type.toLowerCase()} />
                                </div>
                            </Card>
                        {/each}
                    </div>
                    <Button type="primary" onclick={routeToDeployments}>
                        <span class="text-dt-5">Details</span>
                        <span class="material-symbols-rounded">chevron_right</span>
                    </Button>
                </Card>
                <Card>
                    <div class="w-full flex items-center justify-start p-2">
                        <p class="font-medium text-gv-dark-text text-dt-4">Berichte Hub</p>
                    </div>
                    <div class="w-full h-full flex flex-col items-center p-2 gap-4">
                        <div class="flex w-full items-center justify-start gap-2">
                            <span class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">chat_info</span>
                            <p class="font-medium text-gv-dark-text text-dt-5">Feedback</p>
                            <p class="font-medium text-gv-light-text text-dt-5">{adminDashboardStore.reportHub.feedbackCount}</p>
                        </div>
                        <div class="flex w-full items-center justify-start gap-2">
                            <span class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">bug_report</span>
                            <p class="font-medium text-gv-dark-text text-dt-5">Bug Reports</p>
                            <p class="font-medium text-gv-light-text text-dt-5">{adminDashboardStore.reportHub.bugReportCount}</p>
                        </div>
                        <div class="flex w-full items-center justify-start gap-2">
                            <span class="material-symbols-rounded-filled text-icon-dt-6 text-gv-sentiment-selected">star</span>
                            <p class="font-medium text-gv-dark-text text-dt-5">Bewertung</p>
                            <p class="font-medium text-gv-light-text text-dt-5">{adminDashboardStore.reportHub.averageSentiment}</p>
                        </div>
                        <div class="flex w-full items-center justify-start gap-2">
                            <span class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">language</span>
                            <p class="font-medium text-gv-dark-text text-dt-5">Hash</p>
                            <p class="font-medium text-gv-light-text text-dt-5">{adminDashboardStore.reportHub.mostUsedHash}</p>
                        </div>
                        <Button type="primary" onclick={routeToReportHub}>
                            <span class="text-dt-6">Details</span>
                            <span class="material-symbols-rounded">chevron_right</span>
                        </Button>
                    </div>
                </Card>
                <Card>
                    <div class="w-full flex items-center justify-start p-2">
                        <p class="font-medium text-gv-dark-text text-dt-4">Nutzerverwaltung</p>
                    </div>
                    <div class="w-full h-full flex flex-col items-center max-[1300px]:max-h-[50vh] p-2 gap-4">
                        <div class="flex w-full items-center justify-start gap-2">
                            <span class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">groups</span>
                            <p class="font-medium text-gv-dark-text text-dt-5">Benutzer</p>
                            <p class="font-medium text-gv-light-text text-dt-5">{adminDashboardStore.userManagement.userCount}</p>
                        </div>
                        <div class="flex w-full items-center justify-start gap-2">
                            <span class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">verified_off</span>
                            <p class="font-medium text-gv-dark-text text-dt-5">Orphaned Benutzer</p>
                            <p class="font-medium text-gv-light-text text-dt-5">{`${adminDashboardStore.userManagement.orphanedUserCount} / ${adminDashboardStore.userManagement.userCount}`}</p>
                        </div>
                        <Button type="primary" onclick={routeToUserManagement}>
                            <span class="text-dt-6">Details</span>
                            <span class="material-symbols-rounded">chevron_right</span>
                        </Button>
                    </div>
                </Card>
            </div>
        </div>
    </div>
</main>
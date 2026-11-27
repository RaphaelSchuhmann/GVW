<script>
    import { viewport } from "../../stores/viewport.svelte.js";
    import {push} from "svelte-spa-router";
    import {adminDashboardStore} from "../../stores/adminDashboard.svelte.js";

    import ToastStack from "../../components/ToastStack.svelte";
    import DesktopSidebar from "../../components/DesktopSidebar.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import Card from "../../components/Card.svelte";
    import Button from "../../components/Button.svelte";
    import HorizontalNavBar from "../../components/AdminHorizontalNavBar.svelte";
    import Chip from "../../components/Chip.svelte";

    async function routeToReportHub() { await push("/admin/reportHub"); }
    async function routeToUserManagement() { await push("/admin/userManagement"); }
    async function routeToDeployments() { await push("/admin/deployments"); }
    async function routeToDeploymentDetails(id) { await push(`/admin/deployment/details?id=${id}&editing=false`) ;}
</script>

<ToastStack />

<main class="flex h-screen overflow-hidden">
    <DesktopSidebar currentPage="adminDashboard" />
    <div class="flex-1 min-h-0 overflow-y-auto">
        <div class="flex flex-col w-full h-full flex-1 overflow-hidden p-10 min-h-0">
            <HorizontalNavBar currentPage="overview" />
            <PageHeader title="Admin Dashboard" subTitle=""
                        showSlot={false} marginTop="5" hideSubTitle={true} />
            <div class="flex max-[1300px]:flex-col min-[1300px]:h-full w-full gap-4 mt-10 overflow-y-auto">
                <div class="w-full h-full flex flex-col items-center">
                    <Card fillHeight={viewport.width > 1300}>
                        <div class="w-full flex items-center justify-start p-1">
                            <p class="text-gv-dark-text text-dt-4">Deployments</p>
                        </div>
                        <div class="flex flex-col w-full h-full gap-2 mt-2">
                            {#each adminDashboardStore.deployments as deployment, index (deployment.id)}
                                <button class="cursor-pointer" onclick={() => routeToDeploymentDetails(deployment.id)}>
                                    <Card>
                                        <div class="flex w-full items-center justify-start gap-2">
                                            <p class="text-gv-dark-text text-dt-5">{deployment.title}</p>
                                            <div class="ml-auto">
                                                <Chip text={deployment.type} colorType={deployment.type.toLowerCase()} />
                                            </div>
                                        </div>
                                    </Card>
                                </button>
                            {/each}
                        </div>
                        <Button type="primary" onclick={routeToDeployments}>
                            <span class="text-dt-5">Details</span>
                            <span class="material-symbols-rounded">chevron_right</span>
                        </Button>
                    </Card>
                </div>
                <div class="w-full {viewport.width >= 1300 ? 'h-full' : ''} gap-4 flex flex-col items-center">
                    <Card fillHeight={viewport.width > 1300}>
                        <div class="w-full flex items-center justify-start p-2">
                            <p class="font-medium text-gv-dark-text text-dt-3">Berichte Hub</p>
                        </div>
                        <div class="w-full h-full flex flex-col items-center max-[1300px]:max-h-[50vh] p-2 gap-4">
                            <div class="flex w-full items-center justify-start gap-2">
                                <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">chat_info</span>
                                <p class="font-medium text-gv-dark-text text-dt-4">Feedback</p>
                                <p class="font-medium text-gv-light-text text-dt-4">{adminDashboardStore.reportHub.feedbackCount}</p>
                            </div>
                            <div class="flex w-full items-center justify-start gap-2">
                                <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">bug_report</span>
                                <p class="font-medium text-gv-dark-text text-dt-4">Bug Reports</p>
                                <p class="font-medium text-gv-light-text text-dt-4">{adminDashboardStore.reportHub.bugReportCount}</p>
                            </div>
                            <div class="flex w-full items-center justify-start gap-2">
                                <span class="material-symbols-rounded-filled text-icon-dt-5 text-gv-sentiment-selected">star</span>
                                <p class="font-medium text-gv-dark-text text-dt-4">Bewertung</p>
                                <p class="font-medium text-gv-light-text text-dt-4">{adminDashboardStore.reportHub.averageSentiment}</p>
                            </div>
                            <div class="flex w-full items-center justify-start gap-2">
                                <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">language</span>
                                <p class="font-medium text-gv-dark-text text-dt-4">Hash</p>
                                <p class="font-medium text-gv-light-text text-dt-4">{adminDashboardStore.reportHub.mostUsedHash}</p>
                            </div>
                            <Button type="primary" onclick={routeToReportHub}>
                                <span class="text-dt-5">Details</span>
                                <span class="material-symbols-rounded">chevron_right</span>
                            </Button>
                        </div>
                    </Card>
                    <Card fillHeight={viewport.width > 1300}>
                        <div class="w-full flex items-center justify-start p-2">
                            <p class="font-medium text-gv-dark-text text-dt-3">Nutzerverwaltung</p>
                        </div>
                        <div class="w-full h-full flex flex-col items-center max-[1300px]:max-h-[50vh] p-2 gap-4">
                            <div class="flex w-full items-center justify-start gap-2">
                                <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">groups</span>
                                <p class="font-medium text-gv-dark-text text-dt-4">Benutzer</p>
                                <p class="font-medium text-gv-light-text text-dt-4">{adminDashboardStore.userManagement.userCount}</p>
                            </div>
                            <div class="flex w-full items-center justify-start gap-2">
                                <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">verified_off</span>
                                <p class="font-medium text-gv-dark-text text-dt-4">Orphaned Benutzer</p>
                                <p class="font-medium text-gv-light-text text-dt-4">{`${adminDashboardStore.userManagement.orphanedUserCount} / ${adminDashboardStore.userManagement.userCount}`}</p>
                            </div>
                            <Button type="primary" onclick={routeToUserManagement}>
                                <span class="text-dt-5">Details</span>
                                <span class="material-symbols-rounded">chevron_right</span>
                            </Button>
                        </div>
                    </Card>
                </div>
            </div>
        </div>
    </div>
</main>
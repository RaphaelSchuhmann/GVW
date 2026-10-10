<script>
    import { deploymentStore } from "../../stores/deployments.svelte.js";

    import ToastStack from "../../components/ToastStack.svelte";
    import MobileSidebar from "../../components/MobileSidebar.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import HorizontalNavBar from "../../components/AdminHorizontalNavBar.svelte";
    import SearchBar from "../../components/SearchBar.svelte";
    import Filter from "../../components/Filter.svelte";
    import Card from "../../components/Card.svelte";
    import Chip from "../../components/Chip.svelte";

    let sidebarOpen = $state(false);

    function openSidebar() { sidebarOpen = true; }
</script>

<ToastStack isMobile={true} />

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
                <HorizontalNavBar currentPage="deployments" />
            </div>
            <PageHeader
                title="Deployments"
                subTitle=""
                showSlot={false}
                hideSubTitle={true}
                marginTop="5"
            />

            <div class="flex min-[1300px]:items-center max-[1300px]:flex-col w-full gap-4 mt-5">
                <SearchBar page="deployments" />
                <div class="w-full">
                    <Filter page="deployments"
                            options={["Alle Typen", "Scheduled", "Running", "Successful", "Cancelled", "Failed"]}
                            textWrap={false} />
                </div>
            </div>

            <div class="flex flex-col w-full overflow-y-auto h-full gap-2 mt-5">
                {#each deploymentStore.display as deployment, index (deployment.id)}
                    <Card>
                        <div class="w-full flex flex-col items-start gap-2">
                            <p class="text-gv-dark-text text-dt-5">{deployment.title}</p>
                            <Chip text={deployment.type} colorType={deployment.type.toLowerCase()} />
                        </div>
                    </Card>
                {/each}
            </div>
        </div>
    </div>
</main>

<script>
    import { viewport } from "../../stores/viewport.svelte";
    import { route } from "../../services/utils.js";
    import { fetchAndSetRaw } from "../../services/filterService.svelte";
    import { user } from "../../stores/user.svelte";
    import { lastRefresh } from "../../stores/sseStore.svelte.js";
    import { addToast } from "../../stores/toasts.svelte.js";
    import {
        deploymentExists,
        getDeploymentUpdates,
        getFullDeployment
    } from "../../services/deploymentService.svelte.js";

    import AdminDashboardDeploymentDetailsDesktop from "./AdminDashboardDeploymentDetailsDesktop.svelte";
    import GlobalLoader from "../../components/GlobalLoader.svelte";

    const hash = window.location.hash;
    const queryString = hash.split("?")[1];
    const params = new URLSearchParams(queryString);

    const deploymentId = params.get("id");
    let isEditing = $state(params.get("editing") === "true");

    let deploymentData = $state({ rev: "" });
    let ready = $state(false);

    $effect(() => {
        if (!user.loaded || ready) return;

        if (user.role !== "admin") {
            route("/dashboard");
            return;
        }

        if (viewport.isMobile) {
            route("/admin/deployments");
            return;
        }

        async function loadDeployment() {
            if (!deploymentId) {
                await route("/admin/deployments");
                return;
            }

            ready = false;

            const result = await getFullDeployment(deploymentId);

            if (!result) {
                await route("/admin/deployments");
                return;
            }

            deploymentData = result;
            deploymentUpdates = result.updates;
            ready = true;
        }

        loadDeployment();
    });

    let isDeleting = $state(false);

    $effect(() => {
        const _triggerDeployments = lastRefresh.DEPLOYMENTS;

        if (!ready || isDeleting) return;

        (async () => {
            const exists = await deploymentExists(deploymentId);
            if (!exists) {
                addToast({
                    title: "Deployment nicht mehr verfügbar",
                    subTitle: viewport.isMobile ? "" : "Dieses Deployment wurde gelöscht und ist nicht mehr verfügbar.",
                    type: "error"
                });

                await fetchAndSetRaw();
                await route("/admin/deployments");
            }
        })();
    });

    $effect(() => {
        const _triggerDeploymentUpdates = lastRefresh.DEPLOYMENT_UPDATES;

        if (!ready || isDeleting) return;

        async function getUpdates() {
            if (!deploymentId) {
                await route("/admin/deployments");
                return;
            }

            const updates = await getDeploymentUpdates(deploymentId);

            // Only update the updates if the new length is bigger than the old one
            // as updates cannot / should not be deleted
            if (deploymentUpdates.length < updates.length) {
                deploymentUpdates = updates;
            }
        }
    });

    function updateIsEditing(val) { isEditing = val; }

    function updateIsDeleting(val) { isDeleting = val; }

    let isLoading = $derived(!deploymentData || !deploymentData.rev || !deploymentId || !ready);

    let deploymentUpdates = $state([]);
</script>

<GlobalLoader loading={isLoading}>
    {#key deploymentData.rev}
        <AdminDashboardDeploymentDetailsDesktop {deploymentData} deploymentUpdates={deploymentUpdates} bind:isEditing bind:isDeleting
                                                       onChangeIsEditing={updateIsEditing}
                                                       onChangeIsDeleting={updateIsDeleting} />
    {/key}
</GlobalLoader>
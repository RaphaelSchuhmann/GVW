<script>
    import { viewport } from "../../stores/viewport.svelte";
    import { fetchAndSetRaw } from "../../services/filterService.svelte";

    import ReportDetailsDesktop from "./ReportDetailsDesktop.svelte";
    import { auth } from "../../stores/auth.svelte";
    import { lastRefresh } from "../../stores/sseStore.svelte.js";
    import Spinner from "../../components/Spinner.svelte";
    import { user } from "../../stores/user.svelte.js";
    import { push } from "svelte-spa-router";
    import { getReport, reportExists } from "../../services/reportService.svelte.js";
    import { addToast } from "../../stores/toasts.svelte.js";
    import { editorMetadataStore } from "../../stores/textEditorStore.svelte.js";

    let isGlobalLoading = $derived(user.name.length === 0);

    const hash = window.location.hash;
    const queryString = hash.split("?")[1];
    const params = new URLSearchParams(queryString);

    const reportId = params.get("id");
    let reportData = $state(null);

    let ready = false;

    $effect(() => {
        if (!auth.token) return;

        (async () => {
            if (user.role !== "admin" && user.role !== "board_member" && user.role !== "secretary") {
                await push("/dashboard");
            }

            // Note that this editor is intended for desktop only!
            if (viewport.isMobile) {
                await push("/reports");
                return;
            } else if (!reportId) {
                await push("/reports");
                return;
            }

            let active = true;

            getReport(reportId).then(data => {
                if (active) {
                    reportData = data;
                    if (reportData.content.length > 0) {
                        reportData.content.forEach(item => {
                            item.version = 0;
                        });
                    }
                }
            });

            editorMetadataStore.activeFeature = "reports";

            ready = true;

            return () => {
                active = false;
            };
        })();
    });

    $effect(() => {
        const _trigger = lastRefresh.REPORTS;

        if (!ready) return;

        (async () => {
            const exists = await reportExists(reportId);
            if (!exists) {
                addToast({
                    title: "Bericht nicht mehr verfügbar",
                    subTitle: viewport.isMobile ? "" : "Dieser Bericht wurde gelöscht und ist nicht mehr verfügbar.",
                    type: "error"
                });

                await fetchAndSetRaw();
                await push("/reports");
            }
        })
    })
</script>

{#if isGlobalLoading}
    <div class="w-full h-screen flex justify-center items-center">
        <Spinner title="GVW Office" subTitle="Daten werden geladen..."/>
    </div>
{:else}
    <ReportDetailsDesktop reportData={reportData} />
{/if}
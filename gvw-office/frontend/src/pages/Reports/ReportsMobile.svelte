<!-- src/pages/Reports/ReportsMobile.svelte -->
<script>
    import { user } from "../../stores/user.svelte.js";

    import ToastStack from "../../components/ToastStack.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import SearchBar from "../../components/SearchBar.svelte";
    import Filter from "../../components/Filter.svelte";
    import Button from "../../components/Button.svelte";
    import ReportItem from "../../components/ReportItem.svelte";
    import Modal from "../../components/Modal.svelte";
    import Input from "../../components/Input.svelte";
    import Dropdown from "../../components/Dropdown.svelte";
    import { addReport, highlight, reportTypeMap } from "../../services/reportService.svelte.js";
    import Spinner from "../../components/Spinner.svelte";
    import { fetchAndSetRaw } from "../../services/filterService.svelte.js";
    import { reportDeepSearchStore, reportsStore } from "../../stores/report.svelte.js";
    import { formatISODateString } from "../../services/dateTimeUtils.js";
    import MobileSidebar from "../../components/MobileSidebar.svelte";
    import Chip from "../../components/Chip.svelte";

    let addReportModal = null;
    let deepSearchResultModal = null;

    let reportInputs = $state({
        title: "",
        type: "",
        description: ""
    });

    let isSubmitting = $state(false);

    const submitDisabled = $derived.by(() => {
        const hasEmptyFields = !reportInputs.title || reportInputs.title.trim() === "";
        const hasUnselectedDropdowns = !reportInputs.type || reportInputs.type.toLowerCase() === "wählen";
        return hasEmptyFields || hasUnselectedDropdowns || isSubmitting;
    });

    async function submitNewReport() {
        isSubmitting = true;

        let report = {
            title: reportInputs.title,
            description: reportInputs.description ? reportInputs.description : "Keine Beschreibung",
            type: reportInputs.type,
            author: user.name
        };

        try {
            await addReport(report);
        } finally {
            isSubmitting = false;
        }

        addReportModal.hideModal();
        await fetchAndSetRaw();
    }

    function resetInputs() {
        reportInputs.title = "";
        reportInputs.type = "";
        reportInputs.description = "";
    }

    let sidebarOpen = $state(false);

    function openSidebar() {
        sidebarOpen = true;
    }

    function showAddReportModal() {
        if (addReportModal) {
            addReportModal.showModal();
        }
    }

    function hideAddReportModal() {
        if (addReportModal) {
            addReportModal.hideModal();
        }
    }

    function showDeepSearchResultModal() {
        if (deepSearchResultModal) {
            deepSearchResultModal.showModal();
        }
    }

    function updateReportTypeDropdown(val) {
        reportInputs.type = reportTypeMap[val] || "other";
    }
</script>

<ToastStack isMobile={true} />

<Modal bind:this={addReportModal} extraFunction={resetInputs} isMobile={true}
       title="Bericht erstellen" subTitle="Geben Sie hier die Grunddaten des Berichts an.">
    <Input bind:value={reportInputs.title} title="Titel" placeholder="Titel" />
    <Dropdown title="Berichttyp" options={["Jahresbericht", "Protokoll", "Versammlungsbericht", "Sonstigerbericht"]}
              onChange={updateReportTypeDropdown} marginTop="5" />
    <Input bind:value={reportInputs.description} title="Beschreibung (Optional)" placeholder="Beschreibung"
           marginTop="5" />
    <div class="w-full flex items-center justify-end mt-5 gap-4">
        <Button type="secondary" onclick={hideAddReportModal}>Abbrechen</Button>
        <Button type="primary" disabled={submitDisabled} onclick={submitNewReport} isSubmit={true}>
            {#if isSubmitting}
                <Spinner light={true} />
                <p>Speichern...</p>
            {:else}
                Hinzufügen
            {/if}
        </Button>
    </div>
</Modal>

<Modal bind:this={deepSearchResultModal} isMobile={true}
       title="Deep Search Suchergebnisse" subTitle="" hideSubTitle={true}>
    <div class="flex flex-col items-center gap-4 w-full max-h-[60vh] overflow-y-auto p-1">
        {#each reportDeepSearchStore.data as report (report.id)}
            <ReportItem id={report.id} title={report.title} date={formatISODateString(report.createdAt)}
                        author={report.author} type={report.type}
                        additionalText={highlight(report.snippet, reportDeepSearchStore.query)}
                        isSearchResult={true} isMobile={true} />
        {/each}
    </div>
</Modal>

<MobileSidebar currentPage="reports" bind:isOpen={sidebarOpen} />

<main class="w-full h-dvh overflow-hidden flex flex-col">
    <div class="flex flex-col w-full h-full p-7 overflow-x-hidden overflow-y-auto pb-16">

        <div class="w-full flex items-center justify-start">
            <button class="flex items-center justify-center" onclick={openSidebar}>
                <span class="material-symbols-rounded text-icon-dt-4 text-gv-dark-text">menu</span>
            </button>
        </div>

        <PageHeader title="Berichte" subTitle="Verwaltung von Vereinsberichten" showSlot={false}></PageHeader>

        {#if user.role === "board_member" || user.role === "admin" || user.role === "secretary"}
            <div class="flex flex-col items-center w-full gap-2 mt-5">
                <Button type="primary" onclick={showAddReportModal}>
                    <span class="material-symbols-rounded min-[1000px]:text-icon-dt-4 text-icon-dt-5 mr-2">add</span>
                    <p class="min-[1000px]:text-dt-4 text-dt-5 text-nowrap">Bericht erstellen</p>
                </Button>
            </div>
        {/if}

        <div class="flex flex-col w-full gap-4 mt-5">
            <SearchBar placeholder="Berichte durchsuchen..." page="reports" />
            <div class="w-full">
                <Filter page="reports"
                        options={["Alle Typen", "Jahresbericht", "Protokoll", "Versammlungsbericht", "Sonstigerbericht"]}
                        textWrap={false} />
            </div>
        </div>

        {#if reportDeepSearchStore.data.length > 0}
            <button class="cursor-pointer mt-5 self-start" onclick={showDeepSearchResultModal}>
                <Chip
                    text={`Gefunden in ${reportDeepSearchStore.data.length} ${reportDeepSearchStore.data.length > 1 ? "Berichten" : "Bericht"}`} />
            </button>
        {/if}

        <div class="w-full mt-5 flex flex-col items-center justify-start gap-3">
            {#each reportsStore.display as report (report.id)}
                <ReportItem id={report.id} title={report.title} date={formatISODateString(report.createdAt)}
                            author={report.author} type={report.type} additionalText={report.description}
                            isMobile={true} deletable={false} />
            {/each}
        </div>
    </div>
</main>
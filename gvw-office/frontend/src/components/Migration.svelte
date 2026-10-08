<script>
    let {
        migrationData = {
            id: "",
            database: "",
            field: "",
            action: "",
            conditional: {
                condition: { field: "", value: "" },
                truePath: { field: "", value: "" },
                falsePath: { field: "", value: "" },
                negated: false
            },
            value: ""
        },
        deleteMigration = () => {}
    } = $props();

    async function handleDelete() {
        deleteMigration(migrationData.id);
    }
</script>

<div class="flex flex-col items-start justify-start gap-2">
    <div class="flex items-center justify-start gap-4">
        <div class="flex items-center justify-start gap-2">
            <p class="font-medium text-gv-dark-text text-dt-4">DB</p>
            {@render item(migrationData.database)}
        </div>

        {#if migrationData.field}
            <div class="flex items-center justify-start gap-2">
                <p class="font-medium text-gv-dark-text text-dt-4">Feld</p>
                {@render item(migrationData.field)}
            </div>
        {/if}

        <div class="flex items-center justify-start gap-2">
            <p class="font-medium text-gv-dark-text text-dt-4">Aktion</p>
            {@render item(migrationData.action)}
        </div>

        <button class="flex items-center justify-center p-2 rounded-2 cursor-pointer hover:bg-gv-hover-effect" onclick={handleDelete}>
            <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">delete</span>
        </button>
    </div>

    {#if migrationData.action === "RENAME"}
        <div class="flex items-center justify-start gap-2">
            <p class="text-gv-dark-text text-dt-4">Rename field</p>
            {@render item(migrationData.field)}
            <p class="text-gv-dark-text text-dt-4">to</p>
            {@render item(migrationData.value)}
        </div>
    {:else if migrationData.action === "CONDITIONAL"}
        <div class="flex items-center justify-start gap-2">
            <p class="text-gv-dark-text text-dt-4">If</p>
            {@render item(migrationData.conditional.condition.field)}
            <p class="text-gv-dark-text text-dt-4">is</p>
            {@render item(migrationData.conditional.condition.value)}
        </div>
        
        <div class="flex items-center justify-start gap-2">
            <p class="text-gv-dark-text text-dt-4">Then set</p>
            {@render item(migrationData.conditional.truePath.field)}
            <p class="text-gv-dark-text text-dt-4">to</p>
            {@render item(migrationData.conditional.truePath.value)}
        </div>

        <div class="flex items-center justify-start gap-2">
            <p class="text-gv-dark-text text-dt-4">Else set</p>
            {@render item(migrationData.conditional.falsePath.field)}
            <p class="text-gv-dark-text text-dt-4">to</p>
            {@render item(migrationData.conditional.falsePath.value)}
        </div>
    {/if}
</div>

{#snippet item(text)}
    <div class="flex items-center justify-center p-1 pl-3 pr-3 rounded-1 bg-white border-2 border-gv-border">
        <p class="text-dt-6 text-gv-dark-text">{text}</p>
    </div>
{/snippet}
<script>
    import ButtonSelect from "./ButtonSelect.svelte";
    import StyleButton from "./StyleButton.svelte";
    import ImageUpload from "./ImageUpload.svelte";
    import LinkItem from "./LinkItem.svelte";
    import { textEditorConfigs } from "../../lib/textEditorConfig.svelte.js";
    import ContentDisplay from "./ContentDisplay.svelte";
    import { editorSelectionStore } from "../../stores/textEditorStore.svelte.js";
    import { addBlock, applyStyleInDOM, updateBlockType } from "../../services/textEditorService.svelte";
    import { tick } from "svelte";
    import { sanitize } from "../../services/utils.js";
    import { viewport } from "../../stores/viewport.svelte";

    let {
        itemData = $bindable({}),
        draft = $bindable({}),
        page = "",
        isEditing = $bindable(false),
        isMobile = false,
        ...restProps
    } = $props();

    const config = textEditorConfigs[page];
    let data = $state({
        title: "",
        author: "",
        readingTime: "",
        content: []
    });

    let activeBlock = $state(null);

    $effect(() => {
        if (isEditing) {
            data.title = draft?.title;
            data.author = draft?.author;
            data.readingTime = draft?.readingTimeInMinutes;
            data.content = draft?.content;
        } else {
            data.title = itemData?.title;
            data.author = itemData?.author;
            data.readingTime = itemData?.readingTimeInMinutes;
            data.content = itemData?.content;
        }
    });

    $effect(() => {
        if (!data.content) return;

        // Extra save guard if user ends up having no blocks it automatically
        // adds a new empty text block
        if (data.content.length === 0 && isEditing) {
            addBlock(data.content, 0, "", "text");
        }
    })

    function manualBlockInsert() {
        const id = addBlock(data.content, 0, "", "text", true);

        tick().then(() => {
           const el = document.querySelector(`[data-id='${id}']`);
           if (el) {
               // @ts-ignore
               el.focus();
           }
        });
    }

    function changeBlockType(type) {
        // updateBlockType() returns the activeBlock id again as it can happen
        // that the activeBlock is already set to null after the function
        // finished its execution
        const updatedBlockId = updateBlockType($state.snapshot(activeBlock), type, data.content);

        tick().then(() => {
            const updatedBlock = document.querySelector(`[data-id="${updatedBlockId}"]`);

            if (updatedBlock) {
                // @ts-ignore
                updatedBlock.focus();

                const selection = window.getSelection();
                const range = document.createRange();
                range.selectNodeContents(updatedBlock);
                range.collapse(false);
                selection.removeAllRanges();
                selection.addRange(range);
            }
        });
    }

    export function getContent() {
        if (isEditing) {
            draft.title = data.title.trim();
            draft.content = data.content;
            for (const item of draft.content) {
                if (item.type === "image") continue;
                item.data = sanitize(item.data);
            }
        }
    }

    let isBlockquote = $derived(!!draft.content.find(i => i.id === activeBlock && i.type === "blockquote"));
    let activeBlockType = $derived(config?.optionMap[editorSelectionStore.activeStyles.blockType] || config?.optionMap["text"]);

    function changeToBlockQuote(e) {
        e.preventDefault();
        changeBlockType("blockquote");
    }

    function handleApplyStyle(e) {
        const style = e.currentTarget.dataset.style;
        e.preventDefault();
        applyStyleInDOM(style);
    }
</script>

<div class="w-full h-full flex-1 flex flex-col items-start justify-start {isMobile ? 'rounded-none' : 'rounded-1 drop-shadow-md'} bg-white  min-h-0">
    {#if isEditing}
        <div class="w-full flex items-center justify-start gap-2 border-b-2 border-gv-border p-2">
            <ButtonSelect
                bind:selected={activeBlockType}
                optionMap={config?.optionMap} options={config?.options} fillHeight={true}
                disabled={activeBlock === null} onChange={changeBlockType} />

            <div class="w-0.75 bg-gv-separator h-full rounded-1"></div>

            <div class="flex items-center gap-1">
                <StyleButton icon="format_bold" disabled={activeBlock === null}
                             bind:isToggled={editorSelectionStore.activeStyles.isBold} {...{ 'data-style': 'strong' }}
                             onMouseDown={handleApplyStyle} />
                <StyleButton icon="format_underlined" disabled={activeBlock === null}
                             bind:isToggled={editorSelectionStore.activeStyles.isUnderline} {...{ 'data-style': 'u' }}
                             onMouseDown={handleApplyStyle} />
                <StyleButton icon="format_italic" disabled={activeBlock === null}
                             bind:isToggled={editorSelectionStore.activeStyles.isItalic} {...{ 'data-style': 'em' }}
                             onMouseDown={handleApplyStyle} />
            </div>

            <div class="w-0.75 bg-gv-separator h-full rounded-1"></div>

            <div class="flex items-center gap-1">
                <!--<StyleButton icon="format_list_bulleted" disabled={true} /> TODO: Coming 1.1-->
                <ImageUpload disabled={activeBlock === null} activeBlock={activeBlock}
                             items={data.content} />
                <!--<LinkItem id={itemData.id} page={page} disabled={true}/> TODO: Coming 1.2-->
                <StyleButton icon="format_quote" disabled={activeBlock === null} bind:isToggled={isBlockquote}
                             onMouseDown={changeToBlockQuote} />
            </div>

            <div class="w-0.75 bg-gv-separator h-full rounded-1"></div>

            <div class="flex items-center gap-1">
                <button
                    class="p-2 flex items-center justify-center cursor-pointer rounded-2 hover:bg-gv-hover-effect"
                    onclick={manualBlockInsert}
                >
                    <span class="material-symbols-rounded text-icon-dt-4">add</span>
                </button>
            </div>
        </div>
    {/if}
    <div class="flex flex-col w-full flex-1 items-start justify-start min-h-0 overflow-hidden">
        <div class="flex flex-col items-start justify-start {viewport.width < 1000 ? isMobile ? 'p-0 w-full' : 'p-2 w-full' : viewport.width > 1200 ? 'p-5 max-w-3/5 w-3/5 pl-8 pr-8' : 'p-5 max-w-4/5 w-4/5 pl-8 pr-8'} gap-4 h-full overflow-y-auto min-h-0 mx-auto">
            <ContentDisplay reportId={itemData.id} bind:title={data.title} author={data.author}
                            readingTime={data.readingTime} bind:content={data.content} isEditing={isEditing}
                            bind:activeBlock={activeBlock} configKey={page} isMobile={isMobile} />
        </div>
    </div>
</div>
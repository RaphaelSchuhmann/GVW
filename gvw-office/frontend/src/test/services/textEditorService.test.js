import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
    addBlock,
    updateBlockType,
    deleteBlock,
    insertImageBlock,
    bulkInsertImageBlocks,
    applyStyleInDOM,
    getActiveStylesInRange,
    isCaretAtBoundary
} from '../../services/textEditorService.svelte.js';
import { pendingImages, previewUrls } from '../../services/textEditorService.svelte.js';

// Mock dependencies
vi.mock('../../api/http.svelte.js', () => ({
    normalizeResponse: vi.fn((resp) => resp)
}));

vi.mock('../../api/globalErrorHandler.svelte.js', () => ({
    handleGlobalApiError: vi.fn(() => false)
}));

vi.mock('../../services/utils.js', () => ({
    generateUUID: vi.fn(() => 'test-uuid-123'),
    renameFile: vi.fn((file, name) => new File([file], name, { type: file.type })),
    sanitize: vi.fn((html) => html)
}));

describe('textEditorService.svelte.js', () => {
    beforeEach(() => {
        // Clear the maps before each test
        pendingImages.clear();
        previewUrls.clear();
        vi.clearAllMocks();
    });

    afterEach(() => {
        pendingImages.clear();
        previewUrls.clear();
    });

    describe('addBlock', () => {
        it('adds a block to empty items array', () => {
            const items = [];
            const id = addBlock(items, 0, 'test content', 'text');
            
            expect(items.length).toBe(1);
            expect(items[0].id).toBe('test-uuid-123');
            expect(items[0].type).toBe('text');
            expect(items[0].data).toBe('test content');
            expect(id).toBe('test-uuid-123');
        });

        it('adds a block after specified index when insertAtIndex is false', () => {
            const items = [
                { id: '1', type: 'text', data: 'first' }
            ];
            const id = addBlock(items, 0, 'second', 'text', false);
            
            expect(items.length).toBe(2);
            expect(items[1].data).toBe('second');
            expect(id).toBe('test-uuid-123');
        });

        it('adds a block at specified index when insertAtIndex is true', () => {
            const items = [
                { id: '1', type: 'text', data: 'first' },
                { id: '2', type: 'text', data: 'second' }
            ];
            const id = addBlock(items, 1, 'inserted', 'text', true);
            
            expect(items.length).toBe(3);
            expect(items[1].data).toBe('inserted');
            expect(id).toBe('test-uuid-123');
        });

        it('returns undefined for invalid block type', () => {
            const items = [];
            const id = addBlock(items, 0, 'content', 'invalid-type');
            
            expect(items.length).toBe(0);
            expect(id).toBeUndefined();
        });

        it('returns undefined for negative index when items is not empty', () => {
            const items = [{ id: '1', type: 'text', data: 'first' }];
            const id = addBlock(items, -1, 'content', 'text');
            
            expect(items.length).toBe(1);
            expect(id).toBeUndefined();
        });

        it('returns undefined for index -1', () => {
            const items = [{ id: '1', type: 'text', data: 'first' }];
            const id = addBlock(items, -1, 'content', 'text');
            
            expect(items.length).toBe(1);
            expect(id).toBeUndefined();
        });

        it('supports all valid block types', () => {
            const validTypes = ['text', 'image', 'file', 'blockquote', 'h1', 'h2', 'h3', 'h4'];
            
            validTypes.forEach(type => {
                const items = [];
                const id = addBlock(items, 0, 'content', type);
                expect(id).toBe('test-uuid-123');
                expect(items[0].type).toBe(type);
            });
        });
    });

    describe('updateBlockType', () => {
        it('updates block type for valid type', () => {
            const items = [{ id: '1', type: 'text', data: 'content' }];
            const id = updateBlockType('1', 'h1', items);
            
            expect(items[0].type).toBe('h1');
            expect(id).toBe('1');
        });

        it('toggles blockquote back to text if already blockquote', () => {
            const items = [{ id: '1', type: 'blockquote', data: 'content' }];
            const id = updateBlockType('1', 'blockquote', items);
            
            expect(items[0].type).toBe('text');
            expect(id).toBe('1');
        });

        it('returns undefined for invalid type', () => {
            const items = [{ id: '1', type: 'text', data: 'content' }];
            const id = updateBlockType('1', 'invalid', items);
            
            expect(items[0].type).toBe('text');
            expect(id).toBeUndefined();
        });

        it('returns undefined for empty type', () => {
            const items = [{ id: '1', type: 'text', data: 'content' }];
            const id = updateBlockType('1', '', items);
            
            expect(items[0].type).toBe('text');
            expect(id).toBeUndefined();
        });

        it('returns undefined for empty blockId', () => {
            const items = [{ id: '1', type: 'text', data: 'content' }];
            const id = updateBlockType('', 'h1', items);
            
            expect(items[0].type).toBe('text');
            expect(id).toBeUndefined();
        });

        it('returns undefined when block not found', () => {
            const items = [{ id: '1', type: 'text', data: 'content' }];
            const id = updateBlockType('nonexistent', 'h1', items);
            
            expect(items[0].type).toBe('text');
            expect(id).toBeUndefined();
        });

        it('returns undefined for empty items array', () => {
            const items = [];
            const id = updateBlockType('1', 'h1', items);
            
            expect(id).toBeUndefined();
        });
    });

    describe('deleteBlock', () => {
        it('deletes block by id', () => {
            const items = [
                { id: '1', type: 'text', data: 'first' },
                { id: '2', type: 'text', data: 'second' },
                { id: '3', type: 'text', data: 'third' }
            ];
            
            deleteBlock(items, '2');
            
            expect(items.length).toBe(2);
            expect(items.find(i => i.id === '2')).toBeUndefined();
        });

        it('removes image data from pendingImages and previewUrls when deleting image block', () => {
            const items = [
                { id: '1', type: 'image', data: 'temp-image-id' }
            ];

            pendingImages.set('temp-image-id', new File(['test'], 'test.jpg'));
            previewUrls.set('temp-image-id', 'blob:test-url');

            deleteBlock(items, '1', true);

            expect(pendingImages.has('temp-image-id')).toBe(false);
            expect(previewUrls.has('temp-image-id')).toBe(false);
        });

        it('returns early for empty blockId', () => {
            const items = [{ id: '1', type: 'text', data: 'content' }];
            const originalLength = items.length;
            
            deleteBlock(items, '');
            
            expect(items.length).toBe(originalLength);
        });

        it('returns early when block not found', () => {
            const items = [{ id: '1', type: 'text', data: 'content' }];
            const originalLength = items.length;
            
            deleteBlock(items, 'nonexistent');
            
            expect(items.length).toBe(originalLength);
        });
    });

    describe('insertImageBlock', () => {
        it('inserts image block into empty items array', () => {
            const file = new File(['test'], 'image.jpg', { type: 'image/jpeg' });
            const items = [];
            
            insertImageBlock(file, items, 0);
            
            expect(items.length).toBe(1);
            expect(items[0].type).toBe('image');
            expect(pendingImages.size).toBe(1);
            expect(previewUrls.size).toBe(1);
        });

        it('inserts image block after specified index', () => {
            const file = new File(['test'], 'image.jpg', { type: 'image/jpeg' });
            const items = [{ id: '1', type: 'text', data: 'first' }];
            
            insertImageBlock(file, items, 0);
            
            expect(items.length).toBe(2);
            expect(items[1].type).toBe('image');
        });

        it('creates temporary ID for image', () => {
            const file = new File(['test'], 'image.jpg', { type: 'image/jpeg' });
            const items = [];
            
            insertImageBlock(file, items, 0);
            
            const tempId = items[0].data;
            expect(tempId).toMatch(/^temp_/);
            expect(tempId).toContain('.jpg');
        });

        it('stores file in pendingImages with temp ID', () => {
            const file = new File(['test'], 'image.jpg', { type: 'image/jpeg' });
            const items = [];
            
            insertImageBlock(file, items, 0);
            
            const tempId = items[0].data;
            expect(pendingImages.has(tempId)).toBe(true);
        });

        it('creates object URL in previewUrls', () => {
            const file = new File(['test'], 'image.jpg', { type: 'image/jpeg' });
            const items = [];
            
            insertImageBlock(file, items, 0);
            
            const tempId = items[0].data;
            expect(previewUrls.has(tempId)).toBe(true);
            expect(previewUrls.get(tempId)).toMatch(/^blob:/);
        });
    });

    describe('bulkInsertImageBlocks', () => {
        it('inserts multiple image blocks', () => {
            const files = [
                new File(['test1'], 'image1.jpg', { type: 'image/jpeg' }),
                new File(['test2'], 'image2.jpg', { type: 'image/jpeg' })
            ];
            const items = [];
            
            bulkInsertImageBlocks(files, items, 0);
            
            expect(items.length).toBe(2);
            expect(items.every(item => item.type === 'image')).toBe(true);
        });

        it('inserts blocks after specified index', () => {
            const files = [
                new File(['test1'], 'image1.jpg', { type: 'image/jpeg' })
            ];
            const items = [{ id: '1', type: 'text', data: 'first' }];
            
            bulkInsertImageBlocks(files, items, 0);
            
            expect(items.length).toBe(2);
            expect(items[1].type).toBe('image');
        });

        it('stores all files in pendingImages', () => {
            const files = [
                new File(['test1'], 'image1.jpg', { type: 'image/jpeg' }),
                new File(['test2'], 'image2.jpg', { type: 'image/jpeg' })
            ];
            const items = [];
            
            bulkInsertImageBlocks(files, items, 0);
            
            expect(pendingImages.size).toBe(2);
        });

        it('creates object URLs for all images', () => {
            const files = [
                new File(['test1'], 'image1.jpg', { type: 'image/jpeg' }),
                new File(['test2'], 'image2.jpg', { type: 'image/jpeg' })
            ];
            const items = [];
            
            bulkInsertImageBlocks(files, items, 0);
            
            expect(previewUrls.size).toBe(2);
        });

        it('assigns unique IDs to each block', () => {
            const files = [
                new File(['test1'], 'image1.jpg', { type: 'image/jpeg' }),
                new File(['test2'], 'image2.jpg', { type: 'image/jpeg' })
            ];
            const items = [];
            
            bulkInsertImageBlocks(files, items, 0);
            
            const ids = items.map(item => item.id);
            expect(new Set(ids).size).toBe(2);
        });
    });

    describe('applyStyleInDOM', () => {
        beforeEach(() => {
            // Mock document.execCommand
            document.execCommand = vi.fn();
            // Mock getSelection
            globalThis.getSelection = vi.fn(() => ({
                rangeCount: 1
            }));
        });

        it('applies bold style for "strong" action', () => {
            applyStyleInDOM('strong');
            expect(document.execCommand).toHaveBeenCalledWith('bold', false, null);
        });

        it('applies bold style for "b" action', () => {
            applyStyleInDOM('b');
            expect(document.execCommand).toHaveBeenCalledWith('bold', false, null);
        });

        it('applies italic style for "em" action', () => {
            applyStyleInDOM('em');
            expect(document.execCommand).toHaveBeenCalledWith('italic', false, null);
        });

        it('applies italic style for "i" action', () => {
            applyStyleInDOM('i');
            expect(document.execCommand).toHaveBeenCalledWith('italic', false, null);
        });

        it('applies underline style for "u" action', () => {
            applyStyleInDOM('u');
            expect(document.execCommand).toHaveBeenCalledWith('underline', false, null);
        });

        it('returns early for empty action', () => {
            applyStyleInDOM('');
            expect(document.execCommand).not.toHaveBeenCalled();
        });

        it('returns early when no selection', () => {
            globalThis.getSelection = vi.fn(() => null);
            applyStyleInDOM('bold');
            expect(document.execCommand).not.toHaveBeenCalled();
        });

        it('returns early when selection has no ranges', () => {
            globalThis.getSelection = vi.fn(() => ({
                rangeCount: 0
            }));
            applyStyleInDOM('bold');
            expect(document.execCommand).not.toHaveBeenCalled();
        });
    });

    describe('getActiveStylesInRange', () => {
        it('returns correct style object structure', () => {
            const mockRange = {
                collapsed: true,
                startContainer: { nodeType: Node.TEXT_NODE, parentElement: null }
            };
            
            const result = getActiveStylesInRange(mockRange);
            
            expect(result).toHaveProperty('isBold');
            expect(result).toHaveProperty('isItalic');
            expect(result).toHaveProperty('isUnderline');
            expect(typeof result.isBold).toBe('boolean');
            expect(typeof result.isItalic).toBe('boolean');
            expect(typeof result.isUnderline).toBe('boolean');
        });

        it('handles collapsed range (caret position)', () => {
            const mockRange = {
                collapsed: true,
                startContainer: { nodeType: Node.TEXT_NODE, parentElement: null }
            };
            
            const result = getActiveStylesInRange(mockRange);
            
            expect(result).toBeDefined();
        });

        it('handles non-collapsed range (text selection)', () => {
            const container = document.createElement('div');

            const strong = document.createElement('strong');
            strong.textContent = 'bold text';

            container.appendChild(strong);
            document.body.appendChild(container);

            const range = document.createRange();
            range.selectNodeContents(strong);

            const result = getActiveStylesInRange(range);

            expect(result).toBeDefined();
            expect(result.isBold).toBe(true);

            container.remove();
        });
    });

    describe('isCaretAtBoundary', () => {
        beforeEach(() => {
            // Mock window.getSelection
            global.window = {
                getSelection: vi.fn(() => ({
                    rangeCount: 1,
                    getRangeAt: vi.fn(() => ({
                        getClientRects: vi.fn(() => [{ top: 100, bottom: 110 }])
                    }))
                }))
            };
        });

        it('returns false when no selection', () => {
            global.window.getSelection = vi.fn(() => ({
                rangeCount: 0
            }));
            
            const el = { getBoundingClientRect: vi.fn(() => ({ top: 0, bottom: 200 })) };
            const result = isCaretAtBoundary(el, 'top');
            
            expect(result).toBe(false);
        });

        it('checks top boundary correctly', () => {
            const el = { getBoundingClientRect: vi.fn(() => ({ top: 95, bottom: 200 })) };
            const result = isCaretAtBoundary(el, 'top');
            
            expect(result).toBe(true);
        });

        it('checks bottom boundary correctly', () => {
            const el = { getBoundingClientRect: vi.fn(() => ({ top: 0, bottom: 115 })) };
            const result = isCaretAtBoundary(el, 'bottom');
            
            expect(result).toBe(true);
        });

        it('returns true when cursor rect is undefined', () => {
            global.window.getSelection = vi.fn(() => ({
                rangeCount: 1,
                getRangeAt: vi.fn(() => ({
                    getClientRects: vi.fn(() => [])
                }))
            }));
            
            const el = { getBoundingClientRect: vi.fn(() => ({ top: 0, bottom: 200 })) };
            const result = isCaretAtBoundary(el, 'top');
            
            expect(result).toBe(true);
        });

        it('returns false when not near boundary', () => {
            const el = { getBoundingClientRect: vi.fn(() => ({ top: 0, bottom: 200 })) };
            const result = isCaretAtBoundary(el, 'top');
            
            expect(result).toBe(false);
        });
    });
});

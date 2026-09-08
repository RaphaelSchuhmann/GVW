import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
    capitalizeWords,
    determineChoirType,
    triggerFileDownload,
    renameFile,
    sanitize,
    generateUUID
} from '../../services/utils.js';

describe('utils.js', () => {
    describe('capitalizeWords', () => {
        it('capitalizes first letter of each word', () => {
            expect(capitalizeWords('hello world')).toBe('Hello World');
            expect(capitalizeWords('test string')).toBe('Test String');
        });

        it('handles spaces, hyphens, and underscores as word separators', () => {
            expect(capitalizeWords('hello-world_test')).toBe('Hello-World_Test');
            expect(capitalizeWords('multiple   spaces')).toBe('Multiple   Spaces');
        });

        it('converts rest of string to lowercase', () => {
            expect(capitalizeWords('HELLO WORLD')).toBe('Hello World');
            expect(capitalizeWords('TeSt StRiNg')).toBe('Test String');
        });

        it('handles empty string', () => {
            expect(capitalizeWords('')).toBe('');
        });

        it('handles single word', () => {
            expect(capitalizeWords('hello')).toBe('Hello');
        });
    });

    describe('determineChoirType', () => {
        it('returns true when string contains a number', () => {
            expect(determineChoirType('choir1')).toBe(true);
            expect(determineChoirType('test123')).toBe(true);
            expect(determineChoirType('abc456def')).toBe(true);
        });

        it('returns false when string does not contain a number', () => {
            expect(determineChoirType('choir')).toBe(false);
            expect(determineChoirType('test')).toBe(false);
            expect(determineChoirType('abcdef')).toBe(false);
        });

        it('handles empty string', () => {
            expect(determineChoirType('')).toBe(false);
        });
    });

    describe('triggerFileDownload', () => {
        let mockUrl;
        let mockAnchor;

        beforeEach(() => {
            vi.useFakeTimers();

            mockUrl = 'blob:test-url';
            mockAnchor = {
                href: '',
                download: '',
                click: vi.fn()
            };

            global.URL.createObjectURL = vi.fn(() => mockUrl);
            global.URL.revokeObjectURL = vi.fn();
            global.document.createElement = vi.fn(() => mockAnchor);
        });

        afterEach(() => {
            vi.useRealTimers();
            vi.restoreAllMocks();
        });


        it('creates object URL from blob', () => {
            const blob = new Blob(['test'], { type: 'application/zip' });
            triggerFileDownload(blob, 'test-file');
            
            expect(URL.createObjectURL).toHaveBeenCalledWith(blob);
        });

        it('creates anchor element with correct attributes', () => {
            const blob = new Blob(['test'], { type: 'application/zip' });
            triggerFileDownload(blob, 'test-file');
            
            expect(document.createElement).toHaveBeenCalledWith('a');
            expect(mockAnchor.href).toBe(mockUrl);
            expect(mockAnchor.download).toBe('test-file.zip');
        });

        it('triggers click on anchor element', () => {
            const blob = new Blob(['test'], { type: 'application/zip' });
            triggerFileDownload(blob, 'test-file');
            
            expect(mockAnchor.click).toHaveBeenCalled();
        });

        it('revokes object URL after click', () => {
            const blob = new Blob(['test'], { type: 'application/zip' });
            triggerFileDownload(blob, 'test-file');
            
            // setTimeout with 0 delay should execute in next tick
            vi.advanceTimersByTime(0);
            expect(URL.revokeObjectURL).toHaveBeenCalledWith(mockUrl);
        });
    });

    describe('renameFile', () => {
        it('creates a new File with updated name', () => {
            const originalFile = new File(['content'], 'old-name.txt', { type: 'text/plain' });
            const renamed = renameFile(originalFile, 'new-name.txt');
            
            expect(renamed.name).toBe('new-name.txt');
            expect(renamed.type).toBe('text/plain');
        });

        it('preserves original file type', () => {
            const originalFile = new File(['content'], 'old-name.jpg', { type: 'image/jpeg' });
            const renamed = renameFile(originalFile, 'new-name.jpg');
            
            expect(renamed.type).toBe('image/jpeg');
        });

        it('preserves file content', async () => {
            const content = 'test content';
            const originalFile = new File(
                [content],
                'old-name.txt',
                { type: 'text/plain' }
            );

            const renamed = renameFile(originalFile, 'new-name.txt');

            const result = await new Promise((resolve, reject) => {
                const reader = new FileReader();

                reader.onload = () => resolve(reader.result);
                reader.onerror = reject;

                reader.readAsText(renamed);
            });

            expect(result).toBe(content);
        });
    });

    describe('sanitize', () => {
        it('returns empty string for falsy input', () => {
            expect(sanitize('')).toBe('');
            expect(sanitize(null)).toBe('');
            expect(sanitize(undefined)).toBe('');
        });

        it('normalizes whitespace characters', () => {
            expect(sanitize('test&nbsp;text')).toBe('test text');
            expect(sanitize('test\u00A0text')).toBe('test text');
        });

        it('removes trailing spaces from lines', () => {
            expect(sanitize('line1   \nline2')).toBe('line1\nline2');
        });

        it('removes leading spaces after newlines', () => {
            expect(sanitize('line1\n   line2')).toBe('line1\nline2');
        });

        it('keeps allowed tags', () => {
            const input = '<b>bold</b> <i>italic</i> <u>underline</u>';
            expect(sanitize(input)).toBe(input);
        });

        it('removes dangerous HTML elements and attributes', () => {
            const result = sanitize(
                '<script>alert("xss")</script>' +
                '<iframe src="evil">evil content</iframe>' +
                '<object data="evil">evil content</object>' +
                '<b onclick="alert(1)">safe text</b>'
            );

            expect(result).not.toContain('<script');
            expect(result).not.toContain('<iframe');
            expect(result).not.toContain('<object');
            expect(result).not.toContain('onclick');
            expect(result).toContain('safe text');
        });

        it('keeps safe text content', () => {
            expect(sanitize('<div>content</div>')).toBe('content');
            expect(sanitize('<p>Hello <strong>world</strong></p>')).toBe('Hello <strong>world</strong>');
        });

        it('removes disallowed attributes', () => {
            const input = '<b onclick="alert()">bold</b>';
            const result = sanitize(input);
            expect(result).not.toContain('onclick');
        });

        it('keeps allowed attributes', () => {
            const input = '<a href="https://example.com" class="link">link</a>';
            expect(sanitize(input)).toContain('href');
            expect(sanitize(input)).toContain('class');
        });

        it('handles nested tags', () => {
            const input = '<b><i>bold italic</i></b>';
            expect(sanitize(input)).toBe(input);
        });

        it('trims final result', () => {
            expect(sanitize('  <b>test</b>  ')).toBe('<b>test</b>');
        });

        it('handles complex HTML with mixed tags', () => {
            const input = '<p>paragraph</p><b>bold</b><script>alert(1)</script>';
            const result = sanitize(input);
            expect(result).toContain('paragraph');
            expect(result).toContain('bold');
            expect(result).not.toContain('script');
        });
    });

    describe('generateUUID', () => {
        it('generates a valid UUID v4 string', () => {
            const uuid = generateUUID();
            expect(uuid).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
        });

        it('generates unique UUIDs', () => {
            const uuid1 = generateUUID();
            const uuid2 = generateUUID();
            expect(uuid1).not.toBe(uuid2);
        });

        it('generates UUID of correct length', () => {
            const uuid = generateUUID();
            expect(uuid.length).toBe(36);
        });

        it('has correct format with five groups separated by hyphens', () => {
            const uuid = generateUUID();
            const parts = uuid.split('-');
            expect(parts.length).toBe(5);
            expect(parts[0].length).toBe(8);
            expect(parts[1].length).toBe(4);
            expect(parts[2].length).toBe(4);
            expect(parts[3].length).toBe(4);
            expect(parts[4].length).toBe(12);
        });
    });
});

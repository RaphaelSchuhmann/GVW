import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { initSSE, teardownEventSource } from '../../services/sse-handler.js';
import { EventSourcePolyfill } from 'event-source-polyfill';
import { lastRefresh } from '../../stores/sseStore.svelte.js';
import { auth } from '../../stores/auth.svelte.js';
import { logout } from '../../services/userService.svelte.js';
import { push } from 'svelte-spa-router';

let mockEventSource;

vi.mock('../../stores/sseStore.svelte.js', () => ({
    lastRefresh: {
        EVENTS: 0,
        MEMBERS: 0,
        LIBRARY: 0
    }
}));

vi.mock('../../stores/auth.svelte.js', () => ({
    auth: {
        token: 'test-token'
    }
}));

vi.mock('../../services/userService.svelte.js', () => ({
    logout: vi.fn()
}));

vi.mock('svelte-spa-router', () => ({
    push: vi.fn()
}));

vi.mock('event-source-polyfill', () => ({
    EventSourcePolyfill: vi.fn(function () {
        return mockEventSource;
    })
}));

describe('sse-handler.js - Application/State Logic', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        mockEventSource = {
            addEventListener: vi.fn(),
            onerror: null,
            close: vi.fn()
        };

        lastRefresh.EVENTS = 0;
        lastRefresh.MEMBERS = 0;
        lastRefresh.LIBRARY = 0;
    });

    afterEach(() => {
        teardownEventSource();
    });

    describe('initSSE - Application Logic', () => {
        it('prevents duplicate SSE connections', () => {
            initSSE();

            const callCount = EventSourcePolyfill.mock.calls.length;

            initSSE();

            expect(EventSourcePolyfill.mock.calls.length).toBe(callCount);
        });

        it('sets up refresh event listener', () => {
            initSSE();

            expect(mockEventSource.addEventListener).toHaveBeenCalledWith(
                'refresh',
                expect.any(Function)
            );
        });

        it('updates lastRefresh store on refresh event', () => {
            initSSE();

            const refreshHandler = mockEventSource.addEventListener.mock.calls.find(
                call => call[0] === 'refresh'
            )[1];

            refreshHandler({ data: 'EVENTS' });

            expect(lastRefresh.EVENTS).toBeGreaterThan(0);
        });

        it('handles refresh event for unknown type gracefully', () => {
            initSSE();

            const refreshHandler = mockEventSource.addEventListener.mock.calls.find(
                call => call[0] === 'refresh'
            )[1];

            expect(() => {
                refreshHandler({ data: 'unknown-type' });
            }).not.toThrow();
        });

        it('sets up error handler', () => {
            initSSE();

            expect(mockEventSource.onerror).not.toBeNull();
            expect(typeof mockEventSource.onerror).toBe('function');
        });

        it('calls logout on 401 error', async () => {
            initSSE();

            await mockEventSource.onerror({ status: 401 });

            expect(logout).toHaveBeenCalled();
        });

        it('calls logout on 403 error', async () => {
            initSSE();

            await mockEventSource.onerror({ status: 403 });

            expect(logout).toHaveBeenCalled();
        });

        it('redirects to login page on auth error', async () => {
            initSSE();

            await mockEventSource.onerror({ status: 401 });

            expect(push).toHaveBeenCalledWith('/?cpwErr=false');
        });

        it('does not call logout on non-auth errors', async () => {
            initSSE();

            await mockEventSource.onerror({ status: 500 });

            expect(logout).not.toHaveBeenCalled();
        });

        it('does not redirect on non-auth errors', async () => {
            initSSE();

            await mockEventSource.onerror({ status: 500 });

            expect(push).not.toHaveBeenCalled();
        });

        it('uses auth token in connection headers', () => {
            initSSE();

            expect(EventSourcePolyfill).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    headers: expect.objectContaining({
                        Authorization: `Bearer ${auth.token}`
                    })
                })
            );
        });
    });

    describe('teardownEventSource - Application Logic', () => {
        it('closes active SSE connection', () => {
            initSSE();

            teardownEventSource();

            expect(mockEventSource.close).toHaveBeenCalled();
        });

        it('sets eventSource to null after teardown', () => {
            initSSE();

            teardownEventSource();

            const callCountBefore = EventSourcePolyfill.mock.calls.length;

            initSSE();

            expect(EventSourcePolyfill.mock.calls.length).toBe(
                callCountBefore + 1
            );
        });

        it('handles teardown when no connection exists', () => {
            expect(() => teardownEventSource()).not.toThrow();
        });

        it('can be called multiple times safely', () => {
            initSSE();

            teardownEventSource();
            teardownEventSource();
            teardownEventSource();

            expect(mockEventSource.close).toHaveBeenCalledTimes(1);
        });

        it('allows re-initialization after teardown', () => {
            initSSE();
            teardownEventSource();

            const callCountBefore = EventSourcePolyfill.mock.calls.length;

            initSSE();

            expect(EventSourcePolyfill.mock.calls.length).toBe(
                callCountBefore + 1
            );
        });
    });

    describe('Refresh Event Handler - State Logic', () => {
        it('updates timestamp for valid refresh type', () => {
            initSSE();

            const refreshHandler = mockEventSource.addEventListener.mock.calls.find(
                call => call[0] === 'refresh'
            )[1];

            refreshHandler({ data: 'MEMBERS' });

            expect(lastRefresh.MEMBERS).toBeGreaterThan(0);
        });

        it('handles multiple refresh events', () => {
            initSSE();

            const refreshHandler = mockEventSource.addEventListener.mock.calls.find(
                call => call[0] === 'refresh'
            )[1];

            refreshHandler({ data: 'EVENTS' });
            refreshHandler({ data: 'MEMBERS' });
            refreshHandler({ data: 'LIBRARY' });

            expect(lastRefresh.EVENTS).toBeGreaterThan(0);
            expect(lastRefresh.MEMBERS).toBeGreaterThan(0);
            expect(lastRefresh.LIBRARY).toBeGreaterThan(0);
        });

        it('ignores refresh events for undefined types', () => {
            initSSE();

            const refreshHandler = mockEventSource.addEventListener.mock.calls.find(
                call => call[0] === 'refresh'
            )[1];

            const originalValues = { ...lastRefresh };

            refreshHandler({ data: 'nonexistent-type' });

            expect(lastRefresh).toEqual(originalValues);
        });
    });

    describe('Error Handler - State Logic', () => {
        it('handles error without status property', async () => {
            initSSE();

            await expect(
                mockEventSource.onerror({})
            ).resolves.not.toThrow();
        });

        it('handles error with null status', async () => {
            initSSE();

            await mockEventSource.onerror({ status: null });

            expect(logout).not.toHaveBeenCalled();
        });

        it('handles error with undefined status', async () => {
            initSSE();

            await mockEventSource.onerror({ status: undefined });

            expect(logout).not.toHaveBeenCalled();
        });
    });
});
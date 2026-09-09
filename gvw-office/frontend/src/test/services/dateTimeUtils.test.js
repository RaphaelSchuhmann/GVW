import { describe, it, expect } from "vitest";
import {
    daysInMonth,
    firstWeekdayOfMonth,
    isToday,
    formatISODateString,
    germanDateToISO,
    getLastDayOfCurrentMonth,
    isISOString,
    yearToISOString,
    getYearFromISOString,
    removeMillisecondsFromTimeStamp,
    getRoundedTime
} from "../../services/dateTimeUtils";

describe("dateTimeUtils.svelte.js", () => {
    describe("daysInMonth", () => {
        it("returns the correct number of days for a regular month", () => {
            expect(daysInMonth(2026, 0)).toBe(31);
            expect(daysInMonth(2026, 3)).toBe(30);
        });

        it("handles February correctly for leap years", () => {
            expect(daysInMonth(2024, 1)).toBe(29);
            expect(daysInMonth(2026, 1)).toBe(28);
        });
    });

    describe("firstWeekdayOfMonth", () => {
        it("returns Monday as 0", () => {
            expect(firstWeekdayOfMonth(2026, 5)).toBe(0);
        });

        it("returns Sunday as 6", () => {
            expect(firstWeekdayOfMonth(2026, 7)).toBe(5);
            expect(firstWeekdayOfMonth(2026, 10)).toBe(6);
        });
    });

    describe("isToday", () => {
        beforeEach(() => {
            vi.useFakeTimers();
            vi.setSystemTime(new Date("2026-04-10T12:00:00Z"));
        });

        afterEach(() => {
            vi.useRealTimers();
        });

        it("returns true for today's date", () => {
            const today = new Date();

            expect(
                isToday(
                    today.getDate(),
                    today.getMonth(),
                    today.getFullYear()
                )
            ).toBe(true);
        });

        it("returns false for yesterday", () => {
            const yesterday = new Date();
            yesterday.setDate(yesterday.getDate() - 1);

            expect(
                isToday(
                    yesterday.getDate(),
                    yesterday.getMonth(),
                    yesterday.getFullYear()
                )
            ).toBe(false);
        });
    });

    describe("formatISODateString", () => {
        it("formats a valid ISO date using the German format", () => {
            expect(formatISODateString("2026-04-10T11:55:00Z"))
                .toBe("10.04.2026");
        });

        it("returns an error message for an empty input", () => {
            expect(formatISODateString("")).toBe("Ungültiges Datum");
        });

        it("returns an error message for an invalid date", () => {
            expect(formatISODateString("not-a-date"))
                .toBe("Ungültiges Datum");
        });
    });

    describe("germanDateToISO", () => {
        it("converts a German date to an ISO string", () => {
            expect(germanDateToISO("09.04.2026"))
                .toBe("2026-04-09T00:00:00.000Z");
        });

        it("returns null for empty input", () => {
            expect(germanDateToISO("")).toBeNull();
            expect(germanDateToISO(null)).toBeNull();
        });

        it("returns null for malformed input", () => {
            expect(germanDateToISO("09.04")).toBeNull();
        });
    });

    describe("getLastDayOfCurrentMonth", () => {
        it("returns the last day of the current month", () => {
            const today = new Date();
            const result = getLastDayOfCurrentMonth();

            expect(result.getFullYear()).toBe(today.getFullYear());
            expect(result.getMonth()).toBe(today.getMonth());
            expect(result.getDate())
                .toBe(daysInMonth(today.getFullYear(), today.getMonth()));
        });
    });

    describe("isISOString", () => {
        it("returns true for a valid ISO timestamp", () => {
            expect(isISOString("2026-04-09T00:00:00.000Z")).toBe(true);
        });

        it("returns false when the input is not a string", () => {
            expect(isISOString(null)).toBe(false);
            expect(isISOString(123)).toBe(false);
        });

        it("returns false for strings without ISO structure", () => {
            expect(isISOString("2026-04-09")).toBe(false);
            expect(isISOString("09.04.2026")).toBe(false);
        });

        it("returns false for an invalid ISO date", () => {
            expect(isISOString("2026-99-99T00:00:00.000Z")).toBe(false);
        });
    });

    describe("yearToISOString", () => {
        it("converts a year to January 1st in ISO format", () => {
            expect(yearToISOString(2026))
                .toBe("2026-01-01T00:00:00.000Z");
        });

        it("accepts a numeric year string", () => {
            expect(yearToISOString("2026"))
                .toBe("2026-01-01T00:00:00.000Z");
        });

        it("returns an empty string for invalid input", () => {
            expect(yearToISOString("")).toBe("");
            expect(yearToISOString(null)).toBe("");
            expect(yearToISOString("26")).toBe("");
            expect(yearToISOString("abcd")).toBe("");
        });
    });

    describe("getYearFromISOString", () => {
        it("extracts the year from a valid ISO date", () => {
            expect(getYearFromISOString("2026-01-01T00:00:00.000Z"))
                .toBe("2026");
        });

        it("returns an empty string for invalid input", () => {
            expect(getYearFromISOString("")).toBe("");
            expect(getYearFromISOString("not-a-date")).toBe("");
        });
    });

    describe("removeMillisecondsFromTimeStamp", () => {
        it("removes milliseconds from a timestamp", () => {
            expect(
                removeMillisecondsFromTimeStamp(
                    "2026-01-01T12:34:56.789Z"
                )
            ).toBe("2026-01-01T12:34:56");
        });

        it("returns the timestamp unchanged when no milliseconds exist", () => {
            const timestamp = "2026-01-01T12:34:56Z";

            expect(removeMillisecondsFromTimeStamp(timestamp))
                .toBe(timestamp);
        });

        it("returns an empty string for nullish input", () => {
            expect(removeMillisecondsFromTimeStamp(null)).toBe("");
            expect(removeMillisecondsFromTimeStamp(undefined)).toBe("");
        });

        it("returns non-string values unchanged", () => {
            expect(removeMillisecondsFromTimeStamp(123)).toBe(123);
            expect(removeMillisecondsFromTimeStamp(null)).toBe("");
            expect(removeMillisecondsFromTimeStamp(undefined)).toBe("");
        });
    });

    describe("getRoundedTime", () => {
        describe('30-minute interval rounding boundaries', () => {
            it('rounds 0–14 minutes down to 0 minutes', () => {
                const dateAt0 = new Date('2026-09-09T15:00:00');
                const dateAt10 = new Date('2026-09-09T15:10:00');
                const dateAt14 = new Date('2026-09-09T15:14:59');

                expect(getRoundedTime(dateAt0)).toEqual({ hours: 15, minutes: 0 });
                expect(getRoundedTime(dateAt10)).toEqual({ hours: 15, minutes: 0 });
                expect(getRoundedTime(dateAt14)).toEqual({ hours: 15, minutes: 0 });
            });

            it('rounds 15–44 minutes to 30 minutes', () => {
                const dateAt15 = new Date('2026-09-09T15:15:00');
                expect(getRoundedTime(dateAt15)).toEqual({ hours: 15, minutes: 30 });

                const dateAt16 = new Date('2026-09-09T15:16:00');
                expect(getRoundedTime(dateAt16)).toEqual({ hours: 15, minutes: 30 });

                const dateAt40 = new Date('2026-09-09T15:40:00');
                expect(getRoundedTime(dateAt40)).toEqual({ hours: 15, minutes: 30 });

                const dateAt44 = new Date('2026-09-09T15:44:59');
                expect(getRoundedTime(dateAt44)).toEqual({ hours: 15, minutes: 30 });
            });

            it('rounds 45–59 minutes up to 60 minutes and advances the hour', () => {
                const dateAt45 = new Date('2026-09-09T15:45:00');
                expect(getRoundedTime(dateAt45)).toEqual({ hours: 16, minutes: 0 });

                const dateAt46 = new Date('2026-09-09T15:46:00');
                expect(getRoundedTime(dateAt46)).toEqual({ hours: 16, minutes: 0 });

                const dateAt59 = new Date('2026-09-09T15:59:59');
                expect(getRoundedTime(dateAt59)).toEqual({ hours: 16, minutes: 0 });
            });
        });

        describe('edge cases and rollover boundaries', () => {
            it('handles hour rollover across midnight (23:45+ -> 00:00)', () => {
                const lateNight = new Date('2026-09-09T23:50:00');
                expect(getRoundedTime(lateNight)).toEqual({ hours: 0, minutes: 0 });
            });

            it('handles midnight boundary (00:10 -> 00:00)', () => {
                const midnight = new Date('2026-09-09T00:10:00');
                expect(getRoundedTime(midnight)).toEqual({ hours: 0, minutes: 0 });
            });

            it('does not mutate the original Date object passed into it', () => {
                const input = new Date('2026-09-09T15:46:00');
                const originalTimestamp = input.getTime();

                getRoundedTime(input);

                expect(input.getTime()).toBe(originalTimestamp);
            });
        });

        describe('default parameter behavior', () => {
            beforeEach(() => {
                vi.useFakeTimers();
            });

            afterEach(() => {
                vi.useRealTimers();
            });

            it('defaults to current time when no date argument is provided', () => {
                // Mock system time to 10:18 AM
                vi.setSystemTime(new Date('2026-09-09T10:18:00'));

                expect(getRoundedTime()).toEqual({ hours: 10, minutes: 30 });
            });
        });
    });
});
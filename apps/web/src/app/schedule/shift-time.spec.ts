import { lisbonInstant, lisbonTime, placeSegments, shiftSegments } from './shift-time';

describe('shift-time', () => {
  it('turns a Lisbon date and time into the UTC instant the API keeps, in winter and in summer', () => {
    expect(lisbonInstant('2026-11-24', '08:00')).toBe('2026-11-24T08:00:00Z');
    expect(lisbonInstant('2027-06-30', '08:00')).toBe('2027-06-30T07:00:00Z');
    expect(lisbonInstant('2027-07-01', '00:30')).toBe('2027-06-30T23:30:00Z');
    // The clocks go forward at 01:00 on 2027-03-28: 00:30 is still winter time, 02:30 already summer time.
    expect(lisbonInstant('2027-03-28', '00:30')).toBe('2027-03-28T00:30:00Z');
    expect(lisbonInstant('2027-03-28', '02:30')).toBe('2027-03-28T01:30:00Z');
  });

  it('reads an instant on the Lisbon clock, in winter (UTC) and in summer (UTC+1)', () => {
    expect(lisbonTime('2026-11-24T12:00:00Z')).toEqual({ date: '2026-11-24', minutes: 720, time: '12:00' });
    expect(lisbonTime('2027-06-30T23:30:00Z')).toEqual({ date: '2027-07-01', minutes: 30, time: '00:30' });
  });

  it('keeps a day shift in one piece, labelled "From … to …"', () => {
    expect(shiftSegments('2026-11-24T12:00:00Z', '2026-11-24T13:30:00Z')).toEqual([
      { date: '2026-11-24', from: 720, to: 810, label: 'From 12:00 to 13:30', continued: false },
    ]);
  });

  it('splits a night shift at midnight, with the whole shift as the label on both days', () => {
    expect(shiftSegments('2026-11-24T20:00:00Z', '2026-11-25T08:00:00Z')).toEqual([
      { date: '2026-11-24', from: 1200, to: 1440, label: 'From 20:00 to 08:00', continued: false },
      { date: '2026-11-25', from: 0, to: 480, label: 'From 20:00 to 08:00', continued: true },
    ]);
  });

  it('ends a shift at midnight on its own day, with nothing on the next', () => {
    expect(shiftSegments('2026-11-24T20:00:00Z', '2026-11-25T00:00:00Z')).toEqual([
      { date: '2026-11-24', from: 1200, to: 1440, label: 'From 20:00 to 00:00', continued: false },
    ]);
  });

  it('places nothing for a shift that ends before it starts', () => {
    expect(shiftSegments('2026-11-24T13:00:00Z', '2026-11-24T12:00:00Z')).toEqual([]);
  });

  it('puts overlapping shifts side by side and the others full width', () => {
    const segment = (item: string, from: number, to: number) => ({
      item,
      date: '2026-11-24',
      from,
      to,
      label: '',
      continued: false,
    });
    const placed = placeSegments([segment('b', 600, 720), segment('a', 480, 720), segment('c', 720, 780)]);

    expect(placed.map((part) => [part.item, part.leftPercent, part.widthPercent])).toEqual([
      ['a', 0, 50],
      ['b', 50, 50],
      ['c', 0, 50],
    ]);
    expect(placed[0].topPercent).toBeCloseTo((480 / 1440) * 100);
    expect(placed[0].heightPercent).toBeCloseTo((240 / 1440) * 100);
  });
});

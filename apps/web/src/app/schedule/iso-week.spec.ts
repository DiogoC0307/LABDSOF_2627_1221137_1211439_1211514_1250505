import {
  addDays,
  addMonths,
  firstOfMonth,
  isoWeek,
  isoWeekNumber,
  longDay,
  mondayOf,
  monthMondays,
  monthTitle,
  shortDay,
  todayInLisbon,
  weekDays,
} from './iso-week';

describe('iso-week', () => {
  it('finds the Monday of any day of the week, Sunday included', () => {
    expect(mondayOf('2026-11-23')).toBe('2026-11-23');
    expect(mondayOf('2026-11-26')).toBe('2026-11-23');
    expect(mondayOf('2026-11-29')).toBe('2026-11-23');
    expect(mondayOf('2027-01-03')).toBe('2026-12-28');
  });

  it('lists the seven days from Monday to Sunday, across a month', () => {
    expect(weekDays('2026-11-30')).toEqual([
      '2026-11-30',
      '2026-12-01',
      '2026-12-02',
      '2026-12-03',
      '2026-12-04',
      '2026-12-05',
      '2026-12-06',
    ]);
  });

  it('keeps 7 days in the week of the clock change (2027-03-28 in Lisbon)', () => {
    expect(weekDays('2027-03-22').at(-1)).toBe('2027-03-28');
    expect(weekDays('2027-03-29')[0]).toBe('2027-03-29');
  });

  it('names the ISO week as the API expects, by the year of its Thursday', () => {
    expect(isoWeek('2026-11-23')).toBe('2026-W48');
    expect(isoWeek('2025-12-29')).toBe('2026-W01');
    expect(isoWeek('2026-12-28')).toBe('2026-W53');
    expect(isoWeek('2027-01-04')).toBe('2027-W01');
  });

  it('moves by days and weeks across months and years', () => {
    expect(addDays('2026-11-23', 7)).toBe('2026-11-30');
    expect(addDays('2026-11-23', -7)).toBe('2026-11-16');
    expect(addDays('2026-12-28', 7)).toBe('2027-01-04');
  });

  it('numbers the week by the year of its Thursday', () => {
    expect(isoWeekNumber('2026-11-23')).toBe(48);
    expect(isoWeekNumber('2025-12-29')).toBe(1);
    expect(isoWeekNumber('2026-12-28')).toBe(53);
  });

  it('moves by months across years', () => {
    expect(firstOfMonth('2026-11-25')).toBe('2026-11-01');
    expect(addMonths('2026-11-01', 1)).toBe('2026-12-01');
    expect(addMonths('2026-12-01', 1)).toBe('2027-01-01');
    expect(addMonths('2026-01-01', -1)).toBe('2025-12-01');
  });

  it('lists the calendar rows of a month: every week that shows one of its days', () => {
    // November 2026 starts on a Sunday and ends on a Monday: six rows.
    expect(monthMondays('2026-11-01')).toEqual([
      '2026-10-26',
      '2026-11-02',
      '2026-11-09',
      '2026-11-16',
      '2026-11-23',
      '2026-11-30',
    ]);
    // February 2027 starts on a Monday and has 28 days: four rows.
    expect(monthMondays('2027-02-01')).toEqual(['2027-02-01', '2027-02-08', '2027-02-15', '2027-02-22']);
    expect(monthTitle('2026-11-01')).toBe('November 2026');
  });

  it('labels a day short for the column and long for screen readers', () => {
    expect(shortDay('2026-11-23')).toBe('Mon 23');
    expect(longDay('2026-11-23')).toBe('Monday 23 November');
  });

  it("takes today's date in Lisbon, not in UTC or on the device", () => {
    // 23:30 UTC on 2027-06-30 is already 00:30 on 2027-07-01 in Lisbon (summer, UTC+1).
    expect(todayInLisbon(new Date('2027-06-30T23:30:00Z'))).toBe('2027-07-01');
    // In winter Lisbon is on UTC.
    expect(todayInLisbon(new Date('2026-11-29T23:30:00Z'))).toBe('2026-11-29');
  });
});

import { relativeWeekLabel, weekRange } from './week-label';

// Week selector labels. This week starts on Monday 2026-11-23.
describe('week-label', () => {
  const thisMonday = '2026-11-23';

  describe('relativeWeekLabel', () => {
    it('names the week that holds today "This week"', () => {
      expect(relativeWeekLabel('2026-11-23', thisMonday)).toBe('This week');
    });

    it('names the week after "Next week"', () => {
      expect(relativeWeekLabel('2026-11-30', thisMonday)).toBe('Next week');
    });

    it('names the week before "Last week"', () => {
      expect(relativeWeekLabel('2026-11-16', thisMonday)).toBe('Last week');
    });

    it('counts the weeks ahead, also into the next year', () => {
      expect(relativeWeekLabel('2026-12-07', thisMonday)).toBe('In 2 weeks');
      expect(relativeWeekLabel('2027-01-04', thisMonday)).toBe('In 6 weeks');
    });

    it('counts the weeks back, across a change of summer time', () => {
      expect(relativeWeekLabel('2026-11-09', thisMonday)).toBe('2 weeks ago');
      // Summer time ended on 25 October 2026; the count is in whole weeks all the same.
      expect(relativeWeekLabel('2026-10-19', thisMonday)).toBe('5 weeks ago');
    });
  });

  describe('weekRange', () => {
    it('gives the month once for a week inside one month', () => {
      expect(weekRange('2026-11-23')).toBe('23 – 29 November 2026');
    });

    it('gives both short months for a week across two months', () => {
      expect(weekRange('2026-11-30')).toBe('30 Nov – 6 Dec 2026');
      expect(weekRange('2026-08-31')).toBe('31 Aug – 6 Sep 2026');
    });

    it('gives both years for a week across two years', () => {
      expect(weekRange('2026-12-28')).toBe('28 Dec 2026 – 3 Jan 2027');
    });
  });
});

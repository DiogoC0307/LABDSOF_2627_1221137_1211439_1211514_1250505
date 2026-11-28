# End-to-end test (SCRUM-100)

The principal journey runs in a browser (Playwright, Chromium) against the Docker Compose stack, with synthetic data only. The test cases are in [`docs/testing/test-strategy.md`](../docs/testing/test-strategy.md) (`TC-SCRUM-100-nn`).

| Test case | What it checks | State |
|---|---|---|
| TC-SCRUM-100-01 | The supervisor takes the first week from next week with no shifts and adds a Saturday shift for a crew member. They assign the member that "who can come" offers (checked by `elegibilidade-worker`) and publish. The week becomes *Published* | Runs |
| TC-SCRUM-100-02 | The member is told "Schedule published", "You are on 1 shift" | Runs |
| TC-SCRUM-100-03 | The member sees their shift | `test.fixme` until SCRUM-67 |
| TC-SCRUM-100-04 | The member says they can't come | `test.fixme` until SCRUM-71 |
| TC-SCRUM-100-05 | A free and qualified colleague is asked and accepts the cover | `test.fixme` until SCRUM-72 and SCRUM-73 |
| TC-SCRUM-100-06 | The shift shows the colleague | `test.fixme` until SCRUM-73 |

When a story is in `main`, replace its `test.fixme` with the steps, and update this table and the Test Strategy.

## Run it locally

```bash
docker compose up -d --build --wait          # from the repository root: the stack with its seeded users, AI off
cd e2e
npm ci
npx playwright install chromium              # once; in CI with --with-deps
npx playwright test                          # E2E_BASE_URL defaults to http://localhost:8000
npx playwright show-report                   # the HTML report; the trace of a failed test is in it
```

## Rules the test keeps

- **Lisbon time:** the browser uses `timezoneId: 'Europe/Lisbon'` and `locale: 'pt-PT'`, and the CI job sets `TZ=Europe/Lisbon`. "Next week", Saturday and night shifts must not change with the zone of the machine, and the CI runner is in UTC.
- **No fixed waits:** the worker's answers are awaited with `expect.poll` or `toPass` and an explicit timeout (60 s, as long as the PWA itself follows a publication). `waitForTimeout` is not used.
- **Isolated data:** each run registers its own member, `E2E Member <GITHUB_RUN_ID>` (`local-<time>` outside CI), and publishes the first week from next week that has no shifts. On a new database this is next week. A run stopped halfway, or a second run on the same database, therefore does not reuse the first run's data.
- **Under 5 minutes:** `globalTimeout` is 5 minutes. On Compose the run takes about 6 s with the worker up.
- **No retries**, and the service worker is blocked, so the PWA's cache never serves an app from an earlier stack.
- **It fails when the worker is down:** with `elegibilidade-worker` stopped, "who can come" never offers the member. TC-SCRUM-100-01 then fails after 60 s, and the report keeps the trace and a screenshot ("Couldn't check who can come.").

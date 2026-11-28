# Rehearsal: worker down (SCRUM-98), Docker Compose, 2026-11-28

The runbook scenario [Worker down](../deployment-and-operations.md#worker-down) was run once, on Docker Compose.

- **Who:** Diogo Pereira, with Claude Code driving the browser (Playwright) and the shell.
- **Version:** branch `docs/SCRUM-98-worker-down-demo`, on `main` at `d495544`.
- **Stack:** Compose project `escala98`, with the profile `observability` and `AI_ENABLED=true`. AI was on so that the "In my own words" section is shown; no model login was set. The stack was removed afterwards (`down -v`).
- **Data:** synthetic only. The users are the seeded *Synthetic Supervisor* and *Synthetic Member*; the free text was "Synthetic test: I cannot come on Saturday 12 December."

Times are UTC. "Gauge" is `escala_checks_waiting{purpose}` on `escala-api` (`ASSIGNMENT` / `CANDIDATE` / `EXTRACTION`).

## Step log

| Time (UTC) | Step | Command or action | Result |
|---|---|---|---|
| 17:32:30 | Start | `AI_ENABLED=true docker compose -p escala98 --profile observability up -d --wait` | All services healthy at 17:33:06 |
| 17:33:10 | Before | `docker compose -p escala98 exec rabbitmq rabbitmqctl list_queues -q name messages consumers` | `worker.avaliacao-pedida` 0 messages, 1 consumer; `worker.extracao-pedida` 0, 1; both DLQs 0. Gauge 0 / 0 / 0 |
| 17:33–17:34 | Set-up, worker up | Supervisor, *Next week* (30 Nov – 6 Dec): Add shift, Saturday 5 December 08:00–20:00, roles Driver and Crew member; Assign *Synthetic Member* as crew member | "Who can come" answered at once: *Synthetic Member* for Crew member; nobody is a driver |
| 17:34:38 | **Stop the worker** | `docker compose -p escala98 stop elegibilidade-worker` | Stopped. Both worker queues: 0 consumers |
| 17:34:51 | "Who can come" | Supervisor adds the role Crew chief to the shift | "Checking who can come…" ([01](2026-11-28-worker-down/01-who-can-come-checking.png)). After 20 s (17:35:12): "Couldn't check who can come." with "Check again" ([02](2026-11-28-worker-down/02-who-can-come-failed.png)) |
| 17:35:26 | Publish the week | *Publish schedule*, then *Publish* | `POST /api/v1/schedule-publications` answered `202` with status `CHECKING` ([03](2026-11-28-worker-down/03-publish-checking.png)). After 60 s (17:36:26): "Still checking the members on this week. It is published when the checks finish, unless one finds a conflict; nobody has been told yet. Reload the page in a moment to see it." ([04](2026-11-28-worker-down/04-publish-still-checking.png)) |
| 17:36:51 | Free text | *Synthetic Member*, When I can't, In my own words: the synthetic text, *Send* | `202`, message `PENDING`, "Reading…" ([05](2026-11-28-worker-down/05-free-text-reading.png)). After 10 s (17:37:01): "This is taking too long. Your text is still here; use the form above." ([06](2026-11-28-worker-down/06-free-text-form-fallback.png)) |
| 17:37:09 | Nothing applied | `rabbitmqctl list_queues …`; `psql` on `pg-escala-api` | `worker.avaliacao-pedida` 2 messages, 0 consumers; `worker.extracao-pedida` 1, 0. `week_publication` `CHECKING`; `assignment` `PROPOSED` / evaluation `PENDING`; Crew chief `candidate_check` `PENDING`; the message `DISCARDED` by the PWA after 10 s; **0 notifications**. Gauge 1 / 1 / 0 |
| 17:37:20, 17:37:40 | More waiting checks | *Check again* twice on Crew chief | Each time "Couldn't check who can come." after 20 s. At 17:38:06: `worker.avaliacao-pedida` 4 messages; gauge 1 / 3 / 0 |
| 17:38:47 | Dashboard | Grafana http://localhost:3000, *Escala - operations*, panel *Checks waiting for the worker*, last 15 min | `CANDIDATE` goes up to 3 and `ASSIGNMENT` to 1. `EXTRACTION` shows 1 for one scrape, then 0 once the message was discarded ([07](2026-11-28-worker-down/07-grafana-waiting-grows.png)) |
| 17:38:52 | **Start the worker** | `docker compose -p escala98 start elegibilidade-worker` | Started |
| 17:39:01 | Recovered | Same queue and `psql` checks | 9 s after the start: both worker queues 0 messages, 1 consumer each; `week_publication` `PUBLISHED` |
| 17:39:26 | Applied | Same checks, plus the `escala-api` log | `assignment` `CONFIRMED`, `ELIGIBLE`; the 4 `candidate_check` rows `EVALUATED`; 1 notification `WEEKLY_SCHEDULE_PUBLISHED`; every DLQ 0; gauge 0 / 0 / 0. The late answer to the free text was ignored on purpose: `ExtracaoConcluida ignored, no message waits for it unavailabilityRequestId=f369d3f9-…`; the message stays `DISCARDED` with no proposal |
| 17:39:26–17:40:18 | What the user sees | Supervisor: *Check again* on Crew chief; reload; *Next week* | *Synthetic Supervisor* can be assigned as crew chief ([08](2026-11-28-worker-down/08-who-can-come-answered.png)); the week shows **Published** ([09](2026-11-28-worker-down/09-week-published.png)) |
| 17:39:26–17:40:18 | Member notified | *Synthetic Member*: reload, the bell | "Schedule published" ([10](2026-11-28-worker-down/10-member-notified.png)) |
| 17:40:18 | Dashboard | The same panel | Every line back to 0 ([11](2026-11-28-worker-down/11-grafana-waiting-drains.png)) |

## Result

The rehearsal passed.

- **With the worker stopped (RNF04):** nothing was applied or notified. The week stayed `CHECKING`, its assignment `PROPOSED`, and the "who can come" checks `PENDING`. Free text fell back to the form after 10 s, and the requests waited in `worker.avaliacao-pedida` and `worker.extracao-pedida`.
- **When the worker returned:** every waiting check completed in 9 s, the week was published, the member was notified, and no message went to a DLQ.
- **The dashboard:** the panel showed the waiting checks growing and draining.

## Seen during the rehearsal

- In the supervisor's week view at 1280 px wide, the start of the "Still checking…" note is cut off on the left edge ([04](2026-11-28-worker-down/04-publish-still-checking.png)). This is a layout matter for the design owner. It was not changed here.
- On `localhost:8000`, the browser first showed the app of an earlier Compose stack, cached by the service worker. SCRUM-166 (PR #104) adds the "A new version of Escala is available" prompt for this case.

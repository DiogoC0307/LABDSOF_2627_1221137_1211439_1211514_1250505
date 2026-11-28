# Functional test: cover requests (SCRUM-72, SCRUM-71 AC1)

- **Date:** 2026-11-28, 18:53 to 19:00 UTC.
- **Version:** branch `feat/SCRUM-72-cover-requests` at `c84ae11`, plus the icon fix `1483a37`.
- **Who:** Diogo Pereira, with AI assistance (Claude), driving the PWA with Playwright.
- **Where:** Docker Compose on a laptop, a project of its own (`escala-cover72`) with new databases: `escala-api`, `elegibilidade-worker`, `web`, PostgreSQL and RabbitMQ.
- **Setting changed for the test:** `COVER_NO_ACCEPTANCE_AFTER=PT2M` and `COVER_CHECK_MS=10000` (defaults `PT30M` and `60000`), so the `NO_ACCEPTANCE` alert comes within minutes. `COVER_ALERT_BEFORE_SHIFT` stayed `PT2H`.
- **Data:** synthetic only. The mock users, and four drivers registered by the admin through the API: "Driver On Slot", "Driver Free", "Driver Away" (unavailable on Saturday 5 Dec, 18:00 to 23:00 UTC, declared through the API) and "Driver Other Shift".

## Steps and results

| # | TC | Step | Expected | Result |
|---|---|---|---|---|
| 1 | — | The supervisor builds week 2026-W49 in the PWA: two driver shifts on Saturday 5 Dec, 20:00 to 08:00 ("Driver On Slot" on one, "Driver Other Shift" on the other), and a crew chief shift on Sunday 6 Dec, 20:00 to 08:00 (the supervisor, the only crew chief). Publish. | Week `PUBLISHED`, three `CONFIRMED` assignments | Pass ([01](01-week-published.png)). The editor already listed "Driver Away" under "Can't come during this shift" |
| 2 | TC-SCRUM-71-01 | "Driver On Slot" opens their Saturday shift and presses "I can't come" ([02](02-driver-before-cannot-come.png)) | `202`, `slotFreed: true`; their assignment `CANCELLED`; one `OPEN` cover request, `CANNOT_COME`, `DRIVER` | Pass ([03](03-driver-cannot-come.png)) |
| 3 | TC-SCRUM-72-01 | The other drivers are checked by the worker | "Driver Free" `OFFERED` (`ELIGIBLE`); "Driver Away" `NOT_OFFERED` (`INELIGIBLE`, RN06); "Driver Other Shift" `NOT_OFFERED` (`INELIGIBLE`, RN05) | Pass, about 1 s after step 2 |
| 4 | TC-SCRUM-72-05 | Each driver opens the bell | "Driver Free": "Can you cover a shift?", "Driver, Sat 5 Dec, 20:00 – 08:00 (next day)". The others: nothing about the cover | Pass ([04](04-free-driver-asked.png), [05](05-away-driver-not-asked.png), [06](06-on-duty-driver-not-asked.png)) |
| 5 | TC-SCRUM-72-06 | The supervisor opens the bell | "A slot is open … The member can't come; others are being asked." (`SLOT_OPENED`, `CANNOT_COME`) | Pass ([07](07-supervisor-alerts.png), last item) |
| 6 | TC-SCRUM-72-04 | Nobody accepts (accepting comes with SCRUM-73) | "Nobody has covered yet … Nobody accepted in time." (`NO_ACCEPTANCE`), once | Pass: sent 2 min 4 s after the request (the check runs every 10 s) ([07](07-supervisor-alerts.png)) |
| 7 | TC-SCRUM-72-03 | The supervisor presses "I can't come" on the Sunday crew chief shift | No offer; "A slot is open" and "Nobody can cover … No member is free and qualified." (`NO_CANDIDATES`) at once | Pass: both sent in the same second ([07](07-supervisor-alerts.png), first two items) |

The `escala-api` log lines of the cover requests carry IDs, the role, the reason and the escalation only.

## Found and fixed during the test

The first run showed the three new notification icons as text ("wa", "P", "F"): the PWA loads only the Material Symbols named in `apps/web/src/index.html`, and `front_hand`, `person_off` and `warning` were missing. They were added; the screenshots above are from after the fix. No unit test covers this list.

## Not covered here

- Accepting a cover: SCRUM-73.
- `NO_ANSWER` (SCRUM-71 AC2, Diogo Costa): only in `CoverRequestIT` (TC-SCRUM-72-07) and `notifications.spec.ts`.
- dev, qa and prod.

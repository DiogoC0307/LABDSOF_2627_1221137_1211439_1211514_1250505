# Evaluation dataset: unavailability in the member's own words
 
Used by **06 · Responsible AI Opportunity Assessment** (Escala). 60 cases: 35 normal, 15 difficult, 10 adverse.
 
- **Origin:** written by the team from the patterns described in research (F4, F5; notes B-01 to B-04). **No real messages were collected or copied.**
- **Fields:** `id`, `category` (normal / difficult / adverse), `input` (European Portuguese), `referenceDate` ("today" for the message), `expected.periods`, optional `note`.
- **A period:** `startDate`, `endDate` (inclusive). Optional `weekdays` (MON…SUN) for repeating patterns within the range. Optional `startTime`/`endTime`, applied to every day in the range. A window that ends before it starts crosses midnight. Without times, the whole day is unavailable.
- **Empty `periods`:** nothing usable was found, so the form is shown. This covers vague messages, messages about when the member CAN serve, messages about someone else, prompt injection, dates in the past or more than 6 months ahead, empty input and input longer than 500 characters.
## Conventions (configurable per corps in the product; fixed here)
 
| Expression | Reading |
|---|---|
| "manhã" (morning) | 08:00–14:00 |
| "tarde" (afternoon) | 14:00–20:00 |
| "noite" (night) | 20:00–08:00 |
| "fds" / "fim de semana" (weekend) | the coming Saturday and Sunday |
| "esta semana" (this week) | from the reference date to Sunday |
| "próxima semana" (next week) | Monday to Sunday of the following week |
| A weekday alone ("sexta") | its next occurrence after the reference date |
| "até dezembro" (until December) | until 30 November |
| "nos próximos N dias" (in the next N days) | starting tomorrow |
 
Reference dates: 2026-10-26 (Monday), 2026-11-12 (Thursday), 2026-12-28 (Monday, for the change of year).
 
## Limitations
 
The dataset is synthetic and small. It was written by four students, not by firefighters, and it is in Portuguese only. Real accuracy may be lower.
 
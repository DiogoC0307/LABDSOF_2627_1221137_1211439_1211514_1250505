# 0016 · Use Claude Haiku through the Claude Code CLI for free-text extraction

| Field | Value |
|---|---|
| Date | 2026-10-27 |
| Status | Proposed |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

ADR 0011 puts the extraction of free-text unavailability behind a feature flag with a form fallback, and leaves open which model is used and how it is reached. The member writes something like "não posso aos domingos de manhã até dezembro", the worker returns proposed periods, and the member confirms or edits them (RF31–RF33; **06 · Responsible AI Opportunity Assessment**).

- The AI never decides who fills a slot (ADR 0007).
- 06 sets the bar: periods exactly right ≥ 85 %, dates right ≥ 95 %, right number of periods ≥ 95 %, adverse cases handled 100 %, p95 latency < 4 s.

Escala is an academic project. It is used only for tests and for demonstrations with synthetic data, one person at a time, and it is not offered to the public. The team has no GPU server, and the deployed VM (managed by Diogo Costa) cannot run a language model. The team already has Claude subscriptions.

## Decision

We will call Claude Haiku from `elegibilidade-worker` by running the Claude Code CLI in non-interactive mode, authenticated with a team member's Claude subscription.

- **How the worker calls it:** the worker starts `claude -p --model haiku --output-format json --json-schema <schema>`. The free text and the reference date go in the prompt. The JSON schema forces the answer into the period list defined on 06: `startDate` and `endDate`, and optionally `weekdays`, `startTime` and `endTime`. An empty list means nothing usable was found.
- **Where it runs:** inside the worker's container. The image includes Node and the CLI [TODO: CLI version]. The image gets bigger, so the worker's memory limit (today 640Mi in dev and qa, 768Mi in prod) must be checked again with the CLI running.
- **What it can access:** each call runs in an empty working directory, with no tools allowed, no MCP servers and our own system prompt, so free text cannot make the model read files or run commands.
- **Credentials:** a long-lived OAuth token of Diogo Pereira's or Diogo Sousa's Claude subscription, created with `claude setup-token` (valid for one year). It is sealed as the Secret `claude-code-auth` in the worker's folder (`infra/k8s/<env>/elegibilidade-worker/`) and read with `secretKeyRef` into the environment variable `CLAUDE_CODE_OAUTH_TOKEN`; never in the image or in the repository in clear (RNF07). `ANTHROPIC_API_KEY` is never set in the worker, because it takes precedence over the token. Work item: SCRUM-90.
- **Where AI is on:** dev and qa. AI stays off in prod (the feature flag of ADR 0011).
- **Timeout:** 10 s. When the CLI does not answer in time, or returns no valid structured output, the worker returns "no proposal" and the member uses the form (RF35, ADR 0011).
- **Rate limit:** at most 10 requests per member per hour, configurable (09, T-11).
- **Replaceable client:** the call sits behind an interface (`LlmClient`). `ClaudeCliClient` is the one in use. `AnthropicApiClient` (Anthropic Messages API with an API key) is written but not used, so moving to the API is a configuration change.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Anthropic Messages API with an API key | The right option for a product used by the public: faster, no limit on parallel use, and a clear data policy for commercial use. For a demonstration with one user at a time it adds a cost and a billing account the team does not need. It stays ready as `AnthropicApiClient`. |
| Local open model (e.g. Ollama on the VM) | The VM cannot run a model large enough to read Portuguese dates well, and the team has no GPU; a smaller model is unlikely to reach dates right ≥ 95 %. It would also add a heavy container to run and monitor. |
| Mount the CLI's stored login from the VM into the worker (Secret volume or `hostPath`) | The stored login is a short-lived token that the CLI rewrites when it refreshes it. A Secret volume is read-only, so the refresh fails; a `hostPath` mount would work around that, but it ties the pod to the VM's disk and bypasses the Sealed Secrets handling of every other secret. |
| Another hosted provider (e.g. OpenAI, Google) | The team already works with Claude and has subscriptions; one provider keeps the AI evaluation and the AI usage declaration simple. |
| No language model: rules and date parsing only | Kept as the non-AI baseline of the AI evaluation, not as the product: members' text mixes dates, relative days ("sexta", "fim de semana") and reasons, which rules handle poorly (06). [TODO: Java baseline, pending (SCRUM-54)] |

## Consequences

- **Positive:**
  - No cost and no billing account for the project.
  - No model to host, so the worker and the VM stay small.
  - `--json-schema` returns periods that already match the schema on 06 (RF32).
- **Negative:**
  - **Only for demonstration.** Anthropic's terms for Claude Code ask products used by other people to authenticate with an API key, not with a subscription. Before any real use by a corps, the worker must switch to `AnthropicApiClient`. This is also recorded on **AI Usage and Third-Party Attribution** and as an open risk on **09 · Security and Privacy Assessment**.
  - **Latency.** The CLI's fast mode (`--bare`) does not accept a subscription login, so each call starts the full CLI. The p95 < 4 s target of 06 is at risk. SCRUM-38 measures it with the evaluation dataset; if it fails, we propose p95 < 10 s, with the reason written on 06 before the target changes, because the member can always use the form.
  - **One user at a time.** Subscription limits are meant for individual use, so parallel calls may be refused.
  - **Data leaves Escala.** The free text goes to Anthropic and may contain sensitive reasons such as health. The UI says so next to the text box and offers the form; only the text and the reference date are sent; Escala deletes the free text after confirmation (RNF09); demonstrations use synthetic text only; the account owner turns off the use of chats to improve the models [TODO: confirm].
  - **The token is a secret.** Anyone with access to the worker container or to the Secret `claude-code-auth` can use that member's Claude account until the token expires or is revoked.
  - **The token expires after one year.** It must be created again with `claude setup-token` and resealed before then.
  - **A bigger worker image.** Node and the CLI add to the image and to the memory the worker needs.
- **Trade-offs:** we accept a slower, single-user setup valid only for demonstration, in exchange for the AI capability working end to end at no cost, with the move to an API key ready as a configuration change.

## Assignment requirement served

§5.6.2 and §5.6.3 (the AI capability, its human review and the switch that turns AI off); §12, since the `evaluation` profile runs with AI off (ADR 0011).

## Related

- Jira: SCRUM-38 (measure the CLI on the evaluation dataset), SCRUM-90 (the token as a Sealed Secret), SCRUM-75, SCRUM-40
- Requirements: RF31–RF33, RF35, RNF07, RNF09
- Supersedes: —
- Other ADRs or pages: 0007, 0011, 0015; **06 · Responsible AI Opportunity Assessment**; **09 · Security and Privacy Assessment**; **AI Usage and Third-Party Attribution**
- Date evidence: decided by the PO on 2026-10-27 after a draft of 2026-10-26; stays Proposed until SCRUM-38 measures it. Credentials changed on 2026-11-07 (Diogo Costa): a `setup-token` in a Sealed Secret instead of the CLI login mounted from the VM.
- Source: Claude Code documentation, "Run Claude Code programmatically" (`-p`, `--bare`, `--output-format json`, `--json-schema`), https://code.claude.com/docs/en/headless

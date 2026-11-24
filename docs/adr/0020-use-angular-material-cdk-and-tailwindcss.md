# 0020 · Use Angular Material + CDK and TailwindCSS in the web client

| Field | Value |
|---|---|
| Date | 2026-11-24 |
| Status | Proposed |
| Deciders | Diogo Pereira, Diogo Sousa, Pedro Costa, Diogo Costa |

## Context

The PWA (`apps/web`, ADR 0017) is styled by hand: one global `src/styles.scss` with no component library. Every control the Sprint 2 stories add (the week's schedule editor in SCRUM-65, swaps and covers, members) is built and styled from scratch. The result already drifts and the PWA must reach WCAG 2.1 level AA on the main screens (RNF10), which is checked at the Sprint 3 review, and work on a 360px phone as well as on the station computers (ADR 0017). 

Each hand-built dialog, menu or date field has to get focus handling, keyboard navigation and screen-reader labels right on its own.

## Decision

We will build the client's UI with **Angular Material** and the **Angular CDK** for components and behaviour, and **TailwindCSS** for layout, spacing and responsive rules.

- `@angular/material` and `@angular/cdk` at the same major version as Angular, added with `ng add @angular/material`, so `ng update` upgrades them with the framework.
- **Material** provides the interactive controls: buttons, form fields, select, date picker, dialog, menu, toolbar and snack bar. One Material 3 theme is defined in `src/styles.scss` with `mat.theme()`, which exposes the `--mat-sys-*` colour, typography and shape tokens.
- **CDK** provides behaviour where Material's look is not wanted: `BreakpointObserver` for the phone and desktop layouts, `FocusTrap` and `LiveAnnouncer` for accessibility, and `Overlay` for custom pop-ups.
- **Tailwind** (v4, through PostCSS with `@tailwindcss/postcss` and `.postcssrc.json`) is used for page layout, spacing, typography and breakpoints. It lives in its own plain CSS file, `src/tailwind.css`, listed in `angular.json` next to `styles.scss`.
- **One palette.** Tailwind's theme colours point to Material's tokens (`@theme inline { --color-primary: var(--mat-sys-primary); … }`), so a utility class and a Material component always show the same colour.
- **Tailwind does not restyle Material.** Tailwind's Preflight reset is not imported (only its theme and utilities layers), so it does not override Material's base styles. A Material component is changed through its theme tokens or `mat.*-overrides()` mixins, never with utility classes aimed at its internal elements.
- **Gradual migration.** The existing hand-written classes in `styles.scss` are replaced page by page, starting with the. There is no single rewrite of the client.

## Alternatives considered

| Alternative | Why rejected |
|---|---|
| Keep hand-written SCSS and plain HTML controls | Every dialog, menu and date field must be built and made accessible by hand to reach RNF10, and the controls already drift apart. |
| Angular Material and CDK without Tailwind | Material has no layout or spacing utilities, so every page's layout stays in hand-written SCSS, which is where current inconsistencies come from. Responsive rules would be repeated in each component's stylesheet. |
| Tailwind without a component library (own components on top of the CDK) | Fast for layout, but the team would build and test its own select, date picker, dialog and menu, including their keyboard and screen-reader behaviour, in the time left before Sprint 3. |
| PrimeNG | A complete component set with its own theming system, but it is released separately from Angular, so every Angular major waits for a matching PrimeNG release. Material and the CDK are released by the Angular team with each major. |
| Bootstrap | Its components expect Bootstrap's own JavaScript or a separate Angular wrapper (ng-bootstrap), and its grid and utilities overlap with what Tailwind gives, with less control over the design tokens. |

## Consequences

- **Positive:**
  - Material and CDK components already handle focus, keyboard navigation and ARIA attributes, which brings the main screens closer to RNF10 before the Sprint 3 check.
  - Controls in the same form share one height and look, from one theme.
  - Layout and breakpoints are written in the template with Tailwind classes without requiring new SCSS per component.
  - The CDK's component harnesses (`@angular/cdk/testing`) let unit tests find and use Material controls the same way across components.
- **Negative:**
  - The initial bundle grows, and so does what the service worker caches on first load (RNF06). Each Material module must be imported only where it is used, and the build must stay within the `angular.json` budgets (500 kB warning, 1 MB error).
  - Two styling systems to learn and review. Code reviews must check the rule above: Tailwind for layout, Material themes and overrides for components.
  - The default Material look is generic; the team has to define the theme (colours and typography) for Escala.
  - Long lists of utility classes make templates harder to read. Repeated groups become components, not copied class strings.
- **Trade-offs:** we accept a larger bundle and a second styling tool in exchange for accessible ready-made controls and consistent, responsive layouts. A team of four cannot build those by hand and still meet RNF10 by Sprint 3.

## Assignment requirement served

§6.1 (client application: supports the principal journeys; accessibility and usability); RNF10 (WCAG 2.1 AA); supports RNF06 (installable PWA) and RNF11 (acceptance of the main journeys).

## Related

- Jira: SCRUM-164; SCRUM-165 (shared navbar, first use); SCRUM-163 (menu overflow and control heights)
- Requirements: RNF06, RNF10, RNF11
- Supersedes: —
- Other ADRs or pages: 0017 (Angular PWA); **07 · Technical Design** (technology selection); **08 · Architecture Decision Records** (index)
- Date evidence: proposed on 2026-11-24 in the pull request for SCRUM-164, the day SCRUM-163 was reported in the dev acceptance and before any page uses these libraries.

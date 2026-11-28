import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

/**
 * The principal journey (SCRUM-100, §7.2.2 row 10), in a browser against the Compose stack, with synthetic data only.
 *
 * The parts in main run: the supervisor builds next week, assigns a member and publishes; the eligibility rules in
 * elegibilidade-worker check the assignment; the member is told "Schedule published". The parts that wait for other
 * stories are test.fixme, so they show in the report as not done yet, and are switched on when those stories are in.
 *
 * Each run registers its own member, named after the CI run, so a run stopped halfway leaves nothing that the next
 * run picks up by mistake. The worker's answers are waited for with expect.poll or toPass and an explicit timeout,
 * never with a fixed wait.
 */

const RUN = process.env['GITHUB_RUN_ID'] ?? `local-${Date.now()}`;
const MEMBER = `E2E Member ${RUN}`;
const ROLE = 'Crew member';

/** The worker evaluates in seconds; the PWA itself gives up following a publication after 60 s. */
const WORKER_ANSWER = 60_000;

function startsWith(text: string): RegExp {
  return new RegExp(`^${text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`);
}

/** Signs in with the simulated sign-in screen, as a person does. */
async function signIn(page: Page, fullName: string): Promise<void> {
  await page.goto('/');
  await page.getByRole('button', { name: startsWith(fullName) }).click();
  await expect(page.getByRole('button', { name: 'Sign out' })).toBeVisible();
}

/** The admin registers this run's member through the API: active roster, crew member, no contact. */
async function registerMember(request: APIRequestContext): Promise<void> {
  const users = await request.get('/api/v1/auth/mock-users');
  expect(users.ok()).toBe(true);
  const admin = ((await users.json()) as { id: string; fullName: string }[]).find(
    (user) => user.fullName === 'Synthetic Admin',
  );
  expect(admin, 'the seeded Synthetic Admin').toBeDefined();
  const signIn = await request.post('/api/v1/auth/mock-sign-in', { data: { memberId: admin!.id } });
  expect(signIn.ok()).toBe(true);
  const { token } = (await signIn.json()) as { token: string };

  const created = await request.post('/api/v1/members', {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      fullName: MEMBER,
      rank: 'FIREFIGHTER_3RD_CLASS',
      rosterStatus: 'ACTIVE_ROSTER',
      withinAgeLimit: true,
      contact: null,
      functions: ['CREW_MEMBER'],
      licences: [],
      certifications: [],
    },
  });
  expect(created.status(), await created.text()).toBe(201);
}

test.describe.serial('Principal journey (SCRUM-100)', () => {
  let publicationId = '';

  test.beforeAll(async ({ request }) => {
    await registerMember(request);
  });

  test('TC-SCRUM-100-01 the supervisor builds the next empty week, assigns a member who can come, and publishes; the week is Published', async ({
    page,
  }) => {
    await signIn(page, 'Synthetic Supervisor');

    await test.step('the first week from next week with no shifts yet: next week on a new database', async () => {
      // a run stopped halfway on the same database may have published next week already; this run takes the next one
      const status = page.locator('output.week-status');
      for (let ahead = 1; ; ahead++) {
        expect(ahead, 'a week with no shifts within 26 weeks').toBeLessThanOrEqual(26);
        const loaded = page.waitForResponse(
          (response) => response.url().includes('/api/v1/piquetes?week=') && response.ok(),
        );
        await page.getByRole('button', { name: 'Next week', exact: true }).click();
        const week = (await (await loaded).json()) as unknown[];
        if (week.length === 0) {
          break;
        }
      }
      await expect(status).toHaveText('No shifts yet');
    });

    await test.step('a Saturday shift that needs a crew member', async () => {
      const edit = page.getByRole('button', { name: 'Edit schedule' });
      if (await edit.isVisible()) {
        await edit.click();
      }
      await page.getByRole('button', { name: /^Add shift on Saturday \d/ }).click();
      const dialog = page.getByRole('dialog', { name: 'Add shift' });
      await dialog.getByRole('button', { name: ROLE, exact: true }).click();
      await dialog.getByRole('button', { name: 'Create shift' }).click();
      await expect(dialog).toBeHidden();
    });

    await test.step(`"who can come" offers ${MEMBER} (checked by the worker), and the supervisor assigns them`, async () => {
      const panel = page.getByRole('region', { name: 'Shift details' });
      const assign = panel.getByRole('button', { name: `Assign ${MEMBER} as ${ROLE}` });
      await expect(async () => {
        // with many members, the list shows a few and a "Show N more" button
        const more = panel.getByRole('button', { name: new RegExp(`^Show \\d+ more who can be ${ROLE}$`) });
        if (await more.isVisible()) {
          await more.click();
        }
        await expect(assign).toBeVisible({ timeout: 1_000 });
      }).toPass({ timeout: WORKER_ANSWER });
      await assign.click();
      await expect(panel.getByRole('button', { name: `Remove ${MEMBER} as ${ROLE}` })).toBeVisible();
    });

    await test.step('publish; the worker checks the assignment and the week becomes Published', async () => {
      await page.getByRole('button', { name: 'Publish schedule' }).click();
      const started = page.waitForResponse(
        (response) =>
          response.request().method() === 'POST' && response.url().endsWith('/api/v1/schedule-publications'),
      );
      await page.getByRole('dialog').getByRole('button', { name: 'Publish', exact: true }).click();
      const response = await started;
      expect(response.status()).toBe(202);
      publicationId = ((await response.json()) as { id: string }).id;

      await expect
        .poll(
          async () => {
            const publication = await page.request.get(`/api/v1/schedule-publications/${publicationId}`, {
              headers: { Authorization: `Bearer ${await sessionToken(page)}` },
            });
            return ((await publication.json()) as { status: string }).status;
          },
          { message: 'the publication, checked by the worker', timeout: WORKER_ANSWER },
        )
        .toBe('PUBLISHED');
      await expect(async () => {
        await expect(page.locator('output.week-status')).toHaveText('Published', { timeout: 1_000 });
      }).toPass({ timeout: WORKER_ANSWER });
    });
  });

  test('TC-SCRUM-100-02 the member assigned is told "Schedule published", with their shift', async ({ page }) => {
    await signIn(page, MEMBER);
    const bell = page.getByRole('button', { name: /^Notifications/ });
    const panel = page.getByRole('dialog', { name: 'Notifications' });
    // the bell asks every 30 s; the first ask is at sign-in, after the week was published
    await expect(async () => {
      if (!(await panel.isVisible())) {
        await bell.click();
      }
      await expect(panel.getByText('Schedule published')).toBeVisible({ timeout: 1_000 });
    }).toPass({ timeout: 45_000 });
    await expect(panel.getByText(/You are on 1 shift\b/)).toBeVisible();
  });

  // Switched on when the stories are in main (SCRUM-100 depends on SCRUM-63, 66, 67, 71, 72, 73).
  test.fixme('TC-SCRUM-100-03 the member sees their shift on the schedule (SCRUM-67)', async () => {});
  test.fixme("TC-SCRUM-100-04 the member says they can't come (SCRUM-71)", async () => {});
  test.fixme('TC-SCRUM-100-05 a free and qualified colleague is asked and accepts the cover (SCRUM-72, SCRUM-73)', async () => {});
  test.fixme('TC-SCRUM-100-06 the shift shows the colleague instead of the member (SCRUM-73)', async () => {});
});

/** The token of the signed-in user, as the PWA keeps it (escala.session in localStorage). */
async function sessionToken(page: Page): Promise<string> {
  const session = await page.evaluate(() => localStorage.getItem('escala.session'));
  expect(session, 'a signed-in session').not.toBeNull();
  return (JSON.parse(session!) as { token: string }).token;
}

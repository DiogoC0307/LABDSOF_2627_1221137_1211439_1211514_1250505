import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { App } from '../app';
import { http, setValue, signInAs, testProviders } from '../testing/test-support';

const types = [
  { code: 'TAS', name: 'Tripulante de Ambulância de Socorro' },
  { code: 'TAT', name: 'Tripulante de Ambulância de Transporte' },
];

const driver = {
  id: '7d0c2f2e-0000-4000-8000-000000000077',
  fullName: 'Synthetic Driver One',
  accessRole: 'MEMBER',
  active: true,
  rank: 'FIREFIGHTER_1ST_CLASS',
  rosterStatus: 'ACTIVE_ROSTER',
  withinAgeLimit: true,
  functions: ['DRIVER'],
  licences: [{ licenceClass: 'C', expiresOn: '2030-06-30' }],
  certifications: [
    { type: 'TAS', name: 'Tripulante de Ambulância de Socorro', issuedOn: '2024-04-01', expiresOn: '2027-03-31' },
  ],
};

/** Members: registration and editing by the admin, the list for supervisor and admin (SCRUM-77). */
describe('Members', () => {
  beforeEach(async () => {
    localStorage.clear();
    signInAs('ADMIN');
    await TestBed.configureTestingModule({
      imports: [App],
      providers: testProviders,
    }).compileComponents();
  });

  afterEach(() => {
    http().verify();
    localStorage.clear();
  });

  async function start(url: string) {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl(url);
    fixture.detectChanges();
    return fixture;
  }

  function page(fixture: ComponentFixture<App>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function field<T extends HTMLElement>(fixture: ComponentFixture<App>, name: string): T {
    const element = page(fixture).querySelector<T>(`[name="${name}"]`);
    if (!element) {
      throw new Error(`no field ${name}`);
    }
    return element;
  }

  /** Opens the registration form and fills in the member of AC1, with the given TAS expiry. */
  async function fillDriver(fixture: ComponentFixture<App>, tasExpiresOn: string) {
    http().expectOne('/api/v1/certification-types').flush(types);
    fixture.detectChanges();
    await fixture.whenStable();
    page(fixture).querySelector<HTMLButtonElement>('button.add-licence')!.click();
    page(fixture).querySelector<HTMLButtonElement>('button.add-certification')!.click();
    fixture.detectChanges();
    await fixture.whenStable();

    setValue(field(fixture, 'fullName'), 'Synthetic Driver One');
    setValue(field(fixture, 'rank'), 'FIREFIGHTER_1ST_CLASS');
    setValue(field(fixture, 'rosterStatus'), 'ACTIVE_ROSTER');
    field<HTMLInputElement>(fixture, 'function-DRIVER').click();
    setValue(field(fixture, 'licenceClass-0'), 'C');
    setValue(field(fixture, 'licenceExpiresOn-0'), '2030-06-30');
    setValue(field(fixture, 'certType-0'), 'TAS');
    setValue(field(fixture, 'certIssuedOn-0'), '2024-04-01');
    setValue(field(fixture, 'certExpiresOn-0'), tasExpiresOn);
    fixture.detectChanges();
    await fixture.whenStable();
  }

  it('TC-SCRUM-77-01 the admin registers a driver with a class C licence and a TAS until 2027-03-31, who then appears in the list', async () => {
    const fixture = await start('/members/new');
    await fillDriver(fixture, '2027-03-31');

    page(fixture).querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
    const post = http().expectOne({ method: 'POST', url: '/api/v1/members' });
    expect(post.request.body).toEqual({
      fullName: 'Synthetic Driver One',
      rank: 'FIREFIGHTER_1ST_CLASS',
      rosterStatus: 'ACTIVE_ROSTER',
      withinAgeLimit: true,
      contact: null,
      functions: ['DRIVER'],
      licences: [{ licenceClass: 'C', expiresOn: '2030-06-30' }],
      certifications: [{ type: 'TAS', issuedOn: '2024-04-01', expiresOn: '2027-03-31' }],
    });
    post.flush(driver, { status: 201, statusText: 'Created' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(TestBed.inject(Router).url).toBe('/members');
    http().expectOne('/api/v1/members').flush([driver]);
    fixture.detectChanges();
    const row = page(fixture).querySelector(`[data-member-id="${driver.id}"]`);
    expect(row?.textContent).toContain('Synthetic Driver One');
    expect(row?.textContent).toContain('ACTIVE_ROSTER');
    expect(row?.textContent).toContain('DRIVER');
    expect(row?.querySelector('.licence')?.textContent).toContain('C');
    expect(row?.querySelector('.certification')?.textContent).toContain('TAS');
    expect(row?.querySelector('.certification')?.textContent).toContain('2027-03-31');
  });

  it('TC-SCRUM-77-02 a certification that expires before it was issued shows an error and nothing is sent', async () => {
    const fixture = await start('/members/new');
    await fillDriver(fixture, '2023-12-31');

    page(fixture).querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
    fixture.detectChanges();

    http().expectNone({ method: 'POST', url: '/api/v1/members' });
    const error = page(fixture).querySelector('[role="alert"] [data-field="certifications[0].expiresOn"]');
    expect(error?.textContent).toContain('expires before it was issued');
    expect(TestBed.inject(Router).url).toBe('/members/new');
  });

  it('TC-SCRUM-77-02 the errors the API answers are shown on the form, which stays open', async () => {
    const fixture = await start('/members/new');
    await fillDriver(fixture, '2027-03-31');

    page(fixture).querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
    http()
      .expectOne({ method: 'POST', url: '/api/v1/members' })
      .flush(
        { status: 400, errors: [{ field: 'certifications[0].expiresOn', code: 'expires_before_issued' }] },
        { status: 400, statusText: 'Bad Request' },
      );
    fixture.detectChanges();

    const error = page(fixture).querySelector('[role="alert"] [data-field="certifications[0].expiresOn"]');
    expect(error?.textContent).toContain('expires before it was issued');
    expect(TestBed.inject(Router).url).toBe('/members/new');
  });

  it('a driver without a driving licence shows an error and nothing is sent', async () => {
    const fixture = await start('/members/new');
    http().expectOne('/api/v1/certification-types').flush(types);
    fixture.detectChanges();
    await fixture.whenStable();
    setValue(field(fixture, 'fullName'), 'Synthetic Driver Two');
    field<HTMLInputElement>(fixture, 'function-DRIVER').click();
    fixture.detectChanges();
    await fixture.whenStable();

    page(fixture).querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();
    fixture.detectChanges();

    http().expectNone({ method: 'POST', url: '/api/v1/members' });
    expect(page(fixture).querySelector('[role="alert"] [data-field="licences"]')?.textContent).toContain('driving licence');
  });

  it('TC-SCRUM-158-01 two clicks on Save member before the answer send one registration; the button is disabled until the answer, then enabled again', async () => {
    const fixture = await start('/members/new');
    await fillDriver(fixture, '2027-03-31');
    const save = page(fixture).querySelector<HTMLButtonElement>('form button[type="submit"]')!;

    save.click();
    save.click();
    fixture.detectChanges();

    const post = http().expectOne({ method: 'POST', url: '/api/v1/members' });
    expect(save.disabled).toBe(true);
    post.flush({ status: 400, errors: [{ field: 'fullName', code: 'required' }] }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(save.disabled).toBe(false);
    expect(TestBed.inject(Router).url).toBe('/members/new');
  });

  it('TC-SCRUM-158-02 two clicks on Save member while editing send the change once; the button is enabled again after an error', async () => {
    const fixture = await start(`/members/${driver.id}/edit`);
    http().expectOne('/api/v1/certification-types').flush(types);
    http().expectOne(`/api/v1/members/${driver.id}`).flush(driver);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const save = page(fixture).querySelector<HTMLButtonElement>('form button[type="submit"]')!;

    save.click();
    save.click();
    fixture.detectChanges();

    http().expectOne({ method: 'PUT', url: `/api/v1/members/${driver.id}` }).flush(driver);
    expect(save.disabled).toBe(true);
    http()
      .expectOne({ method: 'PUT', url: `/api/v1/members/${driver.id}/certifications` })
      .flush('fail', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(save.disabled).toBe(false);
  });

  it('the admin edits a member: details and certifications are saved, then the list is shown', async () => {
    const fixture = await start(`/members/${driver.id}/edit`);
    http().expectOne('/api/v1/certification-types').flush(types);
    http().expectOne(`/api/v1/members/${driver.id}`).flush({ ...driver, contact: 'synthetic@example.test' });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(field<HTMLInputElement>(fixture, 'fullName').value).toBe('Synthetic Driver One');
    expect(field<HTMLInputElement>(fixture, 'certExpiresOn-0').value).toBe('2027-03-31');

    setValue(field(fixture, 'certExpiresOn-0'), '2030-02-28');
    fixture.detectChanges();
    await fixture.whenStable();
    page(fixture).querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();

    const details = http().expectOne({ method: 'PUT', url: `/api/v1/members/${driver.id}` });
    expect(details.request.body.contact).toBe('synthetic@example.test');
    details.flush(driver);
    const qualifications = http().expectOne({ method: 'PUT', url: `/api/v1/members/${driver.id}/certifications` });
    expect(qualifications.request.body.certifications[0].expiresOn).toBe('2030-02-28');
    qualifications.flush(driver);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(TestBed.inject(Router).url).toBe('/members');
    http().expectOne('/api/v1/members').flush([driver]);
  });

  it('the supervisor reads the list without contacts and cannot register or edit', async () => {
    localStorage.clear();
    signInAs('SUPERVISOR');
    const fixture = await start('/members');
    http().expectOne('/api/v1/members').flush([driver]);
    fixture.detectChanges();

    expect(page(fixture).textContent).toContain('Synthetic Driver One');
    expect(page(fixture).querySelector('.contact')).toBeNull();
    expect(page(fixture).querySelector('a.register')).toBeNull();
    expect(page(fixture).querySelector('a.edit')).toBeNull();
  });

  it('the admin sees the contact, the register link and an edit link per member', async () => {
    const fixture = await start('/members');
    http().expectOne('/api/v1/members').flush([{ ...driver, contact: 'synthetic@example.test' }]);
    fixture.detectChanges();

    expect(page(fixture).querySelector('.contact')?.textContent).toContain('synthetic@example.test');
    expect(page(fixture).querySelector('a.register')?.getAttribute('href')).toBe('/members/new');
    expect(page(fixture).querySelector('a.edit')?.getAttribute('href')).toBe(`/members/${driver.id}/edit`);
  });

  it('the guard sends a supervisor away from the registration form', async () => {
    localStorage.clear();
    signInAs('SUPERVISOR');
    await start('/members/new');
    http().expectNone('/api/v1/certification-types');
    http().expectOne('/api/v1/piquetes').flush([]);
    expect(TestBed.inject(Router).url).toBe('/');
  });
});

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';

const pending = {
  id: 'p1',
  dutyDate: '2026-11-05',
  slots: [{ id: 's1', role: 'CREW_MEMBER', status: 'OPEN' }],
};

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
  });

  function http(): HttpTestingController {
    return TestBed.inject(HttpTestingController);
  }

  it('should render the rosters heading', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent).toContain('Rosters');
  });

  it('lists piquetes returned by the API', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([pending]);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('2026-11-05');
    expect(text).toContain('OPEN');
  });

  it('shows an error when the list cannot be loaded', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush('fail', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent).toContain(
      'Could not load piquetes.',
    );
  });

  it('creates a piquete and reloads the list', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([]);

    (fixture.nativeElement as HTMLElement).querySelector('button')?.click();
    const post = http().expectOne('/api/v1/piquetes');
    expect(post.request.method).toBe('POST');
    expect(post.request.body.dutyDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    post.flush(pending);

    http().expectOne('/api/v1/piquetes').flush([pending]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('OPEN');
  });

  it('shows an error when create fails', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http().expectOne('/api/v1/piquetes').flush([]);

    (fixture.nativeElement as HTMLElement).querySelector('button')?.click();
    http().expectOne({ method: 'POST', url: '/api/v1/piquetes' }).flush('fail', {
      status: 500,
      statusText: 'Server Error',
    });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent).toContain(
      'Could not create the piquete.',
    );
  });
});
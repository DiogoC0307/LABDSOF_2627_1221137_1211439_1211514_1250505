import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { Router } from '@angular/router';
import { signInAs, testProviders } from '../testing/test-support';
import { Navbar } from './navbar';

/**
 * The navbar (SCRUM-167): pages by role with an icon and a label, the current page, the user's name and Sign out,
 * Install, in the phone layout (top bar and bottom tab bar) and the layout from 768px. jsdom applies no media
 * queries, so both layouts are in the page here; which one shows is checked through their classes.
 */
describe('Navbar', () => {
  let fixture: ComponentFixture<Navbar>;
  let loader: HarnessLoader;

  async function render(role: string, url = '/') {
    localStorage.clear();
    signInAs(role);

    await TestBed.configureTestingModule({
      imports: [Navbar],
      providers: testProviders,
    }).compileComponents();
    
    fixture = TestBed.createComponent(Navbar);
    await navigate(url);
    loader = TestbedHarnessEnvironment.loader(fixture);
  }

  /** RouterLinkActive sets isActive in a microtask after the navigation, so the page renders twice. */
  async function navigate(url: string) {
    await TestBed.inject(Router).navigateByUrl(url);
    fixture.detectChanges();

    await fixture.whenStable();
    fixture.detectChanges();
  }

  function page(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  /** The phone tab bar and the bar from 768px */
  function navs(): { tabBar: HTMLElement; desktop: HTMLElement } {
    const [tabBar, desktop] = Array.from(
      page().querySelectorAll<HTMLElement>('nav[aria-label="Main"]'),
    );
    
    return { tabBar, desktop };
  }

  /** A link's visible text, without its icon's name. */
  function label(link: Element): string {
    const icon = link.querySelector('mat-icon')?.textContent ?? '';
    return link.textContent!.replace(icon, '').trim();
  }

  function labels(nav: HTMLElement, selector = 'a'): string[] {
    return Array.from(nav.querySelectorAll(selector), label);
  }

  afterEach(() => localStorage.clear());

  it('shows the phone bars below 768px and the single bar from 768px', async () => {
    await render('MEMBER');
    const [phoneTop, desktop] = Array.from(page().querySelectorAll('header'));
    expect(phoneTop.classList).toContain('md:hidden');
    expect(navs().tabBar.classList).toContain('md:hidden');
    expect(navs().tabBar.classList).toContain('fixed');
    expect(desktop.classList).toContain('hidden');
    expect(desktop.classList).toContain('md:block');
    expect(desktop.contains(navs().desktop)).toBe(true);
  });

  it("a member sees Schedule and When I can't, not Members, in both layouts", async () => {
    await render('MEMBER');
    expect(labels(navs().tabBar)).toEqual(['Schedule', "When I can't"]);
    expect(labels(navs().desktop)).toEqual(['Schedule', "When I can't"]);
  });

  it.each(['SUPERVISOR', 'ADMIN'])('"%s" also sees Members, in both layouts', async (role) => {
    await render(role);
    expect(labels(navs().tabBar)).toEqual(['Schedule', "When I can't", 'Members']);
    expect(labels(navs().desktop)).toEqual(['Schedule', "When I can't", 'Members']);
  });

  it('every page has its icon next to a visible label', async () => {
    await render('ADMIN');
    for (const nav of [navs().tabBar, navs().desktop]) {
      const icons = Array.from(nav.querySelectorAll('a mat-icon'), (icon) =>
        icon.textContent!.trim(),
      );
      expect(icons).toEqual(['calendar_month', 'event_busy', 'group']);
      nav.querySelectorAll('mat-icon').forEach((icon) => {
        expect(icon.classList).toContain('material-symbols-outlined');
        expect(icon.getAttribute('aria-hidden')).toBe('true');
      });
    }
  });

  it('marks only the current page, also under it, and Schedule only on its own route', async () => {
    await render('ADMIN', '/members/new');
    expect(labels(navs().tabBar, 'a[aria-current="page"]')).toEqual(['Members']);
    expect(labels(navs().desktop, 'a[aria-current="page"]')).toEqual(['Members']);

    await navigate('/');
    expect(labels(navs().tabBar, 'a[aria-current="page"]')).toEqual(['Schedule']);
    expect(labels(navs().desktop, 'a[aria-current="page"]')).toEqual(['Schedule']);
    expect(page().querySelectorAll('a[aria-current]')).toHaveLength(2);
  });

  it("from 768px, shows the user's name just before Sign out, at the right end of the bar", async () => {
    await render('ADMIN');
    const end = page().querySelector('header.md\\:block .toolbar-end')!;
    expect(end.parentElement?.lastElementChild).toBe(end);
    const last = end.lastElementChild!;
    expect(last.querySelector('button')?.textContent?.trim()).toBe('Sign out');
    expect(last.previousElementSibling?.classList).toContain('session');
    expect(last.previousElementSibling?.textContent?.trim()).toBe('Synthetic Admin');
  });

  it('emits signOut from the Sign out button of either layout', async () => {
    await render('MEMBER');
    let signedOut = 0;
    fixture.componentInstance.signOut.subscribe(() => signedOut++);
    const buttons = page().querySelectorAll<HTMLButtonElement>('button.sign-out');
    expect(Array.from(buttons, label)).toEqual(['Sign out', 'Sign out']);
    buttons.forEach((button) => button.click());
    expect(signedOut).toBe(2);
  });

  it('offers Install in both layouts only when the browser allows it', async () => {
    await render('MEMBER');
    expect(page().querySelectorAll('.install')).toHaveLength(0);

    fixture.componentRef.setInput('canInstall', true);
    fixture.detectChanges();
    let installs = 0;
    fixture.componentInstance.install.subscribe(() => installs++);
    const phone = page().querySelector<HTMLButtonElement>('header.md\\:hidden .install')!;
    expect(label(phone)).toBe('Install Escala');
    phone.click();
    await (await loader.getHarness(MatButtonHarness.with({ text: 'Install Escala' }))).click();
    expect(installs).toBe(2);
  });
});

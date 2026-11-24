import { Config, Generator } from '@angular/service-worker/config';
import config from '../../ngsw-config.json';

interface NavigationUrl {
  positive: boolean;
  regex: string;
}

/** ngsw.json as `ng build` writes it: the same Generator, with an empty dist (the asset hashes do not matter here). */
async function navigationUrls(): Promise<NavigationUrl[]> {
  const emptyDist = {
    list: async () => [],
    read: async () => '',
    hash: async () => '',
    write: async () => undefined,
  };
  const manifest = (await new Generator(emptyDist, '/').process(config as Config)) as { navigationUrls: NavigationUrl[] };
  return manifest.navigationUrls;
}

/** The test of ngsw-worker.js (isNavigationRequest): any include pattern and no exclude pattern. */
function servesTheApp(urls: NavigationUrl[], path: string): boolean {
  const include = urls.filter((url) => url.positive).map((url) => new RegExp(url.regex));
  const exclude = urls.filter((url) => !url.positive).map((url) => new RegExp(url.regex));
  return include.some((regex) => regex.test(path)) && !exclude.some((regex) => regex.test(path));
}

/**
 * Which navigations the service worker answers with index.html (SCRUM-162). Before the fix, opening an API URL in a
 * browser with the PWA installed showed the app instead of the API's answer, e.g. /api/actuator/health/readiness.
 */
describe('Service worker navigation URLs', () => {
  it('TC-SCRUM-162-01: API URLs go to the network; app routes still get index.html, offline too', async () => {
    const urls = await navigationUrls();

    for (const path of [
      '/api/actuator/health',
      '/api/actuator/health/readiness',
      '/api/actuator/health/liveness',
      '/api/v1/piquetes',
      '/api/v1/members/00000000-0000-4000-8000-000000000001',
    ]) {
      expect(servesTheApp(urls, path), path).toBe(false);
    }

    for (const path of [
      '/',
      '/unavailability',
      '/members',
      '/members/new',
      '/members/00000000-0000-4000-8000-000000000001/edit',
      '/an-unknown-route',
      '/apiary',
    ]) {
      expect(servesTheApp(urls, path), path).toBe(true);
    }

    // Angular's defaults are kept: files and URLs with "__" are never answered with index.html.
    for (const path of ['/main-ABC123.js', '/manifest.webmanifest', '/icons/icon-192x192.png', '/__debug', '/a__b/c']) {
      expect(servesTheApp(urls, path), path).toBe(false);
    }
  });
});

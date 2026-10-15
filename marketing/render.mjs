// Renders every banner in banners.html to PNG (exact size + @2x).
// Run: NODE_PATH=<dir containing playwright> node marketing/render.mjs  (needs Playwright + Chromium)
import { chromium } from 'playwright';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const dir = path.dirname(fileURLToPath(import.meta.url));
const browser = await chromium.launch({ executablePath: process.env.CHROME || undefined });
import fs from 'node:fs';
// 1x = exact platform sizes (Play requires exactly 1024x500); 2x = sharper copies for retina / print.
for (const [scale, sub] of [[1, ''], [2, '@2x']]) {
  const out = path.join(dir, 'banners', sub);
  fs.mkdirSync(out, { recursive: true });
  const page = await browser.newPage({ deviceScaleFactor: scale, viewport: { width: 1700, height: 1200 } });
  await page.goto('file://' + path.join(dir, 'banners.html'));
  await page.waitForLoadState('networkidle');
  await page.evaluate(() => document.fonts.ready);
  for (const id of await page.evaluate(() => window.BANNERS)) {
    await page.locator(`[id="${id}"]`).screenshot({ path: path.join(out, id + '.png') });
    console.log('rendered', sub || '1x', id);
  }
  await page.close();
}
await browser.close();

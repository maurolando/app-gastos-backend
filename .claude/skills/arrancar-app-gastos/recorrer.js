const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
  const errores = [];
  page.on('console', m => { if (m.type() === 'error') errores.push(m.text()); });
  page.on('pageerror', e => errores.push('pageerror: ' + e.message));

  await page.goto('http://localhost:4200/', { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('input[formcontrolname="nombre"]', { timeout: 30000 });
  await page.screenshot({ path: 'shot-login.png' });

  await page.fill('input[formcontrolname="nombre"]', 'Mauro');
  await page.fill('input[formcontrolname="clave"]', '123');
  await page.click('button[type="submit"]');
  await page.waitForTimeout(4000);
  await page.screenshot({ path: 'shot-dashboard.png', fullPage: true });

  console.log('URL final:', page.url());
  console.log('Titulo:', await page.title());
  console.log('Texto (400):', (await page.innerText('body')).slice(0, 400).replace(/\n+/g, ' | '));
  console.log('Errores de consola:', errores.length ? JSON.stringify(errores, null, 1) : 'ninguno');
  await browser.close();
})();

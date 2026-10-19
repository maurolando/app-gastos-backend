const { chromium } = require('playwright');

// Crea una cuenta desde el login, mira el dashboard, sale y crea otra: la segunda
// tiene que arrancar vacia, sin nada de la primera.
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
  const errores = [];
  page.on('console', m => { if (m.type() === 'error') errores.push(m.text()); });
  page.on('pageerror', e => errores.push('pageerror: ' + e.message));

  async function crearCuenta(nombre, clave, captura) {
    await page.goto('http://localhost:4200/login', { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('input[formcontrolname="nombre"]', { timeout: 30000 });
    await page.click('button.cambiar-modo');
    await page.fill('input[formcontrolname="nombre"]', nombre);
    await page.fill('input[formcontrolname="clave"]', clave);
    await page.fill('input[formcontrolname="confirmar"]', clave);
    if (captura) await page.screenshot({ path: 'shot-registro.png' });
    await page.click('button[type="submit"]');
    await page.waitForTimeout(4000);
  }

  const sufijo = Date.now() % 100000;
  await crearCuenta('Prueba' + sufijo, 'secreta1', true);
  await page.screenshot({ path: 'shot-dashboard.png', fullPage: true });
  console.log('URL tras registrarse:', page.url());

  await page.click('.logout-btn');
  await page.waitForTimeout(1500);
  await crearCuenta('Otra' + sufijo, 'secreta2', false);
  await page.screenshot({ path: 'shot-dashboard-otra.png', fullPage: true });
  console.log('URL segunda cuenta:', page.url());
  console.log('Texto (400):', (await page.innerText('body')).slice(0, 400).replace(/\n+/g, ' | '));
  console.log('Errores de consola:', errores.length ? JSON.stringify(errores, null, 1) : 'ninguno');
  await browser.close();
})();

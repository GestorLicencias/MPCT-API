const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  await page.goto('https://www.universidadperu.com/empresas/20100119227.php');
  
  const text = await page.evaluate(() => {
    return Array.from(document.querySelectorAll('td, th, p, li, div')).map(el => el.innerText).filter(t => t.toLowerCase().includes('actividad'));
  });
  console.log(text);

  const tds = await page.evaluate(() => {
    return Array.from(document.querySelectorAll('tr')).map(el => el.innerText);
  });
  console.log(tds.join('\n'));

  await browser.close();
})();

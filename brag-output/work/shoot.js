const { chromium } = require('/opt/node22/lib/node_modules/playwright');
const path = require('path');
const fs = require('fs');
(async () => {
  const mode = process.argv[2];
  const browser = await chromium.launch({ args: ['--allow-file-access-from-files'] });
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
  await page.goto('file://' + path.resolve(__dirname, 'video.html'));
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(500);
  if (mode === 'stills') {
    const times = process.argv[3].split(',').map(Number);
    fs.mkdirSync(path.join(__dirname, 'stills'), { recursive: true });
    for (const t of times) {
      await page.evaluate(t => render(t), t);
      await page.screenshot({ path: path.join(__dirname, 'stills', `t${t.toFixed(2)}.jpg`), quality: 80, type: 'jpeg' });
    }
  } else {
    const dur = await page.evaluate(() => DURATION);
    const n = Math.round(dur * 30);
    fs.mkdirSync(path.join(__dirname, 'frames'), { recursive: true });
    for (let i = 0; i < n; i++) {
      await page.evaluate(t => render(t), i / 30);
      await page.screenshot({ path: path.join(__dirname, 'frames', `f${String(i).padStart(4, '0')}.jpg`), quality: 92, type: 'jpeg' });
    }
    console.log('frames', n, dur);
  }
  await browser.close();
})();

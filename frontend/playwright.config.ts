import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests/browser',
  fullyParallel: false,
  workers: 1,
  timeout: 60_000,
  use: {
    baseURL: 'http://127.0.0.1:5174',
    trace: 'retain-on-failure',
    launchOptions: {
      executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE || undefined,
    },
  },
  webServer: [
    {
      command:
        './mvnw -B -ntp test-compile spring-boot:test-run -Dspring-boot.run.main-class=com.payflow.BrowserTestApplication',
      cwd: '../backend',
      url: 'http://127.0.0.1:18080/actuator/health',
      timeout: 180_000,
      reuseExistingServer: !process.env.CI,
    },
    {
      command: 'npm run dev -- --host 127.0.0.1 --port 5174 --strictPort',
      url: 'http://127.0.0.1:5174',
      env: { API_PROXY_TARGET: 'http://127.0.0.1:18080' },
      reuseExistingServer: !process.env.CI,
    },
  ],
});

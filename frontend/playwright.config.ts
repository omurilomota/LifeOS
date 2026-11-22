import process from 'node:process'
import { defineConfig, devices } from '@playwright/test'

const isCI = !!process.env.CI
const apiBaseUrl = process.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  forbidOnly: isCI,
  retries: isCI ? 2 : 1,
  reporter: isCI ? [['github'], ['html', { open: 'never' }]] : 'list',
  timeout: 90_000,
  expect: { timeout: 15_000 },
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: 'mvn spring-boot:run -Dspring-boot.run.profiles=local',
      cwd: '../backend',
      url: 'http://localhost:8080/api/v1/health',
      reuseExistingServer: !isCI,
      timeout: 180_000,
    },
    {
      command: 'npm run dev -- --host 0.0.0.0',
      url: 'http://localhost:5173',
      reuseExistingServer: !isCI,
      timeout: 120_000,
      env: { VITE_API_BASE_URL: apiBaseUrl },
    },
  ],
})

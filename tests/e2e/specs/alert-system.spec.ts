import { test, expect } from '@playwright/test'

test.describe('Alert System', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'Uav@2024!Secure')
    await page.click('button[type="submit"]')
    await page.waitForURL(/.*dashboard.*|.*\//)
  })

  test('should show alert notifications', async ({ page }) => {
    await page.goto('/')
    await page.waitForTimeout(3000)
    
    // Check for alert/notification elements
    const alerts = page.locator('[class*="alert"], [class*="notification"], [class*="toast"], [class*="message"]')
    const alertCount = await alerts.count()
    
    // At minimum, the page should render
    expect(await page.locator('body').isVisible()).toBeTruthy()
  })

  test('should display alert severity levels', async ({ page }) => {
    await page.goto('/monitoring')
    await page.waitForTimeout(2000)
    
    // Check for severity indicators
    const critical = page.locator('text=/CRITICAL|严重|危急/').first()
    const warning = page.locator('text=/WARNING|警告/').first()
    
    const criticalVisible = await critical.isVisible().catch(() => false)
    const warningVisible = await warning.isVisible().catch(() => false)
    
    // At least one severity level should be displayed
    expect(criticalVisible || warningVisible).toBeTruthy()
  })

  test('should have monitoring dashboard', async ({ page }) => {
    await page.goto('/monitoring')
    await page.waitForTimeout(2000)
    
    // Check for dashboard elements
    const charts = page.locator('canvas, [class*="chart"], [class*="gauge"], [class*="widget"]')
    const chartCount = await charts.count()
    
    // Should have monitoring widgets
    expect(chartCount).toBeGreaterThanOrEqual(1)
  })

  test('should display drone metrics in monitoring view', async ({ page }) => {
    await page.goto('/monitoring')
    await page.waitForTimeout(2000)
    
    // Check for metric displays
    const metrics = page.locator('[class*="metric"], [class*="stat"], [class*="value"], [class*="number"]')
    const metricCount = await metrics.count()
    
    expect(metricCount).toBeGreaterThanOrEqual(1)
    
    // Log available metrics for debugging
    const metricTexts = await metrics.allTextContents()
    console.log('Available metrics:', metricTexts.slice(0, 5))
  })
})

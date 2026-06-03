import { test, expect } from '@playwright/test'

test.describe('Data Source Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'Uav@2024!Secure')
    await page.click('button[type="submit"]')
    await page.waitForURL(/.*dashboard.*|.*\//)
  })

  test('should display data sources page', async ({ page }) => {
    await page.goto('/data-sources')
    await page.waitForTimeout(2000)
    
    await expect(page.locator('body')).toBeVisible()
    
    // Check for data source cards or table
    const content = page.locator('[class*="card"], table, .ant-table, [class*="list"]')
    const contentCount = await content.count()
    expect(contentCount).toBeGreaterThanOrEqual(1)
  })

  test('should show data source status', async ({ page }) => {
    await page.goto('/data-sources')
    await page.waitForTimeout(1500)
    
    // Look for online/offline indicators
    const statusText = page.locator('text=/在线|离线|Online|Offline|异常|Error/').first()
    const statusDot = page.locator('[class*="dot"], [class*="badge"], [class*="indicator"]').first()
    
    const textVisible = await statusText.isVisible().catch(() => false)
    const dotVisible = await statusDot.isVisible().catch(() => false)
    
    expect(textVisible || dotVisible).toBeTruthy()
  })

  test('should navigate between data source types', async ({ page }) => {
    await page.goto('/data-sources')
    await page.waitForTimeout(1000)
    
    // Try clicking tabs or filters
    const tabs = page.locator('[class*="tab"], .ant-tabs-tab, [class*="filter"]')
    const tabCount = await tabs.count()
    
    if (tabCount >= 2) {
      await tabs.nth(0).click()
      await page.waitForTimeout(300)
      await tabs.nth(1).click()
      await page.waitForTimeout(300)
    }
  })
})

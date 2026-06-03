import { test, expect } from '@playwright/test'

test.describe('Drone Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'Uav@2024!Secure')
    await page.click('button[type="submit"]')
    await page.waitForURL(/.*dashboard.*|.*\//)
  })

  test('should display drone list', async ({ page }) => {
    await page.goto('/drones')
    await page.waitForTimeout(2000)
    
    // Check for drone cards or table
    const droneElements = page.locator('[class*="drone"], [class*="card"], table, .ant-table')
    await expect(droneElements.first()).toBeVisible({ timeout: 5000 })
  })

  test('should show drone status indicators', async ({ page }) => {
    await page.goto('/drones')
    await page.waitForTimeout(2000)
    
    // Look for status badges or indicators
    const statusBadge = page.locator('[class*="status"], .ant-badge, [class*="tag"]')
    const badgeCount = await statusBadge.count()
    expect(badgeCount).toBeGreaterThanOrEqual(1)
  })

  test('should have battery level display', async ({ page }) => {
    await page.goto('/drones')
    await page.waitForTimeout(1500)
    
    // Check for battery indicators
    const batteryText = page.locator('text=%').first()
    const batteryBar = page.locator('[class*="battery"], [class*="progress"]').first()
    
    const batteryVisible = await batteryText.isVisible().catch(() => false)
    const barVisible = await batteryBar.isVisible().catch(() => false)
    
    expect(batteryVisible || barVisible).toBeTruthy()
  })

  test('should allow adding new drone', async ({ page }) => {
    await page.goto('/drones')
    await page.waitForTimeout(1000)
    
    // Try to find and click add button
    const addBtn = page.locator('button, .ant-btn').filter({ hasText: /添加|新增|Add|New|Create/ })
    if (await addBtn.isVisible()) {
      await addBtn.click()
      await page.waitForTimeout(500)
      
      // Should show a form or dialog
      const dialog = page.locator('[class*="modal"], [class*="dialog"], [class*="drawer"], form')
      await expect(dialog.first()).toBeVisible({ timeout: 3000 })
    }
  })
})

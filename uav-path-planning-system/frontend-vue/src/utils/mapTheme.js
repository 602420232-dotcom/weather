/**
 * Leaflet map dark mode utilities
 * Provides dark tile URLs and theme-aware tile layer creation
 */
import L from 'leaflet'

const LIGHT_TILE_URL = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
const LIGHT_ATTRIBUTION = '&copy; OpenStreetMap contributors'

const DARK_TILE_URL = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'
const DARK_ATTRIBUTION = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/">CARTO</a>'

/**
 * Check if current theme is dark
 */
export function isDarkTheme() {
  const theme = document.documentElement.getAttribute('data-theme')
  return theme === 'dark' || theme === 'brand' || theme === 'highContrast'
}

/**
 * Get the appropriate tile URL for current theme
 */
export function getTileUrl() {
  return isDarkTheme() ? DARK_TILE_URL : LIGHT_TILE_URL
}

/**
 * Get attribution for current theme
 */
export function getTileAttribution() {
  return isDarkTheme() ? DARK_ATTRIBUTION : LIGHT_ATTRIBUTION
}

/**
 * Create a theme-aware tile layer
 */
export function createThemeTileLayer(options = {}) {
  const dark = isDarkTheme()
  return L.tileLayer(
    dark ? DARK_TILE_URL : LIGHT_TILE_URL,
    {
      attribution: dark ? DARK_ATTRIBUTION : LIGHT_ATTRIBUTION,
      maxZoom: 18,
      ...options
    }
  )
}

/**
 * Switch a map's base tile layer to match the current theme.
 * Removes the existing base tile layer and adds the appropriate one.
 */
export function switchMapTheme(map, currentTileLayer) {
  if (!map) return null

  // Remove old tile layer
  if (currentTileLayer) {
    try { map.removeLayer(currentTileLayer) } catch (_) {}
  }

  // Add new tile layer at bottom
  const dark = isDarkTheme()
  const newLayer = L.tileLayer(
    dark ? DARK_TILE_URL : LIGHT_TILE_URL,
    {
      attribution: dark ? DARK_ATTRIBUTION : LIGHT_ATTRIBUTION,
      maxZoom: 18
    }
  )
  newLayer.addTo(map)

  // Move tile layer to bottom of all layers
  newLayer.bringToBack()

  return newLayer
}

/**
 * Setup a watcher that switches map tiles when theme changes.
 * Returns a cleanup function to remove the MutationObserver.
 */
export function observeMapTheme(map, tileLayerRef, onSwitch) {
  let currentTile = tileLayerRef

  const observer = new MutationObserver(() => {
    const newTile = switchMapTheme(map, currentTile)
    currentTile = newTile
    if (onSwitch) onSwitch(newTile)
  })

  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme']
  })

  return () => {
    observer.disconnect()
  }
}

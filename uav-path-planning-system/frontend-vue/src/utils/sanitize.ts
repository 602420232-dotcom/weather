/**
 * HTML sanitization utility
 * Strips dangerous tags/attributes to prevent XSS attacks
 * For production use, consider DOMPurify: npm install dompurify
 */

const ALLOWED_TAGS: Set<string> = new Set([
  'b', 'i', 'em', 'strong', 'a', 'p', 'br', 'ul', 'ol', 'li',
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'blockquote', 'code', 'pre',
  'span', 'div', 'img', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
  'hr', 'sub', 'sup', 'del', 'ins', 'mark'
])

const ALLOWED_ATTRS: Set<string> = new Set([
  'href', 'title', 'alt', 'src', 'width', 'height', 'class', 'id',
  'target', 'rel', 'style'
])

const DANGEROUS_PATTERNS: RegExp[] = [
  /javascript\s*:/gi,
  /on\w+\s*=/gi,
  /<script[\s\S]*?<\/script>/gi,
  /<iframe[\s\S]*?<\/iframe>/gi,
  /<object[\s\S]*?<\/object>/gi,
  /<embed[\s\S]*?>/gi,
  /<link[\s\S]*?>/gi,
  /<meta[\s\S]*?>/gi,
]

/**
 * Basic HTML sanitizer - removes dangerous content
 * @param html - Raw HTML string
 * @returns Sanitized HTML string
 */
export function sanitizeHtml(html: string): string {
  if (!html || typeof html !== 'string') return ''

  let sanitized: string = html

  // Remove dangerous patterns
  for (const pattern of DANGEROUS_PATTERNS) {
    sanitized = sanitized.replace(pattern, '')
  }

  // Remove dangerous protocols from href/src
  sanitized = sanitized.replace(/(href|src)\s*=\s*["']javascript:[^"']*["']/gi, '$1=""')

  return sanitized
}

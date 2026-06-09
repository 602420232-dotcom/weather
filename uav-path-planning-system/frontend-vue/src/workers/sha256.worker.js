/**
 * SHA-256 Web Worker
 * 将文件分片的哈希计算从主线程移至 Worker 线程，
 * 避免大文件上传时阻塞 UI 渲染。
 *
 * @usage
 *   const worker = new Worker(new URL('@/workers/sha256.worker.js', import.meta.url), { type: 'module' })
 *   worker.onmessage = (e) => { ... }
 *   worker.postMessage({ chunk: ArrayBuffer, index: number })
 */

self.onmessage = async (e) => {
  const { chunk, index } = e.data
  try {
    const hashBuffer = await crypto.subtle.digest('SHA-256', chunk)
    const hashArray = Array.from(new Uint8Array(hashBuffer))
    const hashHex = hashArray.map((b) => b.toString(16).padStart(2, '0')).join('')
    self.postMessage({ index, hash: hashHex, success: true })
  } catch (err) {
    self.postMessage({ index, hash: null, success: false, error: err.message || '未知错误' })
  }
}

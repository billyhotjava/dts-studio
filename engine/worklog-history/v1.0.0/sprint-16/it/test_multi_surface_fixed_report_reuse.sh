#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:3003}"
ANALYTICS_USERNAME="${ANALYTICS_USERNAME:-admin}"
ANALYTICS_PASSWORD="${ANALYTICS_PASSWORD:-Devops123@}"
CHROME_PORT="${CHROME_PORT:-9224}"

tmp_script="$(mktemp /tmp/dts-fixed-report-reuse.XXXXXX.mjs)"
cleanup() {
  rm -f "$tmp_script"
}
trap cleanup EXIT

cat >"$tmp_script" <<'EOF'
import { spawn } from 'node:child_process'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const BASE = process.env.BASE_URL || 'http://127.0.0.1:3003'
const USERNAME = process.env.ANALYTICS_USERNAME || 'admin'
const PASSWORD = process.env.ANALYTICS_PASSWORD || 'Devops123@'
const CHROME_PORT = Number(process.env.CHROME_PORT || '9224')
const userDataDir = mkdtempSync(join(tmpdir(), 'dts-chrome-'))
const chrome = spawn(
  'google-chrome',
  [
    '--headless=new',
    '--disable-gpu',
    '--no-first-run',
    '--no-default-browser-check',
    `--user-data-dir=${userDataDir}`,
    `--remote-debugging-port=${CHROME_PORT}`,
    'about:blank',
  ],
  { stdio: ['ignore', 'ignore', 'pipe'] },
)

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function fetchJson(url, retries = 50) {
  for (let i = 0; i < retries; i += 1) {
    try {
      const res = await fetch(url)
      if (res.ok) {
        return res.json()
      }
    } catch {}
    await sleep(100)
  }
  throw new Error(`Failed to fetch ${url}`)
}

class CDP {
  constructor(wsUrl) {
    this.ws = new WebSocket(wsUrl)
    this.nextId = 1
    this.pending = new Map()
    this.events = []
    this.ws.onmessage = (event) => {
      const msg = JSON.parse(event.data)
      if (msg.id) {
        const slot = this.pending.get(msg.id)
        if (!slot) return
        this.pending.delete(msg.id)
        if (msg.error) slot.reject(new Error(msg.error.message || JSON.stringify(msg.error)))
        else slot.resolve(msg.result)
        return
      }
      this.events.push(msg)
    }
  }

  async ready() {
    if (this.ws.readyState === WebSocket.OPEN) return
    await new Promise((resolve, reject) => {
      this.ws.onopen = () => resolve()
      this.ws.onerror = (error) => reject(error)
    })
  }

  send(method, params = {}) {
    const id = this.nextId++
    this.ws.send(JSON.stringify({ id, method, params }))
    return new Promise((resolve, reject) => this.pending.set(id, { resolve, reject }))
  }

  async waitFor(predicate, timeoutMs = 10000) {
    const started = Date.now()
    while (Date.now() - started < timeoutMs) {
      const index = this.events.findIndex(predicate)
      if (index >= 0) {
        return this.events.splice(index, 1)[0]
      }
      await sleep(50)
    }
    throw new Error('Timed out waiting for event')
  }

  async evaluate(expression) {
    const result = await this.send('Runtime.evaluate', {
      expression,
      awaitPromise: true,
      returnByValue: true,
    })
    if (result.exceptionDetails) {
      throw new Error(result.exceptionDetails.text || 'evaluation failed')
    }
    return result.result?.value
  }

  close() {
    this.ws.close()
  }
}

function assertCheck(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

async function main() {
  try {
    const version = await fetchJson(`http://127.0.0.1:${CHROME_PORT}/json/version`)
    const browser = new CDP(version.webSocketDebuggerUrl)
    await browser.ready()
    const { targetId } = await browser.send('Target.createTarget', { url: 'about:blank' })
    const pages = await fetchJson(`http://127.0.0.1:${CHROME_PORT}/json/list`)
    const page = pages.find((item) => item.id === targetId)
    if (!page?.webSocketDebuggerUrl) {
      throw new Error('Missing page websocket URL')
    }

    const client = new CDP(page.webSocketDebuggerUrl)
    await client.ready()
    await client.send('Page.enable')
    await client.send('Runtime.enable')
    await client.send('Log.enable')

    async function goto(url, settleMs = 1200) {
      await client.send('Page.navigate', { url })
      await client.waitFor((event) => event.method === 'Page.loadEventFired', 15000)
      await sleep(settleMs)
    }

    async function snapshot() {
      return client.evaluate(`(() => ({
        href: window.location.href,
        text: document.body.innerText || '',
        links: Array.from(document.querySelectorAll('a[href]')).map((a) => ({
          text: (a.textContent || '').trim(),
          href: a.getAttribute('href') || '',
        })),
      }))()`)
    }

    await goto(`${BASE}/auth/login`)
    const loginResult = await client.evaluate(`(async () => {
      const res = await fetch('/api/session', {
        method: 'POST',
        credentials: 'include',
        headers: { 'content-type': 'application/json', accept: 'application/json' },
        body: JSON.stringify({ username: ${JSON.stringify(USERNAME)}, password: ${JSON.stringify(PASSWORD)} }),
      })
      const text = await res.text()
      if (!res.ok) return { ok: false, status: res.status, text }
      sessionStorage.setItem('dts.copilot.login.username', ${JSON.stringify(USERNAME)})
      window.location.href = '/'
      return { ok: true, status: res.status }
    })()`)
    assertCheck(loginResult?.ok === true, `login failed: ${JSON.stringify(loginResult)}`)

    const waitForPath = async (fragment, timeoutMs = 15000) => {
      const started = Date.now()
      while (Date.now() - started < timeoutMs) {
        const href = await client.evaluate('window.location.href')
        if (String(href).includes(fragment)) return href
        await sleep(200)
      }
      throw new Error(`Timed out waiting for ${fragment}`)
    }

    await waitForPath('/dashboards')

    const results = []

    await goto(`${BASE}/fixed-reports`)
    const fixedReports = await snapshot()
    assertCheck(fixedReports.text.includes('固定报表'), 'fixed reports page missing title')
    assertCheck(fixedReports.text.includes('待补数据面'), 'fixed reports page missing placeholder badge')
    results.push({ path: '/fixed-reports', status: 'ok' })

    const surfaces = [
      {
        path: '/dashboards',
        expectedLinkPrefix: '/dashboards/new?fixedReportTemplate=',
        expectedContextText: '固定报表创建上下文',
        expectedRunLinkPrefix: '/fixed-reports/',
      },
      {
        path: '/report-factory',
        expectedLinkPrefix: '/report-factory?fixedReportTemplate=',
        expectedContextText: '固定报表创建上下文',
        expectedRunLinkPrefix: '/fixed-reports/',
      },
      {
        path: '/screens',
        expectedLinkPrefix: '/screens?fixedReportTemplate=',
        expectedContextText: '已从固定报表入口带入当前页面',
        expectedRunLinkPrefix: '/fixed-reports/',
      },
    ]

    for (const surface of surfaces) {
      const settleMs = surface.path === '/screens' ? 2500 : 1200
      await goto(`${BASE}${surface.path}`, settleMs)
      const info = await snapshot()
      assertCheck(info.text.includes('固定报表快捷入口'), `${surface.path} missing quick start section`)
      const creationLink = info.links.find((item) => item.href.startsWith(surface.expectedLinkPrefix))
      assertCheck(Boolean(creationLink), `${surface.path} missing creation-flow link`)
      await goto(`${BASE}${creationLink.href}`, 1800)
      const handoff = await snapshot()
      assertCheck(handoff.text.includes(surface.expectedContextText), `${surface.path} handoff page missing fixed report context`)
      assertCheck(
        handoff.links.some((item) => item.href.startsWith(surface.expectedRunLinkPrefix)),
        `${surface.path} handoff page missing fixed report run link`,
      )
      results.push({ path: surface.path, creationLink: creationLink.href, status: 'ok' })
    }

    console.log(JSON.stringify(results, null, 2))
  } finally {
    chrome.kill('SIGKILL')
    await sleep(200)
    rmSync(userDataDir, { recursive: true, force: true })
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
EOF

node "$tmp_script"
echo "PASS test_multi_surface_fixed_report_reuse"

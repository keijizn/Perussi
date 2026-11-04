const { test, beforeEach } = require('node:test')
const assert = require('node:assert/strict')
const { buildSync } = require('esbuild')
const { Module } = require('node:module')
const path = require('node:path')
const axios = require('axios')

const source = buildSync({ entryPoints: [path.join(__dirname, '../src/services/session.ts')], bundle: true, write: false, platform: 'node', format: 'cjs', external: ['axios'] }).outputFiles[0].text
const compiled = new Module(path.join(__dirname, 'session.cjs'), module)
compiled.filename = path.join(__dirname, 'session.cjs')
compiled.paths = module.paths
compiled._compile(source, compiled.filename)
const { installSessionInterceptors, expirationTime, SESSION_EXPIRED } = compiled.exports
let events
const jwt = (seconds) => `header.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + seconds })).toString('base64url')}.signature`
beforeEach(() => {
  const data = new Map()
  global.localStorage = { getItem: key => data.get(key) ?? null, setItem: (key, value) => data.set(key, value), removeItem: key => data.delete(key) }
  global.window = new EventTarget()
  events = 0
  window.addEventListener(SESSION_EXPIRED, () => events++)
})
function session(token = jwt(60)) {
  localStorage.setItem('token', token)
  localStorage.setItem('user', '{"email":"fixture@example.com"}')
  return token
}
function client(adapter) {
  const result = axios.create({ adapter })
  installSessionInterceptors(result)
  return result
}
function reject(config, status) {
  return Promise.reject(new axios.AxiosError('test', 'ERR_BAD_RESPONSE', config, null, { status, data: {}, headers: {}, config }))
}
test('expired token is cleared before requesting protected data', async () => {
  session(jwt(-1))
  let called = false
  await assert.rejects(client(() => { called = true }).get('/pedidos/meus'))
  assert.equal(called, false)
  assert.equal(localStorage.getItem('token'), null)
  assert.equal(localStorage.getItem('user'), null)
  assert.equal(events, 1)
})
test('valid token is attached and server 401 expires the current session', async () => {
  const token = session()
  await assert.rejects(client(config => { assert.equal(config.headers.Authorization, `Bearer ${token}`); return reject(config, 401) }).get('/pedidos/meus'))
  assert.equal(events, 1)
  assert.equal(localStorage.getItem('token'), null)
})
test('403 and server errors preserve the session', async () => {
  const token = session()
  for (const status of [403, 500, 503]) await assert.rejects(client(config => reject(config, status)).get('/pedidos/meus'))
  assert.equal(localStorage.getItem('token'), token)
  assert.equal(events, 0)
})
test('network failure preserves the session', async () => {
  const token = session()
  await assert.rejects(client(config => Promise.reject(new axios.AxiosError('offline', 'ERR_NETWORK', config))).get('/pedidos/meus'))
  assert.equal(localStorage.getItem('token'), token)
  assert.equal(events, 0)
})
test('login errors do not send an old token or expire a session', async () => {
  session(jwt(-1))
  await assert.rejects(client(config => { assert.equal(config.headers.Authorization, undefined); return reject(config, 401) }).post('/auth/login'))
  assert.equal(events, 0)
})
test('delayed 401 from a previous login does not erase a new session', async () => {
  session()
  const newer = jwt(120)
  await assert.rejects(client(config => { session(newer); return reject(config, 401) }).get('/pedidos/meus'))
  assert.equal(localStorage.getItem('token'), newer)
  assert.equal(events, 0)
})
test('malformed and missing-expiration tokens are treated as invalid', () => {
  for (const token of ['broken', 'a.e30.signature', 'a.b.c']) assert.equal(expirationTime(token), 0)
  assert.ok(expirationTime(jwt(60)) > Date.now())
})

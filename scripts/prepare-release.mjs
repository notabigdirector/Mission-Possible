import { readFile } from 'node:fs/promises'

const token = process.env.GH_TOKEN || process.env.GITHUB_TOKEN
if (!token) {
  console.error('缺少 GH_TOKEN 或 GITHUB_TOKEN 环境变量')
  process.exit(1)
}

const owner = 'notabigdirector'
const repo = 'Mission-Possible'
const { version } = JSON.parse(await readFile(new URL('../package.json', import.meta.url), 'utf8'))
const tag = `v${version}`
const base = `https://api.github.com/repos/${owner}/${repo}`

const headers = {
  Authorization: `Bearer ${token}`,
  Accept: 'application/vnd.github+json',
}

async function api(path, options = {}) {
  const res = await fetch(`${base}${path}`, {
    ...options,
    headers: { ...headers, ...options.headers },
  })
  if (!res.ok && res.status !== 404) {
    throw new Error(`${options.method || 'GET'} ${path}: ${res.status} ${await res.text()}`)
  }
  return res
}

async function listReleasesForTag() {
  const found = []
  for (let page = 1; ; page++) {
    const res = await api(`/releases?per_page=100&page=${page}`)
    if (res.status === 404) break
    const list = await res.json()
    found.push(...list.filter((r) => r.tag_name === tag))
    if (list.length < 100) break
  }
  return found
}

// 删除该 tag 的所有历史 Release,确保发布前只有一条空记录
for (const release of await listReleasesForTag()) {
  console.log(`删除旧 Release: ${release.tag_name} (id=${release.id})`)
  await api(`/releases/${release.id}`, { method: 'DELETE' })
}

// 预建唯一的空 Release,draft: false 立即发布,避免 electron-builder 并发创建重复记录
const created = await api('/releases', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ tag_name: tag, name: version, draft: false, prerelease: false }),
})
if (!created.ok) {
  throw new Error(`创建 Release 失败: ${created.status} ${await created.text()}`)
}

const release = await created.json()
console.log(`已创建 Release: ${release.tag_name} (id=${release.id})`)

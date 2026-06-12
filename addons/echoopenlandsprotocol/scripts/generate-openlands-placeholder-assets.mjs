import fs from 'node:fs'
import path from 'node:path'
import zlib from 'node:zlib'

const MODULE_ID = 'echoopenlandsprotocol'
const TEXTURE_SIZE = 16
const FACE_NAMES = ['down', 'up', 'north', 'south', 'west', 'east']

function parseArgs(argv) {
  const args = { moduleRoot: null }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return args
}

function findModuleRoot(explicitRoot) {
  if (explicitRoot) return path.resolve(explicitRoot)
  let cursor = process.cwd()
  for (;;) {
    const descriptor = path.join(cursor, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fs.existsSync(descriptor)) return cursor
    const candidate = path.join(cursor, 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fs.existsSync(candidate)) return path.join(cursor, 'addons', MODULE_ID)
    const parent = path.dirname(cursor)
    if (parent === cursor) break
    cursor = parent
  }
  throw new Error('Could not find echoopenlandsprotocol module root. Pass --module-root <path>.')
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'))
}

function writeJson(filePath, payload) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`)
}

function normalizeId(value) {
  return String(value).includes(':') ? String(value).split(':').pop() : String(value)
}

function localTextureKey(texture, expectedPrefix) {
  const raw = String(texture ?? '')
  const prefix = `${expectedPrefix}/`
  if (raw.startsWith(prefix)) return raw.slice(prefix.length)
  return raw
}

function hashText(text) {
  let hash = 2166136261
  for (let index = 0; index < text.length; index += 1) {
    hash ^= text.charCodeAt(index)
    hash = Math.imul(hash, 16777619)
  }
  return hash >>> 0
}

function paletteFor(kind, entry) {
  const id = normalizeId(entry.id)
  const category = String(entry.category ?? entry.useType ?? '')
  const tags = Array.isArray(entry.tags) ? entry.tags.join(' ') : ''
  const hay = `${id} ${category} ${tags}`
  if (kind === 'item') {
    if (hay.includes('food') || hay.includes('berries') || hay.includes('meat') || hay.includes('grain') || hay.includes('crop')) return [154, 92, 61]
    if (hay.includes('metal') || hay.includes('ingot') || hay.includes('ore')) return [165, 132, 88]
    if (hay.includes('tool') || hay.includes('pick') || hay.includes('knife') || hay.includes('hammer')) return [92, 103, 106]
    if (hay.includes('waystone') || hay.includes('rubbing') || hay.includes('road')) return [101, 116, 136]
    if (hay.includes('fiber') || hay.includes('binding') || hay.includes('hide')) return [124, 114, 82]
    return [121, 103, 79]
  }
  if (hay.includes('wood') || hay.includes('branchwood') || hay.includes('pine')) return [105, 76, 50]
  if (hay.includes('stone') || hay.includes('granite') || hay.includes('shale') || hay.includes('deepstone')) return [105, 110, 105]
  if (hay.includes('ore') || hay.includes('crystal')) return [94, 101, 112]
  if (hay.includes('terrain') || hay.includes('soil') || hay.includes('mud') || hay.includes('clay')) return [102, 111, 68]
  if (hay.includes('road') || hay.includes('waystone')) return [112, 107, 91]
  return [116, 98, 72]
}

function colorWithNoise(base, id, x, y, channelShift = 0) {
  const hash = hashText(`${id}:${x}:${y}:${channelShift}`)
  const delta = ((hash & 15) - 7)
  return Math.max(0, Math.min(255, base + delta))
}

function drawBlockTexture(entry) {
  const id = normalizeId(entry.id)
  const base = paletteFor('block', entry)
  const accent = base.map((value) => Math.min(255, value + 48))
  const dark = base.map((value) => Math.max(0, value - 38))
  const pixels = Buffer.alloc(TEXTURE_SIZE * TEXTURE_SIZE * 4)
  for (let y = 0; y < TEXTURE_SIZE; y += 1) {
    for (let x = 0; x < TEXTURE_SIZE; x += 1) {
      let color = [
        colorWithNoise(base[0], id, x, y, 0),
        colorWithNoise(base[1], id, x, y, 1),
        colorWithNoise(base[2], id, x, y, 2),
      ]
      if ((x + y) % 9 === 0) color = dark
      if (Math.abs(x - y) <= 1) color = accent
      if (x === 0 || y === 0 || x === TEXTURE_SIZE - 1 || y === TEXTURE_SIZE - 1) color = dark
      const offset = (y * TEXTURE_SIZE + x) * 4
      pixels[offset] = color[0]
      pixels[offset + 1] = color[1]
      pixels[offset + 2] = color[2]
      pixels[offset + 3] = 255
    }
  }
  return pixels
}

function drawItemTexture(entry) {
  const id = normalizeId(entry.id)
  const base = paletteFor('item', entry)
  const accent = base.map((value) => Math.min(255, value + 58))
  const dark = base.map((value) => Math.max(0, value - 45))
  const pixels = Buffer.alloc(TEXTURE_SIZE * TEXTURE_SIZE * 4)
  for (let y = 0; y < TEXTURE_SIZE; y += 1) {
    for (let x = 0; x < TEXTURE_SIZE; x += 1) {
      const offset = (y * TEXTURE_SIZE + x) * 4
      const inside = x >= 3 && x <= 12 && y >= 3 && y <= 12
      const diagonal = Math.abs(x - y) <= 1
      if (!inside && !diagonal) {
        pixels[offset] = 0
        pixels[offset + 1] = 0
        pixels[offset + 2] = 0
        pixels[offset + 3] = 0
        continue
      }
      let color = [
        colorWithNoise(base[0], id, x, y, 0),
        colorWithNoise(base[1], id, x, y, 1),
        colorWithNoise(base[2], id, x, y, 2),
      ]
      if (x === 3 || x === 12 || y === 3 || y === 12) color = dark
      if (diagonal) color = accent
      pixels[offset] = color[0]
      pixels[offset + 1] = color[1]
      pixels[offset + 2] = color[2]
      pixels[offset + 3] = 255
    }
  }
  return pixels
}

function crc32(buffer) {
  let crc = 0xffffffff
  for (const byte of buffer) {
    crc ^= byte
    for (let bit = 0; bit < 8; bit += 1) {
      crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1))
    }
  }
  return (crc ^ 0xffffffff) >>> 0
}

function pngChunk(type, data) {
  const typeBuffer = Buffer.from(type, 'ascii')
  const length = Buffer.alloc(4)
  length.writeUInt32BE(data.length, 0)
  const crcInput = Buffer.concat([typeBuffer, data])
  const crc = Buffer.alloc(4)
  crc.writeUInt32BE(crc32(crcInput), 0)
  return Buffer.concat([length, typeBuffer, data, crc])
}

function pngBuffer(width, height, rgba) {
  const header = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10])
  const ihdr = Buffer.alloc(13)
  ihdr.writeUInt32BE(width, 0)
  ihdr.writeUInt32BE(height, 4)
  ihdr[8] = 8
  ihdr[9] = 6
  ihdr[10] = 0
  ihdr[11] = 0
  ihdr[12] = 0
  const stride = width * 4
  const raw = Buffer.alloc((stride + 1) * height)
  for (let y = 0; y < height; y += 1) {
    raw[y * (stride + 1)] = 0
    rgba.copy(raw, y * (stride + 1) + 1, y * stride, y * stride + stride)
  }
  return Buffer.concat([
    header,
    pngChunk('IHDR', ihdr),
    pngChunk('IDAT', zlib.deflateSync(raw)),
    pngChunk('IEND', Buffer.alloc(0)),
  ])
}

function writePng(filePath, rgba) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.writeFileSync(filePath, pngBuffer(TEXTURE_SIZE, TEXTURE_SIZE, rgba))
}

function blockModel(id, textureKey) {
  const texture = `${MODULE_ID}:block/${textureKey}`
  const faces = Object.fromEntries(FACE_NAMES.map((face) => [face, { texture: '#all' }]))
  return {
    credit: 'Generated Echo-owned Openlands placeholder model. Replace before public art approval.',
    textures: { all: texture },
    elements: [
      {
        from: [0, 0, 0],
        to: [16, 16, 16],
        faces,
      },
    ],
  }
}

function itemModel(id, textureKey) {
  const texture = `${MODULE_ID}:item/${textureKey}`
  return {
    credit: 'Generated Echo-owned Openlands placeholder item model. Replace before public art approval.',
    textures: { layer0: texture },
    elements: [
      {
        from: [2, 2, 7.5],
        to: [14, 14, 8.5],
        faces: {
          north: { texture: '#layer0' },
          south: { texture: '#layer0' },
          west: { texture: '#layer0' },
          east: { texture: '#layer0' },
          up: { texture: '#layer0' },
          down: { texture: '#layer0' },
        },
      },
    ],
  }
}

function generate(moduleRoot) {
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const assetsRoot = path.join(resourcesRoot, 'assets', MODULE_ID)
  const blocks = readJson(path.join(dataRoot, 'blocks', 'mvp_blocks.json')).blocks ?? []
  const items = readJson(path.join(dataRoot, 'items', 'mvp_items.json')).items ?? []

  let blockAssets = 0
  let itemAssets = 0
  for (const block of blocks) {
    const id = normalizeId(block.id)
    const textureKey = localTextureKey(block.texture, 'block')
    writeJson(path.join(assetsRoot, 'blockstates', `${id}.json`), {
      variants: {
        '': {
          model: `${MODULE_ID}:block/${id}`,
        },
      },
    })
    writeJson(path.join(assetsRoot, 'models', 'block', `${id}.json`), blockModel(id, textureKey))
    writePng(path.join(assetsRoot, 'textures', 'block', `${textureKey}.png`), drawBlockTexture(block))
    blockAssets += 3
  }

  for (const item of items) {
    const id = normalizeId(item.id)
    const textureKey = localTextureKey(item.texture, 'item')
    writeJson(path.join(assetsRoot, 'models', 'item', `${id}.json`), itemModel(id, textureKey))
    writePng(path.join(assetsRoot, 'textures', 'item', `${textureKey}.png`), drawItemTexture(item))
    itemAssets += 2
  }

  return {
    blocks: blocks.length,
    items: items.length,
    blockAssetFiles: blockAssets,
    itemAssetFiles: itemAssets,
  }
}

try {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    console.log(`Usage: node scripts/generate-openlands-placeholder-assets.mjs [--module-root <path>]`)
  } else {
    const moduleRoot = findModuleRoot(args.moduleRoot)
    const result = generate(moduleRoot)
    console.log(`Generated Openlands placeholder assets: ${result.blocks} blocks (${result.blockAssetFiles} files), ${result.items} items (${result.itemAssetFiles} files).`)
  }
} catch (error) {
  console.error(error.message)
  process.exitCode = 1
}

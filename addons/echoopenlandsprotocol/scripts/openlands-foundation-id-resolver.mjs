export function normalizeEchoId(value) {
  if (typeof value !== 'string') return value
  return value.includes(':') ? value.split(':').pop() : value
}

export function addNormalizedIds(target, values) {
  for (const value of values ?? []) target.add(normalizeEchoId(value))
  return target
}

export function addFoundationKnownIds(target, conformance, kind, aliasBridge = null) {
  const registryKeyByKind = {
    block: 'blocksMovedToFoundation',
    item: 'itemsMovedToFoundation',
    recipe: 'recipesMovedToFoundation',
  }
  const registryKey = registryKeyByKind[kind]
  if (!registryKey) return target

  addNormalizedIds(target, conformance?.foundationRegistries?.[registryKey] ?? [])
  const knownCanonicalIds = new Set(target)
  for (const alias of aliasBridge?.aliases ?? []) {
    const canonicalId = normalizeEchoId(alias.canonicalId)
    if (!knownCanonicalIds.has(canonicalId)) continue
    target.add(canonicalId)
    target.add(normalizeEchoId(alias.legacyId))
  }
  return target
}

export function addFoundationKnownBlocks(target, conformance, aliasBridge = null) {
  return addFoundationKnownIds(target, conformance, 'block', aliasBridge)
}

export function addFoundationKnownItems(target, conformance, aliasBridge = null) {
  return addFoundationKnownIds(target, conformance, 'item', aliasBridge)
}

export function addFoundationKnownRecipes(target, conformance, aliasBridge = null) {
  return addFoundationKnownIds(target, conformance, 'recipe', aliasBridge)
}

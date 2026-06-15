#!/usr/bin/env node
import { promises as fs } from 'node:fs'
import path from 'node:path'
import { generateContentGraph, summarizeContentGraphEvidence } from './generate-content-graph.mjs'

const FORBIDDEN_RUNTIME_PATTERNS = [
  /net\.minecraftforge/,
  /net\.neoforged/,
  /net\.minecraft/,
  /BlockEntity/,
  /Screen\s*$/,
  /Menu\s*$/,
  /registry\./,
]

function detectRuntimeClasses(node) {
  const text = JSON.stringify(node)
  const hits = []
  for (const pattern of FORBIDDEN_RUNTIME_PATTERNS) {
    if (pattern.test(text)) hits.push(pattern.source)
  }
  return hits
}

function portableFieldViolations(node) {
  // Check that no runtime class strings appear outside runtimeHints/data
  const hits = []
  const inspect = (value, path) => {
    if (typeof value === 'string') {
      for (const pattern of FORBIDDEN_RUNTIME_PATTERNS) {
        if (pattern.test(value)) hits.push(`${path}: ${value}`)
      }
    } else if (Array.isArray(value)) {
      value.forEach((v, i) => inspect(v, `${path}[${i}]`))
    } else if (typeof value === 'object' && value !== null) {
      for (const [key, child] of Object.entries(value)) {
        if (key === 'runtimeHints') continue
        inspect(child, `${path}.${key}`)
      }
    }
  }
  inspect(node, '$')
  return hits
}

async function fileExists(filePath) {
  try {
    await fs.access(filePath)
    return true
  } catch {
    return false
  }
}

function valueType(value) {
  if (Array.isArray(value)) return 'array'
  if (value === null) return 'null'
  if (Number.isInteger(value)) return 'integer'
  return typeof value
}

function schemaTypeMatches(expected, value) {
  const types = Array.isArray(expected) ? expected : [expected]
  const actual = valueType(value)
  return types.some((type) => type === actual || (type === 'number' && actual === 'integer'))
}

function validateSchemaValue(schema, value, pointer = '$') {
  const errors = []
  if (schema.const !== undefined && value !== schema.const) {
    errors.push(`${pointer} must equal ${JSON.stringify(schema.const)}`)
    return errors
  }
  if (schema.enum && !schema.enum.some((item) => item === value)) {
    errors.push(`${pointer} must be one of ${schema.enum.map((item) => JSON.stringify(item)).join(', ')}`)
    return errors
  }
  if (schema.type && !schemaTypeMatches(schema.type, value)) {
    errors.push(`${pointer} must be ${Array.isArray(schema.type) ? schema.type.join(' or ') : schema.type}`)
    return errors
  }
  if (typeof value === 'string') {
    if (schema.minLength !== undefined && value.length < schema.minLength) errors.push(`${pointer} must not be empty`)
    if (schema.pattern && !(new RegExp(schema.pattern).test(value))) errors.push(`${pointer} does not match ${schema.pattern}`)
  }
  if (typeof value === 'number' && schema.minimum !== undefined && value < schema.minimum) {
    errors.push(`${pointer} must be >= ${schema.minimum}`)
  }
  if (Array.isArray(value)) {
    if (schema.minItems !== undefined && value.length < schema.minItems) errors.push(`${pointer} must contain at least ${schema.minItems} item(s)`)
    if (schema.uniqueItems && new Set(value.map((item) => JSON.stringify(item))).size !== value.length) errors.push(`${pointer} must contain unique items`)
    if (schema.items) {
      value.forEach((item, index) => errors.push(...validateSchemaValue(schema.items, item, `${pointer}[${index}]`)))
    }
  }
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    for (const field of schema.required ?? []) {
      if (value[field] === undefined) errors.push(`${pointer}.${field} is required`)
    }
    for (const [field, childSchema] of Object.entries(schema.properties ?? {})) {
      if (value[field] !== undefined) errors.push(...validateSchemaValue(childSchema, value[field], `${pointer}.${field}`))
    }
    if (schema.additionalProperties === false) {
      const allowed = new Set(Object.keys(schema.properties ?? {}))
      for (const field of Object.keys(value)) {
        if (!allowed.has(field)) errors.push(`${pointer}.${field} is not allowed`)
      }
    } else if (schema.additionalProperties && typeof schema.additionalProperties === 'object') {
      const known = new Set(Object.keys(schema.properties ?? {}))
      for (const [field, childValue] of Object.entries(value)) {
        if (!known.has(field)) errors.push(...validateSchemaValue(schema.additionalProperties, childValue, `${pointer}.${field}`))
      }
    }
  }
  return errors
}

async function validateEvidenceAgainstSdkSchema(evidence, sdkRoot) {
  const schemaPath = path.resolve(sdkRoot, 'schemas', 'content-graph-evidence.schema.json')
  if (!(await fileExists(schemaPath))) {
    return { missing: schemaPath, errors: [] }
  }
  const schema = JSON.parse(await fs.readFile(schemaPath, 'utf8'))
  return { missing: null, errors: validateSchemaValue(schema, evidence) }
}

export async function validateContentGraph({ strict = false, moduleIds = [], sdkRoot = path.resolve(process.cwd(), '..', 'ECHO-SDK') } = {}) {
  const results = await generateContentGraph({ moduleIds })
  // Build a global node id namespace from all module graphs so cross-module edges validate.
  const allModuleResults = await generateContentGraph({ moduleIds: [] })
  const globalNodeIds = new Set(allModuleResults.flatMap((r) => r.graph.nodes.map((n) => n.id)))
  // Always allow references to synthetic runtime nodes.
  const RUNTIME_IDS = ['neoforge', 'echo_native', 'echo_runtime_standalone', 'hytale'].map((t) => `echo:runtime/${t}`)
  for (const id of RUNTIME_IDS) globalNodeIds.add(id)

  const errors = []
  const warnings = []
  let totalNodes = 0
  let totalEdges = 0

  for (const result of results) {
    const graph = result.graph
    totalNodes += graph.nodes.length
    totalEdges += graph.edges.length
    const nodeIds = new Set([...globalNodeIds, ...graph.nodes.map((n) => n.id)])

    for (const node of graph.nodes) {
      if (!node.schemaVersion) errors.push(`${result.moduleId}: node ${node.id} missing schemaVersion`)
      if (!node.kind) errors.push(`${result.moduleId}: node ${node.id} missing kind`)
      if (!node.id) errors.push(`${result.moduleId}: node missing id`)
      if (!node.moduleId) errors.push(`${result.moduleId}: node ${node.id} missing moduleId`)
      if (!node.provenance?.generatedBy || !node.provenance?.generatedAt) {
        errors.push(`${result.moduleId}: node ${node.id} missing provenance`)
      }
      const runtimeHits = detectRuntimeClasses(node)
      const portableHits = portableFieldViolations(node)
      if (portableHits.length > 0) {
        errors.push(`${result.moduleId}: node ${node.id} contains runtime classes in portable fields: ${portableHits.join(', ')}`)
      }
      if (node.kind === 'echo:ui_intent' && !node.intent) {
        errors.push(`${result.moduleId}: ui_intent ${node.id} missing intent`)
      }
    }

    for (const edge of graph.edges) {
      if (!edge.from || !edge.to) {
        errors.push(`${result.moduleId}: edge ${edge.id} missing from/to`)
        continue
      }
      if (!nodeIds.has(edge.from)) {
        errors.push(`${result.moduleId}: edge ${edge.id} references missing from node ${edge.from}`)
      }
      if (!nodeIds.has(edge.to)) {
        errors.push(`${result.moduleId}: edge ${edge.id} references missing to node ${edge.to}`)
      }
    }

    const plan = result.plans.hytale
    const plannedIds = new Set(plan.nodes.map((n) => n.nodeId))
    for (const node of graph.nodes) {
      if (!plannedIds.has(node.id)) {
        errors.push(`${result.moduleId}: Hytale plan missing node ${node.id}`)
      }
    }

    if (strict && graph.unresolvedReferences.some((ref) => ref.required)) {
      errors.push(`${result.moduleId}: has required unresolved references`)
    }

    warnings.push(...graph.unresolvedReferences.map((ref) => `${result.moduleId}: unresolved ${ref.id} (${ref.context})`))
  }

  const evidence = summarizeContentGraphEvidence(results, { source: 'ECHO-Modules/content-graph-validation' })
  const evidenceSchema = await validateEvidenceAgainstSdkSchema(evidence, sdkRoot)
  if (evidenceSchema.missing) {
    const message = `SDK content graph evidence schema not found at ${evidenceSchema.missing}`
    if (strict) errors.push(message)
    else warnings.push(message)
  } else if (evidenceSchema.errors.length > 0) {
    errors.push(...evidenceSchema.errors.map((error) => `content-graph-evidence: ${error}`))
  }

  return {
    moduleCount: results.length,
    totalNodes,
    totalEdges,
    evidence,
    errors,
    warnings,
    passed: errors.length === 0,
  }
}

function parseArgs(argv) {
  return {
    strict: argv.includes('--strict'),
    help: argv.includes('--help'),
    moduleIds: argv.includes('--module') ? argv[argv.indexOf('--module') + 1]?.split(',') ?? [] : [],
    sdkRoot: argv.includes('--sdk-root') ? argv[argv.indexOf('--sdk-root') + 1] : path.resolve(process.cwd(), '..', 'ECHO-SDK'),
  }
}

const options = parseArgs(process.argv.slice(2))
if (options.help) {
  console.log('Usage: node scripts/validate-content-graph.mjs [--strict] [--module id1,id2] [--sdk-root ../ECHO-SDK]')
  process.exit(0)
}

validateContentGraph({ strict: options.strict, moduleIds: options.moduleIds, sdkRoot: options.sdkRoot })
  .then((result) => {
    console.log(`Validated ${result.moduleCount} module(s), ${result.totalNodes} nodes, ${result.totalEdges} edges.`)
    if (result.warnings.length > 0) {
      console.log(`Warnings (${result.warnings.length}):`)
      for (const warning of result.warnings.slice(0, 20)) console.log(`  - ${warning}`)
      if (result.warnings.length > 20) console.log(`  ... and ${result.warnings.length - 20} more`)
    }
    if (result.errors.length > 0) {
      console.error(`Errors (${result.errors.length}):`)
      for (const error of result.errors.slice(0, 50)) console.error(`  - ${error}`)
      process.exitCode = 1
    } else {
      console.log('Content graph validation passed.')
    }
  })
  .catch((error) => {
    console.error(error)
    process.exitCode = 1
  })

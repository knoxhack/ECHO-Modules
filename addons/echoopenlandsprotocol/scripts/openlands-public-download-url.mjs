function normalizeHostname(value) {
  return String(value ?? '')
    .trim()
    .toLowerCase()
    .replace(/^\[/, '')
    .replace(/\]$/, '')
    .replace(/\.$/, '')
}

function parseIpv4(hostname) {
  const parts = hostname.split('.')
  if (parts.length !== 4) return null
  const octets = []
  for (const part of parts) {
    if (!/^\d{1,3}$/.test(part)) return null
    const octet = Number.parseInt(part, 10)
    if (octet < 0 || octet > 255) return null
    octets.push(octet)
  }
  return octets
}

function ipv4Reason(octets) {
  const [a, b, c, d] = octets
  if (a === 0) return '0.0.0.0/8 is not public'
  if (a === 10) return '10.0.0.0/8 is private'
  if (a === 100 && b >= 64 && b <= 127) return '100.64.0.0/10 is carrier-grade NAT'
  if (a === 127) return '127.0.0.0/8 is loopback'
  if (a === 169 && b === 254) return '169.254.0.0/16 is link-local'
  if (a === 172 && b >= 16 && b <= 31) return '172.16.0.0/12 is private'
  if (a === 192 && b === 168) return '192.168.0.0/16 is private'
  if (a === 192 && b === 0 && c === 0) return '192.0.0.0/24 is not public'
  if (a === 192 && b === 0 && c === 2) return '192.0.2.0/24 is documentation-only'
  if (a === 198 && (b === 18 || b === 19)) return '198.18.0.0/15 is benchmarking-only'
  if (a === 198 && b === 51 && c === 100) return '198.51.100.0/24 is documentation-only'
  if (a === 203 && b === 0 && c === 113) return '203.0.113.0/24 is documentation-only'
  if (a >= 224) return `${a}.${b}.${c}.${d} is multicast or reserved`
  return null
}

function ipv6Reason(hostname) {
  if (!hostname.includes(':')) return null
  const normalized = hostname.toLowerCase()
  if (normalized === '::' || normalized === '0:0:0:0:0:0:0:0') return 'IPv6 unspecified address is not public'
  if (normalized === '::1' || normalized === '0:0:0:0:0:0:0:1') return 'IPv6 loopback is not public'
  if (normalized.startsWith('fc') || normalized.startsWith('fd')) return 'fc00::/7 is unique-local'
  if (/^fe[89ab]/.test(normalized)) return 'fe80::/10 is link-local'
  if (normalized.startsWith('ff')) return 'ff00::/8 is multicast'
  if (normalized.startsWith('2001:db8:') || normalized === '2001:db8::') return '2001:db8::/32 is documentation-only'
  const mappedIpv4 = normalized.match(/::ffff:(\d+\.\d+\.\d+\.\d+)$/)
  if (mappedIpv4) {
    const octets = parseIpv4(mappedIpv4[1])
    if (octets) return ipv4Reason(octets)
  }
  return null
}

function hostnameReason(hostname) {
  if (!hostname) return 'hostname is empty'
  if (hostname === 'localhost' || hostname.endsWith('.localhost')) return 'localhost is not public'
  if (hostname.endsWith('.local') || hostname.endsWith('.lan') || hostname.endsWith('.home') || hostname.endsWith('.internal')) {
    return 'local network hostnames are not public'
  }
  if (hostname.endsWith('.test') || hostname.endsWith('.invalid') || hostname.endsWith('.example')) {
    return 'reserved testing hostnames are not public'
  }
  if (['example.com', 'example.net', 'example.org'].includes(hostname) || hostname.endsWith('.example.com') || hostname.endsWith('.example.net') || hostname.endsWith('.example.org')) {
    return 'example domains are documentation-only'
  }
  if (!hostname.includes('.') && parseIpv4(hostname) === null && !hostname.includes(':')) {
    return 'hostname must be a public DNS name or public IP address'
  }
  const ipv4 = parseIpv4(hostname)
  if (ipv4) return ipv4Reason(ipv4)
  return ipv6Reason(hostname)
}

export function publicHttpsUrlReason(value) {
  if (typeof value !== 'string' || value.length === 0) return 'URL is empty'
  let parsed
  try {
    parsed = new URL(value)
  } catch {
    return 'URL is not parseable'
  }
  if (parsed.protocol !== 'https:') return 'URL must use https'
  return hostnameReason(normalizeHostname(parsed.hostname))
}

export function isPublicHttpsUrl(value) {
  return publicHttpsUrlReason(value) === null
}


import fs from 'node:fs'

const layoutCandidates = [
  'app/layout.tsx',
  'app/layout.jsx',
  'app/layout.js',
  'src/app/layout.tsx',
  'src/app/layout.jsx',
  'src/app/layout.js',
]
const layoutPath = layoutCandidates.find((candidate) => fs.existsSync(candidate))

if (!layoutPath) {
  console.warn('[built-with-v0] Could not find a Next.js root layout to patch')
  process.exit(0)
}

// Badge injection logic ko yahan se hata diya gaya hai taaki yeh aage se kabhi na aaye.
console.log('[built-with-v0] Badge injection skipped successfully.')


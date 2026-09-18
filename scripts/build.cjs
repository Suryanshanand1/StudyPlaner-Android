const { spawnSync } = require("node:child_process")
const path = require("node:path")

function run(cmd, args) {
  const result = spawnSync(cmd, args, { stdio: "inherit" })
  if (result.error) throw result.error
  if (result.status !== 0) process.exit(result.status ?? 1)
}

process.env.NEXT_PUBLIC_APP_VERSION = require("../version.json").version
run(process.execPath, [path.join(__dirname, "generate-icons.cjs")])
run(process.execPath, [path.join(__dirname, "..", "node_modules", "next", "dist", "bin", "next"), "build"])
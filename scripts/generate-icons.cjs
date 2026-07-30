const sharp = require("sharp")
const path = require("path")

const sizes = [192, 512]

async function main() {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
    <rect width="100" height="100" rx="20" fill="#18181b"/>
    <text x="50" y="62" font-size="48" text-anchor="middle" fill="white" font-family="sans-serif" font-weight="bold">S</text>
  </svg>`

  for (const size of sizes) {
    await sharp(Buffer.from(svg))
      .resize(size, size)
      .png()
      .toFile(path.join(__dirname, "..", "public", `icon-${size}.png`))
    console.log(`Generated icon-${size}.png`)
  }
}

main().catch(console.error)

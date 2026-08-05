import { readFileSync, writeFileSync } from 'fs'
import sharp from 'sharp'
import pngToIco from 'png-to-ico'

const svg = readFileSync('resources/icon.svg')

const pngSizes = [16, 24, 32, 48, 64, 128, 256, 512]
const icoSizes = [16, 24, 32, 48, 64, 128, 256]

async function main() {
  const pngs = []
  for (const size of pngSizes) {
    const png = await sharp(svg).resize(size, size).png().toBuffer()
    pngs.push({ size, png })
  }

  writeFileSync('build/icon.png', pngs.find((p) => p.size === 512).png)
  writeFileSync('resources/icon.png', pngs.find((p) => p.size === 512).png)

  const icoBuffers = icoSizes.map((s) => pngs.find((p) => p.size === s).png)
  const ico = await pngToIco(icoBuffers)
  writeFileSync('build/icon.ico', ico)

  console.log('生成完成: build/icon.png, build/icon.ico, resources/icon.png')
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})

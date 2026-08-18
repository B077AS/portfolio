const fs = require("fs");
const path = require("path");

const destDir = path.join("src", "main", "resources", "static", "fonts");
fs.mkdirSync(destDir, { recursive: true });

const fonts = [
  { pkg: "@fontsource/ibm-plex-sans", family: "ibm-plex-sans", weights: [400, 500, 600, 700] },
  { pkg: "@fontsource/ibm-plex-mono", family: "ibm-plex-mono", weights: [400, 500, 600] },
];

for (const { pkg, family, weights } of fonts) {
  const filesDir = path.join("node_modules", pkg, "files");
  for (const weight of weights) {
    const filename = `${family}-latin-${weight}-normal.woff2`;
    const src = path.join(filesDir, filename);
    const dest = path.join(destDir, filename);
    if (!fs.existsSync(src)) {
      throw new Error(`Expected font file not found: ${src}`);
    }
    fs.copyFileSync(src, dest);
  }
}

console.log("Copied brand font files into", destDir);

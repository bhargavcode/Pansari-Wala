// Production CI: skip source maps (Compose/Skiko maps are huge and slow webpack).
// Terser takes ~25 min on the ~27 MiB Compose bundle; esbuild minifies it in seconds.
if (config.mode === "production") {
    config.devtool = false;
    const { EsbuildPlugin } = require("esbuild-loader");
    config.optimization = Object.assign({}, config.optimization, {
        minimize: true,
        minimizer: [new EsbuildPlugin({ target: "esnext", legalComments: "none" })],
    });
}

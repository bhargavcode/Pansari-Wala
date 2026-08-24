// Production CI: skip source maps (Compose/Skiko maps are huge and slow webpack).
if (config.mode === "production") {
    config.devtool = false;
}

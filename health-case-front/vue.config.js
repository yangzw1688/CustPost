const { defineConfig } = require("@vue/cli-service")
const target = process.env.VUE_APP_API_TARGET || "http://localhost:8099"
module.exports = defineConfig({
  devServer: { port: 8081, proxy: { "/api": { target, changeOrigin: true }, "/upload": { target, changeOrigin: true } } }
})

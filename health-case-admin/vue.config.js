const { defineConfig } = require('@vue/cli-service')
const apiTarget = process.env.VUE_APP_API_TARGET || 'http://localhost:8099'

module.exports = defineConfig({
  publicPath: '/admin/',
  devServer: {
    port: 8082,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true
      },
      '/upload': {
        target: apiTarget,
        changeOrigin: true
      }
    }
  }
})

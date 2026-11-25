// Maven 复制资源前先构建两个 Vue 应用，生成同一 JAR 内的页面文件。
const { existsSync } = require('node:fs')
const path = require('node:path')
const { spawnSync } = require('node:child_process')

function run(command, args, cwd) {
  const result = spawnSync(command, args, { cwd, stdio: 'inherit', windowsHide: true })
  if (result.error) throw result.error
  if (result.status !== 0) process.exit(result.status || 1)
}

for (const name of ['health-case-front', 'health-case-admin']) {
  const cwd = path.resolve(__dirname, '..', name)
  const cli = path.join(cwd, 'node_modules/@vue/cli-service/bin/vue-cli-service.js')
  console.log(`Building ${name}`)
  if (!existsSync(cli)) {
    // 优先遵循现有锁文件；固定 Yarn 版本，无需预先全局安装。
    const installArgs = existsSync(path.join(cwd, 'package-lock.json'))
      ? ['ci', '--no-audit', '--no-fund']
      : ['exec', '--yes', '--package=yarn@1.22.22', '--', 'yarn', 'install', '--frozen-lockfile', '--non-interactive']
    if (process.platform === 'win32') {
      run('cmd.exe', ['/d', '/s', '/c', `npm ${installArgs.join(' ')}`], cwd)
    } else {
      run('npm', installArgs, cwd)
    }
  }
  run(process.execPath, [cli, 'build'], cwd)
}

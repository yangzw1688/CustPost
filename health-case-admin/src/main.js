import { createApp } from 'vue'
import App from './App.vue'

import router from './router'


import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/theme.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const app = createApp(App)


app.use(router)

app.use(ElementPlus, { locale: zhCn })

// 图标由各页面按需导入，避免将整套图标重复注册为全局组件。

app.mount('#app')

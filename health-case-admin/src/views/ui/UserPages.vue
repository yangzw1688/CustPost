<template>
  <div class="page-editor">
    <div class="page-heading">
      <div><h2>用户端页面</h2><p>调整首页布局，创建内容页，预览并发布给用户。</p></div>
      <el-button type="primary" :disabled="busy" @click="creating = true">新增页面</el-button>
    </div>
    <el-tabs v-model="activeTab" class="page-tabs">
      <el-tab-pane label="页面编辑" name="pages">
        <div class="editor-workspace" :class="{ 'without-preview': !previewing }">
          <div class="page-selector">
            <label for="editing-page">编辑页面</label>
            <el-select id="editing-page" :model-value="selected" filterable :disabled="busy || loading" @change="selectPage">
              <el-option v-for="item in pages" :key="item.slug" :value="item.slug" :label="item.title + (item.slug === 'home' ? '（首页）' : ' /pages/' + item.slug)" />
            </el-select>
            <el-button :disabled="busy" :loading="loading" @click="reload">重新加载</el-button>
          </div>
          <section v-loading="loading" class="editor-main">
            <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="editor-error" />
            <template v-if="detail && document">
              <div class="editor-toolbar">
                <div><strong>{{ document.title }}</strong><el-tag size="small" :type="dirty ? 'warning' : 'info'">{{ dirty ? '未保存' : detail.version !== detail.publishedVersion ? '草稿待发布' : '已发布' }}</el-tag></div>
                <div class="toolbar-actions">
                  <el-button @click="previewing = !previewing">{{ previewing ? '收起预览' : '显示预览' }}</el-button>
                  <el-button v-if="!editingPage" :disabled="busy" @click="editingPage = true">编辑页面</el-button>
                  <el-button v-else :disabled="busy" @click="cancelEditing">取消编辑</el-button>
                  <el-button :disabled="busy || !editingPage || !dirty" :loading="saving" @click="saveDraft">保存</el-button>
                  <el-button type="primary" :disabled="busy || dirty || detail.version === detail.publishedVersion" :loading="publishing" @click="publish">发布</el-button>
                  <el-dropdown v-if="selected !== 'home'" :disabled="busy" @command="pageAction">
                    <el-button :disabled="busy">更多操作</el-button>
                    <template #dropdown><el-dropdown-menu><el-dropdown-item v-if="detail.publishedVersion" command="unpublish">下线页面</el-dropdown-item><el-dropdown-item command="delete">删除页面</el-dropdown-item></el-dropdown-menu></template>
                  </el-dropdown>
                </div>
              </div>
              <el-form label-width="88px" :disabled="busy || !editingPage" class="page-settings" @submit.prevent>
                <el-form-item label="页面名称"><el-input v-model.trim="document.title" maxlength="80" show-word-limit /></el-form-item>
                <el-form-item label="页面说明"><el-input v-model.trim="document.description" maxlength="200" /></el-form-item>
                <el-form-item label="访问路径"><el-input :model-value="pagePath" readonly /></el-form-item>
                <div class="settings-row">
                  <el-form-item label="主题颜色"><el-color-picker v-model="document.primaryColor" :predefine="['#1677ff', '#087f7b', '#6d5bc0', '#ba6c28']" /></el-form-item>
                  <el-form-item label="底部导航"><el-switch v-model="document.showNavigation" /></el-form-item>
                </div>
                <div v-if="document.showNavigation" class="settings-row">
                  <el-form-item label="首页名称"><el-input v-model.trim="document.homeLabel" maxlength="20" /></el-form-item>
                  <el-form-item label="个人页名称"><el-input v-model.trim="document.mineLabel" maxlength="20" /></el-form-item>
                </div>
              </el-form>
              <section class="notice-settings" aria-labelledby="notice-settings-title">
                <div class="preview-heading">
                  <h3 id="notice-settings-title">通知弹窗</h3>
                  <el-button text type="primary" :disabled="!previewing" @click="previewNotice = !previewNotice">
                    {{ previewNotice ? '返回页面预览' : '预览通知弹窗' }}
                  </el-button>
                </div>
                <el-form label-width="88px" :disabled="busy || !editingPage" @submit.prevent>
                  <el-form-item label="开启弹窗"><el-switch v-model="document.notice.enabled" aria-label="开启通知弹窗" /></el-form-item>
                  <el-form-item label="通知标题"><el-input v-model.trim="document.notice.title" maxlength="80" show-word-limit /></el-form-item>
                  <el-form-item label="通知正文"><el-input v-model="document.notice.message" type="textarea" :rows="4" maxlength="2000" show-word-limit /></el-form-item>
                </el-form>
                <p class="field-hint">发布后，用户进入此页面时会看到通知。同一浏览器标签页关闭后，内容未变就不会重复弹出。</p>
              </section>
              <div class="modules-heading"><h3>页面模块</h3><span>{{ document.blocks.length }}/30</span></div>
              <el-empty v-if="!document.blocks.length" description="添加模块开始编辑页面" :image-size="60" />
              <el-collapse v-model="activeBlock" accordion class="block-list">
                <el-collapse-item v-for="(block, index) in document.blocks" :key="block.id" :name="block.id">
                  <template #title>
                    <div class="block-heading">
                      <span>{{ block.title || blockTypes.find(type => type.value === block.type)?.label }}</span>
                      <div class="block-actions" @click.stop>
                        <el-switch v-model="block.enabled" :disabled="busy || !editingPage" :aria-label="'显示' + (block.title || '模块')" />
                        <el-button text :disabled="busy || !editingPage || index === 0" aria-label="模块上移" @click="move(document.blocks, index, -1)">↑</el-button>
                        <el-button text :disabled="busy || !editingPage || index === document.blocks.length - 1" aria-label="模块下移" @click="move(document.blocks, index, 1)">↓</el-button>
                        <el-button text type="danger" :disabled="busy || !editingPage" @click="removeBlock(index)">移除</el-button>
                      </div>
                    </div>
                  </template>
                  <el-form label-width="88px" :disabled="busy || !editingPage" class="block-form" @submit.prevent>
                    <el-form-item :label="block.type === 'image' ? '图片说明' : block.type === 'button' ? '按钮文字' : '标题'"><el-input v-model.trim="block.title" maxlength="80" /></el-form-item>
                    <el-form-item v-if="['heading', 'image'].includes(block.type)" label="说明"><el-input v-model.trim="block.description" maxlength="200" type="textarea" :rows="2" /></el-form-item>
                    <el-form-item v-if="block.type === 'text'" label="文字内容"><el-input v-model="block.text" type="textarea" :rows="6" maxlength="5000" show-word-limit /></el-form-item>
                    <el-form-item v-if="block.type === 'banner'" label="轮播内容"><el-button @click="activeTab = 'banners'">管理首页轮播</el-button></el-form-item>
                    <template v-if="block.type === 'links'">
                      <div class="settings-row">
                        <el-form-item label="入口布局"><el-radio-group v-model="block.layout"><el-radio-button value="grid">宫格</el-radio-button><el-radio-button value="list">列表</el-radio-button></el-radio-group></el-form-item>
                        <el-form-item v-if="block.layout === 'grid'" label="每行列数"><el-input-number v-model="block.columns" :min="2" :max="4" :precision="0" /></el-form-item>
                        <el-form-item label="全部入口"><el-switch v-model="block.showMore" /></el-form-item>
                      </div>
                      <div v-for="(item, itemIndex) in block.items" :key="item.id" class="link-editor">
                        <div class="link-heading"><strong>{{ item.title || '服务入口' }}</strong><div>
                          <el-switch v-model="item.enabled" :aria-label="'显示入口' + (item.title || '')" />
                          <el-button text :disabled="itemIndex === 0" aria-label="入口上移" @click="move(block.items, itemIndex, -1)">↑</el-button>
                          <el-button text :disabled="itemIndex === block.items.length - 1" aria-label="入口下移" @click="move(block.items, itemIndex, 1)">↓</el-button>
                          <el-button text type="danger" @click="block.items.splice(itemIndex, 1)">移除</el-button>
                        </div></div>
                        <el-form-item label="名称"><el-input v-model.trim="item.title" maxlength="40" /></el-form-item>
                        <el-form-item label="说明"><el-input v-model.trim="item.description" maxlength="200" /></el-form-item>
                        <el-form-item label="跳转地址">
                          <el-autocomplete v-model="item.linkUrl" :fetch-suggestions="suggestLinkAddresses" :debounce="0" clearable maxlength="512" class="link-address-input" placeholder="直接输入链接，或下拉选择页面" inputmode="url" :spellcheck="false" @input="item.linkType = 'url'" @select="item.linkType = 'page'">
                            <template #default="{ item: link }"><span>{{ link.label }}</span></template>
                          </el-autocomplete>
                          <p v-if="item.linkType === 'url' || item.linkType === 'page'" class="link-type-hint">{{ item.linkType === 'page' ? '将跳转到所选页面' : '将直接打开填写的链接' }}</p>
                        </el-form-item>
                        <div class="settings-row">
                          <el-form-item label="图标">
                            <el-select v-model="item.icon">
                              <template #label="{ value }">
                                <span class="icon-selection">
                                  <el-icon aria-hidden="true"><component :is="iconComponents[value]" /></el-icon>
                                  <span class="icon-selection-name"><span>{{ iconOptions.find(icon => icon.value === value)?.label || value }}</span><small>{{ value }}</small></span>
                                </span>
                              </template>
                              <el-option v-for="icon in iconOptions" :key="icon.value" :label="icon.label + '（' + icon.value + '）'" :value="icon.value">
                                <span class="icon-option"><el-icon aria-hidden="true"><component :is="iconComponents[icon.value]" /></el-icon><span>{{ icon.label }}<small>{{ icon.value }}</small></span></span>
                              </el-option>
                            </el-select>
                          </el-form-item>
                          <el-form-item label="图标颜色"><el-select v-model="item.tone"><el-option v-for="tone in toneOptions" :key="tone.value" :label="tone.label" :value="tone.value" /></el-select></el-form-item>
                        </div>
                      </div>
                      <el-button :disabled="block.items.length >= 20" @click="block.items.push(newLink())">添加服务入口</el-button>
                    </template>
                    <template v-if="block.type === 'image'">
                      <el-form-item label="选择图片"><el-select v-model="block.imageId" filterable placeholder="选择页面素材"><el-option v-for="image in assets.filter(item => item.enabled && item.imageUrl)" :key="image.id" :label="image.title" :value="image.id" /></el-select></el-form-item>
                      <el-form-item label="上传图片"><div><input type="file" accept="image/png,image/jpeg,image/webp,image/gif" :disabled="busy || !editingPage" aria-label="上传页面图片" @change="uploadImage($event, block)" /><p class="field-hint">支持 JPG、PNG、GIF、WebP，单张不超过 10MB。</p></div></el-form-item>
                      <el-form-item v-if="assetUrls[String(block.imageId)]" label="当前图片"><el-image :src="assetUrls[String(block.imageId)]" class="asset-preview" fit="contain" /></el-form-item>
                    </template>
                    <el-form-item v-if="['image', 'button'].includes(block.type)" label="跳转地址">
                      <el-autocomplete v-model="block.linkUrl" :fetch-suggestions="suggestLinkAddresses" :debounce="0" clearable maxlength="512" class="link-address-input" placeholder="直接输入链接，或下拉选择页面" inputmode="url" :spellcheck="false" @input="block.linkType = 'url'" @select="block.linkType = 'page'">
                        <template #default="{ item: link }"><span>{{ link.label }}</span></template>
                      </el-autocomplete>
                      <p v-if="block.linkType === 'url' || block.linkType === 'page'" class="link-type-hint">{{ block.linkType === 'page' ? '将跳转到所选页面' : '将直接打开填写的链接' }}</p>
                    </el-form-item>
                  </el-form>
                </el-collapse-item>
              </el-collapse>
              <div class="add-modules"><el-button v-for="type in blockTypes" :key="type.value" :disabled="busy || !editingPage || document.blocks.length >= 30" @click="addBlock(type.value)">添加{{ type.label }}</el-button></div>
            </template>
            <el-empty v-else-if="!loading && !error" description="请选择要编辑的页面" />
          </section>
          <aside v-if="detail && document && previewing" class="live-preview" aria-label="用户端实时预览">
            <div class="preview-heading"><h3>实时预览</h3><el-button text :disabled="uploading || loading" :loading="refreshingPreview" @click="refreshPreview">刷新预览</el-button></div>
            <iframe :key="previewRevision" ref="previewFrame" :src="previewURL" title="用户端页面预览" class="preview-frame" @load="sendPreview"></iframe>
            <p>编辑后先保存，再发布到用户端。</p>
          </aside>
        </div>
      </el-tab-pane>
      <el-tab-pane label="首页轮播" name="banners" lazy><UiImages @changed="loadAssets" /></el-tab-pane>
      <el-tab-pane label="页面素材" name="images" lazy><UiImages slot-name="page_image" @changed="loadAssets" /></el-tab-pane>
    </el-tabs>
    <el-dialog v-model="creating" title="新增用户端页面" width="480px" :close-on-click-modal="!busy">
      <el-form label-width="88px" :disabled="busy" @submit.prevent="createPage">
        <el-form-item label="页面名称" required><el-input v-model.trim="newPage.title" maxlength="80" /></el-form-item>
        <el-form-item label="页面标识" required><el-input v-model.trim="newPage.slug" maxlength="48" placeholder="例如 service-guide" /></el-form-item>
        <p class="field-hint">访问路径为 /pages/{{ newPage.slug || '页面标识' }}，标识保存后固定。</p>
      </el-form>
      <template #footer><el-button :disabled="busy" @click="creating = false">取消</el-button><el-button type="primary" :loading="busy" @click="createPage">创建草稿</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Money, Document, Tickets, Search, Service, FirstAidKit, House, User } from '@element-plus/icons-vue'
import request, { errorMessage } from '@/utils/request'
import UiImages from './UiImages.vue'

const pages = ref([])
const selected = ref('home')
const detail = ref(null)
const document = ref(null)
const baseline = ref('')
const loading = ref(false)
const saving = ref(false)
const editingPage = ref(false)
const publishing = ref(false)
const acting = ref(false)
const uploading = ref(false)
const error = ref('')
const activeTab = ref('pages')
const activeBlock = ref('')
const assets = ref([])
const banners = ref([])
const creating = ref(false)
const newPage = reactive({ slug: '', title: '' })
const previewing = ref(true)
const previewNotice = ref(false)
const refreshingPreview = ref(false)
const previewRevision = ref(0)
const previewFrame = ref(null)
const previewURL = useRouter().resolve({ name: 'MobilePagePreview' }).href
const busy = computed(() => saving.value || publishing.value || acting.value || uploading.value)
const dirty = computed(() => document.value && JSON.stringify(document.value) !== baseline.value)
const pagePath = computed(() => selected.value === 'home' ? '/' : '/pages/' + selected.value)
const assetUrls = computed(() => Object.fromEntries(assets.value.filter(item => item.enabled && item.imageUrl).map(item => [String(item.id), item.imageUrl])))
const blockTypes = [
  { value: 'heading', label: '标题' }, { value: 'banner', label: '轮播' }, { value: 'links', label: '服务入口' },
  { value: 'text', label: '文字' }, { value: 'image', label: '图片' }, { value: 'button', label: '按钮' }
]
const iconOptions = [
  { value: 'Money', label: '理赔' }, { value: 'Document', label: '资料' }, { value: 'Tickets', label: '保单' },
  { value: 'Search', label: '查询' }, { value: 'Service', label: '客服' }, { value: 'FirstAidKit', label: '健康' },
  { value: 'House', label: '首页' }, { value: 'User', label: '用户' }
]
const iconComponents = { Money, Document, Tickets, Search, Service, FirstAidKit, House, User }
const toneOptions = [
  { value: 'blue', label: '青色' }, { value: 'green', label: '绿色' }, { value: 'red', label: '红色' },
  { value: 'orange', label: '橙色' }, { value: 'purple', label: '紫色' }, { value: 'pink', label: '粉色' }
]
const linkOptions = computed(() => [
  { value: '/', label: '首页 /' }, { value: '/submit', label: '资料提交 /submit' },
  { value: '/policy', label: '我的保单 /policy' }, { value: '/service-record', label: '服务记录 /service-record' },
  { value: '/mine', label: '个人信息 /mine' },
  ...pages.value.filter(page => page.slug !== 'home' && page.publishedVersion > 0)
    .map(page => ({ value: '/pages/' + page.slug, label: page.title + ' /pages/' + page.slug }))
])
function suggestLinkAddresses(query, callback) {
  const keyword = query.trim().toLowerCase()
  callback(linkOptions.value.filter(link => link.value.toLowerCase().includes(keyword) || link.label.toLowerCase().includes(keyword)))
}
let controller
let disposed = false
let previewTimer
const id = () => globalThis.crypto?.randomUUID?.() || 'id-' + Date.now() + '-' + Math.random().toString(36).slice(2)
const newLink = () => ({ id: id(), title: '', description: '', linkUrl: '', linkType: 'url', icon: 'Document', tone: 'blue', enabled: true })
function newBlock(type) {
  return { id: id(), type, title: '', description: '', text: '', enabled: true, layout: 'grid', columns: 4, imageId: null, linkUrl: '', linkType: 'url', showMore: false, items: [] }
}
function accept(value) {
  value.document.notice ??= { enabled: false, title: '', message: '' }
  detail.value = value
  document.value = value.document
  baseline.value = JSON.stringify(value.document)
  editingPage.value = false
}
async function cancelEditing() {
  if (busy.value || !await discardChanges()) return
  document.value = JSON.parse(baseline.value)
  editingPage.value = false
  error.value = ''
}
async function loadPage(slug) {
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = ''
  try {
    const { data } = await request.get('/api/ui/pages/' + slug, { signal: current.signal })
    if (disposed) return
    selected.value = slug
    previewNotice.value = false
    accept(data.data)
  } catch (err) {
    if (err.code !== 'ERR_CANCELED' && !disposed) error.value = errorMessage(err)
  } finally {
    if (!disposed && controller === current) loading.value = false
  }
}
async function listPages() {
  const { data } = await request.get('/api/ui/pages')
  if (!disposed) pages.value = data.data
}
async function loadAssets() {
  const results = await Promise.allSettled([
    request.get('/api/ui/manage', { params: { slot: 'page_image' } }),
    request.get('/api/ui/manage', { params: { slot: 'home_banner' } })
  ])
  if (disposed) return
  if (results[0].status === 'fulfilled') assets.value = results[0].value.data.data
  if (results[1].status === 'fulfilled') banners.value = results[1].value.data.data.filter(item => item.enabled)
  for (const result of results) if (result.status === 'rejected') ElMessage.error(errorMessage(result.reason))
}
async function refreshPreview() {
  if (refreshingPreview.value || !document.value) return
  const slug = selected.value
  refreshingPreview.value = true
  try {
    await loadAssets()
    if (disposed || slug !== selected.value || !previewing.value) return
    // 从当前编辑内容复现用户进入页面的状态，清除预览中关闭过的弹窗。
    previewNotice.value = document.value.notice?.enabled === true
    previewRevision.value++
  } finally {
    refreshingPreview.value = false
  }
}
async function discardChanges() {
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('当前修改尚未保存，继续将放弃这些修改。', '离开编辑', { confirmButtonText: '放弃修改', cancelButtonText: '继续编辑', type: 'warning' })
    return true
  } catch { return false }
}
async function selectPage(slug) {
  if (slug !== selected.value && await discardChanges()) await loadPage(slug)
}
async function reload() {
  if (!await discardChanges()) return
  try { await listPages(); await loadPage(selected.value) }
  catch (err) { error.value = errorMessage(err) }
}
function move(list, index, offset) {
  const target = index + offset
  if (target < 0 || target >= list.length) return
  list.splice(target, 0, list.splice(index, 1)[0])
}
function addBlock(type) {
  const block = newBlock(type)
  document.value.blocks.push(block)
  activeBlock.value = block.id
}
async function removeBlock(index) {
  try { await ElMessageBox.confirm('确定移除这个模块吗？保存并发布后生效。', '移除模块', { type: 'warning' }) }
  catch { return }
  document.value.blocks.splice(index, 1)
}
async function persistDraft() {
  const { data } = await request.put('/api/ui/pages/' + selected.value, { version: detail.value.version, document: document.value })
  if (!disposed) accept(data.data)
}
async function saveDraft() {
  if (busy.value || !editingPage.value || !dirty.value) return
  saving.value = true
  error.value = ''
  try {
    try {
      await ElMessageBox.confirm('确认将“' + document.value.title + '”的当前修改保存为草稿吗？保存后可单独发布到用户端。', '确认保存', {
        confirmButtonText: '确认保存', cancelButtonText: '继续编辑', type: 'warning', closeOnClickModal: false
      })
    } catch { return }
    if (disposed) return
    await persistDraft()
    await listPages()
    if (!disposed) ElMessage.success('草稿已保存')
  }
  catch (err) { if (!disposed) error.value = errorMessage(err) }
  finally { if (!disposed) saving.value = false }
}
async function publish() {
  if (busy.value || dirty.value || detail.value.version === detail.value.publishedVersion) return
  publishing.value = true
  error.value = ''
  try {
    try {
      await ElMessageBox.confirm('确认将“' + document.value.title + '”的已保存草稿发布到用户端吗？', '确认发布', {
        confirmButtonText: '确认发布', cancelButtonText: '取消', type: 'warning', closeOnClickModal: false
      })
    } catch { return }
    if (disposed) return
    const { data } = await request.post('/api/ui/pages/' + selected.value + '/publish', { version: detail.value.version })
    if (disposed) return
    accept(data.data)
    await listPages()
    ElMessage.success('页面已发布，用户端刷新后生效')
  } catch (err) { if (!disposed) error.value = errorMessage(err) }
  finally { if (!disposed) publishing.value = false }
}
async function createPage() {
  if (busy.value) return
  if (!newPage.title || !/^[a-z][a-z0-9-]{0,47}$/.test(newPage.slug)) { ElMessage.error('请填写页面名称和有效的页面标识'); return }
  if (!await discardChanges()) return
  acting.value = true
  try {
    const heading = newBlock('heading')
    heading.title = newPage.title
    const { data } = await request.post('/api/ui/pages', {
      slug: newPage.slug,
      document: { title: newPage.title, description: '', primaryColor: '#1677ff', showNavigation: true, homeLabel: '首页', mineLabel: '个人信息', blocks: [heading] }
    })
    if (disposed) return
    selected.value = newPage.slug
    previewNotice.value = false
    accept(data.data)
    editingPage.value = true
    creating.value = false
    newPage.slug = ''
    newPage.title = ''
    activeTab.value = 'pages'
    await listPages()
  } catch (err) { if (!disposed) ElMessage.error(errorMessage(err)) }
  finally { if (!disposed) acting.value = false }
}
async function pageAction(action) {
  if (busy.value) return
  if (!await discardChanges()) return
  const deleting = action === 'delete'
  try { await ElMessageBox.confirm(deleting ? '确定删除此页面吗？已发布的链接将无法继续访问。' : '下线后用户将无法访问此页面，草稿会保留。', deleting ? '删除页面' : '下线页面', { type: 'warning' }) }
  catch { return }
  acting.value = true
  try {
    if (deleting) {
      await request.delete('/api/ui/pages/' + selected.value, { params: { version: detail.value.version } })
      await loadPage('home')
    } else {
      const { data } = await request.post('/api/ui/pages/' + selected.value + '/unpublish', { version: detail.value.version })
      if (!disposed) accept(data.data)
    }
    await listPages()
    if (!disposed) ElMessage.success(deleting ? '页面已删除' : '页面已下线')
  } catch (err) { if (!disposed) error.value = errorMessage(err) }
  finally { if (!disposed) acting.value = false }
}
async function uploadImage(event, block) {
  if (busy.value || !editingPage.value) return
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type) || !file.size || file.size > 10 * 1024 * 1024) { ElMessage.error('请选择不超过10MB的有效图片'); return }
  uploading.value = true
  const form = new FormData()
  form.append('assetKey', 'page-' + id())
  form.append('slot', 'page_image')
  form.append('title', block.title || file.name.slice(0, 80))
  form.append('file', file)
  try {
    try {
      await ElMessageBox.confirm('确认将所选图片上传并保存到页面素材吗？页面内容还需保存并发布。', '确认保存图片', {
        confirmButtonText: '确认上传并保存', cancelButtonText: '取消', type: 'warning', closeOnClickModal: false
      })
    } catch { return }
    if (disposed) return
    const { data } = await request.post('/api/ui/save', form, { timeout: 120000 })
    if (disposed) return
    block.imageId = data.data
    await loadAssets()
    ElMessage.success('图片已上传，请保存并发布页面')
  } catch (err) { if (!disposed) ElMessage.error(errorMessage(err)) }
  finally { if (!disposed) uploading.value = false }
}
function sendPreview() {
  if (!previewFrame.value?.contentWindow || !document.value) return
  const page = JSON.parse(JSON.stringify({ document: document.value, images: assetUrls.value, banners: banners.value, previewNotice: previewNotice.value }))
  previewFrame.value.contentWindow.postMessage({ type: 'USER_PAGE_PREVIEW', page }, window.location.origin)
}
function previewReady(event) {
  if (event.source !== previewFrame.value?.contentWindow || event.origin !== window.location.origin) return
  if (event.data?.type === 'USER_PAGE_PREVIEW_READY') sendPreview()
  if (event.data?.type === 'USER_PAGE_NOTICE_CLOSED') previewNotice.value = false
}
watch([document, banners, assetUrls, previewNotice], () => {
  clearTimeout(previewTimer)
  if (previewing.value) previewTimer = setTimeout(sendPreview, 100)
}, { deep: true })
onBeforeRouteLeave(() => busy.value ? false : discardChanges())
onMounted(async () => {
  window.addEventListener('message', previewReady)
  try { await listPages(); if (!disposed) await loadPage('home'); await loadAssets() }
  catch (err) { if (!disposed) error.value = errorMessage(err) }
})
onBeforeUnmount(() => {
  disposed = true
  controller?.abort()
  clearTimeout(previewTimer)
  window.removeEventListener('message', previewReady)
})
</script>
<style scoped>
.page-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 18px; }
.page-heading h2 { margin: 0 0 8px; font-size: 23px; }
.page-heading p { margin: 0; color: #718592; font-size: 13px; }
/* Tabs must not become an overflow ancestor of the sticky preview. */
.page-tabs :deep(.el-tabs__content) { overflow: visible; }
.editor-workspace { display: grid; grid-template-columns: minmax(0,1fr) minmax(300px,390px); gap: 20px; align-items: start; }
.editor-workspace.without-preview { grid-template-columns: minmax(0,1fr); }
.page-selector { grid-column: 1 / -1; display: flex; align-items: center; gap: 12px; }
.page-selector label { flex-shrink: 0; color: #526775; }
.page-selector .el-select { max-width: 500px; }
.editor-main { min-height: 300px; min-width: 0; }
.editor-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 14px; flex-wrap: wrap; margin-bottom: 18px; }
.editor-toolbar > div { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.toolbar-actions .el-button { margin: 0; }
.page-settings { background: white; padding: 22px 22px 6px; border: 1px solid #e4ebef; border-radius: 12px; }
.notice-settings { margin-top: 16px; padding: 16px 22px 6px; background: white; border: 1px solid #e4ebef; border-radius: 12px; }
.settings-row { display: flex; flex-wrap: wrap; gap: 0 16px; }
.settings-row > .el-form-item { flex: 1; min-width: 200px; }
.el-select { width: 100%; }
.link-address-input { width: 100%; }
.link-type-hint { flex-basis: 100%; margin: 6px 0 0; font-size: 12px; line-height: 1.6; color: #718592; }
.icon-selection, .icon-option { display: flex; align-items: center; gap: 8px; min-width: 0; }
.icon-selection .el-icon, .icon-option .el-icon { flex-shrink: 0; font-size: 18px; color: var(--el-color-primary); }
.icon-selection > span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.icon-selection-name { display: flex; flex-direction: column; gap: 1px; line-height: 1.1; }
.icon-selection-name small { color: #718592; font-size: 11px; }
.icon-option small { margin-left: 8px; color: #718592; font-size: 12px; }
.modules-heading { display: flex; justify-content: space-between; align-items: center; margin: 24px 0 12px; }
.modules-heading h3 { margin: 0; font-size: 16px; }
.modules-heading span { font-size: 12px; color: #8b9ca6; }
.block-list { border: 0; }
.block-list :deep(.el-collapse-item) { margin-bottom: 12px; border: 1px solid #e4ebef; border-radius: 12px; overflow: hidden; }
.block-list :deep(.el-collapse-item__header) { padding: 12px 18px; min-height: 64px; height: auto; }
.block-list :deep(.el-collapse-item__content) { padding-bottom: 0; }
.block-heading { flex: 1; min-width: 0; display: flex; justify-content: space-between; align-items: center; gap: 14px; padding-right: 10px; }
.block-heading > span { overflow-wrap: anywhere; }
.block-actions { display: flex; align-items: center; flex-shrink: 0; gap: 5px; }
.block-actions .el-button { padding: 4px; margin: 0; min-height: 28px; }
.block-form { padding: 12px 22px 20px; }
.link-editor { margin: 14px 0; padding: 16px; border-radius: 10px; background: #f5f8fa; border: 1px solid #e9eef1; }
.link-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; gap: 10px; }
.link-heading > div { display: flex; align-items: center; gap: 5px; }
.link-heading .el-button { padding: 4px; margin: 0; }
.add-modules { display: flex; flex-wrap: wrap; gap: 10px; padding: 10px 0 24px; }
.add-modules .el-button { margin: 0; }
.asset-preview { max-width: 260px; max-height: 160px; border-radius: 8px; }
.field-hint { color: #718592; font-size: 12px; line-height: 1.8; }
.editor-error { margin-bottom: 18px; }
.live-preview { position: sticky; top: 16px; min-width: 0; padding: 14px; background: white; border: 1px solid #e4ebef; border-radius: 14px; }
.preview-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.preview-heading h3 { margin: 0; font-size: 15px; }
.live-preview > p { margin: 12px 0 0; font-size: 12px; color: #718592; }
.preview-frame { display: block; width: 100%; max-width: 390px; margin-inline: auto; height: min(720px,calc(100vh - 210px)); min-height: 160px; border: 1px solid #e4ebef; border-radius: 12px; background: #f5f7fa; }
@media(max-width: 1200px) { .editor-workspace { grid-template-columns: minmax(0,1fr) 310px; } .block-heading { flex-wrap: wrap; } .page-settings { padding: 16px 10px 0; } .block-form { padding: 8px 12px 16px; } }
@media(max-width: 960px) { .editor-workspace { grid-template-columns: minmax(0,1fr); } .live-preview { grid-row: 2; top: 8px; z-index: 5; } .preview-frame { height: min(320px,40vh); } .page-heading { align-items: flex-start; } }
</style>

<template>
  <div class="claim-image-page">
    <el-card shadow="never">
      <header class="toolbar">
        <h1>理赔影像</h1>
        <el-button :icon="Refresh" :loading="casesLoading" @click="loadCases">刷新案件</el-button>
      </header>
      <el-input
        v-model="query"
        :prefix-icon="Search"
        class="case-search"
        aria-label="搜索案件"
        placeholder="搜索案件编号、患者姓名或病例名称"
        clearable
      />
      <el-alert v-if="casesError" :title="casesError" type="error" :closable="false" show-icon />
      <el-table v-else v-loading="casesLoading" :data="visibleCases" :empty-text="emptyCasesText" row-key="id">
        <el-table-column prop="id" label="案件编号" min-width="100" />
        <el-table-column prop="caseTitle" label="病例名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="name" label="患者姓名" min-width="100" />
        <el-table-column prop="attachmentCount" label="附件数" min-width="85" />
        <el-table-column label="提交时间" min-width="155">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="110">
          <template #default="{ row }">
            <el-button type="primary" link @click="openCase(row)">
              {{ selectedCase?.id === row.id ? '查看当前影像' : '查看影像' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!casesError && filteredCases.length > pageSize" class="pagination">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="filteredCases.length"
          layout="total, prev, pager, next"
        />
      </div>
    </el-card>

    <el-card v-if="selectedCase" shadow="never">
      <header ref="gallery" class="toolbar">
        <div>
          <h2>{{ selectedCase.caseTitle || '案件影像' }}</h2>
          <p class="case-caption">案件 {{ selectedCase.id }} · {{ selectedCase.name || '未填写患者姓名' }}</p>
        </div>
        <div class="gallery-actions">
          <el-button :icon="Refresh" :loading="filesLoading" @click="loadFiles">刷新链接</el-button>
          <router-link :to="{ name: 'CaseDetail', params: { id: selectedCase.id } }">查看案件</router-link>
        </div>
      </header>
      <el-alert v-if="filesError" :title="filesError" type="error" :closable="false" show-icon />
      <div v-else v-loading="filesLoading" class="file-content">
        <el-radio-group v-model="fileKind" aria-label="附件类型" size="small">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="image">图片</el-radio-button>
          <el-radio-button value="pdf">PDF</el-radio-button>
        </el-radio-group>
        <div v-if="visibleFiles.length" class="file-grid">
          <article v-for="file in visibleFiles" :key="file.id" class="file-card">
            <el-image
              v-if="file.kind === 'image' && file.url"
              :src="file.url"
              :alt="file.label"
              :preview-src-list="previewUrls"
              :initial-index="previewUrls.indexOf(file.url)"
              fit="contain"
              preview-teleported
              class="thumbnail"
            >
              <template #error><div class="file-placeholder">图片加载失败，可尝试刷新链接</div></template>
            </el-image>
            <div v-else class="file-placeholder">
              <el-icon :size="34"><Document v-if="file.kind === 'pdf'" /><Picture v-else /></el-icon>
              <span v-if="file.kind === 'pdf'">PDF</span>
            </div>
            <div class="file-info">
              <p :title="file.label">{{ file.label }}</p>
              <a v-if="file.url" :href="file.url" target="_blank" rel="noopener noreferrer">
                {{ file.kind === 'pdf' ? '打开 PDF' : '打开原图' }}
              </a>
              <span v-else>文件链接不可用</span>
            </div>
          </article>
        </div>
        <el-empty
          v-else-if="!filesLoading"
          :description="files.length ? '该案件没有此类型的附件' : '该案件暂无附件'"
          :image-size="60"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Document, Picture, Refresh, Search } from '@element-plus/icons-vue'
import { isPdfAttachment } from '@/shared/files/case-files.mjs'
import { formatDateTime } from '@/utils/format'
import { errorMessage, getList } from '@/utils/request'

const caseUrl = '/api/service-record'
const pageSize = 10
const cases = ref([])
const query = ref('')
const page = ref(1)
const casesLoading = ref(true)
const casesError = ref('')
const selectedCase = ref(null)
const gallery = ref(null)
const files = ref([])
const fileKind = ref('all')
const filesLoading = ref(false)
const filesError = ref('')
let casesRequest
let filesRequest

const filteredCases = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  if (!keyword) return cases.value
  return cases.value.filter(row => [row.id, row.name, row.caseTitle]
    .some(value => String(value ?? '').toLowerCase().includes(keyword)))
})
const visibleCases = computed(() => filteredCases.value.slice((page.value - 1) * pageSize, page.value * pageSize))
const emptyCasesText = computed(() => {
  if (casesLoading.value) return '加载中…'
  return query.value.trim() ? '没有匹配的案件' : '暂无已上传影像的案件'
})
const visibleFiles = computed(() => files.value.filter(file => fileKind.value === 'all' || file.kind === fileKind.value))
const previewUrls = computed(() => visibleFiles.value.filter(file => file.kind === 'image' && file.url).map(file => file.url))

watch(query, () => { page.value = 1 })

function attachmentUrl(value) {
  if (typeof value !== 'string') return ''
  try {
    const url = new URL(value, window.location.origin)
    if (value.startsWith('/upload/case/') && url.origin === window.location.origin
      && url.pathname.startsWith('/upload/case/')) return value
    if (/^https?:\/\//i.test(value) && !url.username && !url.password) return value
  } catch {
    return ''
  }
  return ''
}

async function loadCases() {
  casesRequest?.abort()
  casesRequest = new AbortController()
  const { signal } = casesRequest
  casesLoading.value = true
  casesError.value = ''
  try {
    const rows = await getList(`${caseUrl}/attachment-cases`, { signal })
    if (signal.aborted) return
    cases.value = rows
    page.value = Math.min(page.value, Math.max(1, Math.ceil(filteredCases.value.length / pageSize)))
    if (selectedCase.value) {
      const selectedId = selectedCase.value.id
      selectedCase.value = rows.find(row => row.id === selectedId) || null
      if (!selectedCase.value) filesRequest?.abort()
    }
  } catch (error) {
    if (!signal.aborted) casesError.value = errorMessage(error)
  } finally {
    if (!signal.aborted) casesLoading.value = false
  }
}

async function openCase(row) {
  if (selectedCase.value?.id !== row.id || filesError.value) {
    selectedCase.value = row
    fileKind.value = 'all'
    loadFiles()
  }
  await nextTick()
  gallery.value?.scrollIntoView({ block: 'start' })
}

async function loadFiles() {
  filesRequest?.abort()
  filesRequest = new AbortController()
  const { signal } = filesRequest
  filesLoading.value = true
  filesError.value = ''
  files.value = []
  try {
    const rows = await getList(`${caseUrl}/${selectedCase.value.id}/attachments`, { signal })
    if (signal.aborted) return
    files.value = rows.map(file => ({
      id: file.id,
      label: file.originalName || file.fileName || '未命名文件',
      kind: isPdfAttachment(file) ? 'pdf' : 'image',
      url: attachmentUrl(file.fileUrl)
    }))
  } catch (error) {
    if (!signal.aborted) filesError.value = errorMessage(error)
  } finally {
    if (!signal.aborted) filesLoading.value = false
  }
}

onMounted(loadCases)
onBeforeUnmount(() => {
  casesRequest?.abort()
  filesRequest?.abort()
})
</script>

<style scoped>
.claim-image-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.toolbar,
.gallery-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar {
  justify-content: space-between;
  margin-bottom: 16px;
}

h1, h2 {
  margin: 0;
  font-size: 18px;
}

.case-search {
  max-width: 360px;
  margin-bottom: 14px;
}

.case-caption {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.file-content {
  min-height: 130px;
}

.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 190px), 1fr));
  gap: 14px;
  margin-top: 14px;
}

.file-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
}

.thumbnail, .file-placeholder {
  width: 100%;
  height: 138px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 6px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.file-info {
  padding: 10px 12px;
  font-size: 13px;
}

.file-info p {
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}

a {
  color: var(--el-color-primary);
}

.file-info span {
  color: var(--el-text-color-secondary);
}
</style>

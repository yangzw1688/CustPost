<template>
  <main class="managed-page original-page" :style="{ '--hc-primary': content.primaryColor, '--el-color-primary': content.primaryColor }">
    <template v-for="block in visibleBlocks" :key="block.id">
      <header v-if="block.type === 'heading'" class="top-area">
        <h1>{{ block.title }}</h1><p v-if="block.description">{{ block.description }}</p>
      </header>

      <section v-else-if="block.type === 'banner'" class="banner-wrapper" aria-label="首页轮播">
        <el-empty v-if="!banners.length" description="暂无轮播内容" :image-size="48" />
        <el-carousel v-else height="168px" :interval="4500" arrow="never" indicator-position="outside">
          <el-carousel-item v-for="item in banners" :key="item.id">
            <component :is="item.linkUrl && !preview ? 'button' : 'div'" class="banner-item"
              :type="item.linkUrl && !preview ? 'button' : undefined" @click="navigate(item.linkUrl, 'url')">
              <img v-if="item.imageUrl && !failedImages.has(item.id)" :src="item.imageUrl" :alt="item.title" class="banner-image" @error="failedImages.add(item.id)" />
              <div class="banner-overlay" :class="!item.imageUrl || failedImages.has(item.id) ? item.tone || 'blue' : ''"></div>
              <div class="banner-content"><strong>{{ item.title }}</strong><p>{{ item.description }}</p></div>
              <span v-if="item.linkUrl" class="banner-action">了解更多 <el-icon><ArrowRight /></el-icon></span>
            </component>
          </el-carousel-item>
        </el-carousel>
        <el-button v-if="failedImages.size" text size="small" @click="$emit('refresh')">图片加载失败，重新加载</el-button>
      </section>

      <section v-else-if="block.type === 'links'" class="content-card">
        <div v-if="block.title || block.showMore" class="section-title">
          <h2 v-if="block.title">{{ block.title }}</h2>
          <button v-if="block.showMore" type="button" class="title-more" @click="dialogBlock = block">全部 <el-icon><ArrowRight /></el-icon></button>
        </div>
        <div :class="block.layout === 'list' ? 'service-list' : 'function-grid'" :style="{ '--grid-columns': block.columns || 4 }">
          <button v-for="item in enabledItems(block)" :key="item.id" type="button" :class="block.layout === 'list' ? 'service-item' : 'function-item'" @click="navigate(item.linkUrl, item.linkType)">
            <span class="item-icon" :class="item.tone"><el-icon :size="block.layout === 'list' ? 22 : 25"><component :is="icons[item.icon] || Document" /></el-icon></span>
            <span class="item-content"><strong>{{ item.title }}</strong><small v-if="block.layout === 'list' && item.description">{{ item.description }}</small></span>
            <el-icon v-if="block.layout === 'list'" class="service-arrow"><ArrowRight /></el-icon>
          </button>
          <button v-if="block.showMore && block.layout !== 'list'" type="button" class="function-item" @click="dialogBlock = block">
            <span class="item-icon more"><el-icon :size="25"><MoreFilled /></el-icon></span><span class="item-content"><strong>更多</strong></span>
          </button>
        </div>
      </section>

      <section v-else-if="block.type === 'text'" class="content-card text-block">
        <div v-if="block.title" class="section-title"><h2>{{ block.title }}</h2></div>
        <p>{{ block.text }}</p>
      </section>

      <section v-else-if="block.type === 'image'" class="image-block">
        <div v-if="images[String(block.imageId)]" class="image-frame">
          <el-image :src="images[String(block.imageId)]" :alt="block.title || '页面图片'" fit="cover">
            <template #error><div class="image-state">图片暂时无法加载</div></template>
          </el-image>
          <button v-if="block.linkUrl && !preview" type="button" class="image-link" :aria-label="block.title || '打开图片关联页面'"
            @click="navigate(block.linkUrl, block.linkType)" />
        </div>
        <div v-else class="image-state">{{ preview ? '请选择并上传图片' : '图片暂时不可用' }}</div>
        <p v-if="block.description">{{ block.description }}</p>
      </section>

      <section v-else-if="block.type === 'button'" class="button-block">
        <el-button type="primary" @click="navigate(block.linkUrl, block.linkType)">{{ block.title || '按钮' }}<el-icon><ArrowRight /></el-icon></el-button>
      </section>
    </template>

    <nav v-if="content.showNavigation" class="tab-bar" aria-label="底部导航">
      <button type="button" class="tab-item" :class="{ active: homeActive }" :aria-current="homeActive ? 'page' : undefined" @click="navigate('/', 'page')"><el-icon :size="23"><House /></el-icon><span>{{ content.homeLabel }}</span></button>
      <button type="button" class="tab-item" @click="navigate('/mine', 'page')"><el-icon :size="23"><User /></el-icon><span>{{ content.mineLabel }}</span></button>
    </nav>
    <el-dialog v-model="dialogVisible" :title="dialogBlock?.title || '服务导航'" width="390px">
      <div class="dialog-services"><el-button v-for="item in enabledItems(dialogBlock)" :key="item.id" @click="dialogBlock = null; navigate(item.linkUrl, item.linkType)">{{ item.title }}</el-button></div>
    </el-dialog>
  </main>
</template>

<script setup>
/* global defineProps, defineEmits */
import { computed, reactive, ref, watch } from 'vue'
import { ArrowRight, House, User, Money, Document, Tickets, Search, Service, FirstAidKit, MoreFilled } from '@element-plus/icons-vue'

const props = defineProps({
  content: { type: Object, required: true },
  banners: { type: Array, default: () => [] },
  images: { type: Object, default: () => ({}) },
  preview: { type: Boolean, default: false },
  homeActive: { type: Boolean, default: false }
})
const emit = defineEmits(['navigate', 'refresh'])
const icons = { House, User, Money, Document, Tickets, Search, Service, FirstAidKit }
const visibleBlocks = computed(() => (props.content.blocks || []).filter(block => block.enabled !== false))
const failedImages = reactive(new Set())
const dialogBlock = ref(null)
const dialogVisible = computed({ get: () => dialogBlock.value !== null, set: value => { if (!value) dialogBlock.value = null } })
const enabledItems = block => (block?.items || []).filter(item => item.enabled !== false)
function navigate(url, linkType = 'auto') {
  if (!props.preview && url) emit('navigate', url, linkType)
}
watch(() => [props.banners, props.images], () => failedImages.clear())
</script>

<style scoped>
.managed-page { padding-bottom: 88px; }
.top-area { background: #fff; padding: 24px 20px 16px; }
.top-area h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: .4px; color: #172b4d; overflow-wrap: anywhere; }
.top-area p { margin: 7px 0 0; font-size: 13px; color: #8592a5; line-height: 1.8; }
.banner-wrapper { background: #fff; padding: 0 14px 14px; }
:deep(.el-carousel__container) { border-radius: 16px; overflow: hidden; }
.banner-item { display: block; position: relative; width: 100%; height: 168px; border: 0; padding: 0; text-align: left; overflow: hidden; background: #dfeaf8; }
.banner-item:focus-visible { outline-offset: -4px; }
.banner-image { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.banner-overlay { position: absolute; inset: 0; background: linear-gradient(90deg,#123957a6,#12395710); }
.banner-overlay.blue { background: linear-gradient(120deg,#386ca6,#709cce); }
.banner-overlay.green { background: linear-gradient(120deg,#388c8d,#83b7af); }
.banner-content { position: absolute; left: 20px; right: 18px; top: 37px; color: #fff; }
.banner-content strong { font-size: 23px; font-weight: 650; overflow-wrap: anywhere; }
.banner-content p { font-size: 12px; margin: 10px 0 0; color: #f0f6ff; }
.banner-action { position: absolute; right: 14px; bottom: 14px; color: #fff; font-size: 11px; border: 1px solid #ffffff70; padding: 5px 9px; border-radius: 20px; display: flex; align-items: center; gap: 3px; }
:deep(.el-carousel__button) { width: 8px; height: 5px; border-radius: 4px; }
:deep(.el-carousel__indicator.is-active .el-carousel__button) { width: 18px; background: var(--hc-primary); }
.content-card { margin: 14px 14px 0; padding: 20px 16px; background: #fff; border: 1px solid #eaf0f7; border-radius: 16px; box-shadow: 0 4px 16px #183b6805; }
.section-title { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 20px; }
.section-title h2 { display: flex; align-items: center; gap: 8px; margin: 0; font-size: 17px; font-weight: 600; color: #243850; overflow-wrap: anywhere; }
.section-title h2::before { content: ''; flex: 0 0 3px; height: 16px; background: var(--hc-primary); border-radius: 3px; }
.title-more { display: flex; gap: 3px; align-items: center; margin-left: auto; color: #8a97a8; font-size: 12px; border: 0; background: transparent; padding: 5px 0; white-space: nowrap; }
.function-grid { display: grid; grid-template-columns: repeat(var(--grid-columns),minmax(0,1fr)); gap: 24px 4px; }
.function-item { min-width: 0; display: flex; flex-direction: column; align-items: center; gap: 9px; padding: 0; background: transparent; border: 0; }
.item-icon { display: grid; place-items: center; flex-shrink: 0; width: 48px; height: 48px; border-radius: 15px; transition: transform .2s; }
.function-item:hover .item-icon { transform: translateY(-3px); }
.item-icon.blue { background: #e8f7f5; color: #42a69c; }
.item-icon.green { background: #edf8ef; color: #5b9c69; }
.item-icon.red { background: #fff1f0; color: #e26b66; }
.item-icon.orange { background: #fff7e6; color: #d69335; }
.item-icon.purple { background: #f3effb; color: #8a71b8; }
.item-icon.pink { background: #fff0f6; color: #c97ba1; }
.item-icon.more { background: #f1f4f8; color: #8391a4; }
.item-content { min-width: 0; display: flex; flex-direction: column; gap: 7px; }
.item-content strong { font-size: 12px; color: #546379; font-weight: 400; overflow-wrap: anywhere; }
.service-item { width: 100%; display: flex; align-items: center; gap: 12px; padding: 16px 0; background: transparent; border: 0; text-align: left; }
.service-item + .service-item { border-top: 1px solid #f0f3f8; }
.service-item .item-icon { width: 44px; height: 44px; border-radius: 13px; }
.service-item .item-content strong { font-size: 14px; font-weight: 600; color: #34465d; }
.service-item .item-content small { font-size: 12px; color: #94a0b1; line-height: 1.7; }
.service-arrow { margin-left: auto; flex-shrink: 0; color: #bbc5d1; }
.text-block p { margin: 0; color: #65758a; font-size: 14px; line-height: 1.9; white-space: pre-wrap; overflow-wrap: anywhere; }
.image-block { margin: 14px 14px 0; }
.image-frame { position: relative; border-radius: 14px; }
.image-link { position: absolute; inset: 0; width: 100%; padding: 0; border: 0; border-radius: inherit; background: transparent; }
.image-block .el-image { display: block; width: 100%; min-height: 80px; border-radius: 14px; }
.image-block p { margin: 8px 0 0; color: #8592a5; font-size: 12px; line-height: 1.7; }
.image-state { min-height: 130px; display: flex; align-items: center; justify-content: center; flex-wrap: wrap; background: #eaf0f7; border-radius: 14px; font-size: 12px; color: #8592a5; }
.button-block { margin: 18px 14px; }
.button-block .el-button { width: 100%; min-height: 46px; border-radius: 12px; gap: 8px; white-space: normal; }
.tab-bar button { background: transparent; border: 0; }
.tab-item.active { color: var(--hc-primary); }
.dialog-services { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.dialog-services .el-button { margin: 0; white-space: normal; }
</style>

<template>
  <el-select :model-value="modelValue" :disabled="disabled" filterable remote clearable :remote-method="search" :loading="loading"
             placeholder="搜索保险公司名称或编码" @update:model-value="$emit('update:modelValue', $event)">
    <el-option v-for="item in options" :key="item.id" :label="item.name + ' · ' + item.code" :value="item.id" />
  </el-select>
  <span v-if="error" class="select-error" role="alert">{{ error }}</span>
</template>
<script setup>
/* global defineProps, defineEmits */
import { onBeforeUnmount, ref, watch } from 'vue'
import request, { errorMessage } from '@/utils/request'
const props = defineProps({ modelValue: Number, disabled: Boolean })
defineEmits(['update:modelValue'])
const options = ref([]), loading = ref(false), error = ref('')
let controller
async function search(keyword = '') {
  controller?.abort()
  const current = new AbortController(); controller = current; loading.value = true; error.value = ''
  try {
    const { data } = await request.get('/api/insurers', { params: { keyword, size: 50 }, signal: current.signal })
    if (!current.signal.aborted) options.value = data.data.rows
    if (props.modelValue && !options.value.some(item => item.id === props.modelValue)) {
      const selected = await request.get('/api/insurers/' + props.modelValue, { signal: current.signal })
      if (!current.signal.aborted) options.value.unshift(selected.data.data)
    }
  } catch (failure) { if (!current.signal.aborted) error.value = errorMessage(failure) }
  finally { if (controller === current) loading.value = false }
}
watch(() => props.modelValue, () => search(), { immediate: true })
onBeforeUnmount(() => controller?.abort())
</script>
<style scoped>.select-error { display: block; color: #cb5960; font-size: 12px; }</style>

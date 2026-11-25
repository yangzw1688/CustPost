<template>
  <div v-loading="loading" class="policy-detail-page">
    <!-- 顶部 -->
    <div class="page-header">
      <div>
        <h2>保单详情</h2>
        <p>查看保单的详细信息</p>
      </div>

      <el-button @click="goBack">
        <el-icon>
          <ArrowLeft />
        </el-icon>
        返回列表
      </el-button>
    </div>

    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

    <!-- 基础信息 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <div class="card-title">
          <span>基本信息</span>

          <el-tag
              :type="getStatusType(policy.status)"
          >
            {{ getStatusText(policy.status) }}
          </el-tag>
        </div>
      </template>

      <el-descriptions
          :column="3"
          border
      >
        <el-descriptions-item label="保单号">
          {{ policy.policyNo }}
        </el-descriptions-item>

        <el-descriptions-item label="保险产品">
          {{ policy.productName }}
        </el-descriptions-item>
        <el-descriptions-item label="所属项目">{{ projectName || '未分配项目' }}</el-descriptions-item>
        <el-descriptions-item label="保险公司">{{ insurerName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="所属项目">{{ projectName || '未分配项目' }}</el-descriptions-item>
        <el-descriptions-item label="保险公司">{{ insurerName || '—' }}</el-descriptions-item>

        <el-descriptions-item label="保单状态">
          <el-tag :type="getStatusType(policy.status)">
            {{ getStatusText(policy.status) }}
          </el-tag>
        </el-descriptions-item>

        <el-descriptions-item label="投保人">
          {{ policy.applicantName }}
        </el-descriptions-item>

        <el-descriptions-item label="被保人">
          {{ policy.insuredName }}
        </el-descriptions-item>

        <el-descriptions-item label="保费">
          ¥ {{ formatMoney(policy.premium) }}
        </el-descriptions-item>

        <el-descriptions-item label="生效日期">
          {{ policy.startDate }}
        </el-descriptions-item>

        <el-descriptions-item label="失效日期">
          {{ policy.endDate }}
        </el-descriptions-item>

        <el-descriptions-item label="创建时间">
          {{ policy.createTime }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 投保人信息 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <div class="card-title">
          投保人信息
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="姓名">
          {{ policy.applicantName }}
        </el-descriptions-item>

        <el-descriptions-item label="证件类型">
          {{ policy.applicantIdType || '—' }}
        </el-descriptions-item>

        <el-descriptions-item label="证件号码">
          {{ maskNumber(policy.applicantIdNo) }}
        </el-descriptions-item>

        <el-descriptions-item label="联系电话">
          {{ maskNumber(policy.applicantPhone) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 被保人信息 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <div class="card-title">
          被保人信息
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="姓名">
          {{ policy.insuredName }}
        </el-descriptions-item>

        <el-descriptions-item label="证件类型">
          {{ policy.insuredIdType || '—' }}
        </el-descriptions-item>

        <el-descriptions-item label="证件号码">
          {{ maskNumber(policy.insuredIdNo) }}
        </el-descriptions-item>

        <el-descriptions-item label="联系电话">
          {{ maskNumber(policy.insuredPhone) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 操作记录 -->
    <el-card shadow="never" class="detail-card">
      <template #header>
        <div class="card-title">
          保单操作记录
        </div>
      </template>

      <el-table v-if="!loadError" :data="pagedOperationLogs" row-key="id" stripe border
                empty-text="暂无保单操作记录">
        <el-table-column label="操作时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作类型" width="120">
          <template #default="{ row }">{{ policyOperationLabel(row.operationType) }}</template>
        </el-table-column>
        <el-table-column label="操作人" min-width="120">
          <template #default="{ row }">{{ row.operator || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作内容" min-width="250">
          <template #default="{ row }">
            <div class="log-content">{{ row.operationContent || '—' }}</div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="operationLogs.length" v-model:current-page="logPage"
                     :page-size="logPageSize" :total="operationLogs.length"
                     layout="total, prev, pager, next" class="log-pagination" />
    </el-card>
  </div>
</template>


<script>
import request, { errorMessage } from '@/utils/request'
import { formatDateTime, policyOperationLabel } from '@/utils/format'
import { ArrowLeft } from '@element-plus/icons-vue'

export default {
  name: 'PolicyDetail',

  components: {
    ArrowLeft
  },

  data () {
    return {
      loading: false,

      policy: {},
      projectName: '',
      insurerName: '',
      projectName: '',
      insurerName: '',

      operationLogs: []
    }
  },

  computed: {
    pagedOperationLogs() {
      const start = (this.logPage - 1) * this.logPageSize
      return this.operationLogs.slice(start, start + this.logPageSize)
    }
  },
  watch: {
    '$route.params.id': {
      immediate: true,
      handler(id) {
        this.loadPolicyDetail(id)
      }
    }
  },
  beforeUnmount() {
    this.detailRequest?.abort()
  },

  methods: {

    /**
     * 查询保单详情
     */
    async loadPolicyDetail (id) {
      this.detailRequest?.abort()
      this.policy = {}
      this.operationLogs = []
      this.logPage = 1
      this.loadError = ''
      if (!id) {
        this.loading = false
        this.loadError = '保单ID不能为空'
        return
      }

      const current = new AbortController()
      this.detailRequest = current
      this.loading = true

      try {
        const response = await axios.get(
            `/api/policy/${id}`
        )

        if (response.data.code !== 200) {
          this.$message.error(
              response.data.message || '获取保单详情失败'
          )
          return
        }

        const data = response.data.data || {}

        // 保单信息
        this.policy = data.policy || {}
        this.projectName = data.projectName || ''
        this.insurerName = data.insurerName || ''
        this.projectName = data.projectName || ''
        this.insurerName = data.insurerName || ''

        // 操作记录
        this.operationLogs = data.operationLogs || []

      } catch (error) {

        this.$message.error(
            '获取保单详情失败，请稍后重试'
        )
      } finally {
        this.loading = false
      }
    },

    /**
     * 返回保单列表
     */
    goBack () {
      this.$router.push('/policy/list')
    },

    /**
     * 保单状态文字
     */
    getStatusText (status) {
      const map = {
        PENDING: '待生效',
        ACTIVE: '生效中',
        EXPIRED: '已失效',
        CANCELLED: '已取消'
      }

      return map[status] || '未知'
    },

    /**
     * 保单状态颜色
     */
    getStatusType (status) {
      const map = {
        PENDING: 'warning',
        ACTIVE: 'success',
        EXPIRED: 'info',
        CANCELLED: 'danger'
      }

      return map[status] || ''
    },

    /**
     * 金额格式化
     */
    formatMoney (value) {
      return Number(value || 0).toLocaleString(
          'zh-CN',
          {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
          }
      )
    }
  }
}
</script>

<style scoped>
.policy-detail-page {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.detail-card {
  margin-bottom: 20px;
  border: none;
}

.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 600;
}

.log-pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

.log-content {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.log-pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

.log-content {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>

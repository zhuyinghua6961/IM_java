<template>
  <div class="whitelist-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-button :icon="ArrowLeft" text @click="goBack">返回</el-button>
      <h2>白名单管理</h2>
      <div></div>
    </div>

    <!-- 说明卡片 -->
    <el-card class="info-card" shadow="never">
      <template #header>
        <div class="card-header">
          <el-icon><InfoFilled /></el-icon>
          <span>什么是白名单？</span>
        </div>
      </template>
      <div class="info-content">
        <p>✅ 白名单中的好友可以直接拉你进群，无需你的同意</p>
        <p>📨 不在白名单中的好友拉你进群时，需要发送邀请等待你同意</p>
        <p>🛡️ 通过白名单，你可以控制谁有权限直接将你拉入群聊</p>
      </div>
    </el-card>

    <!-- 白名单列表 -->
    <el-card class="list-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>白名单好友 ({{ whitelistFriends.length }})</span>
          <el-button type="primary" :icon="Plus" @click="showAddDialog = true">
            添加好友
          </el-button>
        </div>
      </template>

      <!-- 空状态 -->
      <div v-if="whitelistFriends.length === 0" class="empty-state">
        <el-empty description="还没有添加白名单好友">
          <el-button type="primary" @click="showAddDialog = true">
            立即添加
          </el-button>
        </el-empty>
      </div>

      <!-- 白名单列表 -->
      <div v-else class="whitelist-list">
        <div
          v-for="friend in whitelistFriends"
          :key="friend.userId"
          class="whitelist-item"
        >
          <el-avatar :size="48" :src="friend.avatar">
            {{ friend.nickname?.charAt(0) }}
          </el-avatar>
          <div class="friend-info">
            <div class="friend-name">{{ friend.nickname }}</div>
            <div class="friend-tip">可以直接拉你进群</div>
          </div>
          <el-button
            type="danger"
            plain
            size="small"
            @click="handleRemove(friend)"
          >
            移除
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 添加好友到白名单对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="添加白名单好友"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="dialog-content">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索好友"
          :prefix-icon="Search"
          clearable
          @input="handleSearch"
        />

        <div class="friend-list">
          <!-- 空状态 -->
          <div v-if="filteredFriends.length === 0" class="empty-search">
            <el-empty description="没有找到好友" :image-size="80" />
          </div>

          <!-- 好友列表 -->
          <div
            v-for="friend in filteredFriends"
            :key="friend.userId"
            class="friend-item"
            :class="{ disabled: friend.inWhitelist }"
          >
            <el-avatar :size="40" :src="friend.avatar">
              {{ friend.nickname?.charAt(0) }}
            </el-avatar>
            <div class="friend-info">
              <div class="friend-name">{{ friend.nickname }}</div>
            </div>
            <el-button
              v-if="friend.inWhitelist"
              type="info"
              size="small"
              plain
              disabled
            >
              已添加
            </el-button>
            <el-button
              v-else
              type="primary"
              size="small"
              @click="handleAdd(friend)"
            >
              添加
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  InfoFilled,
  Plus,
  Search
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getFriendList } from '@/api/friend'

const router = useRouter()

const whitelistFriends = ref([])
const allFriends = ref([])
const showAddDialog = ref(false)
const searchKeyword = ref('')

// 过滤后的好友列表
const filteredFriends = computed(() => {
  if (!searchKeyword.value) {
    return allFriends.value
  }
  return allFriends.value.filter(f =>
    f.nickname.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// 返回上一页
const goBack = () => {
  router.back()
}

// 加载白名单列表
const loadWhitelist = async () => {
  try {
    const res = await request.get('/whitelist/list')
    whitelistFriends.value = res.data || []
  } catch (error) {
    console.error('加载白名单失败:', error)
    ElMessage.error('加载白名单失败')
  }
}

// 加载好友列表
const loadFriends = async () => {
  try {
    const res = await getFriendList()
    const friends = res.data || []
    
    // 标记哪些好友已在白名单中
    const whitelistIds = new Set(whitelistFriends.value.map(f => f.userId))
    allFriends.value = friends.map(f => ({
      ...f,
      inWhitelist: whitelistIds.has(f.userId)
    }))
  } catch (error) {
    console.error('加载好友列表失败:', error)
  }
}

// 搜索好友
const handleSearch = () => {
  // 搜索由computed自动处理
}

// 添加到白名单
const handleAdd = async (friend) => {
  try {
    await request.post(`/whitelist/add/${friend.userId}`)
    ElMessage.success(`已将 ${friend.nickname} 添加到白名单`)
    
    // 重新加载数据
    await loadWhitelist()
    await loadFriends()
  } catch (error) {
    console.error('添加白名单失败:', error)
    ElMessage.error(error.response?.data?.message || '添加失败')
  }
}

// 从白名单移除
const handleRemove = async (friend) => {
  try {
    await ElMessageBox.confirm(
      `确定要将 ${friend.nickname} 从白名单移除吗？移除后，TA拉你进群需要你的同意。`,
      '移除白名单',
      {
        confirmButtonText: '确定移除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await request.delete(`/whitelist/remove/${friend.userId}`)
    ElMessage.success('已移除')
    
    // 重新加载数据
    await loadWhitelist()
    await loadFriends()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移除白名单失败:', error)
      ElMessage.error('移除失败')
    }
  }
}

onMounted(async () => {
  await loadWhitelist()
  await loadFriends()
})
</script>

<style scoped>
.whitelist-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

/* 卡片 */
.info-card,
.list-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-weight: 600;
}

.card-header .el-icon {
  color: #409eff;
}

/* 说明内容 */
.info-content {
  line-height: 1.8;
}

.info-content p {
  margin: 8px 0;
  color: #606266;
}

/* 空状态 */
.empty-state {
  padding: 40px;
}

/* 白名单列表 */
.whitelist-list {
  max-height: 500px;
  overflow-y: auto;
}

.whitelist-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.whitelist-item:hover {
  background: #f5f7fa;
}

.whitelist-item:last-child {
  border-bottom: none;
}

.friend-info {
  flex: 1;
  margin-left: 12px;
}

.friend-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.friend-tip {
  font-size: 13px;
  color: #67c23a;
}

/* 对话框 */
.dialog-content {
  padding: 10px 0;
}

.friend-list {
  margin-top: 20px;
  max-height: 400px;
  overflow-y: auto;
}

.empty-search {
  padding: 20px;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 8px;
  transition: background 0.2s;
}

.friend-item:hover:not(.disabled) {
  background: #f5f7fa;
}

.friend-item.disabled {
  opacity: 0.6;
}
</style>

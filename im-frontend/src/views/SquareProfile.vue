<template>
  <div class="square-profile">
    <div class="profile-header">
      <el-avatar :size="64" :src="profile.avatar">
        {{ (profile.nickname || `用户 ${profile.userId}`).charAt(0) }}
      </el-avatar>
      <div class="profile-info">
        <div class="profile-name">{{ profile.nickname || `用户 ${profile.userId}` }}</div>
        <div class="profile-stats">
          <span>粉丝 {{ profile.fansCount || 0 }}</span>
          <span>关注 {{ profile.followCount || 0 }}</span>
          <span>帖子 {{ profile.postCount || 0 }}</span>
          <span>获赞 {{ profile.likeCount || 0 }}</span>
        </div>
      </div>
      <div class="profile-actions" v-if="!profile.self">
        <el-button
          v-if="profile.followed"
          size="small"
          @click="toggleFollow"
        >已关注</el-button>
        <el-button
          v-else
          size="small"
          type="primary"
          @click="toggleFollow"
        >关注</el-button>
      </div>
    </div>

    <el-divider>{{ profile.self ? '我的广场' : 'TA 的广场' }}</el-divider>

    <el-tabs v-model="activeTab" class="profile-tabs">
      <el-tab-pane :label="profile.self ? '我的帖子' : 'TA 的帖子'" name="posts" />
      <el-tab-pane :label="profile.self ? '我的收藏' : 'TA 的收藏'" name="favorites" />
      <el-tab-pane :label="profile.self ? '我的点赞' : 'TA 的点赞'" name="likes" />
    </el-tabs>

    <el-scrollbar class="square-list">
      <div
        v-for="post in posts"
        :key="post.postId"
        class="square-item"
      >
        <div class="square-content">
          <h3 v-if="post.title" class="post-title">{{ post.title }}</h3>
          <p class="post-text">{{ post.content }}</p>
        </div>
        <div class="square-actions">
          <span class="post-time">
            <span v-if="post.updateTime && post.updateTime !== post.createTime">
              编辑于 {{ formatTime(post.updateTime) }}
            </span>
            <span v-else>
              {{ formatTime(post.createTime) }}
            </span>
          </span>
        </div>
      </div>

      <div v-if="!loading && posts.length === 0" class="empty-tip">
        暂无帖子
      </div>

      <div class="load-more" v-if="hasMore">
        <el-button :loading="loading" @click="loadMore">加载更多</el-button>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getSquareProfile,
  getUserSquarePosts,
  getUserFavoriteSquarePosts,
  getUserLikedSquarePosts,
  followSquareUser,
  unfollowSquareUser
} from '@/api/square'

const route = useRoute()

const profile = ref({})
const posts = ref([])
const page = ref(1)
const size = ref(10)
const hasMore = ref(true)
const loading = ref(false)
const activeTab = ref('posts')

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return time
  return d.toLocaleString()
}

const loadProfile = async () => {
  const userId = route.params.userId
  if (!userId) return
  try {
    const res = await getSquareProfile(userId)
    profile.value = res.data || {}
  } catch (error) {
    console.error('加载广场主页失败:', error)
  }
}

const loadPosts = async (reset = false) => {
  const userId = route.params.userId
  if (!userId || loading.value) return
  loading.value = true
  try {
    if (reset) {
      page.value = 1
      posts.value = []
      hasMore.value = true
    }
    let api = getUserSquarePosts
    if (activeTab.value === 'favorites') {
      api = getUserFavoriteSquarePosts
    } else if (activeTab.value === 'likes') {
      api = getUserLikedSquarePosts
    }

    const res = await api(userId, page.value, size.value)
    const data = res.data || {}
    const records = data.records || []
    if (records.length < size.value) {
      hasMore.value = false
    }
    posts.value = posts.value.concat(records)
  } catch (error) {
    console.error('加载用户广场帖子失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  if (!hasMore.value) return
  page.value += 1
  loadPosts()
}

watch(activeTab, () => {
  loadPosts(true)
})

const toggleFollow = async () => {
  try {
    const userId = profile.value.userId
    if (!userId) return
    if (profile.value.followed) {
      await unfollowSquareUser(userId)
      profile.value.followed = false
      ElMessage.success('已取消关注')
    } else {
      await followSquareUser(userId)
      profile.value.followed = true
      ElMessage.success('关注成功')
    }
  } catch (error) {
    console.error('切换关注状态失败:', error)
  }
}

onMounted(() => {
  loadProfile()
  loadPosts(true)
})
</script>

<style scoped>
.square-profile {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f5f5;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
}

.profile-info {
  flex: 1;
}

.profile-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.profile-stats span {
  margin-right: 16px;
  color: #666;
  font-size: 14px;
}

.profile-actions {
  display: flex;
  justify-content: flex-end;
}

.square-list {
  margin-top: 16px;
  max-height: calc(100vh - 180px);
}

.profile-tabs {
  margin-top: 12px;
  background: #fff;
  border-radius: 8px;
  padding: 0 16px;
}

.square-item {
  background: #fff;
  padding: 16px;
  margin-bottom: 12px;
  border-radius: 8px;
}

.post-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
}

.post-text {
  white-space: pre-wrap;
}

.square-actions {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

.load-more {
  text-align: center;
  padding: 10px 0 20px;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
</style>

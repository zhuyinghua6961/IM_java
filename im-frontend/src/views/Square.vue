<template>
  <div class="square-container">
    <div class="square-header">
      <h2>广场</h2>
      <el-button type="primary" :icon="Plus" @click="showPublishDialog = true">
        发布帖子
      </el-button>
    </div>

    <el-scrollbar class="square-list">
      <div
        v-for="post in posts"
        :key="post.postId"
        class="square-item"
      >
        <div class="square-user">
          <el-avatar :size="40" :src="post.avatar">
            {{ (post.nickname || `用户 ${post.userId}`).charAt(0) }}
          </el-avatar>
          <div class="user-info">
            <div class="user-name">{{ post.nickname || `用户 ${post.userId}` }}</div>
            <div class="post-time">{{ post.createTime }}</div>
          </div>
        </div>

        <div class="square-content">
          <h3 v-if="post.title" class="post-title">{{ post.title }}</h3>
          <p class="post-text">{{ post.content }}</p>
          <div v-if="post.images && post.images.length" class="post-images">
            <el-image
              v-for="(img, index) in post.images"
              :key="index"
              :src="img"
              fit="cover"
              class="post-image"
            />
          </div>
        </div>

        <div class="square-actions">
          <el-button text :icon="ChatDotRound" @click="openComments(post)">
            评论 {{ post.commentCount || 0 }}
          </el-button>
          <el-button
            text
            :type="post.liked ? 'primary' : 'default'"
            :icon="Star"
            @click="toggleLike(post)"
          >
            {{ post.liked ? '已赞' : '点赞' }} {{ post.likeCount || 0 }}
          </el-button>
        </div>
      </div>

      <div v-if="!loading && posts.length === 0" class="empty-tip">
        暂无帖子，快来发布第一条吧 ~
      </div>

      <div class="load-more" v-if="hasMore">
        <el-button :loading="loading" @click="loadMore">加载更多</el-button>
      </div>
    </el-scrollbar>

    <!-- 发布帖子对话框 -->
    <el-dialog v-model="showPublishDialog" title="发布帖子" width="500px">
      <el-form :model="publishForm" label-width="60px">
        <el-form-item label="标题">
          <el-input v-model="publishForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="publishForm.content"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="分享点什么..."
          />
        </el-form-item>
        <el-form-item label="图片">
          <el-input
            v-model="publishForm.imagesText"
            type="textarea"
            :rows="2"
            placeholder="可选，输入图片 URL，多个用换行分隔"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="handlePublish">
          发布
        </el-button>
      </template>
    </el-dialog>

    <!-- 评论对话框 -->
    <el-dialog v-model="showCommentDialog" :title="'评论帖子 #' + currentPostId" width="500px">
      <div class="comment-list">
        <div
          v-for="comment in comments"
          :key="comment.commentId"
          class="comment-item"
        >
          <div class="comment-header">
            <span class="comment-user">{{ comment.nickname || `用户 ${comment.userId}` }}</span>
            <span class="comment-time">{{ comment.createTime }}</span>
          </div>
          <div class="comment-content">{{ comment.content }}</div>
        </div>
        <div v-if="!commentLoading && comments.length === 0" class="empty-comment-tip">
          还没有评论，来抢沙发吧 ~
        </div>
      </div>

      <el-input
        v-model="commentContent"
        type="textarea"
        :rows="3"
        maxlength="200"
        show-word-limit
        placeholder="说点什么..."
        class="comment-input"
      />

      <template #footer>
        <el-button @click="showCommentDialog = false">关闭</el-button>
        <el-button type="primary" :loading="commentSubmitting" @click="handleSubmitComment">
          发表
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, ChatDotRound, Star } from '@element-plus/icons-vue'
import {
  publishSquarePost,
  getSquarePosts,
  likeSquarePost,
  unlikeSquarePost,
  getSquareComments,
  addSquareComment
} from '@/api/square'

const posts = ref([])
const page = ref(1)
const size = ref(10)
const hasMore = ref(true)
const loading = ref(false)

const showPublishDialog = ref(false)
const publishing = ref(false)
const publishForm = ref({
  title: '',
  content: '',
  imagesText: ''
})

const showCommentDialog = ref(false)
const currentPostId = ref(null)
const comments = ref([])
const commentPage = ref(1)
const commentSize = ref(20)
const commentLoading = ref(false)
const commentContent = ref('')
const commentSubmitting = ref(false)

const loadPosts = async (reset = false) => {
  if (loading.value) return
  loading.value = true
  try {
    if (reset) {
      page.value = 1
      posts.value = []
      hasMore.value = true
    }
    const res = await getSquarePosts(page.value, size.value)
    const data = res.data || {}
    const records = data.records || []
    if (records.length < size.value) {
      hasMore.value = false
    }
    posts.value = posts.value.concat(records)
  } catch (error) {
    console.error('加载广场帖子失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  if (!hasMore.value) return
  page.value += 1
  loadPosts()
}

const handlePublish = async () => {
  if (!publishForm.value.content.trim()) {
    ElMessage.warning('内容不能为空')
    return
  }
  publishing.value = true
  try {
    const images = publishForm.value.imagesText
      ? publishForm.value.imagesText
          .split(/\n|,/) // 换行或逗号分隔
          .map(s => s.trim())
          .filter(Boolean)
      : []
    await publishSquarePost({
      title: publishForm.value.title || null,
      content: publishForm.value.content,
      images,
      video: null,
      tags: []
    })
    ElMessage.success('发布成功')
    showPublishDialog.value = false
    publishForm.value = { title: '', content: '', imagesText: '' }
    await loadPosts(true)
  } catch (error) {
    console.error('发布帖子失败:', error)
  } finally {
    publishing.value = false
  }
}

const toggleLike = async (post) => {
  try {
    if (post.liked) {
      await unlikeSquarePost(post.postId)
      post.liked = false
      post.likeCount = Math.max((post.likeCount || 1) - 1, 0)
    } else {
      await likeSquarePost(post.postId)
      post.liked = true
      post.likeCount = (post.likeCount || 0) + 1
    }
  } catch (error) {
    console.error('切换点赞状态失败:', error)
  }
}

const openComments = async (post) => {
  currentPostId.value = post.postId
  comments.value = []
  commentPage.value = 1
  showCommentDialog.value = true
  await loadComments()
}

const loadComments = async () => {
  if (!currentPostId.value) return
  commentLoading.value = true
  try {
    const res = await getSquareComments(currentPostId.value, commentPage.value, commentSize.value)
    const data = res.data || {}
    comments.value = data.records || []
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    commentLoading.value = false
  }
}

const handleSubmitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  if (!currentPostId.value) return
  commentSubmitting.value = true
  try {
    await addSquareComment(currentPostId.value, {
      content: commentContent.value,
      parentId: null
    })
    ElMessage.success('评论成功')
    commentContent.value = ''
    await loadComments()
    // 同步更新列表中的评论数
    const idx = posts.value.findIndex(p => p.postId === currentPostId.value)
    if (idx !== -1) {
      posts.value[idx].commentCount = (posts.value[idx].commentCount || 0) + 1
    }
  } catch (error) {
    console.error('发表评论失败:', error)
  } finally {
    commentSubmitting.value = false
  }
}

onMounted(() => {
  loadPosts(true)
})
</script>

<style scoped>
.square-container {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f5f5;
}

.square-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
}

.square-list {
  max-height: calc(100vh - 160px);
}

.square-item {
  background: white;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 8px;
}

.square-user {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.user-info {
  flex: 1;
}

.user-name {
  font-weight: bold;
  margin-bottom: 5px;
}

.post-time {
  color: #999;
  font-size: 12px;
}

.square-content {
  margin-bottom: 15px;
}

.post-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
}

.post-text {
  white-space: pre-wrap;
}

.post-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 10px;
}

.post-image {
  width: 100%;
  height: 120px;
  border-radius: 4px;
}

.square-actions {
  display: flex;
  gap: 20px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
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

.comment-list {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 10px;
}

.comment-item {
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.comment-content {
  font-size: 14px;
}

.empty-comment-tip {
  text-align: center;
  color: #999;
  padding: 20px 0;
}

.comment-input {
  margin-top: 10px;
}
</style>

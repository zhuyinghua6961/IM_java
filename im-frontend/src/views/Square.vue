<template>
  <div class="square-container">
    <div class="square-header">
      <h2>广场</h2>
      <el-radio-group v-model="activeTab" size="small" @change="handleTabChange">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="hot">热门</el-radio-button>
        <el-radio-button label="follow">我的关注</el-radio-button>
        <el-radio-button label="notify">
          <span class="tab-label">
            我的广场消息
            <span v-if="notifyUnread > 0" class="unread-dot">{{ notifyUnread }}</span>
          </span>
        </el-radio-button>
      </el-radio-group>
      <el-button type="default" plain style="margin-left: 8px;" @click="goProfile(currentUserId)">
        我的广场主页
      </el-button>
      <el-button type="primary" :icon="Plus" @click="showPublishDialog = true">
        发布帖子
      </el-button>
    </div>

    <div class="square-search-bar">
      <div class="square-search-row square-search-main">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索帖子内容 / 标题"
          clearable
          class="square-search-input"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchTags"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="选择或输入标签"
          class="square-search-tags"
        >
          <el-option
            v-for="tag in allTagOptions"
            :key="tag"
            :label="tag"
            :value="tag"
          />
        </el-select>
        <el-button type="primary" size="small" @click="handleSearch">
          搜索
        </el-button>
        <el-button size="small" @click="resetSearch">重置</el-button>
      </div>
      <div class="square-search-row square-search-filters">
        <span class="square-filter-label">筛选：</span>
        <el-checkbox
          v-model="filterHasImage"
          @change="handleFilterChange"
        >
          有图
        </el-checkbox>
        <el-checkbox
          v-model="filterHasVideo"
          @change="handleFilterChange"
        >
          有视频
        </el-checkbox>
        <el-select
          v-model="filterVisibleType"
          placeholder="可见范围"
          clearable
          class="square-filter-select"
          @change="handleFilterChange"
        >
          <el-option label="全部可见范围" :value="null" />
          <el-option label="仅好友可见" :value="1" />
        </el-select>
        <el-select
          v-model="sortOrder"
          placeholder="排序"
          clearable
          class="square-filter-select"
          @change="handleFilterChange"
        >
          <el-option label="时间最新" value="time" />
          <el-option label="评论最多" value="comment" />
          <el-option label="点赞最多" value="like" />
        </el-select>
      </div>
    </div>

    <el-scrollbar v-if="activeTab !== 'notify'" class="square-list" ref="postScrollbar">
      <div
        v-for="post in posts"
        :key="post.postId"
        class="square-item"
        :id="'square-post-' + post.postId"
      >
        <div class="square-user">
          <el-avatar :size="40" :src="post.avatar" @click="goProfile(post.userId)" style="cursor: pointer;">
            {{ (post.nickname || `用户 ${post.userId}`).charAt(0) }}
          </el-avatar>
          <div class="user-info">
            <div class="user-name" @click="goProfile(post.userId)" style="cursor: pointer;">
              {{ post.nickname || `用户 ${post.userId}` }}
            </div>
            <div class="post-time">
              <span v-if="post.updateTime && post.updateTime !== post.createTime">
                编辑于 {{ formatTime(post.updateTime) }}
              </span>
              <span v-else>
                {{ formatTime(post.createTime) }}
              </span>
            </div>
          </div>
          <div class="user-actions" v-if="String(post.userId) !== String(currentUserId)">
            <el-button
              v-if="post.followed"
              size="small"
              text
              @click="toggleFollow(post)"
            >已关注</el-button>
            <el-button
              v-else
              size="small"
              type="primary"
              text
              @click="toggleFollow(post)"
            >关注</el-button>
          </div>
        </div>

        <div class="square-content">
          <h3 v-if="post.title" class="post-title">{{ post.title }}</h3>
          <p class="post-text">{{ post.content }}</p>
          <div v-if="post.tags && post.tags.length" class="post-tags">
            <el-tag
              v-for="tag in post.tags"
              :key="tag"
              size="small"
              class="post-tag-item"
            >
              {{ tag }}
            </el-tag>
          </div>
          <div v-if="post.images && post.images.length" class="post-images">
            <div
              v-for="(img, index) in getDisplayImages(post)"
              :key="index"
              class="post-image-wrapper"
            >
              <el-image
                :src="img"
                fit="cover"
                class="post-image"
                @click.stop="openImagePreview(img)"
              />
              <div
                v-if="isImageOverlayVisible(post, index)"
                class="post-image-overlay"
                @click.stop="toggleExpandImages(post)"
              >
                +{{ post.images.length - 9 }}
              </div>
            </div>
          </div>
        </div>

        <div class="square-actions">
          <el-button text :icon="ChatDotRound" @click="openComments(post)">
            评论 {{ post.commentCount || 0 }}
          </el-button>
          <el-button
            text
            :type="post.liked ? 'primary' : 'default'"
            :icon="Pointer"
            @click="toggleLike(post)"
          >
            {{ post.liked ? '已赞' : '点赞' }} {{ post.likeCount || 0 }}
          </el-button>
          <el-button
            text
            :type="post.favorited ? 'primary' : 'default'"
            :icon="Star"
            @click="toggleFavorite(post)"
          >
            {{ post.favorited ? '已收藏' : '收藏' }} {{ post.favoriteCount || 0 }}
          </el-button>
          <el-button
            v-if="String(post.userId) === String(currentUserId)"
            text
            type="primary"
            @click="openEditPost(post)"
          >
            编辑
          </el-button>
          <el-button
            v-if="String(post.userId) === String(currentUserId)"
            text
            type="danger"
            @click="handleDeletePost(post)"
          >
            删除
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

    <!-- 我的广场消息 -->
    <el-scrollbar v-else class="square-list">
      <div class="notify-type-filter">
        <el-radio-group v-model="notifyTypeFilter" size="small">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="LIKE">点赞</el-radio-button>
          <el-radio-button label="COMMENT">评论</el-radio-button>
          <el-radio-button label="FOLLOW_POST">发帖</el-radio-button>
        </el-radio-group>
      </div>

      <el-tabs v-model="notifyInnerTab">
        <el-tab-pane label="未读" name="unread">
          <div class="notify-actions" v-if="unreadNotifications.length > 0">
            <el-button text size="small" @click="handleMarkAllRead">全部标记为已读</el-button>
          </div>
          <div
            v-for="item in unreadNotifications"
            :key="item.id"
            class="notify-item"
            :class="{ 'notify-item--unread': item.read === 0 }"
            @click="handleClickNotification(item)"
          >
            <div class="notify-message">{{ item.message }}</div>
            <div class="notify-meta">
              <span class="notify-time">{{ formatTime(item.createTime) }}</span>
              <span class="notify-type" v-if="item.actionType === 'LIKE'">点赞</span>
              <span class="notify-type" v-else-if="item.actionType === 'COMMENT'">评论</span>
              <span class="notify-type" v-else-if="item.actionType === 'FOLLOW_POST'">发帖</span>
            </div>
          </div>

          <div v-if="!notifyLoading && unreadNotifications.length === 0" class="empty-tip">
            暂无未读消息
          </div>
        </el-tab-pane>

        <el-tab-pane label="已读" name="read">
          <div
            v-for="item in readNotifications"
            :key="item.id"
            class="notify-item"
          >
            <div class="notify-message">{{ item.message }}</div>
            <div class="notify-meta">
              <span class="notify-time">{{ formatTime(item.createTime) }}</span>
              <span class="notify-type" v-if="item.actionType === 'LIKE'">点赞</span>
              <span class="notify-type" v-else-if="item.actionType === 'COMMENT'">评论</span>
              <span class="notify-type" v-else-if="item.actionType === 'FOLLOW_POST'">发帖</span>
            </div>
          </div>

          <div v-if="!notifyLoading && readNotifications.length === 0" class="empty-tip">
            暂无已读消息
          </div>
        </el-tab-pane>
      </el-tabs>

      <div class="load-more" v-if="notifyHasMore">
        <el-button :loading="notifyLoading" @click="loadMoreNotifications">加载更多</el-button>
      </div>
    </el-scrollbar>

    <!-- 发布帖子对话框 -->
    <el-dialog v-model="showPublishDialog" :title="isEditingPost ? '编辑帖子' : '发布帖子'" width="500px">
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
        <el-form-item label="标签">
          <el-select
            v-model="publishForm.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入标签"
          >
            <el-option
              v-for="tag in allTagOptions"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="图片/视频">
          <div class="media-upload">
            <el-upload
              class="image-upload"
              action="/api/files/upload/image"
              list-type="picture-card"
              :file-list="imageFileList"
              :limit="18"
              :on-success="handleImageUploadSuccess"
              :on-remove="handleImageRemove"
              :before-upload="beforeImageUpload"
              :http-request="uploadImageRequest"
              :show-file-list="true"
              :multiple="true"
              accept="image/*"
              :disabled="isImageUploadDisabled"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <el-upload
              class="video-upload"
              action="/api/files/upload/video"
              :file-list="videoFileList"
              :limit="1"
              :on-success="handleVideoUploadSuccess"
              :on-remove="handleVideoRemove"
              :before-upload="beforeVideoUpload"
              :http-request="uploadVideoRequest"
              :show-file-list="true"
              accept="video/*"
              :disabled="isVideoUploadDisabled"
            >
              <el-button type="primary" plain>上传视频（可选，最多 1 个）</el-button>
            </el-upload>
            <div class="media-tip">
              已选择 {{ imageUrls.length }} 张图片{{ videoUrl ? '，1 个视频' : '' }}，最多图片和视频合计 18 个
            </div>
          </div>
        </el-form-item>
        <el-form-item label="范围">
          <el-radio-group v-model="publishForm.visibleType">
            <el-radio :label="0">公开</el-radio>
            <el-radio :label="1">仅好友</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="不给谁看">
          <el-select
            v-model="publishForm.excludeUserIds"
            multiple
            filterable
            collapse-tags
            placeholder="可选，从好友里选择不希望看到这条帖的人"
            :loading="friendLoading"
            @visible-change="handleFriendSelectVisible"
          >
            <el-option
              v-for="f in friendOptions"
              :key="f.userId"
              :label="f.remark || f.nickname || f.username || `用户 ${f.userId}`"
              :value="f.userId"
            />
          </el-select>
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
      <div class="comment-list" ref="commentListRef">
        <div
          v-for="comment in comments"
          :key="comment.commentId"
          class="comment-item"
          :id="'square-comment-' + comment.commentId"
          :class="{ 'comment-item--highlight': highlightedCommentId === comment.commentId }"
        >
          <div class="comment-header">
            <span class="comment-user">{{ comment.nickname || `用户 ${comment.userId}` }}</span>
            <span class="comment-time">{{ comment.createTime }}</span>
            <div>
              <el-button text size="small" @click="startReply(comment)">回复</el-button>
              <el-button
                v-if="String(comment.userId) === String(currentUserId)"
                text
                type="danger"
                size="small"
                @click="handleDeleteComment(comment)"
              >
                删除
              </el-button>
            </div>
          </div>
          <div class="comment-content">{{ comment.content }}</div>

          <div
            v-if="comment.replies && comment.replies.length"
            class="comment-replies"
          >
            <div
              v-for="reply in getVisibleReplies(comment)"
              :key="reply.commentId"
              :id="'square-comment-' + reply.commentId"
              :class="['comment-item', 'comment-item--reply', { 'comment-item--highlight': highlightedCommentId === reply.commentId }]"
            >
              <div class="comment-header">
                <span class="comment-user">
                  {{ reply.nickname || `用户 ${reply.userId}` }}
                  <span class="comment-reply-target">
                    回复 {{ comment.nickname || `用户 ${comment.userId}` }}
                  </span>
                </span>
                <span class="comment-time">{{ reply.createTime }}</span>
                <div>
                  <el-button text size="small" @click="startReply(reply)">回复</el-button>
                  <el-button
                    v-if="String(reply.userId) === String(currentUserId)"
                    text
                    type="danger"
                    size="small"
                    @click="handleDeleteComment(reply)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
              <div class="comment-content">{{ reply.content }}</div>
            </div>
            <div
              v-if="comment.replies.length > maxVisibleReplies"
              class="comment-replies-toggle"
            >
              <el-button
                text
                size="small"
                @click="toggleRepliesExpand(comment)"
              >
                {{
                  isRepliesExpanded(comment)
                    ? '收起回复'
                    : `展开更多回复（剩余 ${comment.replies.length - maxVisibleReplies} 条）`
                }}
              </el-button>
            </div>
          </div>
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
        :placeholder="replyTarget ? `回复 ${replyTarget.nickname || `用户 ${replyTarget.userId}`}` : '说点什么...'"
        class="comment-input"
      />

      <template #footer>
        <el-button @click="showCommentDialog = false">关闭</el-button>
        <el-button type="primary" :loading="commentSubmitting" @click="handleSubmitComment">
          发表
        </el-button>
      </template>
    </el-dialog>

    <!-- 图片预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      width="400px"
      align-center
      class="image-preview-dialog"
    >
      <div class="image-preview-square">
        <img :src="previewImageUrl" alt="preview" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, ChatDotRound, Star, Pointer } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { getFriendList } from '@/api/friend'
import {
  publishSquarePost,
  getSquarePosts,
  getHotSquarePosts,
  getFollowSquarePosts,
  deleteSquarePost,
  likeSquarePost,
  unlikeSquarePost,
  favoriteSquarePost,
  unfavoriteSquarePost,
  getSquareComments,
  addSquareComment,
  deleteSquareComment,
  getSquareNotifications,
  markSquareNotificationsRead,
  markSquareNotificationsReadByIds,
  followSquareUser,
  unfollowSquareUser,
  getSquarePostDetail,
  updateSquarePost
} from '@/api/square'

const router = useRouter()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.userId || null)

const activeTab = ref('all')
const notifyInnerTab = ref('unread')

const posts = ref([])
const page = ref(1)
const size = ref(10)
const hasMore = ref(true)
const loading = ref(false)

const postScrollbar = ref(null)

// 图片九宫格展开状态
const expandedPostId = ref(null)

const showPublishDialog = ref(false)
const publishing = ref(false)
const isEditingPost = ref(false)
const editingPostId = ref(null)
const publishForm = ref({
  title: '',
  content: '',
  tags: [],
  visibleType: 0,
  excludeUserIds: []
})

// 媒体上传状态
const imageUrls = ref([])
const videoUrl = ref('')
const imageFileList = ref([])
const videoFileList = ref([])

const totalMediaCount = computed(() => {
  return imageUrls.value.length + (videoUrl.value ? 1 : 0)
})

const isImageUploadDisabled = computed(() => {
  // 如果已有视频，则图片数量 + 1 不得超过 18
  const maxImages = videoUrl.value ? 17 : 18
  return imageUrls.value.length >= maxImages
})

const isVideoUploadDisabled = computed(() => {
  // 只能有 1 个视频，且总数不能超过 18
  if (videoUrl.value) return true
  return imageUrls.value.length >= 18
})

const friendOptions = ref([])
const friendLoading = ref(false)

// 搜索关键字和标签
const allTagOptions = ref(['日常', '吐槽', '求助', '分享', '照片', '视频'])
const searchKeyword = ref('')
const searchTags = ref([])

const filterHasImage = ref(false)
const filterHasVideo = ref(false)
const filterVisibleType = ref(null)
const sortOrder = ref('')

const showCommentDialog = ref(false)
const currentPostId = ref(null)
const comments = ref([])
const commentPage = ref(1)
const commentSize = ref(20)
const commentLoading = ref(false)
const commentContent = ref('')
const commentSubmitting = ref(false)
const replyTarget = ref(null)
const commentListRef = ref(null)
const highlightedCommentId = ref(null)

const maxVisibleReplies = 2
const expandedReplyCommentIds = ref([])

// 广场通知
const notifications = ref([])
const notifyPage = ref(1)
const notifySize = ref(20)
const notifyHasMore = ref(true)
const notifyLoading = ref(false)
const notifyUnread = ref(0)
const notifyTypeFilter = ref('all')

// 图片预览
const previewVisible = ref(false)
const previewImageUrl = ref('')

const filterNotificationsByType = (list) => {
  if (notifyTypeFilter.value === 'all') return list
  return list.filter(item => item.actionType === notifyTypeFilter.value)
}

const unreadNotifications = computed(() =>
  filterNotificationsByType(notifications.value.filter(item => item.read === 0))
)

const readNotifications = computed(() =>
  filterNotificationsByType(notifications.value.filter(item => item.read === 1))
)

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return time
  return d.toLocaleString()
}

const loadPosts = async (reset = false) => {
  if (loading.value) return
  loading.value = true
  try {
    if (reset) {
      page.value = 1
      posts.value = []
      hasMore.value = true
    }
    let res
    if (activeTab.value === 'follow') {
      res = await getFollowSquarePosts(page.value, size.value)
    } else if (activeTab.value === 'hot') {
      res = await getHotSquarePosts(page.value, size.value)
    } else {
      const visibleTypeParam = filterVisibleType.value === 1 ? 1 : null
      const sortParam =
        sortOrder.value === 'comment' || sortOrder.value === 'like' ? sortOrder.value : null
      res = await getSquarePosts(
        page.value,
        size.value,
        searchKeyword.value,
        searchTags.value,
        filterHasImage.value,
        filterHasVideo.value,
        visibleTypeParam,
        sortParam
      )
    }
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

const highlightComment = async (commentId) => {
  if (!commentId) return
  highlightedCommentId.value = commentId
  await nextTick()
  const container = commentListRef.value
  if (!container) return
  const el = container.querySelector(`#square-comment-${commentId}`)
  if (!el) return
  const offsetTop = el.offsetTop - container.offsetTop
  container.scrollTop = offsetTop
  setTimeout(() => {
    if (highlightedCommentId.value === commentId) {
      highlightedCommentId.value = null
    }
  }, 3000)
}

const goProfile = (userId) => {
  if (!userId) return
  router.push(`/square/profile/${userId}`)
}

const handleSearch = () => {
  activeTab.value = 'all'
  loadPosts(true)
}

const resetSearch = () => {
  searchKeyword.value = ''
  searchTags.value = []
  handleSearch()
}

const handleFilterChange = () => {
  activeTab.value = 'all'
  loadPosts(true)
}

watch(
  () => [searchKeyword.value, searchTags.value.length, activeTab.value],
  ([kw, tagLen, tab]) => {
    if (tab !== 'all') return
    const hasKeyword = kw && String(kw).trim().length > 0
    const hasTags = tagLen > 0
    if (!hasKeyword && !hasTags) {
      loadPosts(true)
    }
  }
)

const openEditPost = (post) => {
  if (!post) return
  isEditingPost.value = true
  editingPostId.value = post.postId
  publishForm.value = {
    title: post.title || '',
    content: post.content || '',
    tags: Array.isArray(post.tags) ? [...post.tags] : [],
    visibleType: typeof post.visibleType === 'number' ? post.visibleType : 0,
    excludeUserIds: Array.isArray(post.excludeUserIds) ? [...post.excludeUserIds] : []
  }
  imageUrls.value = Array.isArray(post.images) ? [...post.images] : []
  videoUrl.value = post.video || ''
  imageFileList.value = imageUrls.value.map((url, index) => ({ name: `image-${index}`, url }))
  videoFileList.value = videoUrl.value ? [{ name: 'video', url: videoUrl.value }] : []
  showPublishDialog.value = true
}

const openImagePreview = (url) => {
  if (!url) return
  previewImageUrl.value = url
  previewVisible.value = true
}

const getDisplayImages = (post) => {
  const images = post.images || []
  if (expandedPostId.value === post.postId) {
    return images
  }
  if (images.length <= 9) {
    return images
  }
  return images.slice(0, 9)
}

const isImageOverlayVisible = (post, index) => {
  const images = post.images || []
  if (images.length <= 9) {
    return false
  }
  if (expandedPostId.value === post.postId) {
    return false
  }
  // 只在第9张（索引8）显示 +N 覆盖
  return index === 8
}

const toggleExpandImages = (post) => {
  if (!post || !post.postId) return
  expandedPostId.value = expandedPostId.value === post.postId ? null : post.postId
}

const handleMarkAllRead = async () => {
  try {
    await markSquareNotificationsRead()
    notifyUnread.value = 0
    notifications.value = notifications.value.map(item => ({ ...item, read: 1 }))
  } catch (error) {
    console.error('标记广场消息已读失败:', error)
  }
}

const loadNotifications = async (reset = false) => {
  if (notifyLoading.value) return
  notifyLoading.value = true
  try {
    if (reset) {
      notifyPage.value = 1
      notifications.value = []
      notifyHasMore.value = true
    }
    const res = await getSquareNotifications(notifyPage.value, notifySize.value)
    const data = res.data || {}
    const records = data.records || []
    if (typeof data.unread === 'number') {
      notifyUnread.value = data.unread
    }
    if (records.length < notifySize.value) {
      notifyHasMore.value = false
    }
    notifications.value = notifications.value.concat(records)
  } catch (error) {
    console.error('加载广场通知失败:', error)
  } finally {
    notifyLoading.value = false
  }
}

const handleClickNotification = async (item) => {
  if (!item) return

  const id = item.id
  if (item.read === 0 && id) {
    // 先本地更新已读状态，提升体验
    item.read = 1
    notifyUnread.value = Math.max((notifyUnread.value || 0) - 1, 0)
    try {
      await markSquareNotificationsReadByIds([id])
    } catch (error) {
      console.error('单条标记广场消息已读失败:', error)
    }
  }

  const postId = item.postId
  if (postId) {
    try {
      // 切换到全部帖子 Tab
      activeTab.value = 'all'
      // 重置列表并加载第一页
      await loadPosts(true)

      // 继续加载后续页面，直到包含目标帖子或到达末尾（有限次数）
      await ensurePostLoaded(postId)

      // 滚动到目标帖子位置
      await scrollToPost(postId)

      const targetPost = posts.value.find(p => p.postId === postId)
      // 评论类通知，自动打开评论对话框并高亮对应评论
      if (item.actionType === 'COMMENT' && targetPost) {
        await openComments(targetPost)
        if (item.commentId) {
          await highlightComment(item.commentId)
        }
      }
    } catch (error) {
      console.error('根据广场通知加载帖子详情失败:', error)
    }
  }
}

const loadMoreNotifications = () => {
  if (!notifyHasMore.value) return
  notifyPage.value += 1
  loadNotifications()
}

const handleTabChange = () => {
  if (activeTab.value === 'notify') {
    loadNotifications(true)
  } else {
    loadPosts(true)
  }
}

const ensurePostLoaded = async (postId) => {
  if (!postId) return
  let guard = 0
  while (!posts.value.some(p => p.postId === postId) && hasMore.value && guard < 10) {
    page.value += 1
    await loadPosts()
    guard += 1
  }
}

const scrollToPost = async (postId) => {
  if (!postId) return
  await nextTick()
  if (!postScrollbar.value || !postScrollbar.value.wrapRef) return
  const container = postScrollbar.value.wrapRef
  const el = document.getElementById(`square-post-${postId}`)
  if (!el) return
  const offsetTop = el.offsetTop - container.offsetTop
  try {
    postScrollbar.value.setScrollTop(offsetTop)
  } catch (error) {
    console.error('滚动到目标帖子失败:', error)
  }
}

const toggleFollow = async (post) => {
  const targetId = post.userId
  if (!targetId) return
  try {
    if (post.followed) {
      await unfollowSquareUser(targetId)
      post.followed = false
    } else {
      await followSquareUser(targetId)
      post.followed = true
    }
  } catch (error) {
    console.error('切换关注状态失败:', error)
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

  const total = totalMediaCount.value
  if (total > 18) {
    ElMessage.warning('图片和视频总数不能超过 18 个')
    return
  }

  publishing.value = true
  try {
    const payload = {
      title: publishForm.value.title || null,
      content: publishForm.value.content,
      images: imageUrls.value,
      video: videoUrl.value || null,
      tags: publishForm.value.tags || [],
      visibleType: publishForm.value.visibleType,
      excludeUserIds: publishForm.value.excludeUserIds || []
    }

    if (isEditingPost.value && editingPostId.value) {
      await updateSquarePost(editingPostId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await publishSquarePost(payload)
      ElMessage.success('发布成功')
    }
    showPublishDialog.value = false
    isEditingPost.value = false
    editingPostId.value = null
    publishForm.value = { title: '', content: '', visibleType: 0, excludeUserIds: [] }
    imageUrls.value = []
    videoUrl.value = ''
    imageFileList.value = []
    videoFileList.value = []
    await loadPosts(true)
  } catch (error) {
    console.error('发布帖子失败:', error)
  } finally {
    publishing.value = false
  }
}

// 图片上传相关
const uploadImageRequest = async (options) => {
  const { file, onSuccess, onError } = options
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await request.post('/files/upload/image', formData)
    const url = res.data?.url
    if (!url) {
      throw new Error('上传图片失败：未返回URL')
    }
    imageUrls.value.push(url)
    file.url = url
    onSuccess(res, file)
  } catch (error) {
    console.error('上传图片失败:', error)
    ElMessage.error('上传图片失败')
    onError(error)
  }
}

const handleImageUploadSuccess = (response, file, fileList) => {
  imageFileList.value = fileList
}

const handleImageRemove = (file, fileList) => {
  const url = file?.response?.data?.url || file.url
  if (url) {
    imageUrls.value = imageUrls.value.filter(u => u !== url)
  }
  imageFileList.value = fileList
}

const beforeImageUpload = (file) => {
  const maxImages = videoUrl.value ? 17 : 18
  if (imageUrls.value.length >= maxImages) {
    ElMessage.warning(`最多只能上传 ${maxImages} 张图片`)
    return false
  }

  // 只允许图片类型
  if (!file.type || !file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return false
  }
  return true
}

// 视频上传相关
const uploadVideoRequest = async (options) => {
  const { file, onSuccess, onError } = options
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await request.post('/files/upload/video', formData)
    const url = res.data?.url
    if (!url) {
      throw new Error('上传视频失败：未返回URL')
    }
    videoUrl.value = url
    file.url = url
    onSuccess(res, file)
  } catch (error) {
    console.error('上传视频失败:', error)
    ElMessage.error('上传视频失败')
    onError(error)
  }
}

const handleVideoUploadSuccess = (response, file, fileList) => {
  videoFileList.value = fileList
}

const handleVideoRemove = (file, fileList) => {
  videoUrl.value = ''
  videoFileList.value = fileList
}

const beforeVideoUpload = (file) => {
  if (videoUrl.value) {
    ElMessage.warning('最多只能上传 1 个视频')
    return false
  }
  if (imageUrls.value.length >= 18) {
    ElMessage.warning('图片和视频总数不能超过 18 个')
    return false
  }

  // 只允许视频类型
  if (!file.type || !file.type.startsWith('video/')) {
    ElMessage.warning('只能上传视频文件')
    return false
  }
  return true
}

const handleFriendSelectVisible = (visible) => {
  if (visible && friendOptions.value.length === 0) {
    loadFriendOptions()
  }
}

const loadFriendOptions = async () => {
  if (friendLoading.value) return
  friendLoading.value = true
  try {
    const res = await getFriendList()
    friendOptions.value = res.data || []
  } catch (error) {
    console.error('加载好友列表失败:', error)
  } finally {
    friendLoading.value = false
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

const toggleFavorite = async (post) => {
  try {
    if (post.favorited) {
      await unfavoriteSquarePost(post.postId)
      post.favorited = false
      post.favoriteCount = Math.max((post.favoriteCount || 1) - 1, 0)
    } else {
      await favoriteSquarePost(post.postId)
      post.favorited = true
      post.favoriteCount = (post.favoriteCount || 0) + 1
    }
  } catch (error) {
    console.error('切换收藏状态失败:', error)
  }
}

const openComments = async (post) => {
  currentPostId.value = post.postId
  comments.value = []
  commentPage.value = 1
  replyTarget.value = null
  commentContent.value = ''
  showCommentDialog.value = true
  await loadComments()
}

const loadComments = async () => {
  if (!currentPostId.value) return
  commentLoading.value = true
  try {
    const res = await getSquareComments(currentPostId.value, commentPage.value, commentSize.value)
    const data = res.data || {}
    const records = data.records || []
    const map = new Map()
    records.forEach(r => {
      map.set(r.commentId, { ...r, replies: [] })
    })
    const roots = []
    records.forEach(r => {
      const node = map.get(r.commentId)
      if (r.parentId) {
        const parent = map.get(r.parentId)
        if (parent) {
          parent.replies.push(node)
        } else {
          roots.push(node)
        }
      } else {
        roots.push(node)
      }
    })
    comments.value = roots
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    commentLoading.value = false
  }
}

const isRepliesExpanded = (comment) => {
  if (!comment) return false
  return expandedReplyCommentIds.value.includes(comment.commentId)
}

const toggleRepliesExpand = (comment) => {
  if (!comment || !comment.commentId) return
  const id = comment.commentId
  const idx = expandedReplyCommentIds.value.indexOf(id)
  if (idx === -1) {
    expandedReplyCommentIds.value.push(id)
  } else {
    expandedReplyCommentIds.value.splice(idx, 1)
  }
}

const getVisibleReplies = (comment) => {
  if (!comment || !Array.isArray(comment.replies)) return []
  if (isRepliesExpanded(comment)) return comment.replies
  if (comment.replies.length <= maxVisibleReplies) return comment.replies
  return comment.replies.slice(0, maxVisibleReplies)
}

const handleSubmitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  if (!currentPostId.value) return
  commentSubmitting.value = true
  try {
    const parentId = replyTarget.value ? replyTarget.value.commentId : null
    await addSquareComment(currentPostId.value, {
      content: commentContent.value,
      parentId
    })
    ElMessage.success(parentId ? '回复成功' : '评论成功')
    commentContent.value = ''
    replyTarget.value = null
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

const startReply = (comment) => {
  if (!comment) return
  replyTarget.value = comment
}

const handleDeletePost = async (post) => {
  try {
    await deleteSquarePost(post.postId)
    ElMessage.success('删除帖子成功')
    posts.value = posts.value.filter(p => p.postId !== post.postId)
  } catch (error) {
    console.error('删除帖子失败:', error)
    ElMessage.error('删除帖子失败')
  }
}

const handleDeleteComment = async (comment) => {
  try {
    await deleteSquareComment(comment.commentId)
    ElMessage.success('删除评论成功')
    comments.value = comments.value.filter(c => c.commentId !== comment.commentId)
    const idx = posts.value.findIndex(p => p.postId === currentPostId.value)
    if (idx !== -1 && posts.value[idx].commentCount) {
      posts.value[idx].commentCount = Math.max((posts.value[idx].commentCount || 1) - 1, 0)
    }
  } catch (error) {
    console.error('删除评论失败:', error)
    ElMessage.error('删除评论失败')
  }
}

onMounted(() => {
  loadPosts(true)
  loadNotifications(true)
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

.square-search-bar {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.square-search-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.square-search-main {
  border-bottom: 1px dashed #f0f0f0;
  padding-bottom: 6px;
}

.square-search-input {
  flex: 1 1 220px;
}

.square-search-tags {
  flex: 1 1 240px;
}

.square-search-main .el-button {
  margin-left: 0;
}

.square-search-main .el-button + .el-button {
  margin-left: 4px;
}

.square-search-filters {
  font-size: 13px;
  color: #606266;
}

.square-filter-label {
  margin-right: 4px;
  white-space: nowrap;
}

.square-filter-select {
  min-width: 120px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.unread-dot {
  min-width: 18px;
  padding: 0 6px;
  border-radius: 9px;
  background-color: #f56c6c;
  color: #fff;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
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

.post-image-wrapper {
  position: relative;
  width: 100%;
  padding-bottom: 100%; /* 正方形 */
  overflow: hidden;
  border-radius: 4px;
}

.post-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
}

.post-image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: 600;
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

.comment-replies {
  margin-top: 6px;
  padding-left: 12px;
  border-left: 2px solid #f0f0f0;
}

.comment-item--reply {
  margin-top: 4px;
  padding: 6px 8px;
  border-radius: 4px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
}

.comment-reply-target {
  margin-left: 4px;
  color: #999;
  font-size: 12px;
}

.empty-comment-tip {
  text-align: center;
  color: #999;
  padding: 20px 0;
}

.comment-input {
  margin-top: 10px;
}

.notify-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}

.notify-item {
  background: #fff;
  padding: 12px 16px;
  margin-bottom: 10px;
  border-radius: 6px;
  border-left: 4px solid transparent;
}

.notify-item--unread {
  border-left-color: #409eff;
  background: #ecf5ff;
}

.notify-message {
  font-size: 14px;
  margin-bottom: 6px;
}

.notify-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}

.notify-type {
  padding: 0 6px;
  border-radius: 10px;
  background: #f0f2f5;
}

.image-preview-dialog .el-dialog__body {
  padding: 0;
}

.image-preview-square {
  position: relative;
  width: 100%;
  padding-bottom: 100%; /* 正方形预览区域 */
  overflow: hidden;
}

.image-preview-square img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>

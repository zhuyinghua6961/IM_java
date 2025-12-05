<template>
  <div class="chat-container">
    <!-- 会话列表 -->
    <div class="conversation-list" :style="{ width: conversationListWidth + 'px' }">
      <div class="list-header">
        <h3>消息</h3>
        <div class="header-actions">
          <el-icon class="search-icon" @click="showSearchDialog = true"><Search /></el-icon>
          <el-badge :value="totalUnread" :hidden="totalUnread === 0" class="badge" />
        </div>
      </div>
      
      <el-scrollbar class="conv-scrollbar">
        <!-- 空状态 -->
        <div v-if="conversations.length === 0" class="empty-state">
          <el-empty description="暂无会话">
            <el-button type="primary" @click="goToContacts">去添加好友</el-button>
          </el-empty>
        </div>
        
        <!-- 会话列表 -->
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: selectedConv?.id === conv.id, pinned: conv.isPinned, hidden: conv.isHidden }"
          @click="selectConversation(conv)"
          @contextmenu.prevent="showContextMenu($event, conv)"
        >
          <el-badge 
            :value="(conv.muted && !conv.hasAtMe) ? '' : conv.unreadCount" 
            :is-dot="conv.muted && !conv.hasAtMe && conv.unreadCount > 0"
            :hidden="conv.unreadCount === 0"
          >
            <el-avatar :size="48" :src="conv.targetAvatar">
              {{ conv.targetName?.charAt(0) }}
            </el-avatar>
          </el-badge>
          <div class="conv-info">
            <div class="conv-top">
              <span class="conv-name">
                <el-icon v-if="conv.isPinned" class="pin-icon"><Top /></el-icon>
                {{ conv.targetName }}
                <el-icon v-if="conv.muted" class="mute-icon" title="消息免打扰"><BellFilled /></el-icon>
              </span>
              <span class="conv-time">{{ formatTime(conv.lastMsgTime) }}</span>
            </div>
            <div class="conv-bottom">
              <span class="last-message">{{ conv.lastMessage }}</span>
            </div>
          </div>
          
          <!-- 悬停操作按钮 -->
          <div class="conv-actions">
            <el-dropdown trigger="click" @command="handleConvAction">
              <el-button text :icon="MoreFilled" class="action-btn" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="{ action: 'pin', conv }" :disabled="conv.isPinned">
                    <el-icon><Top /></el-icon>
                    {{ conv.isPinned ? '已置顶' : '置顶' }}
                  </el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'unpin', conv }" v-if="conv.isPinned">
                    <el-icon><Bottom /></el-icon>
                    取消置顶
                  </el-dropdown-item>
                  <el-dropdown-item 
                    :command="{ action: conv.muted ? 'unmute' : 'mute', conv }"
                  >
                    <el-icon><BellFilled /></el-icon>
                    {{ conv.muted ? '取消免打扰' : '消息免打扰' }}
                  </el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'hide', conv }">
                    <el-icon><Hide /></el-icon>
                    隐藏会话
                  </el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'delete', conv }" divided>
                    <el-icon><Delete /></el-icon>
                    删除会话
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-scrollbar>
    </div>
    
    <!-- 可拖拽分割线 -->
    <div 
      class="resizer" 
      @mousedown="startResize"
      @dblclick="resetWidth"
      title="拖拽调整宽度，双击重置"
    ></div>
    
    <!-- 聊天窗口 -->
    <div class="chat-window" style="flex: 1; min-width: 0;">
      <!-- 未选择会话 -->
      <div v-if="!selectedConv" class="no-chat-selected">
        <el-empty description="选择一个会话开始聊天" />
      </div>
      
      <!-- 已选择会话 -->
      <template v-else>
        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="header-left">
            <el-avatar :size="36" :src="selectedConv.targetAvatar">
              {{ selectedConv.targetName?.charAt(0) }}
            </el-avatar>
            <span class="target-name">{{ selectedConv.targetName }}</span>
          </div>
          <div class="header-right">
            <!-- 群公告按钮（仅群聊显示） -->
            <el-tooltip v-if="selectedConv.chatType === 2 && isGroupAdmin" content="群成员管理" placement="bottom">
              <el-button text @click="openGroupMemberDialog">成员</el-button>
            </el-tooltip>
            <el-tooltip v-if="selectedConv.chatType === 2" content="群公告" placement="bottom">
              <el-button text :icon="Bell" @click="showAnnouncementDialog = true" />
            </el-tooltip>
            <el-button text :icon="MoreFilled" />
          </div>
        </div>
        
        <!-- 消息列表 -->
        <el-scrollbar class="message-list" ref="messageScrollbar">
          <div class="message-content">
            <!-- 日期分隔 -->
            <div class="date-divider">
              <span>今天</span>
            </div>
            
            <!-- 消息项 -->
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-item"
              :class="{ 'is-mine': msg.fromUserId === currentUserId }"
              @mouseenter="hoveredMessageId = msg.id"
              @mouseleave="hoveredMessageId = null"
            >
              <el-avatar :size="40" :src="msg.avatar">
                {{ msg.nickname?.charAt(0) }}
              </el-avatar>
              <div class="message-wrapper">
                <div class="message-info">
                  <span class="message-name">{{ msg.nickname }}</span>
                  <span class="message-time">{{ formatTime(msg.sendTime) }}</span>
                </div>
                <div 
                  :id="'msg-' + msg.id"
                  class="message-bubble" 
                  :class="{ 
                    'recalled': isRecalledMessage(msg), 
                    'sending': isSendingMessage(msg), 
                    'failed': isFailedMessage(msg),
                    'highlight-msg': highlightMessageId === msg.id
                  }">
                  <template v-if="isRecalledMessage(msg)">
                    <span class="recalled-text">{{ getRecalledText(msg) }}</span>
                  </template>
                  <template v-else-if="isFailedMessage(msg)">
                    <span class="failed-text">{{ msg.content }}</span>
                    <el-tooltip :content="msg.failedReason || '发送失败'" placement="top">
                      <span class="failed-indicator">!</span>
                    </el-tooltip>
                  </template>
                  <template v-else-if="isSendingMessage(msg)">
                    <span class="sending-text">{{ msg.content }}</span>
                    <span class="sending-indicator">发送中...</span>
                  </template>
                  <template v-else>
                    <template v-if="msg.msgType === 2">
                      <img
                        class="image-message"
                        :src="msg.url || getMediaUrlFromContent(msg)"
                        @click.stop="previewImage(msg)"
                      />
                    </template>
                    <template v-else-if="msg.msgType === 3">
                      <video
                        class="video-message"
                        :src="msg.url || getMediaUrlFromContent(msg)"
                        controls
                      />
                    </template>
                    <template v-else-if="msg.msgType === 5">
                      <div
                        class="voice-message"
                        :class="{ playing: playingMessageId === msg.id }"
                        @click="playVoice(msg)"
                      >
                        <span class="voice-label">语音</span>
                        <span class="voice-duration">
                          <template v-if="playingMessageId === msg.id && playingCurrentTime > 0">
                            {{ formatVoiceSecond(playingCurrentTime) }} / {{ getVoiceDurationText(msg) }}
                          </template>
                          <template v-else>
                            {{ getVoiceDurationText(msg) }}
                          </template>
                        </span>
                      </div>
                    </template>
                    <template v-else-if="msg.msgType === 4">
                    <a
                      class="file-message"
                      :href="msg.url || getMediaUrlFromContent(msg)"
                      target="_blank"
                      @click.stop
                    >
                      {{ getFileName(msg) }}
                    </a>
                  </template>
                  <template v-else>
                    <template v-if="isImageTextMessage(msg)">
                      <img
                        class="image-message"
                        :src="getUrlFromTextMessage(msg)"
                        @click.stop="previewImage(msg)"
                      />
                    </template>
                    <template v-else-if="isVideoTextMessage(msg)">
                      <video
                        class="video-message"
                        :src="getUrlFromTextMessage(msg)"
                        controls
                      />
                    </template>
                    <template v-else>
                      <span v-html="formatMessageContent(msg.content)"></span>
                    </template>
                  </template>
                  <!-- 悬停操作菜单 -->
                  <div 
                    v-if="hoveredMessageId === msg.id && !isRecalledMessage(msg) && !isSendingMessage(msg)" 
                    class="message-actions"
                  >
                    <el-button 
                      v-if="msg.msgType === 2"
                      text 
                      size="small" 
                      @click="favoriteEmojiFromMessage(msg)"
                      class="action-btn"
                      title="收藏为表情"
                    >
                      收藏表情
                    </el-button>
                    <!-- 撤回按钮（仅自己的消息且5分钟内） -->
                    <el-button 
                      v-if="msg.fromUserId === currentUserId && canRecall(msg)"
                      text 
                      size="small" 
                      @click="recallMessage(msg)"
                      class="action-btn"
                      title="撤回消息"
                    >
                      撤回
                    </el-button>
                    <!-- 删除按钮（所有消息都可以删除） -->
                    <el-button 
                      text 
                      size="small" 
                      @click="deleteMessage(msg)"
                      class="action-btn delete-btn"
                      title="删除消息"
                    >
                      删除
                    </el-button>
                  </div>
                </template>
              </div>
            </div>
          </div>
          
          <!-- 空消息状态 -->
          <div v-if="messages.length === 0" class="empty-messages">
            <el-empty description="暂无消息，开始聊天吧" />
          </div>
        </div>
      </el-scrollbar>
      
      <!-- 输入区域 -->
      <div class="input-area">
        <div class="input-toolbar">
          <el-button text :icon="PictureFilled" title="发送图片或视频" @click="onSelectMedia" />
          <el-button text :icon="Paperclip" title="发送文件" @click="onSelectFile" />
          <el-popover
            placement="top-start"
            trigger="click"
            width="320"
            @show="onEmojiPanelShow"
          >
            <div class="emoji-panel">
              <div class="emoji-panel-header">
                <el-button type="primary" text size="small" @click="onSelectEmojiFile">
                  添加表情
                </el-button>
              </div>
              <div v-if="favoriteEmojis.length > 0" class="emoji-list">
                <div
                  v-for="emoji in favoriteEmojis"
                  :key="emoji.id"
                  class="emoji-item"
                >
                  <img
                    :src="emoji.url"
                    class="emoji-image"
                    @click="sendEmoji(emoji)"
                  />
                  <el-button
                    text
                    size="small"
                    class="emoji-delete-btn"
                    @click.stop="removeEmoji(emoji)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
              <el-empty v-else description="暂无收藏表情" />
              <input
                ref="emojiFileInputRef"
                type="file"
                accept="image/*"
                style="display: none;"
                @change="handleEmojiFileChange"
              />
            </div>
            <template #reference>
              <el-button text :icon="ChatLineRound" title="表情" />
            </template>
          </el-popover>
          <el-button
            text
            :type="isRecording ? 'danger' : 'default'"
            @click="toggleVoiceRecording"
            :title="isRecording ? '点击停止并发送语音' : '点击开始录音，再次点击停止并发送'"
          >
            {{ isRecording ? '停止语音' : '语音' }}
          </el-button>
        </div>
        <div class="input-box" style="position: relative;">
          <el-input
            ref="inputRef"
            v-model="inputMessage"
            type="textarea"
            :rows="4"
            resize="none"
            placeholder="按 Enter 发送，Shift + Enter 换行，输入 @ 可提及成员"
            @keydown.enter="handleKeyDown"
            @input="handleInputChange"
          />
          <!-- @成员选择弹窗 -->
          <div v-if="showAtPanel && selectedConv?.chatType === 2" class="at-panel">
            <div class="at-panel-header">
              <span>选择要@的成员</span>
              <el-button text size="small" @click="showAtPanel = false">关闭</el-button>
            </div>
            <div class="at-panel-search">
              <el-input v-model="atSearchKeyword" placeholder="搜索成员" size="small" clearable />
            </div>
            <div class="at-panel-list">
              <div 
                class="at-item at-all" 
                @click="selectAtMember({ userId: 'all', nickname: '全体成员' })"
              >
                <el-avatar :size="28">全</el-avatar>
                <span>@全体成员</span>
              </div>
              <div 
                v-for="member in filteredAtMembers" 
                :key="member.userId" 
                class="at-item"
                @click="selectAtMember(member)"
              >
                <el-avatar :size="28" :src="member.avatar">
                  {{ (member.groupNickname || member.nickname || member.username)?.charAt(0) }}
                </el-avatar>
                <span>{{ member.groupNickname || member.nickname || member.username }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="input-actions">
          <span class="text-count">{{ inputMessage.length }}/2000</span>
          <el-button type="primary" @click="sendMessage" :disabled="!inputMessage.trim()">
            发送
          </el-button>
        </div>
      </div>
      <input
        ref="mediaInputRef"
        type="file"
        accept="image/*,video/*"
        style="display: none;"
        @change="handleMediaChange"
      />
      <input
        ref="fileInputRef"
        type="file"
        style="display: none;"
        @change="handleFileChange"
      />
    </template>
    </div>
    
    <!-- 消息搜索对话框 -->
    <el-dialog v-model="showSearchDialog" title="搜索消息" width="600px" class="search-dialog">
      <div class="search-header">
        <el-input
          v-model="searchKeyword"
          placeholder="输入关键词搜索消息"
          :prefix-icon="Search"
          clearable
          @keyup.enter="handleSearchMessages"
        />
        <el-button type="primary" @click="handleSearchMessages" :loading="searchLoading">搜索</el-button>
      </div>
      
      <div class="search-results">
        <div v-if="searchResults.length === 0 && searchedOnce" class="empty-search">
          <el-empty description="未找到相关消息" />
        </div>
        <div v-else class="result-list">
          <div
            v-for="item in searchResults"
            :key="item.id"
            class="result-item"
            @click="goToSearchResult(item)"
          >
            <div class="result-header">
              <span class="result-conv-name">{{ getSearchResultConvName(item) }}</span>
              <span class="result-time">{{ formatSearchTime(item.sendTime) }}</span>
            </div>
            <div class="result-content">
              <span v-html="highlightKeyword(item.content, searchKeyword)"></span>
            </div>
          </div>
        </div>
        
        <!-- 分页 -->
        <div v-if="searchTotal > searchSize" class="search-pagination">
          <el-pagination
            v-model:current-page="searchPage"
            :page-size="searchSize"
            :total="searchTotal"
            layout="prev, pager, next"
            @current-change="handleSearchMessages"
          />
        </div>
      </div>
    </el-dialog>
    
    <el-dialog
      v-model="showGroupMemberDialog"
      title="群成员管理"
      width="600px"
      destroy-on-close
    >
      <el-table :data="atMembers" size="small" style="width: 100%">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="成员">
          <template #default="{ row }">
            {{ row.groupNickname || row.nickname || ('用户' + row.userId) }}
            <el-tag v-if="row.role === 2" type="danger" size="small" style="margin-left: 8px;">群主</el-tag>
            <el-tag v-else-if="row.role === 1" type="success" size="small" style="margin-left: 8px;">管理员</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="禁言状态" width="200">
          <template #default="{ row }">
            <span v-if="isMemberMuted(row)">禁言至 {{ formatMuteUntil(row.muteUntil) }}</span>
            <span v-else>未禁言</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button
              text
              size="small"
              @click="handleMuteMember(row, 60)"
              :disabled="!canManageMember(row)"
            >
              禁言1小时
            </el-button>
            <el-button
              text
              size="small"
              @click="handleMuteMember(row, 1440)"
              :disabled="!canManageMember(row)"
            >
              禁言1天
            </el-button>
            <el-button
              text
              size="small"
              @click="handleCustomMuteMember(row)"
              :disabled="!canManageMember(row)"
            >
              自定义
            </el-button>
            <el-button
              text
              size="small"
              type="danger"
              @click="handleUnmuteMember(row)"
              :disabled="!canManageMember(row) || !isMemberMuted(row)"
            >
              解除禁言
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
    
    <!-- 群公告弹窗 -->
    <el-dialog
      v-model="showAnnouncementDialog"
      title="群公告"
      width="500px"
      destroy-on-close
      class="announcement-dialog"
    >
      <div class="announcement-content">
        <!-- 发布公告按钮（仅管理员可见） -->
        <div v-if="isGroupAdmin" class="announcement-actions">
          <el-button type="primary" :icon="Plus" @click="openPublishAnnouncement">发布公告</el-button>
        </div>
        
        <!-- 公告列表 -->
        <div v-loading="announcementLoading" class="announcement-list">
          <div v-if="announcements.length === 0" class="empty-announcement">
            <el-empty description="暂无公告" />
          </div>
          <div v-else>
            <div
              v-for="item in announcements"
              :key="item.id"
              class="announcement-item"
            >
              <div class="announcement-header">
                <div class="announcement-title">
                  <el-tag v-if="item.isTop === 1" type="danger" size="small">置顶</el-tag>
                  <span>{{ item.title }}</span>
                </div>
                <div v-if="isGroupAdmin" class="announcement-ops">
                  <el-button text :icon="Edit" size="small" @click="openEditAnnouncement(item)" />
                  <el-button text :icon="Delete" size="small" type="danger" @click="handleDeleteAnnouncement(item)" />
                </div>
              </div>
              <div class="announcement-body">
                <div class="announcement-text">{{ item.content }}</div>
              </div>
              <div class="announcement-footer">
                <span class="announcement-time">{{ formatAnnouncementTime(item.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
    
    <!-- 编辑/发布公告弹窗 -->
    <el-dialog
      v-model="showEditAnnouncement"
      :title="editingAnnouncement ? '编辑公告' : '发布公告'"
      width="450px"
      destroy-on-close
    >
      <el-form :model="announcementForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="announcementForm.title" placeholder="请输入公告标题" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="announcementForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入公告内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="announcementForm.isTop" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditAnnouncement = false">取消</el-button>
        <el-button type="primary" @click="submitAnnouncement">
          {{ editingAnnouncement ? '保存' : '发布' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteMessage as deleteMessageApi, searchMessages, getMessageContext } from '@/api/message'
import { 
  Search, 
  Plus, 
  MoreFilled, 
  PictureFilled, 
  Paperclip,
  ChatLineRound,
  Top,
  Bottom,
  Delete,
  Hide,
  BellFilled,
  Bell,
  Edit
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import { setGroupMuted, getGroupMuted, muteGroupMember } from '@/api/group'
import { getAnnouncementList, publishAnnouncement, updateAnnouncement, deleteAnnouncement } from '@/api/announcement'
import { setFriendMuted, getFriendMuted } from '@/api/friend'
import { useUserStore } from '@/stores/user'
import wsClient from '@/utils/websocket'
import messageSyncManager from '@/utils/MessageSyncManager'
import { useRouter, useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'

const router = useRouter()
const route = useRoute()
const chatStore = useChatStore()
const userStore = useUserStore()

const selectedConv = ref(null)
const inputMessage = ref('')
const messages = ref([])

// 消息搜索相关
const showSearchDialog = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const searchedOnce = ref(false)
const searchPage = ref(1)
const searchSize = ref(20)
const searchTotal = ref(0)
const highlightMessageId = ref(null) // 高亮显示的消息ID
const messageScrollbar = ref(null)
const hoveredMessageId = ref(null) // 当前悬停的消息ID
const inputRef = ref(null)

// 群公告相关
const showAnnouncementDialog = ref(false)
const announcements = ref([])
const announcementLoading = ref(false)
const showEditAnnouncement = ref(false)
const editingAnnouncement = ref(null)
const announcementForm = ref({
  title: '',
  content: '',
  isTop: false
})

// @成员相关
const showAtPanel = ref(false)
const atSearchKeyword = ref('')
const atMembers = ref([]) // 当前群的成员列表
const atUserMap = ref({}) // 存储被@的用户 {userId: nickname}
const isRecording = ref(false)
const playingMessageId = ref(null)
const playingCurrentTime = ref(0)
const playingTotalDuration = ref(0)
const mediaInputRef = ref(null)
const fileInputRef = ref(null)
const emojiFileInputRef = ref(null)
const favoriteEmojis = ref([])

let mediaRecorder = null
let recordedChunks = []
let recordStartTime = 0
let currentAudio = null
let mediaRecorderMimeType = ''

// 会话列表宽度控制
const conversationListWidth = ref(280)
const isResizing = ref(false)
const minWidth = 200
const maxWidth = 400

const conversations = computed(() => {
  // 对会话进行排序：置顶的在前面，然后按最后消息时间排序
  return chatStore.conversations
    .filter(conv => !conv.isHidden) // 过滤掉隐藏的会话
    .sort((a, b) => {
      // 置顶的会话优先
      if (a.isPinned && !b.isPinned) return -1
      if (!a.isPinned && b.isPinned) return 1
      
      // 都置顶或都不置顶，按最后消息时间排序
      const timeA = new Date(a.lastMsgTime || 0).getTime()
      const timeB = new Date(b.lastMsgTime || 0).getTime()
      return timeB - timeA
    })
})
const currentUserId = computed(() => userStore.userInfo?.userId || null)
const totalUnread = computed(() => {
  // 统计非免打扰会话的未读数 + 免打扰但被@的会话的未读数
  return chatStore.conversations
    .filter(conv => !conv.isHidden && (!conv.muted || conv.hasAtMe))
    .reduce((sum, conv) => sum + (conv.unreadCount || 0), 0)
})

// 过滤@成员列表（排除自己）
const filteredAtMembers = computed(() => {
  const keyword = atSearchKeyword.value.toLowerCase()
  return atMembers.value.filter(member => {
    if (member.userId === currentUserId.value) return false
    if (!keyword) return true
    const name = (member.groupNickname || member.nickname || member.username || '').toLowerCase()
    return name.includes(keyword)
  })
})

// 组件挂载时检查URL参数
onMounted(async () => {
  try {
    console.log('Chat组件开始挂载')
    
    // 先加载会话列表
    try {
      await loadConversationList()
      console.log('会话列表加载完成')
    } catch (error) {
      console.error('加载会话列表失败:', error)
    }
    
    // 处理URL参数
    const { targetId, chatType } = route.query
    if (targetId && chatType) {
      console.log('处理URL参数:', { targetId, chatType })
      try {
        const conversationId = `${chatType}-${targetId}`
        const existingConv = conversations.value.find(conv => conv.id === conversationId)
        
        if (existingConv) {
          selectedConv.value = existingConv
          console.log('使用已存在的会话:', existingConv)
        } else {
          console.log('创建新会话')
          const userInfo = await getUserInfo(Number(targetId), Number(chatType))
          selectedConv.value = {
            id: conversationId,
            targetId: Number(targetId),
            chatType: Number(chatType),
            targetName: userInfo.name,
            targetAvatar: userInfo.avatar,
            unreadCount: 0,
            lastMessage: '',
            lastMsgTime: null
          }
        }
        
        // 加载历史消息
        await loadHistoryMessages()
        console.log('历史消息加载完成')

        // 如果是群聊，会话初始化后加载一次群成员列表，用于权限判断和禁言管理
        if (selectedConv.value && selectedConv.value.chatType === 2) {
          await loadGroupMembersForAt()
        }
      } catch (error) {
        console.error('处理URL参数失败:', error)
      }
    }
    
    // 初始化WebSocket和消息同步
    try {
      if (!wsClient.isConnected()) {
        // 获取token
        const token = localStorage.getItem('token')
        if (token) {
          wsClient.connect(token, (data) => {
            console.log('收到WebSocket消息:', data)
            // 处理接收到的消息
          })
        } else {
          console.warn('未找到token，跳过WebSocket连接')
        }
      }
      setupMessageSyncManager()
      console.log('WebSocket和消息同步初始化完成')
    } catch (error) {
      console.error('WebSocket初始化失败:', error)
    }
    
    // 监听消息更新
    const cleanupMessageListener = watchMessages()
    
    // 清理函数
    onUnmounted(() => {
      try {
        cleanupMessageListener()
        messageSyncManager.destroy()
        // 断开WebSocket连接
        if (wsClient.isConnected()) {
          wsClient.disconnect()
        }
      } catch (error) {
        console.error('清理资源失败:', error)
      }
    })
    
    console.log('Chat组件挂载完成')
  } catch (error) {
    console.error('Chat组件挂载失败:', error)
  }
})

// 加载会话列表
const loadConversationList = async () => {
  try {
    const response = await request.get('/conversation/list')
    
    if (response.data) {
      // 处理会话数据，添加用户信息
      const conversationsWithUserInfo = await Promise.all(
        response.data.map(async (conv) => {
          // 获取对方用户信息
          const userInfo = await getUserInfo(conv.targetId, conv.chatType)
          
          // 获取免打扰状态
          let muted = false
          try {
            if (conv.chatType === 2) {
              // 群聊
              const muteRes = await getGroupMuted(conv.targetId)
              muted = muteRes.data === true
            } else if (conv.chatType === 1) {
              // 单聊
              const muteRes = await getFriendMuted(conv.targetId)
              muted = muteRes.data === true
            }
          } catch (e) {
            // 忽略错误
          }
          
          return {
            id: `${conv.chatType}-${conv.targetId}`,
            targetId: conv.targetId,
            chatType: conv.chatType,
            targetName: userInfo.name,
            targetAvatar: userInfo.avatar,
            unreadCount: conv.unreadCount || 0,
            lastMessage: conv.lastMessage || '',
            lastMsgTime: conv.updateTime,
            muted: muted
          }
        })
      )
      
      // 更新 chatStore
      chatStore.setConversations(conversationsWithUserInfo)
      
      // 恢复@状态
      chatStore.restoreHasAtMe()
      
      console.log('会话列表加载成功:', conversationsWithUserInfo)
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

// 获取用户信息
const getUserInfo = async (targetId, chatType) => {
  try {
    if (chatType === 1) {
      // 单聊 - 获取用户信息
      const response = await request.get(`/user/info/${targetId}`)
      return {
        name: response.data?.nickname || `用户${targetId}`,
        avatar: response.data?.avatar || ''
      }
    } else if (chatType === 2) {
      // 群聊 - 获取群组信息
      const response = await request.get(`/group/detail/${targetId}`)
      return {
        name: response.data?.groupName || `群组${targetId}`,
        avatar: response.data?.avatar || ''
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    return {
      name: chatType === 1 ? `用户${targetId}` : `群组${targetId}`,
      avatar: ''
    }
  }
}

// 加载历史消息
const loadHistoryMessages = async () => {
  if (!selectedConv.value) return
  
  try {
    const response = await request.get('/message/history', {
      params: {
        targetId: selectedConv.value.targetId,
        chatType: selectedConv.value.chatType,
        page: 1,
        size: 50
      }
    })
    
    if (response.data && response.data.list) {
      // 处理消息列表，为群聊消息获取发送者昵称
      const messageList = await Promise.all(
        response.data.list.map(async (msg) => {
          let nickname = ''
          let avatar = ''
          
          if (msg.fromUserId === currentUserId.value) {
            // 当前用户发送的消息
            nickname = userStore.userInfo?.nickname || '我'
            avatar = userStore.userInfo?.avatar || ''
          } else if (selectedConv.value.chatType === 1) {
            // 单聊：使用对方的昵称和头像
            nickname = selectedConv.value.targetName
            avatar = selectedConv.value.targetAvatar
          } else if (selectedConv.value.chatType === 2) {
            // 群聊：获取发送者信息
            try {
              const userInfo = await getUserInfo(msg.fromUserId, 1)
              nickname = userInfo.name
              avatar = userInfo.avatar
            } catch (error) {
              console.error('获取发送者信息失败:', error)
              nickname = `用户${msg.fromUserId}`
              avatar = ''
            }
          }
          
          return {
            // 确保id为字符串，避免JS大整数精度问题
            id: String(msg.id),
            fromUserId: msg.fromUserId,
            content: msg.content,
            msgType: msg.msgType,
            url: msg.url,
            sendTime: new Date(msg.sendTime),
            status: msg.status,
            nickname: nickname,
            avatar: avatar
          }
        })
      )
      
      // 按时间升序排列
      messages.value = messageList.sort((a, b) => new Date(a.sendTime) - new Date(b.sendTime))
      
      // 标记消息已读
      await markMessagesAsRead()
      
      // 加载完成后滚动到底部
      nextTick(() => {
        scrollToBottom()
      })
    }
  } catch (error) {
    console.error('加载历史消息失败:', error)
  }
}

// 标记消息已读
const markMessagesAsRead = async () => {
  if (!selectedConv.value || messages.value.length === 0) return
  
  try {
    // 获取所有未读消息的ID（对方发送给我的消息）
    const unreadMessageIds = messages.value
      .filter(msg => msg.fromUserId !== currentUserId.value)
      .map(msg => msg.id)
    
    if (unreadMessageIds.length > 0) {
      // 调用标记已读接口
      await request.post('/message/read', {
        messageIds: unreadMessageIds
      })
      
      console.log('标记已读成功:', unreadMessageIds)
    }
    
    // 清空会话未读数
    await clearConversationUnread()
    
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

// 清空会话未读数
const clearConversationUnread = async () => {
  if (!selectedConv.value) return
  
  try {
    await request.post('/conversation/clear-unread', {
      targetId: selectedConv.value.targetId,
      chatType: selectedConv.value.chatType
    })
    
    // 更新本地会话的未读数
    if (selectedConv.value) {
      selectedConv.value.unreadCount = 0
    }
    
    // 触发会话列表更新事件
    window.dispatchEvent(new CustomEvent('updateConversation', {
      detail: { 
        conversationId: selectedConv.value.id,
        action: 'clear_unread'
      }
    }))
    
    console.log('清空未读数成功')
  } catch (error) {
    console.error('清空未读数失败:', error)
  }
}

// 标记单个消息已读
const markSingleMessageAsRead = async (messageId) => {
  try {
    await request.post('/message/read', {
      messageIds: [messageId]
    })
    console.log('标记单个消息已读成功:', messageId)
  } catch (error) {
    console.error('标记单个消息已读失败:', error)
  }
}

// 监听消息更新
const watchMessages = () => {
  // 监听新消息事件
  const handleNewMessage = async (event) => {
    const { conversationId, message } = event.detail
    
    // 如果是当前会话的消息，添加到消息列表
    if (selectedConv.value && conversationId === selectedConv.value.id) {
      const exists = messages.value.find(m => m.id === message.id)
      if (!exists) {
        // 获取发送者昵称和头像
        let senderNickname = '未知用户'
        let senderAvatar = ''
        
        if (message.fromUserId === currentUserId.value) {
          senderNickname = userStore.userInfo?.nickname || '我'
          senderAvatar = userStore.userInfo?.avatar || ''
        } else if (selectedConv.value.chatType === 1) {
          // 单聊：使用对方昵称和头像
          senderNickname = selectedConv.value.targetName
          senderAvatar = selectedConv.value.targetAvatar
        } else if (selectedConv.value.chatType === 2) {
          // 群聊：需要获取发送者信息
          try {
            const userInfo = await getUserInfo(message.fromUserId, 1)
            senderNickname = userInfo.name
            senderAvatar = userInfo.avatar
          } catch (error) {
            console.error('获取发送者信息失败:', error)
            senderNickname = `用户${message.fromUserId}`
            senderAvatar = ''
          }
        }
        
        messages.value.push({
          ...message,
          nickname: senderNickname,
          avatar: senderAvatar
        })
        
        // 滚动到底部
        setTimeout(() => {
          if (messageScrollbar.value) {
            messageScrollbar.value.setScrollTop(messageScrollbar.value.wrapRef.scrollHeight)
          }
        }, 100)
        
        // 如果是别人发给我的消息，自动标记已读
        if (message.fromUserId !== currentUserId.value) {
          setTimeout(() => {
            markSingleMessageAsRead(message.id)
          }, 500)
        }
      }
    }
  }
  
  // 添加事件监听
  window.addEventListener('newMessage', handleNewMessage)
  
  // 返回清理函数
  return () => {
    window.removeEventListener('newMessage', handleNewMessage)
  }
}

// 选择会话
const selectConversation = (conv) => {
  selectedConv.value = conv
  loadHistoryMessages()
  
  // 如果是群聊，会话切换时加载群成员列表，用于@选择和权限判断
  if (conv.chatType === 2) {
    loadGroupMembersForAt()
  }
  
  // 清除@状态
  if (conv.hasAtMe) {
    chatStore.clearHasAtMe(conv.id)
  }
  
  // 更新消息同步管理器的当前会话
  messageSyncManager.setCurrentConversation(conv)
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || !selectedConv.value) return
  
  const messageData = {
    chatType: selectedConv.value.chatType,
    msgType: 1, // 1-文本消息
    content: inputMessage.value.trim()
  }
  
  // 根据聊天类型设置接收方
  if (selectedConv.value.chatType === 1) {
    // 单聊
    messageData.toUserId = selectedConv.value.targetId
  } else if (selectedConv.value.chatType === 2) {
    // 群聊
    messageData.groupId = selectedConv.value.targetId
    // 确保群成员列表已加载（用于解析@用户）
    if (atMembers.value.length === 0) {
      await loadGroupMembersForAt()
    }
    // 解析@用户
    const atUserIds = parseAtUsers(messageData.content)
    if (atUserIds) {
      messageData.atUserIds = atUserIds
    }
  }
  
  // 生成临时ID
  const tempId = 'temp-' + Date.now()
  
  // 先添加到本地消息列表（显示发送中状态）
  const tempMessage = {
    id: tempId,
    fromUserId: currentUserId.value,
    content: messageData.content,
    msgType: 1,
    sendTime: new Date(),
    status: 0, // 0-发送中
    nickname: userStore.userInfo?.nickname || '我'
  }
  
  messages.value.push(tempMessage)
  
  // 清空输入框和@记录
  inputMessage.value = ''
  atUserMap.value = {}
  
  // 检查WebSocket连接状态
  console.log('WebSocket连接状态:', wsClient.isConnected())
  console.log('准备发送消息:', messageData)
  
  // 发送WebSocket消息，并处理ACK
  console.log('🔵 开始发送消息，临时ID:', tempId)
  wsClient.sendMessage(messageData, (error, ackData) => {
    console.log('🔵 收到ACK回调', { error, ackData, tempId })
    
    if (error) {
      console.error('❌ 消息发送失败:', error)
      // 标记消息为发送失败
      const msgIndex = messages.value.findIndex(m => m.id === tempId)
      if (msgIndex !== -1) {
        messages.value[msgIndex].status = -1 // -1-发送失败
        // 如果后端返回了真实的消息ID，使用它替换临时ID
        if (ackData && ackData.messageId) {
          messages.value[msgIndex].id = String(ackData.messageId)
          console.log('🔵 失败消息ID已更新:', ackData.messageId)
        }
        // 如果是被拉黑，添加特殊的失败原因
        if (error.message === 'BLOCKED') {
          messages.value[msgIndex].failedReason = '对方已将你拉黑，无法发送消息'
        } else if (error.message && error.message.includes('禁言')) {
          // 被禁言的情况，直接展示后端返回的提示文案
          messages.value[msgIndex].failedReason = error.message
        } else {
          messages.value[msgIndex].failedReason = '发送失败，请检查网络'
        }
      }
      return
    }
    
    // 收到ACK，用真实ID替换临时ID
    console.log('🔍 查找临时消息:', tempId, '当前消息列表:', messages.value.map(m => ({ id: m.id, content: m.content })))
    const msgIndex = messages.value.findIndex(m => m.id === tempId)
    console.log('🔍 找到索引:', msgIndex)
    
    if (msgIndex !== -1) {
      const oldId = messages.value[msgIndex].id
      // ACK中的messageId也转为字符串
      messages.value[msgIndex].id = String(ackData.messageId)
      messages.value[msgIndex].status = 1 // 1-发送成功
      console.log(`✅ 消息ID已更新: ${oldId} -> ${ackData.messageId}`)
      console.log('✅ 更新后的消息:', messages.value[msgIndex])
    } else {
      console.error('❌ 未找到临时消息:', tempId)
    }
  })
  
  // 滚动到底部
  scrollToBottom()
  
  // WebSocket发送是异步的，消息状态会通过WebSocket回调更新
  // 如果发送失败，临时消息会保持"发送中"状态，用户可以重试
}

// 语音录制开关（目前只支持单聊）
const toggleVoiceRecording = async () => {
  if (!selectedConv.value) {
    ElMessage.warning('请先选择一个会话')
    return
  }
  if (!isRecording.value) {
    await startRecording()
  } else {
    await stopRecording()
  }
}

const startRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    recordedChunks = []
    const options = {}
    // 优先使用 Chrome 支持较好的 audio/webm;codecs=opus
    if (window.MediaRecorder && MediaRecorder.isTypeSupported && MediaRecorder.isTypeSupported('audio/webm;codecs=opus')) {
      options.mimeType = 'audio/webm;codecs=opus'
      mediaRecorderMimeType = options.mimeType
    } else {
      mediaRecorderMimeType = ''
    }

    mediaRecorder = new MediaRecorder(stream, options)

    mediaRecorder.ondataavailable = (event) => {
      if (event.data && event.data.size > 0) {
        recordedChunks.push(event.data)
      }
    }

    mediaRecorder.onstop = async () => {
      const blobType = mediaRecorderMimeType || 'audio/webm'
      const blob = new Blob(recordedChunks, { type: blobType })
      const duration = (Date.now() - recordStartTime) / 1000
      isRecording.value = false

      // 释放麦克风
      if (mediaRecorder && mediaRecorder.stream) {
        mediaRecorder.stream.getTracks().forEach(track => track.stop())
      }

      try {
        await uploadAndSendVoice(blob, duration)
      } catch (error) {
        console.error('发送语音失败:', error)
        ElMessage.error('发送语音失败')
      }
    }

    mediaRecorder.start()
    recordStartTime = Date.now()
    isRecording.value = true
  } catch (error) {
    console.error('无法开始录音:', error)
    ElMessage.error('无法访问麦克风，请检查浏览器权限')
  }
}

const stopRecording = async () => {
  if (mediaRecorder && isRecording.value) {
    mediaRecorder.stop()
  }
}

const uploadAndSendVoice = async (blob, duration) => {
  if (!blob || blob.size === 0) {
    ElMessage.warning('录音太短，未发送')
    return
  }

  const formData = new FormData()
  formData.append('file', blob, 'voice-message.webm')

  const res = await request.post('/files/upload/audio', formData)
  const { url, size } = res.data || {}

  if (!url) {
    throw new Error('上传语音失败：未返回URL')
  }

  await sendVoiceMessage(url, duration, size)
}

const onSelectMedia = () => {
  if (!selectedConv.value) {
    ElMessage.warning('请先选择一个会话')
    return
  }
  if (mediaInputRef.value) {
    mediaInputRef.value.click()
  }
}

const handleMediaChange = async (event) => {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (file.type.startsWith('image/')) {
    try {
      await uploadAndSendImage(file)
    } catch (error) {
      console.error('发送图片失败:', error)
      ElMessage.error('发送图片失败')
    }
    return
  }
  if (file.type.startsWith('video/')) {
    try {
      await uploadAndSendVideo(file)
    } catch (error) {
      console.error('发送视频失败:', error)
      ElMessage.error('发送视频失败')
    }
    return
  }
  ElMessage.warning('只支持发送图片或视频文件')
}

const uploadAndSendImage = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  const res = await request.post('/files/upload/image', formData)
  const { url, size, fileName } = res.data || {}

  if (!url) {
    throw new Error('上传图片失败：未返回URL')
  }

  await sendMediaMessage('image', url, size, fileName || file.name)
}

const uploadAndSendVideo = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  const res = await request.post('/files/upload/video', formData)
  const { url, size, fileName } = res.data || {}

  if (!url) {
    throw new Error('上传视频失败：未返回URL')
  }

  await sendMediaMessage('video', url, size, fileName || file.name)
}

const onEmojiPanelShow = async () => {
  try {
    const res = await request.get('/emoji/list')
    let list = Array.isArray(res.data) ? res.data : []
    // 按添加时间倒序排列（id 越大越靠前）
    list = list.slice().sort((a, b) => {
      const idA = Number(a && a.id ? a.id : 0)
      const idB = Number(b && b.id ? b.id : 0)
      return idB - idA
    })
    favoriteEmojis.value = list
  } catch (error) {
    console.error('加载收藏表情失败:', error)
    ElMessage.error('加载收藏表情失败')
  }
}

const onSelectEmojiFile = () => {
  if (!emojiFileInputRef.value) return
  emojiFileInputRef.value.click()
}

const handleEmojiFileChange = async (event) => {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  try {
    await uploadAndAddEmoji(file)
  } catch (error) {
    console.error('添加表情失败:', error)
    ElMessage.error('添加表情失败')
  }
}

const uploadAndAddEmoji = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  const res = await request.post('/files/upload/image', formData)
  const { url, size, fileName } = res.data || {}

  if (!url) {
    throw new Error('上传表情失败：未返回URL')
  }

  const emojiRes = await request.post('/emoji', {
    url,
    fileName: fileName || file.name,
    size
  })

  const emoji = emojiRes.data
  if (emoji) {
    const exists = favoriteEmojis.value.find(item => item.id === emoji.id)
    if (!exists) {
      favoriteEmojis.value.unshift(emoji)
    }
  }
  ElMessage.success('已添加到收藏表情')
}

const sendEmoji = async (emoji) => {
  if (!selectedConv.value) {
    ElMessage.warning('请先选择一个会话')
    return
  }
  await sendMediaMessage('image', emoji.url, emoji.size, emoji.fileName)
}

const removeEmoji = async (emoji) => {
  try {
    await request.delete(`/emoji/${emoji.id}`)
    const index = favoriteEmojis.value.findIndex(item => item.id === emoji.id)
    if (index !== -1) {
      favoriteEmojis.value.splice(index, 1)
    }
    ElMessage.success('已删除表情')
  } catch (error) {
    console.error('删除表情失败:', error)
    ElMessage.error('删除表情失败')
  }
}

const favoriteEmojiFromMessage = async (message) => {
  if (!message || message.msgType !== 2) return
  const url = message.url || getMediaUrlFromContent(message)
  if (!url) {
    ElMessage.error('找不到图片地址')
    return
  }

  let fileName = '表情'
  let size = undefined
  try {
    if (message.content) {
      const obj = typeof message.content === 'string' ? JSON.parse(message.content) : message.content
      if (obj) {
        if (obj.fileName) fileName = obj.fileName
        if (obj.size) size = obj.size
      }
    }
  } catch (error) {
    console.warn('解析图片消息内容失败:', error)
  }

  try {
    const res = await request.post('/emoji', {
      url,
      fileName,
      size
    })
    const emoji = res.data
    if (emoji) {
      const exists = favoriteEmojis.value.find(item => item.id === emoji.id)
      if (!exists) {
        favoriteEmojis.value.unshift(emoji)
      }
    }
    ElMessage.success('已收藏为表情')
  } catch (error) {
    console.error('收藏表情失败:', error)
    ElMessage.error('收藏表情失败')
  }
}

const onSelectFile = () => {
  if (!selectedConv.value) {
    ElMessage.warning('请先选择一个会话')
    return
  }
  if (fileInputRef.value) {
    fileInputRef.value.click()
  }
}

const handleFileChange = async (event) => {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  try {
    await uploadAndSendFile(file)
  } catch (error) {
    console.error('发送文件失败:', error)
    ElMessage.error('发送文件失败')
  }
}

const uploadAndSendFile = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  const res = await request.post('/files/upload/file', formData)
  const { url, size, fileName } = res.data || {}

  if (!url) {
    throw new Error('上传文件失败：未返回URL')
  }

  await sendMediaMessage('file', url, size, fileName || file.name)
}

const sendMediaMessage = async (type, url, size, fileName) => {
  if (!selectedConv.value) return

  let msgType
  if (type === 'image') {
    msgType = 2
  } else if (type === 'video') {
    msgType = 3
  } else if (type === 'file') {
    msgType = 4
  } else {
    msgType = 1
  }

  const meta = {
    url,
    size,
    fileName,
    type
  }
  const content = JSON.stringify(meta)

  const messageData = {
    chatType: selectedConv.value.chatType,
    msgType,
    content,
    url
  }

  if (selectedConv.value.chatType === 1) {
    messageData.toUserId = selectedConv.value.targetId
  } else if (selectedConv.value.chatType === 2) {
    messageData.groupId = selectedConv.value.targetId
  }

  const tempId = 'temp-' + Date.now()

  const tempMessage = {
    id: tempId,
    fromUserId: currentUserId.value,
    content,
    url,
    msgType,
    sendTime: new Date(),
    status: 0,
    nickname: userStore.userInfo?.nickname || '我'
  }

  messages.value.push(tempMessage)
  scrollToBottom()

  console.log('🔵 开始发送媒体消息，临时ID:', tempId, '类型:', type)
  wsClient.sendMessage(messageData, (error, ackData) => {
    console.log('🔵 收到媒体ACK回调', { error, ackData, tempId })

    if (error) {
      console.error('❌ 媒体消息发送失败:', error)
      const msgIndex = messages.value.findIndex(m => m.id === tempId)
      if (msgIndex !== -1) {
        messages.value[msgIndex].status = -1
      }
      return
    }

    const msgIndex = messages.value.findIndex(m => m.id === tempId)
    if (msgIndex !== -1) {
      const oldId = messages.value[msgIndex].id
      messages.value[msgIndex].id = String(ackData.messageId)
      messages.value[msgIndex].status = 1
      console.log(`✅ 媒体消息ID已更新: ${oldId} -> ${ackData.messageId}`)
    } else {
      console.error('❌ 未找到临时媒体消息:', tempId)
    }
  })
}

const sendVoiceMessage = async (url, duration, size) => {
  if (!selectedConv.value) return

  const meta = {
    url,
    duration,
    size
  }
  const content = JSON.stringify(meta)

  const messageData = {
    chatType: selectedConv.value.chatType,
    msgType: 5, // 5-语音消息
    content,
    url
  }

  if (selectedConv.value.chatType === 1) {
    messageData.toUserId = selectedConv.value.targetId
  } else if (selectedConv.value.chatType === 2) {
    messageData.groupId = selectedConv.value.targetId
  }

  const tempId = 'temp-' + Date.now()

  const tempMessage = {
    id: tempId,
    fromUserId: currentUserId.value,
    content,
    url,
    msgType: 5,
    sendTime: new Date(),
    status: 0,
    nickname: userStore.userInfo?.nickname || '我'
  }

  messages.value.push(tempMessage)
  scrollToBottom()

  console.log('🔵 开始发送语音消息，临时ID:', tempId)
  wsClient.sendMessage(messageData, (error, ackData) => {
    console.log('🔵 收到语音ACK回调', { error, ackData, tempId })

    if (error) {
      console.error('❌ 语音消息发送失败:', error)
      const msgIndex = messages.value.findIndex(m => m.id === tempId)
      if (msgIndex !== -1) {
        messages.value[msgIndex].status = -1
      }
      return
    }

    const msgIndex = messages.value.findIndex(m => m.id === tempId)
    if (msgIndex !== -1) {
      const oldId = messages.value[msgIndex].id
      messages.value[msgIndex].id = String(ackData.messageId)
      messages.value[msgIndex].status = 1
      console.log(`✅ 语音消息ID已更新: ${oldId} -> ${ackData.messageId}`)
    } else {
      console.error('❌ 未找到临时语音消息:', tempId)
    }
  })
}

const getVoiceMeta = (message) => {
  if (!message || message.msgType !== 5) return {}
  try {
    if (typeof message.content === 'string') {
      return JSON.parse(message.content) || {}
    }
    return message.content || {}
  } catch (error) {
    console.warn('解析语音消息内容失败:', error)
    return {}
  }
}

const extractFirstUrlFromText = (content) => {
  if (!content) return ''
  const text = String(content).trim()
  const match = text.match(/https?:\/\/\S+/i)
  if (!match) return ''
  // 去掉末尾常见的标点符号
  return match[0].replace(/[)、。！？）\]]+$/u, '')
}

const getMediaUrlFromContent = (message) => {
  if (!message || !message.content) return ''
  try {
    if (typeof message.content === 'string') {
      const obj = JSON.parse(message.content)
      return obj && obj.url ? obj.url : ''
    }
    if (message.content && message.content.url) {
      return message.content.url
    }
  } catch (error) {
    console.warn('解析媒体消息内容失败:', error)
  }

  if (typeof message.content === 'string') {
    return extractFirstUrlFromText(message.content)
  }
  return ''
}

const isImageUrl = (url) => {
  if (!url) return false
  const clean = url.split('?')[0].toLowerCase()
  return /\.(png|jpe?g|gif|webp|bmp|svg)$/.test(clean)
}

const isVideoUrl = (url) => {
  if (!url) return false
  const clean = url.split('?')[0].toLowerCase()
  return /\.(mp4|webm|ogg|mov|m4v)$/.test(clean)
}

const isImageTextMessage = (message) => {
  if (!message || message.msgType !== 1) return false
  const url = extractFirstUrlFromText(message.content)
  return isImageUrl(url)
}

const isVideoTextMessage = (message) => {
  if (!message || message.msgType !== 1) return false
  const url = extractFirstUrlFromText(message.content)
  return isVideoUrl(url)
}

const getUrlFromTextMessage = (message) => extractFirstUrlFromText(message?.content || '')

const getFileName = (message) => {
  if (!message) return '文件'
  try {
    if (typeof message.content === 'string' && message.content) {
      const obj = JSON.parse(message.content)
      if (obj && obj.fileName) {
        return obj.fileName
      }
    } else if (message.content && message.content.fileName) {
      return message.content.fileName
    }
  } catch (error) {
    console.warn('解析文件消息内容失败:', error)
  }
  return '文件'
}

const getVoiceDurationText = (message) => {
  const meta = getVoiceMeta(message)
  if (meta && meta.duration) {
    const sec = Math.max(1, Math.round(meta.duration))
    return `${sec}″`
  }
  return ''
}

const formatVoiceSecond = (sec) => {
  if (!sec || sec <= 0) return '0″'
  const s = Math.floor(sec)
  return `${s}″`
}

const previewImage = (message) => {
  const url = message.url || getMediaUrlFromContent(message)
  if (!url) {
    ElMessage.error('找不到图片地址')
    return
  }
  window.open(url, '_blank')
}

const playVoice = (message) => {
  if (!message || message.msgType !== 5) return

  let url = message.url
  if (!url) {
    const meta = getVoiceMeta(message)
    url = meta.url
  }

  if (!url) {
    ElMessage.error('找不到语音地址')
    return
  }

  // 再次点击当前播放的语音，认为是暂停
  if (playingMessageId.value === message.id && currentAudio) {
    currentAudio.pause()
    currentAudio = null
    playingMessageId.value = null
    playingCurrentTime.value = 0
    return
  }

  if (currentAudio) {
    currentAudio.pause()
    currentAudio = null
  }

  const audio = new Audio()

  // 检测浏览器是否支持当前语音格式（主要是 audio/webm / opus）
  const canPlayWebm = audio.canPlayType('audio/webm') || audio.canPlayType('audio/webm; codecs=opus')
  if (!canPlayWebm) {
    ElMessage.error('当前浏览器不支持语音格式，请使用最新版 Chrome 再试')
    return
  }

  const meta = getVoiceMeta(message)
  if (meta && meta.duration) {
    playingTotalDuration.value = meta.duration
  } else {
    playingTotalDuration.value = 0
  }
  playingCurrentTime.value = 0

  audio.src = url
  currentAudio = audio
  playingMessageId.value = message.id

  audio.play().catch(error => {
    console.error('播放语音失败:', error)
    ElMessage.error('播放语音失败')
    playingMessageId.value = null
    currentAudio = null
    playingCurrentTime.value = 0
  })

  audio.ontimeupdate = () => {
    playingCurrentTime.value = audio.currentTime
  }

  audio.onended = () => {
    playingMessageId.value = null
    currentAudio = null
    playingCurrentTime.value = 0
  }
}

// 处理输入变化，检测@符号
const handleInputChange = (value) => {
  // 只在群聊中处理@
  if (selectedConv.value?.chatType !== 2) return
  
  // 检测是否刚输入了@
  if (value.endsWith('@')) {
    showAtPanel.value = true
    atSearchKeyword.value = ''
    loadGroupMembersForAt()
  }
}

// 加载群成员用于@选择
const loadGroupMembersForAt = async () => {
  if (!selectedConv.value || selectedConv.value.chatType !== 2) return
  
  try {
    const res = await request.get(`/group/${selectedConv.value.targetId}/members`)
    atMembers.value = res.data || []
  } catch (error) {
    console.error('加载群成员失败:', error)
  }
}

// 选择@的成员
const selectAtMember = (member) => {
  const nickname = member.groupNickname || member.nickname || member.username || ''
  const userId = member.userId
  
  // 删除最后一个@符号，插入@昵称
  if (inputMessage.value.endsWith('@')) {
    inputMessage.value = inputMessage.value.slice(0, -1) + `@${nickname} `
  } else {
    inputMessage.value += `@${nickname} `
  }
  
  // 记录被@的用户
  atUserMap.value[userId] = nickname
  
  showAtPanel.value = false
  atSearchKeyword.value = ''
  
  // 聚焦输入框
  nextTick(() => {
    inputRef.value?.focus()
  })
}

// 解析消息中的@用户，返回atUserIds
const parseAtUsers = (content) => {
  const atUserIds = new Set()
  
  // 检查是否有@全体成员
  if (content.includes('@全体成员')) {
    return 'all'
  }
  
  // 1. 先检查已记录的@用户（通过选择面板）
  for (const [userId, nickname] of Object.entries(atUserMap.value)) {
    if (content.includes(`@${nickname}`)) {
      if (userId !== 'all') {
        atUserIds.add(userId)
      }
    }
  }
  
  // 2. 再遍历群成员列表匹配（处理直接输入@的情况）
  for (const member of atMembers.value) {
    const names = [
      member.groupNickname,
      member.nickname,
      member.username
    ].filter(Boolean)
    
    for (const name of names) {
      if (content.includes(`@${name}`)) {
        atUserIds.add(String(member.userId))
        break
      }
    }
  }
  
  return atUserIds.size > 0 ? Array.from(atUserIds).join(',') : null
}

// 格式化消息内容，高亮@内容
const formatMessageContent = (content) => {
  if (!content) return ''
  
  // 转义HTML特殊字符
  let escaped = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  
  // 高亮@内容（@xxx 格式）
  escaped = escaped.replace(/@([^\s@]+)/g, '<span class="at-highlight">@$1</span>')
  
  return escaped
}

// 处理键盘事件
const handleKeyDown = (e) => {
  // ESC关闭@面板
  if (e.key === 'Escape' && showAtPanel.value) {
    showAtPanel.value = false
    return
  }
  
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 跳转到通讯录
const goToContacts = () => {
  router.push('/contacts')
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 开始拖拽调整宽度
const startResize = (e) => {
  isResizing.value = true
  const startX = e.clientX
  const startWidth = conversationListWidth.value
  
  const handleMouseMove = (e) => {
    if (!isResizing.value) return
    
    const deltaX = e.clientX - startX
    const newWidth = Math.max(minWidth, Math.min(maxWidth, startWidth + deltaX))
    conversationListWidth.value = newWidth
  }
  
  const handleMouseUp = () => {
    isResizing.value = false
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }
  
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

// 重置宽度
const resetWidth = () => {
  conversationListWidth.value = 280
}

// 显示右键菜单
const showContextMenu = (event, conv) => {
  // 阻止默认右键菜单
  event.preventDefault()
  // 这里可以添加自定义右键菜单逻辑
}

// 处理会话操作
const handleConvAction = async ({ action, conv }) => {
  try {
    switch (action) {
      case 'pin':
        await pinConversation(conv.id)
        conv.isPinned = true
        ElMessage.success('会话已置顶')
        break
      case 'unpin':
        await unpinConversation(conv.id)
        conv.isPinned = false
        ElMessage.success('已取消置顶')
        break
      case 'hide':
        await hideConversation(conv.id)
        conv.isHidden = true
        ElMessage.success('会话已隐藏')
        // 从列表中移除隐藏的会话
        const index = conversations.value.findIndex(c => c.id === conv.id)
        if (index > -1) {
          conversations.value.splice(index, 1)
        }
        break
      case 'mute':
        if (conv.chatType === 2) {
          await setGroupMuted(conv.targetId, true)
        } else if (conv.chatType === 1) {
          await setFriendMuted(conv.targetId, true)
        }
        conv.muted = true
        ElMessage.success('已开启免打扰')
        break
      case 'unmute':
        if (conv.chatType === 2) {
          await setGroupMuted(conv.targetId, false)
        } else if (conv.chatType === 1) {
          await setFriendMuted(conv.targetId, false)
        }
        conv.muted = false
        ElMessage.success('已取消免打扰')
        break
      case 'delete':
        // 根据聊天类型显示不同的提示
        const deleteMessage = conv.chatType === 1 
          ? '确定要删除这个会话吗？\n⚠️ 删除后你将看不到所有聊天记录，但对方不受影响。'
          : '确定要删除这个会话吗？\n删除后会话和聊天记录将从你的列表中消失。'
        
        await ElMessageBox.confirm(deleteMessage, '确认删除', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning',
          dangerouslyUseHTMLString: false
        })
        await deleteConversation(conv.id)
        // 从列表中移除删除的会话
        const delIndex = conversations.value.findIndex(c => c.id === conv.id)
        if (delIndex > -1) {
          conversations.value.splice(delIndex, 1)
        }
        ElMessage.success('会话已删除')
        break
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

// API 调用方法
const pinConversation = async (conversationId) => {
  await request.post('/conversation/pin', { conversationId })
}

const unpinConversation = async (conversationId) => {
  await request.post('/conversation/unpin', { conversationId })
}

const hideConversation = async (conversationId) => {
  await request.post('/conversation/hide', { conversationId })
}

const deleteConversation = async (conversationId) => {
  await request.delete(`/conversation/${conversationId}`)
}

// 检查是否为发送中的消息
const isSendingMessage = (message) => {
  return message.status === 0 && String(message.id).startsWith('temp-')
}

// 检查是否为发送失败的消息
const isFailedMessage = (message) => {
  return message.status === -1
}

// 检查是否为已撤回的消息
const isRecalledMessage = (message) => {
  return message.status === 0 && !String(message.id).startsWith('temp-')
}

// 撤回消息气泡文案
// 单聊：自己 -> “你 撤回了一条消息”，对方 -> “对方 撤回了一条消息”
// 群聊：自己 -> “你 撤回了一条消息”，他人 -> “{昵称} 撤回了一条消息”
const getRecalledText = (message) => {
  // 自己撤回
  if (message.fromUserId === currentUserId.value) {
    return '你 撤回了一条消息'
  }

  // 对方撤回 - 单聊：固定显示“对方”
  if (selectedConv.value && selectedConv.value.chatType === 1) {
    return '对方 撤回了一条消息'
  }

  // 群聊：显示昵称
  const name = message.nickname || '对方'
  return `${name} 撤回了一条消息`
}

// 检查消息是否可以撤回（5分钟内）
const canRecall = (message) => {
  // 检查消息状态：必须是正常状态(1)，且不是临时消息
  if (message.status !== 1) return false
  
  // 检查是否有真实的消息ID（排除临时消息）
  if (!message.id || String(message.id).startsWith('temp-')) return false
  
  // 检查发送时间是否有效
  if (!message.sendTime) return false
  
  const now = new Date()
  const sendTime = new Date(message.sendTime)
  
  // 检查时间是否有效
  if (isNaN(sendTime.getTime())) return false
  
  const diffMinutes = (now - sendTime) / (1000 * 60)
  
  return diffMinutes <= 5 // 5分钟内可以撤回
}

// 撤回消息
const recallMessage = async (message) => {
  try {
    await ElMessageBox.confirm('确定要撤回这条消息吗？', '撤回消息', {
      confirmButtonText: '撤回',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.post('/message/recall', { messageId: message.id })
    
    // 更新本地消息状态
    message.status = 0
    message.recallTime = new Date()
    
    ElMessage.success('消息已撤回')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('撤回消息失败:', error)
      ElMessage.error('撤回消息失败')
    }
  }
}

// 删除消息
const deleteMessage = async (message) => {
  try {
    await ElMessageBox.confirm('删除后仅自己不可见，对方仍然可以看到。确定要删除这条消息吗？', '删除消息', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteMessageApi(message.id)
    
    // 从本地消息列表中移除
    const index = messages.value.findIndex(m => m.id === message.id)
    if (index !== -1) {
      messages.value.splice(index, 1)
    }
    
    ElMessage.success('消息已删除')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除消息失败:', error)
      ElMessage.error('删除消息失败')
    }
  }
}

// 设置消息同步管理器
const setupMessageSyncManager = () => {
  // 设置当前会话
  if (selectedConv.value) {
    messageSyncManager.setCurrentConversation(selectedConv.value)
  }
  
  // 设置消息更新回调
  messageSyncManager.setMessageUpdateCallback(handleMessageUpdate)
  
  // 设置会话更新回调
  messageSyncManager.setConversationUpdateCallback(loadConversationList)
  
  // 监听WebSocket连接状态
  wsClient.onConnect(() => {
    messageSyncManager.onWebSocketConnected()
  })
  
  wsClient.onDisconnect(() => {
    messageSyncManager.onWebSocketDisconnected()
  })
  
  // 监听WebSocket消息
  wsClient.onMessage((data) => {
    if (data.type && ['NEW_MESSAGE', 'MESSAGE_RECALLED', 'MESSAGES_READ', 'CONVERSATION_UPDATED'].includes(data.type)) {
      messageSyncManager.handleWebSocketMessage(data)
    }
  })
}

// 处理消息更新
const handleMessageUpdate = (type, data) => {
  switch (type) {
    case 'NEW_MESSAGE':
      handleNewMessageUpdate(data)
      break
    case 'MESSAGE_RECALLED':
      handleMessageRecalledUpdate(data)
      break
    case 'MESSAGES_READ':
      handleMessagesReadUpdate(data)
      break
    case 'SYNC_MESSAGES':
      handleSyncMessages(data)
      break
  }
}

// 处理新消息更新
const handleNewMessageUpdate = (data) => {
  // 如果是当前会话的消息，添加到消息列表
  if (selectedConv.value && data.conversationId === selectedConv.value.id) {
    const newMessage = {
      // 通知中的messageId也转为字符串
      id: String(data.messageId),
      fromUserId: data.fromUserId,
      content: data.content,
      msgType: data.msgType,
      url: data.url,
      sendTime: new Date(),
      status: 1,
      nickname: data.fromUserId === currentUserId.value ? 
        (userStore.userInfo?.nickname || '我') : 
        selectedConv.value.targetName
    }
    
    messages.value.push(newMessage)
    scrollToBottom()
  }
}

// 处理消息撤回更新
const handleMessageRecalledUpdate = (data) => {
  const targetId = String(data.messageId)
  const message = messages.value.find(m => String(m.id) === targetId)
  if (message) {
    message.status = 0
    message.recallTime = data.recallTime || new Date()
  }
}

// 处理消息已读更新
const handleMessagesReadUpdate = (data) => {
  // 可以在这里更新消息的已读状态显示
  console.log('消息已读更新:', data)
}

// 处理同步消息
const handleSyncMessages = (data) => {
  if (selectedConv.value && data.conversationId === selectedConv.value.id) {
    loadHistoryMessages()
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageScrollbar.value) {
      const scrollContainer = messageScrollbar.value.wrapRef
      if (scrollContainer) {
        scrollContainer.scrollTop = scrollContainer.scrollHeight
      }
    }
  })
}

// 搜索消息
const handleSearchMessages = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  
  searchLoading.value = true
  try {
    const res = await searchMessages(searchKeyword.value.trim(), null, null, searchPage.value, searchSize.value)
    searchResults.value = res.data.list || []
    searchTotal.value = res.data.total || 0
    searchedOnce.value = true
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    searchLoading.value = false
  }
}

// 高亮搜索关键词
const highlightKeyword = (content, keyword) => {
  if (!content || !keyword) return content
  const regex = new RegExp(`(${keyword})`, 'gi')
  return content.replace(regex, '<span class="highlight">$1</span>')
}

// 格式化搜索结果时间
const formatSearchTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 24 * 60 * 60 * 1000) {
    // 24小时内
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (diff < 7 * 24 * 60 * 60 * 1000) {
    // 一周内
    const days = ['日', '一', '二', '三', '四', '五', '六']
    return `周${days[date.getDay()]} ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
  } else {
    // 更早
    return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
  }
}

// 获取搜索结果的会话名称
const getSearchResultConvName = (item) => {
  const targetId = item.chatType === 1 
    ? (item.fromUserId === currentUserId.value ? item.toId : item.fromUserId)
    : item.toId
  const conversationId = `${item.chatType}-${targetId}`
  
  // 从会话列表中查找
  const conv = conversations.value.find(c => c.id === conversationId)
  if (conv) {
    return conv.targetName
  }
  
  // 未找到时显示类型
  return item.chatType === 1 ? '单聊' : '群聊'
}

// 跳转到搜索结果对应的会话并定位到消息
const goToSearchResult = async (item) => {
  showSearchDialog.value = false
  
  try {
    // 调用后端API获取消息上下文（会自动加载到Redis缓存）
    const res = await getMessageContext(item.id, 100)
    const { list, targetId, chatType } = res.data
    
    const conversationId = `${chatType}-${targetId}`
    
    // 查找或创建会话
    let conv = conversations.value.find(c => c.id === conversationId)
    if (!conv) {
      // 如果会话不存在，可能需要创建
      ElMessage.info('该会话不在当前列表中')
      return
    }
    
    // 切换会话
    selectedConv.value = conv
    
    // 处理消息列表，添加用户信息
    const messageList = await Promise.all(
      list.map(async (msg) => {
        let nickname = ''
        let avatar = ''
        
        if (msg.fromUserId === currentUserId.value) {
          nickname = userStore.userInfo?.nickname || '我'
          avatar = userStore.userInfo?.avatar || ''
        } else if (chatType === 1) {
          nickname = conv.targetName
          avatar = conv.targetAvatar
        } else {
          const userInfo = await getUserInfo(msg.fromUserId, 1)
          nickname = userInfo.name
          avatar = userInfo.avatar
        }
        
        return { ...msg, nickname, avatar }
      })
    )
    
    // 更新消息列表
    messages.value = messageList
    
    // 清除@状态
    if (conv.hasAtMe) {
      chatStore.clearHasAtMe(conv.id)
    }
    
    // 滚动到目标消息
    setTimeout(() => {
      scrollToMessage(item.id)
    }, 300)
  } catch (error) {
    console.error('跳转失败:', error)
    ElMessage.error('跳转失败')
  }
}

// 滚动到指定消息并高亮
const scrollToMessage = (messageId) => {
  highlightMessageId.value = messageId
  
  nextTick(() => {
    const element = document.getElementById(`msg-${messageId}`)
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'center' })
      
      // 3秒后取消高亮
      setTimeout(() => {
        highlightMessageId.value = null
      }, 3000)
    } else {
      // 消息未找到，取消高亮
      highlightMessageId.value = null
    }
  })
}

const showGroupMemberDialog = ref(false)

const currentGroupRole = computed(() => {
  if (!selectedConv.value || selectedConv.value.chatType !== 2) return null
  const currentMember = atMembers.value.find(m => m.userId === currentUserId.value)
  if (!currentMember || currentMember.role == null) return null
  return currentMember.role
})

const isMemberMuted = (member) => {
  if (!member || !member.muteUntil) return false
  const t = new Date(member.muteUntil)
  if (isNaN(t.getTime())) return false
  return t.getTime() > Date.now()
}

const formatMuteUntil = (time) => {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return ''
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

const canManageMember = (member) => {
  if (!isGroupAdmin.value) return false
  if (!member) return false
  if (member.userId === currentUserId.value) return false
  if (member.role === 2) return false
  const role = currentGroupRole.value
  if (role === 1 && member.role === 1) return false
  return true
}

const openGroupMemberDialog = async () => {
  if (!selectedConv.value || selectedConv.value.chatType !== 2) return
  if (atMembers.value.length === 0) {
    await loadGroupMembersForAt()
  }
  showGroupMemberDialog.value = true
}

const buildMuteUntil = (minutes) => {
  const date = new Date()
  date.setMinutes(date.getMinutes() + minutes)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  const ss = String(date.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`
}

const handleMuteMember = async (member, minutes) => {
  if (!selectedConv.value || selectedConv.value.chatType !== 2) return
  const groupId = selectedConv.value.targetId
  const muteUntil = buildMuteUntil(minutes)
  try {
    await muteGroupMember(groupId, member.userId, muteUntil)
    ElMessage.success('已设置禁言')
    await loadGroupMembersForAt()
  } catch (error) {
    console.error('设置禁言失败:', error)
    ElMessage.error('设置禁言失败')
  }
}

const handleCustomMuteMember = async (member) => {
  if (!selectedConv.value || selectedConv.value.chatType !== 2) return
  try {
    const { value } = await ElMessageBox.prompt('请输入禁言时长（分钟）', '自定义禁言时长', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：60',
      inputPattern: /^\\d+$/,
      inputErrorMessage: '请输入大于0的数字'
    })
    const minutes = parseInt(value, 10)
    if (isNaN(minutes) || minutes <= 0) {
      ElMessage.warning('请输入大于0的数字')
      return
    }
    await handleMuteMember(member, minutes)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('自定义禁言失败:', error)
      ElMessage.error('自定义禁言失败')
    }
  }
}

const handleUnmuteMember = async (member) => {
  if (!selectedConv.value || selectedConv.value.chatType !== 2) return
  const groupId = selectedConv.value.targetId
  try {
    await muteGroupMember(groupId, member.userId, null)
    ElMessage.success('已解除禁言')
    await loadGroupMembersForAt()
  } catch (error) {
    console.error('解除禁言失败:', error)
    ElMessage.error('解除禁言失败')
  }
}

// ========== 群公告相关方法 ==========

// 加载群公告列表
const loadAnnouncements = async () => {
  if (!selectedConv.value || selectedConv.value.chatType !== 2) return
  
  announcementLoading.value = true
  try {
    const res = await getAnnouncementList(selectedConv.value.targetId)
    announcements.value = res.data || []
  } catch (error) {
    console.error('加载群公告失败:', error)
  } finally {
    announcementLoading.value = false
  }
}

// 判断当前用户是否是群主或管理员
const isGroupAdmin = computed(() => {
  if (!selectedConv.value || selectedConv.value.chatType !== 2) return false
  const currentMember = atMembers.value.find(m => m.userId === currentUserId.value)
  if (!currentMember || currentMember.role == null) return false
  return currentMember.role === 2 || currentMember.role === 1
})

// 打开发布公告弹窗
const openPublishAnnouncement = () => {
  editingAnnouncement.value = null
  announcementForm.value = {
    title: '',
    content: '',
    isTop: false
  }
  showEditAnnouncement.value = true
}

// 打开编辑公告弹窗
const openEditAnnouncement = (announcement) => {
  editingAnnouncement.value = announcement
  announcementForm.value = {
    title: announcement.title,
    content: announcement.content,
    isTop: announcement.isTop === 1
  }
  showEditAnnouncement.value = true
}

// 提交公告（发布或更新）
const submitAnnouncement = async () => {
  if (!announcementForm.value.title.trim()) {
    ElMessage.warning('请输入公告标题')
    return
  }
  if (!announcementForm.value.content.trim()) {
    ElMessage.warning('请输入公告内容')
    return
  }
  
  try {
    if (editingAnnouncement.value) {
      // 更新公告
      await updateAnnouncement(editingAnnouncement.value.id, announcementForm.value)
      ElMessage.success('公告更新成功')
    } else {
      // 发布公告
      await publishAnnouncement({
        groupId: selectedConv.value.targetId,
        ...announcementForm.value
      })
      ElMessage.success('公告发布成功')
    }
    
    showEditAnnouncement.value = false
    await loadAnnouncements()
  } catch (error) {
    console.error('提交公告失败:', error)
  }
}

// 删除公告
const handleDeleteAnnouncement = async (announcement) => {
  try {
    await ElMessageBox.confirm('确定要删除这条公告吗？', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteAnnouncement(announcement.id)
    ElMessage.success('公告已删除')
    await loadAnnouncements()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除公告失败:', error)
    }
  }
}

// 格式化公告时间
const formatAnnouncementTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  
  return date.toLocaleDateString()
}

// 监听群公告弹窗打开
watch(showAnnouncementDialog, (val) => {
  if (val) {
    loadAnnouncements()
    // 确保群成员已加载（用于判断权限）
    if (atMembers.value.length === 0) {
      loadGroupMembersForAt()
    }
  }
})

</script>

<style scoped>
.chat-container {
  display: flex;
  width: 100%;
  height: 100%;
  background: #f0f0f0;
  overflow: hidden;
}

/* 会话列表 */
.conversation-list {
  background: white;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

/* 可拖拽分割线 */
.resizer {
  width: 4px;
  background: #e0e0e0;
  cursor: col-resize;
  position: relative;
  transition: background-color 0.2s;
  flex-shrink: 0;
}

.resizer:hover {
  background: #409eff;
}

.resizer::after {
  content: '';
  position: absolute;
  left: -2px;
  right: -2px;
  top: 0;
  bottom: 0;
}

.list-header {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.list-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.conv-scrollbar {
  flex: 1;
}

.empty-state {
  padding: 60px 20px;
}

.conv-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f5f5f5;
  position: relative;
}

.conv-item:hover {
  background: #f5f7fa;
}

.conv-item:hover .conv-actions {
  opacity: 1;
}

.conv-item.active {
  background: #ecf5ff;
}

.conv-item.pinned {
  background: #f0f9ff;
  border-left: 3px solid #409eff;
}

.conv-item.hidden {
  opacity: 0.5;
}

.conv-info {
  flex: 1;
  margin-left: 12px;
  overflow: hidden;
}

.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.conv-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.conv-time {
  font-size: 12px;
  color: #909399;
}

.conv-bottom {
  font-size: 13px;
  color: #909399;
}

.last-message {
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 会话操作按钮 */
.conv-actions {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.2s;
}

.action-btn {
  padding: 4px !important;
  min-height: auto !important;
  color: #909399 !important;
}

.action-btn:hover {
  color: #409eff !important;
  background: rgba(64, 158, 255, 0.1) !important;
}

/* 置顶图标 */
.pin-icon {
  color: #409eff;
  font-size: 12px;
  margin-right: 4px;
}

/* 免打扰图标 */
.mute-icon {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}

/* 聊天窗口 */
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.no-chat-selected {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-header {
  height: 60px;
  padding: 0 20px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.target-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

/* 消息列表 */
.message-list {
  flex: 1;
  padding: 16px 24px;
  min-height: 0; /* 确保滚动正常工作 */
}

.message-content {
  min-height: 100%;
}

.date-divider {
  text-align: center;
  margin: 20px 0;
}

.date-divider span {
  padding: 4px 12px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  font-size: 12px;
  color: #909399;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  gap: 12px;
}

.message-item.is-mine {
  flex-direction: row-reverse;
}

.message-wrapper {
  max-width: 70%;
}

.message-item.is-mine .message-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #909399;
}

.message-item.is-mine .message-info {
  flex-direction: row-reverse;
}

.message-bubble {
  padding: 16px 20px !important;
  background: white;
  border-radius: 16px !important;
  font-size: 16px !important;
  line-height: 1.5;
  color: #303133;
  word-wrap: break-word;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12) !important;
  min-width: 80px !important;
}

.message-item.is-mine .message-bubble {
  background: #95ec69;
  color: #303133;
}

/* 撤回消息样式 */
.message-bubble.recalled {
  background: #f5f5f5 !important;
  color: #909399 !important;
  font-style: italic;
  text-align: center;
}

.recalled-text {
  font-size: 14px;
}

/* 发送中消息样式 */
.message-bubble.sending {
  opacity: 0.7;
  position: relative;
}

.sending-indicator {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

/* 发送失败样式 */
.message-bubble.failed {
  opacity: 0.8;
}

.failed-text {
  color: #303133;
}

.failed-indicator {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  background: #f56c6c;
  color: white;
  border-radius: 50%;
  font-size: 12px;
  font-weight: bold;
  margin-left: 8px;
  cursor: pointer;
}

/* 消息操作按钮 */
.message-actions {
  position: absolute;
  top: -12px;
  right: -8px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message-bubble:hover .message-actions {
  opacity: 1;
}

.message-actions .action-btn {
  font-size: 12px !important;
  padding: 4px 10px !important;
  height: auto !important;
  background: rgba(0, 0, 0, 0.7) !important;
  color: white !important;
  border-radius: 4px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15) !important;
}

.message-actions .action-btn:hover {
  background: rgba(0, 0, 0, 0.85) !important;
}

.message-actions .delete-btn {
  background: rgba(245, 108, 108, 0.9) !important;
}

.message-actions .delete-btn:hover {
  background: rgba(245, 108, 108, 1) !important;
}

.message-bubble {
  position: relative;
}

.voice-message {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.voice-message.playing .voice-label {
  font-weight: 600;
}

.voice-label {
  font-size: 14px;
}

.voice-duration {
  font-size: 14px;
  color: #606266;
}

.image-message {
  max-width: 200px;
  border-radius: 6px;
  cursor: pointer;
}

.video-message {
  max-width: 260px;
  border-radius: 6px;
}

.empty-messages {
  padding: 60px 20px;
}

/* 输入区域 */
.input-area {
  background: white;
  border-top: 1px solid #e0e0e0;
  padding: 16px 24px;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.input-box {
  margin-bottom: 10px;
}

.input-box :deep(.el-textarea__inner) {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.6;
}

.input-box :deep(.el-textarea__inner):focus {
  border-color: #409eff;
}

/* @成员选择面板 */
.at-panel {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.1);
  max-height: 300px;
  z-index: 100;
  margin-bottom: 8px;
}

.at-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.at-panel-search {
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.at-panel-list {
  max-height: 200px;
  overflow-y: auto;
}

.at-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.at-item:hover {
  background: #f5f7fa;
}

.at-item.at-all {
  border-bottom: 1px solid #f0f0f0;
  color: #409eff;
  font-weight: 500;
}

.at-item span {
  font-size: 14px;
}

/* @高亮样式 */
.at-highlight {
  color: #409eff;
  font-weight: 500;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.emoji-panel {
  padding: 8px;
  max-height: 260px;
  overflow-y: auto;
}

.emoji-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.emoji-item {
  width: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.emoji-image {
  width: 40px;
  height: 40px;
  object-fit: contain;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #f0f0f0;
  background: #fff;
}

.emoji-delete-btn {
  padding: 0 4px !important;
  min-height: auto !important;
  font-size: 12px !important;
}

.text-count {
  font-size: 12px;
  color: #909399;
}

/* 搜索相关样式 */
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-icon {
  font-size: 18px;
  color: #606266;
  cursor: pointer;
  transition: color 0.3s;
}

.search-icon:hover {
  color: #409eff;
}

.search-dialog .search-header {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.search-dialog .search-header .el-input {
  flex: 1;
}

.search-results {
  min-height: 200px;
  max-height: 400px;
  overflow-y: auto;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-item {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.result-item:hover {
  background: #e6f0ff;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.result-conv-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.result-content {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-content :deep(.highlight) {
  color: #409eff;
  font-weight: bold;
}

.empty-search {
  padding: 40px 0;
}

.search-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

/* 群公告弹窗样式 */
.announcement-dialog :deep(.el-dialog__body) {
  padding-top: 12px;
}

.announcement-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.announcement-actions {
  margin-bottom: 8px;
}

.announcement-actions .el-button {
  width: 100%;
}

.announcement-list {
  max-height: 360px;
  overflow-y: auto;
}

.announcement-item {
  padding: 10px 12px;
  border-radius: 8px;
  background: #f5f7fa;
  margin-bottom: 8px;
}

.announcement-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.announcement-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.announcement-ops .el-button {
  padding: 0 4px;
  min-height: auto;
}

.announcement-body {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  margin: 4px 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.announcement-footer {
  display: flex;
  justify-content: flex-end;
}

.announcement-time {
  font-size: 12px;
  color: #909399;
}

/* 高亮消息样式 */
.message-bubble.highlight-msg {
  animation: highlight-flash 0.5s ease-in-out 3;
  box-shadow: 0 0 10px rgba(64, 158, 255, 0.6);
}

@keyframes highlight-flash {
  0%, 100% {
    background-color: inherit;
  }
  50% {
    background-color: rgba(64, 158, 255, 0.3);
  }
}
</style>

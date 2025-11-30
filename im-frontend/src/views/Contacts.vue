<template>
  <div class="contacts-container">
    <div class="contacts-sidebar">
      <div class="sidebar-header">
        <h3>通讯录</h3>
      </div>
      <el-menu :default-active="activeTab" @select="handleTabSelect">
        <el-menu-item index="requests">
          <el-badge :value="unreadRequestCount" :hidden="unreadRequestCount === 0">
            <el-icon><Bell /></el-icon>
          </el-badge>
          <span style="margin-left: 8px">新的朋友</span>
        </el-menu-item>
        <el-menu-item index="groupInvitations">
          <el-badge :value="unreadGroupInvitationCount" :hidden="unreadGroupInvitationCount === 0">
            <el-icon><Message /></el-icon>
          </el-badge>
          <span style="margin-left: 8px">群组邀请</span>
        </el-menu-item>
        <el-menu-item index="groupNotifications">
          <el-badge :value="unreadGroupNotificationCount" :hidden="unreadGroupNotificationCount === 0">
            <el-icon><Bell /></el-icon>
          </el-badge>
          <span style="margin-left: 8px">群消息通知</span>
        </el-menu-item>
        <el-menu-item index="friends">
          <el-icon><User /></el-icon>
          <span>好友</span>
        </el-menu-item>
        <el-menu-item index="groups">
          <el-icon><UserFilled /></el-icon>
          <span>群组</span>
        </el-menu-item>
      </el-menu>
    </div>
    
    <div class="contacts-content">
      <div class="content-header">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索"
          :prefix-icon="Search"
        />
        <el-button 
          v-if="activeTab === 'friends'" 
          type="primary" 
          :icon="Plus" 
          @click="showAddDialog = true"
        >
          添加好友
        </el-button>
        <el-button 
          v-if="activeTab === 'groups'" 
          type="primary" 
          :icon="Plus" 
          @click="showCreateGroupDialog = true"
        >
          创建群组
        </el-button>
      </div>
      
      <el-scrollbar class="content-list">
        <!-- 新的朋友 -->
        <div v-if="activeTab === 'requests'">
          <div v-if="friendRequests.length === 0" class="empty-requests">
            <el-empty description="暂无好友申请" />
          </div>
          <div v-else>
            <div
              v-for="request in friendRequests"
              :key="request.id"
              class="request-item"
            >
              <el-avatar :size="48" :src="request.fromUserAvatar">
                {{ request.fromUserName?.charAt(0) }}
              </el-avatar>
              <div class="request-info">
                <div class="request-name">{{ request.fromUserName }}</div>
                <div class="request-message">{{ request.message }}</div>
                <div class="request-time">{{ formatTime(request.createTime) }}</div>
              </div>
              <div class="request-actions">
                <template v-if="request.status === 0">
                  <el-button type="success" size="small" @click="handleRequest(request.id, 1)">
                    同意
                  </el-button>
                  <el-button type="info" size="small" @click="handleRequest(request.id, 2)">
                    拒绝
                  </el-button>
                </template>
                <template v-else-if="request.status === 1">
                  <el-tag type="success" size="small">已同意</el-tag>
                </template>
                <template v-else-if="request.status === 2">
                  <el-tag type="info" size="small">已拒绝</el-tag>
                </template>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 群组邀请 -->
        <div v-else-if="activeTab === 'groupInvitations'">
          <div v-if="groupInvitations.length === 0" class="empty-requests">
            <el-empty description="暂无群组邀请" />
          </div>
          <div v-else>
            <div
              v-for="invitation in groupInvitations"
              :key="invitation.id"
              class="request-item"
            >
              <el-avatar :size="48" :src="invitation.groupAvatar">
                {{ invitation.groupName?.charAt(0) }}
              </el-avatar>
              <div class="request-info">
                <div class="request-name">{{ invitation.groupName }}</div>
                <div class="request-message">{{ invitation.inviterNickname }} 邀请你加入群组</div>
                <div class="request-time">{{ formatTime(invitation.createTime) }}</div>
              </div>
              <div class="request-actions">
                <template v-if="invitation.status === 1">
                  <el-button type="success" size="small" @click="handleGroupInvitationAction(invitation.id, true)">
                    同意
                  </el-button>
                  <el-button type="info" size="small" @click="handleGroupInvitationAction(invitation.id, false)">
                    拒绝
                  </el-button>
                </template>
                <template v-else-if="invitation.status === 2">
                  <el-tag type="success" size="small">已同意</el-tag>
                </template>
                <template v-else-if="invitation.status === 4">
                  <el-tag type="info" size="small">已拒绝</el-tag>
                </template>
                <template v-else-if="invitation.status === 5">
                  <el-tag type="warning" size="small">已过期</el-tag>
                </template>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 群消息通知 -->
        <div v-else-if="activeTab === 'groupNotifications'">
          <div v-if="groupNotifications.length === 0" class="empty-requests">
            <el-empty description="暂无群消息通知" />
          </div>
          <div v-else>
            <div
              v-for="notification in groupNotifications"
              :key="notification.id"
              class="request-item"
            >
              <el-avatar :size="48" :src="notification.groupAvatar">
                {{ notification.groupName?.charAt(0) }}
              </el-avatar>
              <div class="request-info">
                <div class="request-name">{{ notification.groupName }}</div>
                <div class="request-message">{{ notification.message }}</div>
                <div class="request-time">{{ formatTime(notification.createTime) }}</div>
              </div>
              <div class="request-actions">
                <el-tag :type="getNotificationTypeTag(notification.type)" size="small">
                  {{ getNotificationTypeText(notification.type) }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 好友列表 -->
        <div v-else-if="activeTab === 'friends'">
          <div v-for="friend in friends" :key="friend.userId" class="contact-item">
            <el-avatar :size="40" :src="friend.avatar">
              {{ friend.nickname?.charAt(0) }}
            </el-avatar>
            <span class="contact-name">{{ friend.nickname }}</span>
            <el-dropdown trigger="click" @command="(cmd) => handleFriendAction(cmd, friend)">
              <el-button text :icon="MoreFilled" class="more-btn" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="chat">
                    <el-icon><ChatDotRound /></el-icon>
                    <span>发送消息</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>
                    <el-icon><Delete /></el-icon>
                    <span>删除好友</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
        
        <!-- 群组列表 -->
        <div v-else>
          <div v-for="group in groups" :key="group.groupId" class="contact-item">
            <el-avatar :size="40" :src="group.avatar">
              {{ group.groupName?.charAt(0) }}
            </el-avatar>
            <span class="contact-name">{{ group.groupName }}</span>
            <span class="group-member-count">{{ group.memberCount }}人</span>
            <el-dropdown trigger="click" @command="(cmd) => handleGroupAction(cmd, group)">
              <el-button text :icon="MoreFilled" class="more-btn" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="chat">
                    <el-icon><ChatDotRound /></el-icon>
                    <span>发送消息</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="invite">
                    <el-icon><Plus /></el-icon>
                    <span>邀请成员</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="info">
                    <el-icon><InfoFilled /></el-icon>
                    <span>查看群信息</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="members">
                    <el-icon><User /></el-icon>
                    <span>成员管理</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="exit" divided>
                    <el-icon><Close /></el-icon>
                    <span>退出群组</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-scrollbar>
    </div>
    
    <!-- 添加好友对话框 -->
    <el-dialog v-model="showAddDialog" title="添加好友" width="500px">
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="搜索用户">
          <el-input
            v-model="searchInput"
            placeholder="请输入用户名或用户ID"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button :icon="Search" @click="handleSearch" :loading="searchLoading">搜索</el-button>
            </template>
          </el-input>
        </el-form-item>
        
        <!-- 搜索结果 -->
        <el-form-item v-if="searchResults.length > 0" label="搜索结果">
          <div class="search-results">
            <div
              v-for="user in searchResults"
              :key="user.userId"
              class="search-result-item"
              :class="{ selected: addForm.friendId === user.userId }"
              @click="selectUser(user)"
            >
              <el-avatar :size="40" :src="user.avatar">
                {{ user.nickname?.charAt(0) }}
              </el-avatar>
              <div class="user-info">
                <div class="user-name">{{ user.nickname }}</div>
                <div class="user-id">ID: {{ user.userId }}</div>
              </div>
              <el-icon v-if="addForm.friendId === user.userId" class="check-icon">
                <Check />
              </el-icon>
            </div>
          </div>
        </el-form-item>
        
        <el-form-item v-if="addForm.friendId" label="验证消息">
          <el-input
            v-model="addForm.message"
            type="textarea"
            :rows="3"
            placeholder="请输入验证消息"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleAddFriend"
          :loading="addLoading"
          :disabled="!addForm.friendId"
        >
          发送申请
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 创建群组对话框 -->
    <el-dialog v-model="showCreateGroupDialog" title="创建群组" width="600px">
      <el-form :model="createGroupForm" label-width="100px">
        <el-form-item label="群名称" required>
          <el-input
            v-model="createGroupForm.groupName"
            placeholder="请输入群名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="群公告">
          <el-input
            v-model="createGroupForm.notice"
            type="textarea"
            :rows="3"
            placeholder="请输入群公告"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="选择成员">
          <div class="member-selection">
            <el-checkbox-group v-model="createGroupForm.memberIds">
              <div v-for="friend in friends" :key="friend.userId" class="member-item">
                <el-checkbox :label="friend.userId">
                  <el-avatar :size="30" :src="friend.avatar">
                    {{ friend.nickname?.charAt(0) }}
                  </el-avatar>
                  <span class="member-name">{{ friend.nickname }}</span>
                </el-checkbox>
              </div>
            </el-checkbox-group>
          </div>
        </el-form-item>
        
        <el-form-item label="">
          <el-alert type="info" :closable="false" show-icon>
            已选择 {{ createGroupForm.memberIds.length }} 人
          </el-alert>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateGroupDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleCreateGroup"
          :loading="createGroupLoading"
          :disabled="!createGroupForm.groupName || createGroupForm.memberIds.length === 0"
        >
          创建
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 群组详情对话框 -->
    <el-dialog 
      v-model="showGroupInfoDialog" 
      :title="editingGroupInfo ? '编辑群组信息' : '群组详情'" 
      width="500px">
      <div v-if="currentGroup" class="group-info">
        <div class="info-section" v-if="!editingGroupInfo">
          <el-avatar :size="80" :src="currentGroup.avatar">
            {{ currentGroup.groupName?.charAt(0) }}
          </el-avatar>
        </div>
        
        <template v-if="editingGroupInfo">
          <!-- 编辑模式 -->
          <el-form :model="groupEditForm" label-width="80px">
            <el-form-item label="群名称">
              <el-input v-model="groupEditForm.groupName" placeholder="请输入群名称" />
            </el-form-item>
            <el-form-item label="群头像">
              <el-input v-model="groupEditForm.avatar" placeholder="请输入头像链接" />
            </el-form-item>
            <el-form-item label="群公告">
              <el-input 
                v-model="groupEditForm.notice" 
                type="textarea" 
                :rows="3"
                placeholder="请输入群公告" 
              />
            </el-form-item>
            <el-form-item label="最大人数">
              <el-input-number 
                v-model="groupEditForm.maxMembers" 
                :min="currentGroup.memberCount" 
                :max="1000"
              />
            </el-form-item>
          </el-form>
        </template>
        
        <template v-else>
          <!-- 查看模式 -->
          <div class="info-section">
            <div class="info-label">群名称</div>
            <div class="info-value">{{ currentGroup.groupName }}</div>
          </div>
          <div class="info-section">
            <div class="info-label">群主</div>
            <div class="info-value">{{ currentGroup.ownerName || '未知' }}</div>
          </div>
          <div class="info-section">
            <div class="info-label">群成员</div>
            <div class="info-value">{{ currentGroup.memberCount }} / {{ currentGroup.maxMembers }}</div>
          </div>
          <div class="info-section">
            <div class="info-label">群公告</div>
            <div class="info-value">{{ currentGroup.notice || '暂无公告' }}</div>
          </div>
          <div class="info-section">
            <div class="info-label">创建时间</div>
            <div class="info-value">{{ formatTime(currentGroup.createTime) }}</div>
          </div>
        </template>
      </div>
      <template #footer>
        <template v-if="editingGroupInfo">
          <el-button @click="cancelEditGroupInfo">取消</el-button>
          <el-button type="primary" @click="saveGroupInfo" :loading="savingGroupInfo">保存</el-button>
        </template>
        <template v-else>
          <el-button @click="showGroupInfoDialog = false">关闭</el-button>
          <el-button 
            v-if="currentGroup.ownerId === getCurrentUserId()" 
            type="primary" 
            @click="startEditGroupInfo">
            编辑
          </el-button>
        </template>
      </template>
    </el-dialog>
    
    <!-- 群组成员管理对话框 -->
    <el-dialog v-model="showGroupMembersDialog" title="成员管理" width="600px">
      <div v-if="currentGroup" class="group-members">
        <div class="members-header">
          <h4>{{ currentGroup.groupName }} - 成员列表</h4>
          <span class="member-count">共 {{ groupMembers.length }} 人</span>
          <el-button 
            type="primary" 
            size="small" 
            :icon="Plus" 
            @click="openInviteMembersDialog"
            v-if="currentUserRole >= 1"
          >
            邀请成员
          </el-button>
        </div>
        
        <el-scrollbar max-height="400px">
          <div class="member-list">
            <div
              v-for="member in groupMembers"
              :key="member.userId"
              class="member-item"
            >
              <el-avatar :size="40" :src="member.avatar">
                {{ member.nickname?.charAt(0) }}
              </el-avatar>
              
              <div class="member-info">
                <div class="member-name">{{ member.nickname }}</div>
                <div class="member-join-time">{{ formatTime(member.joinTime) }} 加入</div>
              </div>
              
              <div class="member-role">
                <el-tag :type="getRoleType(member.role)" size="small">
                  {{ getRoleText(member.role) }}
                </el-tag>
              </div>
              
              <!-- 群主的操作按钮 -->
              <div class="member-actions" v-if="currentUserRole === 2 && member.userId !== getCurrentUserId()">
                <el-button
                  v-if="member.role === 0"
                  type="primary"
                  size="small"
                  @click="setGroupAdmin(currentGroup.groupId, member.userId, true)"
                >
                  设为管理员
                </el-button>
                <el-button
                  v-else-if="member.role === 1"
                  type="warning"
                  size="small"
                  @click="setGroupAdmin(currentGroup.groupId, member.userId, false)"
                >
                  取消管理员
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click="removeMember(currentGroup.groupId, member.userId, member.nickname)"
                  v-if="member.role !== 2"
                >
                  移除
                </el-button>
              </div>
              
              <!-- 管理员的操作按钮（只能移除普通成员） -->
              <div class="member-actions" v-else-if="currentUserRole === 1 && member.role === 0 && member.userId !== getCurrentUserId()">
                <el-button
                  type="danger"
                  size="small"
                  @click="removeMember(currentGroup.groupId, member.userId, member.nickname)"
                >
                  移除
                </el-button>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>
      <template #footer>
        <el-button @click="showGroupMembersDialog = false">关闭</el-button>
      </template>
    </el-dialog>
    
    <!-- 邀请成员对话框 -->
    <el-dialog v-model="showInviteMembersDialog" title="邀请成员" width="500px">
      <div v-if="currentGroup" class="invite-members">
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 15px">
          邀请好友加入群组：{{ currentGroup.groupName }}
        </el-alert>
        
        <el-form label-width="80px">
          <el-form-item label="选择好友">
            <div class="friend-selection">
              <el-checkbox-group v-model="selectedInviteMembers">
                <div v-for="friend in availableFriends" :key="friend.userId" class="friend-item">
                  <el-checkbox :label="friend.userId">
                    <el-avatar :size="30" :src="friend.avatar">
                      {{ friend.nickname?.charAt(0) }}
                    </el-avatar>
                    <span class="friend-name">{{ friend.nickname }}</span>
                  </el-checkbox>
                </div>
              </el-checkbox-group>
              
              <div v-if="availableFriends.length === 0" class="no-friends">
                <el-empty description="暂无可邀请的好友" :image-size="60" />
              </div>
            </div>
          </el-form-item>
          
          <el-form-item v-if="selectedInviteMembers.length > 0" label="">
            <el-alert type="success" :closable="false" show-icon>
              已选择 {{ selectedInviteMembers.length }} 人
            </el-alert>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showInviteMembersDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleInviteMembers"
          :loading="inviteMembersLoading"
          :disabled="selectedInviteMembers.length === 0"
        >
          发送邀请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled, ChatDotRound, Delete, InfoFilled, Close, Message, Plus, User, Bell, UserFilled } from '@element-plus/icons-vue'
import { getFriendList, addFriend, handleFriendRequest } from '@/api/friend'
import { searchUser } from '@/api/user'
import { 
  getGroupList, 
  createGroup, 
  getGroupDetail, 
  getGroupMembers,
  handleGroupInvitation,
  getMyGroupInvitations,
  quitGroup,
  dissolveGroup,
  setGroupAdmin as setGroupAdminApi,
  inviteToGroup,
  removeMember as removeMemberApi,
  updateGroup
} from '@/api/group'
import request from '@/utils/request'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('friends')
const searchKeyword = ref('')
const showAddDialog = ref(false)
const showCreateGroupDialog = ref(false)
const showGroupInfoDialog = ref(false)
const showGroupMembersDialog = ref(false)
const showInviteMembersDialog = ref(false)
const editingGroupInfo = ref(false)
const savingGroupInfo = ref(false)
const addLoading = ref(false)
const searchLoading = ref(false)
const createGroupLoading = ref(false)
const inviteMembersLoading = ref(false)
const friends = ref([])
const groups = ref([])
const friendRequests = ref([])
const groupInvitations = ref([])
const groupNotifications = ref([])
const currentGroup = ref(null)
const groupMembers = ref([])
const currentUserRole = ref(0)
const selectedInviteMembers = ref([])

const searchInput = ref('')
const searchResults = ref([])

const addForm = ref({
  friendId: null,
  message: '你好，我想加你为好友'
})

const createGroupForm = ref({
  groupName: '',
  avatar: '',
  notice: '',
  maxMembers: 500,
  memberIds: []
})

const groupEditForm = ref({
  groupName: '',
  avatar: '',
  notice: '',
  maxMembers: 500
})

// 计算未读申请数量
const unreadRequestCount = computed(() => {
  return friendRequests.value.filter(r => r.status === 0).length
})

// 计算未读群组邀请数量
const unreadGroupInvitationCount = computed(() => {
  return groupInvitations.value.filter(i => i.status === 1).length
})

// 计算未读群消息通知数量
const unreadGroupNotificationCount = computed(() => {
  return groupNotifications.value.filter(n => !n.read).length
})

// 计算可邀请的好友（排除已在群里的）
const availableFriends = computed(() => {
  if (!currentGroup.value || groupMembers.value.length === 0) {
    return friends.value
  }
  
  const memberUserIds = groupMembers.value.map(m => m.userId)
  return friends.value.filter(f => !memberUserIds.includes(f.userId))
})

const handleTabSelect = (index) => {
  activeTab.value = index
  
  // 切换到好友申请时加载数据
  if (index === 'requests') {
    loadFriendRequests()
  } else if (index === 'groupInvitations') {
    loadGroupInvitations()
  } else if (index === 'groupNotifications') {
    loadGroupNotifications()
    markGroupNotificationsAsRead()
  } else if (index === 'groups') {
    loadGroupList()
  }
}

// 搜索用户
const handleSearch = async () => {
  if (!searchInput.value.trim()) {
    ElMessage.warning('请输入用户名或用户ID')
    return
  }
  
  try {
    searchLoading.value = true
    const res = await searchUser(searchInput.value)
    searchResults.value = res.data || []
    
    if (searchResults.value.length === 0) {
      ElMessage.info('未找到相关用户')
    }
  } catch (error) {
    console.error('搜索用户失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    searchLoading.value = false
  }
}

// 选择用户
const selectUser = (user) => {
  addForm.value.friendId = user.userId
}

// 添加好友
const handleAddFriend = async () => {
  if (!addForm.value.friendId) {
    ElMessage.warning('请先搜索并选择要添加的用户')
    return
  }
  
  try {
    addLoading.value = true
    await addFriend({
      friendId: addForm.value.friendId,
      message: addForm.value.message
    })
    ElMessage.success('好友申请已发送')
    showAddDialog.value = false
    
    // 重置表单
    addForm.value = {
      friendId: null,
      message: '你好，我想加你为好友'
    }
    searchInput.value = ''
    searchResults.value = []
  } catch (error) {
    console.error('添加好友失败:', error)
    ElMessage.error(error.response?.data?.message || '添加好友失败')
  } finally {
    addLoading.value = false
  }
}

// 创建群组
const handleCreateGroup = async () => {
  if (!createGroupForm.value.groupName.trim()) {
    ElMessage.warning('请输入群名称')
    return
  }
  
  if (createGroupForm.value.memberIds.length === 0) {
    ElMessage.warning('请至少选择一个成员')
    return
  }
  
  try {
    createGroupLoading.value = true
    const res = await createGroup({
      groupName: createGroupForm.value.groupName,
      notice: createGroupForm.value.notice,
      memberIds: createGroupForm.value.memberIds
    })
    
    ElMessage.success(`群组"${createGroupForm.value.groupName}"创建成功！`)
    showCreateGroupDialog.value = false
    
    // 重置表单
    createGroupForm.value = {
      groupName: '',
      notice: '',
      memberIds: []
    }
    
    // 刷新群组列表
    loadGroupList()
  } catch (error) {
    console.error('创建群组失败:', error)
    ElMessage.error(error.response?.data?.message || '创建群组失败')
  } finally {
    createGroupLoading.value = false
  }
}

// 监听对话框关闭，重置搜索
watch(showAddDialog, (newVal) => {
  if (!newVal) {
    searchInput.value = ''
    searchResults.value = []
    addForm.value = {
      friendId: null,
      message: '你好，我想加你为好友'
    }
  }
})

// 加载好友申请列表
const loadFriendRequests = async () => {
  try {
    const res = await request.get('/friend/request/list')
    // 需要获取申请方的用户信息
    const requests = res.data || []
    
    // 获取每个申请的发送方信息
    for (const req of requests) {
      try {
        const userRes = await request.get(`/user/info/${req.fromUserId}`)
        req.fromUserName = userRes.data.nickname
        req.fromUserAvatar = userRes.data.avatar
      } catch (err) {
        req.fromUserName = '未知用户'
      }
    }
    
    friendRequests.value = requests
  } catch (error) {
    console.error('加载好友申请列表失败:', error)
  }
}

// 加载群组邀请列表
const loadGroupInvitations = async () => {
  try {
    const res = await getMyGroupInvitations()
    groupInvitations.value = res.data || []
    console.log('群组邀请列表加载成功:', groupInvitations.value)
  } catch (error) {
    console.error('加载群组邀请列表失败:', error)
    ElMessage.error('加载群组邀请列表失败')
  }
}

// 加载群消息通知列表
const loadGroupNotifications = () => {
  // 从 localStorage 加载通知
  const stored = localStorage.getItem('groupNotifications')
  if (stored) {
    try {
      groupNotifications.value = JSON.parse(stored)
    } catch (error) {
      console.error('解析群消息通知失败:', error)
      groupNotifications.value = []
    }
  }
}

// 标记群消息通知为已读
const markGroupNotificationsAsRead = () => {
  groupNotifications.value.forEach(n => n.read = true)
  localStorage.setItem('groupNotifications', JSON.stringify(groupNotifications.value))
}

// 添加群消息通知
const addGroupNotification = (notification) => {
  groupNotifications.value.unshift({
    id: Date.now(),
    type: notification.type,
    groupId: notification.groupId,
    groupName: notification.groupName || '未知群组',
    groupAvatar: notification.groupAvatar || '',
    message: notification.message,
    createTime: new Date().toISOString(),
    read: false
  })
  
  // 只保留最近100条通知
  if (groupNotifications.value.length > 100) {
    groupNotifications.value = groupNotifications.value.slice(0, 100)
  }
  
  localStorage.setItem('groupNotifications', JSON.stringify(groupNotifications.value))
}

// 获取通知类型标签
const getNotificationTypeTag = (type) => {
  const tagMap = {
    'GROUP_ADMIN_CHANGE': 'warning',
    'GROUP_MEMBER_REMOVED': 'danger',
    'GROUP_MEMBER_QUIT': 'info',
    'GROUP_DIRECT_JOIN': 'success',
    'GROUP_DISSOLVED': 'danger'
  }
  return tagMap[type] || 'info'
}

// 获取通知类型文本
const getNotificationTypeText = (type) => {
  const textMap = {
    'GROUP_ADMIN_CHANGE': '管理员变更',
    'GROUP_MEMBER_REMOVED': '成员被移除',
    'GROUP_MEMBER_QUIT': '成员退出',
    'GROUP_DIRECT_JOIN': '直接加入',
    'GROUP_DISSOLVED': '群组解散'
  }
  return textMap[type] || '系统通知'
}

// 处理好友申请（同意或拒绝）
const handleRequest = async (requestId, status) => {
  try {
    await handleFriendRequest({
      requestId,
      status
    })
    
    ElMessage.success(status === 1 ? '已同意好友申请' : '已拒绝好友申请')
    
    // 重新加载列表
    loadFriendRequests()
    
    // 如果同意了，刷新好友列表
    if (status === 1) {
      const res = await getFriendList()
      friends.value = res.data
    }
  } catch (error) {
    console.error('处理好友申请失败:', error)
    ElMessage.error('操作失败')
  }
}

// 处理群组邀请（同意或拒绝）
const handleGroupInvitationAction = async (invitationId, accept) => {
  try {
    await handleGroupInvitation(invitationId, accept)
    
    ElMessage.success(accept ? '已同意加入群组' : '已拒绝群组邀请')
    
    // 重新加载邀请列表
    loadGroupInvitations()
    
    // 如果同意了，刷新群组列表
    if (accept) {
      loadGroupList()
    }
  } catch (error) {
    console.error('处理群组邀请失败:', error)
    ElMessage.error('操作失败')
  }
}

// 处理好友操作
const handleFriendAction = async (command, friend) => {
  if (command === 'chat') {
    // 发送消息 - 跳转到聊天页面，传递好友ID和聊天类型
    router.push({
      path: '/chat',
      query: {
        targetId: friend.userId,
        chatType: 1, // 1-单聊
        targetName: friend.nickname,
        targetAvatar: friend.avatar
      }
    })
    ElMessage.success(`即将和 ${friend.nickname} 聊天`)
  } else if (command === 'delete') {
    // 删除好友 - 弹出确认对话框
    try {
      await ElMessageBox.confirm(
        `确定要删除好友 ${friend.nickname} 吗？删除后将无法聊天，对方也会从好友列表中移除。`,
        '删除好友',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }
      )
      
      // 调用删除API
      await request.delete(`/friend/${friend.userId}`)
      
      ElMessage.success('已删除好友')
      
      // 重新加载好友列表
      const res = await getFriendList()
      friends.value = res.data
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除好友失败:', error)
        ElMessage.error('删除好友失败')
      }
    }
  }
}

// 处理群组操作
const handleGroupAction = async (command, group) => {
  if (command === 'chat') {
    // 发送群消息 - 跳转到聊天页面，传递群组ID和聊天类型
    router.push({
      path: '/chat',
      query: {
        targetId: group.groupId,
        chatType: 2, // 2-群聊
        targetName: group.groupName,
        targetAvatar: group.avatar
      }
    })
    ElMessage.success(`即将在群 ${group.groupName} 中聊天`)
  } else if (command === 'invite') {
    // 邀请成员 - 直接打开邀请对话框
    currentGroup.value = group
    // 先加载群成员列表用于过滤
    try {
      const res = await getGroupMembers(group.groupId)
      groupMembers.value = res.data || []
      openInviteMembersDialog()
    } catch (error) {
      console.error('获取群成员失败:', error)
      ElMessage.error('获取群成员失败')
    }
  } else if (command === 'info') {
    // 查看群信息
    await showGroupInfo(group.groupId)
  } else if (command === 'members') {
    // 成员管理
    await showGroupMembers(group.groupId)
  } else if (command === 'exit') {
    // 退出群组 - 需要检查用户角色
    await handleGroupExit(group)
  }
}

// 加载群组列表
const loadGroupList = async () => {
  try {
    const res = await getGroupList()
    groups.value = res.data || []
    console.log('群组列表加载成功:', groups.value)
  } catch (error) {
    console.error('加载群组列表失败:', error)
    ElMessage.error('加载群组列表失败')
  }
}

// 查看群组详情
const showGroupInfo = async (groupId) => {
  try {
    const res = await getGroupDetail(groupId)
    currentGroup.value = res.data
    editingGroupInfo.value = false
    showGroupInfoDialog.value = true
  } catch (error) {
    console.error('获取群组详情失败:', error)
    ElMessage.error('获取群组详情失败')
  }
}

// 开始编辑群信息
const startEditGroupInfo = () => {
  groupEditForm.value = {
    groupName: currentGroup.value.groupName,
    avatar: currentGroup.value.avatar || '',
    notice: currentGroup.value.notice || '',
    maxMembers: currentGroup.value.maxMembers
  }
  editingGroupInfo.value = true
}

// 取消编辑群信息
const cancelEditGroupInfo = () => {
  editingGroupInfo.value = false
}

// 保存群信息
const saveGroupInfo = async () => {
  if (!groupEditForm.value.groupName || !groupEditForm.value.groupName.trim()) {
    ElMessage.warning('群名称不能为空')
    return
  }
  
  try {
    savingGroupInfo.value = true
    
    await updateGroup(currentGroup.value.groupId, groupEditForm.value)
    
    ElMessage.success('群组信息修改成功')
    
    // 重新加载群组详情
    const res = await getGroupDetail(currentGroup.value.groupId)
    currentGroup.value = res.data
    
    // 刷新群组列表
    loadGroupList()
    
    editingGroupInfo.value = false
  } catch (error) {
    console.error('修改群组信息失败:', error)
    ElMessage.error(error.response?.data?.message || '修改群组信息失败')
  } finally {
    savingGroupInfo.value = false
  }
}

// 显示群组成员管理
const showGroupMembers = async (groupId) => {
  try {
    const res = await getGroupMembers(groupId)
    groupMembers.value = res.data || []
    
    // 获取当前用户在群组中的角色
    const currentUserId = getCurrentUserId() // 需要实现获取当前用户ID的方法
    const currentMember = groupMembers.value.find(m => m.userId === currentUserId)
    currentUserRole.value = currentMember ? currentMember.role : 0
    
    currentGroup.value = groups.value.find(g => g.groupId === groupId)
    showGroupMembersDialog.value = true
  } catch (error) {
    console.error('获取群组成员失败:', error)
    ElMessage.error('获取群组成员失败')
  }
}

// 设置/取消管理员
const setGroupAdmin = async (groupId, userId, isAdmin) => {
  try {
    await setGroupAdminApi(groupId, userId, isAdmin)
    
    ElMessage.success(isAdmin ? '设置管理员成功' : '取消管理员成功')
    
    // 刷新成员列表
    await showGroupMembers(groupId)
  } catch (error) {
    console.error('设置管理员失败:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

// 移除群成员
const removeMember = async (groupId, userId, nickname) => {
  try {
    await ElMessageBox.confirm(
      `确定要将 ${nickname} 移出群组吗？`,
      '移除成员',
      {
        confirmButtonText: '确定移除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    // 调用移除成员API
    await removeMemberApi(groupId, userId)
    
    ElMessage.success('已移除该成员')
    
    // 刷新成员列表
    await showGroupMembers(groupId)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移除成员失败:', error)
      ElMessage.error(error.response?.data?.message || '移除成员失败')
    }
  }
}

// 打开邀请成员对话框
const openInviteMembersDialog = () => {
  selectedInviteMembers.value = []
  showInviteMembersDialog.value = true
}

// 处理邀请成员
const handleInviteMembers = async () => {
  if (selectedInviteMembers.value.length === 0) {
    ElMessage.warning('请选择要邀请的好友')
    return
  }
  
  if (!currentGroup.value) {
    ElMessage.error('群组信息不存在')
    return
  }
  
  try {
    inviteMembersLoading.value = true
    
    await inviteToGroup(currentGroup.value.groupId, selectedInviteMembers.value)
    
    ElMessage.success(`已向 ${selectedInviteMembers.value.length} 人发送群组邀请`)
    
    // 关闭对话框
    showInviteMembersDialog.value = false
    selectedInviteMembers.value = []
    
    // 刷新成员列表（稍后刷新，等待对方同意）
    setTimeout(() => {
      if (currentGroup.value) {
        showGroupMembers(currentGroup.value.groupId)
      }
    }, 1000)
  } catch (error) {
    console.error('邀请成员失败:', error)
    ElMessage.error(error.response?.data?.message || '邀请成员失败')
  } finally {
    inviteMembersLoading.value = false
  }
}

// 获取当前用户ID
const getCurrentUserId = () => {
  return userStore.userInfo?.userId || null
}

// 获取角色显示文本
const getRoleText = (role) => {
  switch (role) {
    case 2: return '群主'
    case 1: return '管理员'
    default: return '成员'
  }
}

// 获取角色标签类型
const getRoleType = (role) => {
  switch (role) {
    case 2: return 'danger'
    case 1: return 'warning'
    default: return 'info'
  }
}

// 处理群组退出
const handleGroupExit = async (group) => {
  try {
    // 首先获取当前用户在群组中的角色
    const membersRes = await request.get(`/group/${group.groupId}/members`)
    const members = membersRes.data || []
    
    const currentUserId = getCurrentUserId()
    const currentMember = members.find(m => m.userId === currentUserId)
    
    if (!currentMember) {
      ElMessage.error('您不是该群组成员')
      return
    }
    
    if (currentMember.role === 2) {
      // 群主退出 = 解散群组
      await ElMessageBox.confirm(
        `您是群主，退出群组将解散整个群组，所有成员都将被移除。\n\n确定要解散群组 ${group.groupName} 吗？`,
        '解散群组',
        {
          confirmButtonText: '确定解散',
          cancelButtonText: '取消',
          type: 'error',
          dangerouslyUseHTMLString: true
        }
      )
      
      // 调用解散群组接口
      await dissolveGroup(group.groupId)
      ElMessage.success('群组已解散')
    } else {
      // 普通成员/管理员退出
      await ElMessageBox.confirm(
        `确定要退出群组 ${group.groupName} 吗？退出后将无法接收群消息。`,
        '退出群组',
        {
          confirmButtonText: '确定退出',
          cancelButtonText: '取消',
          type: 'warning',
        }
      )
      
      // 调用退出群组接口
      await quitGroup(group.groupId)
      ElMessage.success('退出群组成功')
    }
    
    // 刷新群组列表
    await loadGroupList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('群组操作失败:', error)
      if (error.response?.data?.message) {
        ElMessage.error(error.response.data.message)
      } else {
        ElMessage.error('操作失败')
      }
    }
  }
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
  if (diff < 2592000000) return `${Math.floor(diff / 86400000)}天前`
  
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 格式化完整时间
const formatDateTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

onMounted(async () => {
  // 加载好友申请列表
  loadFriendRequests()
  
  // 加载群组邀请列表
  loadGroupInvitations()
  
  // 加载群消息通知
  loadGroupNotifications()
  
  // 加载好友列表（默认显示）
  try {
    const res = await getFriendList()
    friends.value = res.data
  } catch (error) {
    console.error('加载好友列表失败:', error)
  }
  
  // 加载群组列表
  loadGroupList()
  
  // 监听WebSocket群组通知
  window.addEventListener('groupNotification', (event) => {
    const notification = event.detail
    // 过滤出需要显示在群消息通知中的类型
    const displayTypes = [
      'GROUP_ADMIN_CHANGE',
      'GROUP_MEMBER_REMOVED', 
      'GROUP_MEMBER_QUIT',
      'GROUP_DIRECT_JOIN',
      'GROUP_DISSOLVED'
    ]
    
    if (displayTypes.includes(notification.type)) {
      addGroupNotification(notification)
    }
  })
})
</script>

<style scoped>
.contacts-container {
  display: flex;
  height: 100%;
}
.contacts-sidebar {
  width: 200px;
  background: white;
  border-right: 1px solid #e0e0e0;
}
.contacts-content {
  flex: 1;
  background: #f5f5f5;
  padding: 20px;
}
.content-header {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
.contact-item {
  display: flex;
  align-items: center;
  padding: 12px;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}
.contact-item:hover {
  background: #f5f5f5;
}
.contact-item:hover .more-btn {
  opacity: 1;
}
.contact-name {
  margin-left: 12px;
  font-size: 14px;
  flex: 1;
}
.group-member-count {
  font-size: 12px;
  color: #909399;
  margin-right: 8px;
}
.more-btn {
  opacity: 0;
  transition: opacity 0.2s;
  margin-left: auto;
  font-size: 18px;
  color: #909399;
}
.more-btn:hover {
  color: #409eff;
}
/* 搜索结果样式 */
.search-results {
  max-height: 300px;
  overflow-y: auto;
}
.search-result-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}
.search-result-item:hover {
  background: #f5f5f5;
  border-color: #409eff;
}
.search-result-item.selected {
  background: #ecf5ff;
  border-color: #409eff;
}
.user-info {
  flex: 1;
  margin-left: 12px;
}
.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}
.user-id {
  font-size: 12px;
  color: #909399;
}
.check-icon {
  color: #409eff;
  font-size: 20px;
}

/* 好友申请样式 */
.empty-requests {
  padding: 60px 20px;
}

.request-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: white;
  margin-bottom: 10px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  transition: all 0.3s;
}

.request-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.request-info {
  flex: 1;
  margin-left: 12px;
  overflow: hidden;
}

.request-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.request-message {
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.request-time {
  font-size: 12px;
  color: #909399;
}

.request-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 创建群组样式 */
.member-selection {
  max-height: 300px;
  overflow-y: auto;
}

.member-item {
  padding: 8px;
  border-radius: 6px;
  margin-bottom: 8px;
  transition: background 0.2s;
}

.member-item:hover {
  background: #f5f7fa;
}

.member-item .el-checkbox {
  width: 100%;
}

.member-item .el-avatar {
  margin-left: 8px;
  margin-right: 8px;
}

.member-name {
  font-size: 14px;
  color: #303133;
}

/* 群组详情样式 */
.group-info {
  padding: 10px;
}

.info-section {
  margin-bottom: 20px;
  text-align: center;
}

.info-section:last-child {
  text-align: left;
}

.info-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.notice-item {
  flex-direction: column;
  align-items: flex-start;
}

.info-label {
  font-size: 14px;
  color: #909399;
  width: 100px;
  flex-shrink: 0;
}

.info-value {
  font-size: 14px;
  color: #303133;
  flex: 1;
}

.notice-content {
  margin-top: 8px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  line-height: 1.6;
  white-space: pre-wrap;
  width: 100%;
}

/* 群组成员管理样式 */
.group-members {
  padding: 10px;
}

.members-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e0e0e0;
}

.members-header h4 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.member-count {
  font-size: 13px;
  color: #909399;
  margin-left: auto;
  margin-right: 10px;
}

.member-list {
  padding: 0 10px;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.member-item:hover {
  background: #f5f7fa;
  border-radius: 6px;
  padding-left: 8px;
  padding-right: 8px;
}

.member-item:last-child {
  border-bottom: none;
}

.member-info {
  flex: 1;
  margin-left: 12px;
}

.member-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.member-join-time {
  font-size: 12px;
  color: #909399;
}

.member-role {
  margin-right: 12px;
}

.member-actions {
  display: flex;
  gap: 8px;
}

/* 邀请成员对话框样式 */
.invite-members {
  padding: 10px;
}

.friend-selection {
  max-height: 350px;
  overflow-y: auto;
  padding: 5px;
}

.friend-item {
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 8px;
  transition: background 0.2s;
}

.friend-item:hover {
  background: #f5f7fa;
}

.friend-item .el-checkbox {
  width: 100%;
}

.friend-item .el-avatar {
  margin-left: 8px;
  margin-right: 10px;
  vertical-align: middle;
}

.friend-name {
  font-size: 14px;
  color: #303133;
  vertical-align: middle;
  margin-left: 8px;
}

.no-friends {
  padding: 30px 0;
  text-align: center;
}
</style>

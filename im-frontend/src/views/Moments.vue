<template>
  <div class="moments-container">
    <div class="moments-header">
      <h2>朋友圈</h2>
      <el-button type="primary" :icon="Plus" @click="showPublishDialog = true">
        发布动态
      </el-button>
    </div>
    
    <el-scrollbar class="moments-list">
      <div v-for="moment in moments" :key="moment.momentsId" class="moment-item">
        <div class="moment-user">
          <el-avatar :size="50" :src="moment.userAvatar">
            {{ moment.userName?.charAt(0) }}
          </el-avatar>
          <div class="user-info">
            <div class="user-name">{{ moment.userName }}</div>
            <div class="moment-time">{{ moment.createTime }}</div>
          </div>
        </div>
        
        <div class="moment-content">
          <p>{{ moment.content }}</p>
          <div v-if="moment.images && moment.images.length" class="moment-images">
            <el-image
              v-for="(img, index) in moment.images"
              :key="index"
              :src="img"
              fit="cover"
              class="moment-image"
            />
          </div>
        </div>
        
        <div class="moment-actions">
          <el-button text :icon="ChatDotRound">评论</el-button>
          <el-button text :icon="Star">点赞 {{ moment.likeCount }}</el-button>
        </div>
      </div>
    </el-scrollbar>
    
    <!-- 发布动态对话框 -->
    <el-dialog v-model="showPublishDialog" title="发布动态" width="500px">
      <el-input
        v-model="publishContent"
        type="textarea"
        :rows="5"
        placeholder="分享新鲜事..."
      />
      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" @click="handlePublish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const showPublishDialog = ref(false)
const publishContent = ref('')
const moments = ref([])

const handlePublish = () => {
  console.log('发布动态:', publishContent.value)
  showPublishDialog.value = false
  publishContent.value = ''
}
</script>

<style scoped>
.moments-container {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f5f5;
}
.moments-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
}
.moment-item {
  background: white;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 8px;
}
.moment-user {
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
.moment-time {
  color: #999;
  font-size: 12px;
}
.moment-content {
  margin-bottom: 15px;
}
.moment-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 10px;
}
.moment-image {
  width: 100%;
  height: 150px;
  border-radius: 4px;
}
.moment-actions {
  display: flex;
  gap: 20px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
}
</style>

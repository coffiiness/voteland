<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { voteApi } from '@/api'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { logout } = useAuth()

const votes = ref([])
const loading = ref(true)
const activeTab = ref('all')

const tabs = [
  { id: 'all', label: '전체' },
  { id: 'open', label: '진행중' },
  { id: 'closed', label: '마감됨' }
]

const filteredVotes = computed(() => {
  if (activeTab.value === 'all') return votes.value
  if (activeTab.value === 'open') return votes.value.filter(v => v.voteStatus === 'OPEN')
  if (activeTab.value === 'closed') return votes.value.filter(v => v.voteStatus === 'CLOSED')
  return votes.value
})

const fetchVotes = async () => {
  try {
    const response = await voteApi.getVotes()
    votes.value = response.data.data || response.data || []
  } catch (error) {
    console.error('Failed to fetch votes:', error)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours().toString().padStart(2, '0')
  const minute = date.getMinutes().toString().padStart(2, '0')
  return `${month}월 ${day}일 ${hour}:${minute}`
}

const goToVote = (vote) => {
  if (vote.voteStatus === 'CLOSED') {
    router.push(`/votes/${vote.id}/result`)
  } else {
    router.push(`/votes/${vote.id}`)
  }
}

onMounted(fetchVotes)
</script>

<template>
  <div class="min-h-screen bg-white">
    <!-- Header -->
    <header class="border-b border-gray-200">
      <div class="max-w-3xl mx-auto px-4 py-4 flex items-center justify-between">
        <h1 class="text-xl font-bold text-gray-900">VoteLand</h1>
        <div class="flex items-center gap-3">
          <button
            @click="logout"
            class="text-sm text-gray-500 hover:text-gray-700"
          >
            로그아웃
          </button>
          <router-link
            to="/votes/create"
            class="px-4 py-2 bg-gray-900 text-white text-sm font-medium rounded-lg hover:bg-gray-800 transition"
          >
            투표 만들기
          </router-link>
        </div>
      </div>
    </header>

    <!-- Content -->
    <main class="max-w-3xl mx-auto px-4 py-6">
      <!-- Tabs -->
      <div class="flex gap-2 mb-6">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          @click="activeTab = tab.id"
          :class="[
            'px-4 py-2 text-sm font-medium rounded-lg transition',
            activeTab === tab.id
              ? 'bg-gray-900 text-white'
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          ]"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="text-center py-12 text-gray-500">
        불러오는 중...
      </div>

      <!-- Empty State -->
      <div v-else-if="filteredVotes.length === 0" class="text-center py-12 text-gray-500">
        투표가 없습니다.
      </div>

      <!-- Vote List -->
      <div v-else class="space-y-4">
        <div
          v-for="vote in filteredVotes"
          :key="vote.id"
          @click="goToVote(vote)"
          class="border border-gray-200 rounded-xl p-5 cursor-pointer hover:border-gray-300 transition"
        >
          <div class="flex items-start justify-between mb-3">
            <span
              :class="[
                'px-2.5 py-1 text-xs font-medium rounded',
                vote.voteStatus === 'OPEN'
                  ? 'bg-gray-900 text-white'
                  : 'bg-gray-100 text-gray-500'
              ]"
            >
              {{ vote.voteStatus === 'OPEN' ? '진행중' : '마감' }}
            </span>
            <span class="text-sm text-gray-500">
              {{ formatDate(vote.createdAt) }}
            </span>
          </div>

          <h3 class="text-lg font-semibold text-gray-900 mb-1">
            {{ vote.title }}
          </h3>
          <p class="text-gray-500 text-sm mb-3">
            {{ vote.description }}
          </p>

          <div class="text-sm text-gray-400">
            {{ vote.optionCount || 0 }}개 선택지 | {{ vote.voterCount || 0 }}명 참여
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

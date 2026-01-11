<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { voteApi } from '@/api'

const route = useRoute()
const router = useRouter()

const vote = ref(null)
const selectedOptions = ref([])
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const remainingTime = ref('')
let timer = null

const isSingleChoice = computed(() => vote.value?.voteType === 'SINGLE')

const fetchVote = async () => {
  try {
    const response = await voteApi.getVote(route.params.id)
    vote.value = response.data.data || response.data

    if (vote.value.status === 'CLOSED') {
      router.replace(`/votes/${route.params.id}/result`)
    }
  } catch (e) {
    error.value = '투표를 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

const updateRemainingTime = () => {
  if (!vote.value?.deadline) return

  const deadline = new Date(vote.value.deadline)
  const now = new Date()
  const diff = deadline - now

  if (diff <= 0) {
    remainingTime.value = '마감됨'
    clearInterval(timer)
    return
  }

  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const seconds = Math.floor((diff % (1000 * 60)) / 1000)

  if (hours > 0) {
    remainingTime.value = `${hours}시간 ${minutes}분 남음`
  } else {
    remainingTime.value = `${minutes}분 ${seconds}초 남음`
  }
}

const formatDeadline = (deadline) => {
  if (!deadline) return ''
  const date = new Date(deadline)
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours().toString().padStart(2, '0')
  const minute = date.getMinutes().toString().padStart(2, '0')
  return `${month}월 ${day}일 ${hour}:${minute}`
}

const selectOption = (optionId) => {
  if (isSingleChoice.value) {
    selectedOptions.value = [optionId]
  } else {
    const index = selectedOptions.value.indexOf(optionId)
    if (index > -1) {
      selectedOptions.value.splice(index, 1)
    } else {
      selectedOptions.value.push(optionId)
    }
  }
}

const handleSubmit = async () => {
  if (selectedOptions.value.length === 0) {
    error.value = '선택지를 선택해주세요.'
    return
  }

  submitting.value = true
  error.value = ''

  try {
    await voteApi.submitVote(route.params.id, selectedOptions.value)
    router.push(`/votes/${route.params.id}/result`)
  } catch (e) {
    error.value = e.response?.data?.message || '투표에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchVote()
  timer = setInterval(updateRemainingTime, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="min-h-screen bg-white">
    <!-- Header -->
    <header class="border-b border-gray-200">
      <div class="max-w-2xl mx-auto px-4 py-4 flex items-center justify-between">
        <router-link
          to="/votes"
          class="px-3 py-1.5 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 transition"
        >
          목록으로
        </router-link>
        <span class="text-lg font-medium">투표 참여</span>
        <div class="w-16"></div>
      </div>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="max-w-2xl mx-auto px-4 py-12 text-center text-gray-500">
      불러오는 중...
    </div>

    <!-- Content -->
    <main v-else-if="vote" class="max-w-2xl mx-auto px-4 py-8">
      <!-- Status Badge -->
      <span class="inline-block px-2.5 py-1 text-xs font-medium rounded bg-gray-900 text-white mb-4">
        진행중
      </span>

      <!-- Title & Description -->
      <h1 class="text-2xl font-bold text-gray-900 mb-2">{{ vote.title }}</h1>
      <p class="text-gray-500 mb-6">{{ vote.description }}</p>

      <!-- Deadline Info -->
      <div class="bg-gray-50 rounded-xl p-5 mb-8">
        <div class="flex justify-between items-center">
          <div>
            <div class="text-sm text-gray-500 mb-1">마감</div>
            <div class="font-medium text-gray-900">{{ formatDeadline(vote.deadline) }}</div>
          </div>
          <div class="text-right">
            <div class="text-sm text-gray-500 mb-1">남은 시간</div>
            <div class="font-medium text-gray-900">{{ remainingTime }}</div>
          </div>
        </div>
      </div>

      <!-- Options -->
      <div class="mb-8">
        <div class="text-sm text-gray-500 mb-4">
          {{ isSingleChoice ? '하나를 선택해주세요' : '여러 개를 선택할 수 있습니다' }}
        </div>

        <div class="space-y-3">
          <button
            v-for="option in vote.options"
            :key="option.id"
            type="button"
            @click="selectOption(option.id)"
            :class="[
              'w-full flex items-center gap-4 p-4 border rounded-xl text-left transition',
              selectedOptions.includes(option.id)
                ? 'border-gray-900 bg-gray-50'
                : 'border-gray-200 hover:border-gray-300'
            ]"
          >
            <div
              :class="[
                'w-5 h-5 rounded-full border-2 flex items-center justify-center transition',
                selectedOptions.includes(option.id)
                  ? 'border-gray-900 bg-gray-900'
                  : 'border-gray-300'
              ]"
            >
              <div
                v-if="selectedOptions.includes(option.id)"
                class="w-2 h-2 bg-white rounded-full"
              ></div>
            </div>
            <span class="font-medium text-gray-900">{{ option.content }}</span>
          </button>
        </div>
      </div>

      <!-- Error -->
      <div v-if="error" class="text-red-500 text-sm text-center mb-4">
        {{ error }}
      </div>

      <!-- Submit Button -->
      <button
        @click="handleSubmit"
        :disabled="submitting || selectedOptions.length === 0"
        class="w-full py-4 bg-gray-900 text-white font-medium rounded-xl hover:bg-gray-800 disabled:opacity-50 disabled:cursor-not-allowed transition"
      >
        {{ submitting ? '투표 중...' : '투표하기' }}
      </button>

      <!-- Voter Count -->
      <div class="mt-6 text-center text-sm text-gray-500">
        현재 {{ vote.voterCount || 0 }}명 참여
      </div>
    </main>
  </div>
</template>

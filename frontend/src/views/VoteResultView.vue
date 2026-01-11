<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { voteApi } from '@/api'

const route = useRoute()

const vote = ref(null)
const loading = ref(true)
const error = ref('')
const lastUpdated = ref('')

const totalVotes = computed(() => {
  if (!vote.value?.options) return 0
  return vote.value.options.reduce((sum, opt) => sum + (opt.voteCount || 0), 0)
})

const sortedOptions = computed(() => {
  if (!vote.value?.options) return []
  return [...vote.value.options].sort((a, b) => (b.voteCount || 0) - (a.voteCount || 0))
})

const getPercentage = (count) => {
  if (totalVotes.value === 0) return 0
  return ((count / totalVotes.value) * 100).toFixed(1)
}

const chartColors = ['#1f2937', '#6b7280', '#9ca3af', '#d1d5db', '#e5e7eb']

const fetchResult = async () => {
  try {
    const response = await voteApi.getVoteResult(route.params.id)
    vote.value = response.data.data || response.data
    lastUpdated.value = new Date().toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch (e) {
    error.value = '결과를 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchResult)
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
        <span class="text-lg font-medium">투표 결과</span>
        <div class="flex items-center gap-1.5 text-sm text-gray-500">
          <span class="w-2 h-2 bg-red-500 rounded-full animate-pulse"></span>
          실시간
        </div>
      </div>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="max-w-2xl mx-auto px-4 py-12 text-center text-gray-500">
      불러오는 중...
    </div>

    <!-- Error -->
    <div v-else-if="error" class="max-w-2xl mx-auto px-4 py-12 text-center text-red-500">
      {{ error }}
    </div>

    <!-- Content -->
    <main v-else-if="vote" class="max-w-2xl mx-auto px-4 py-8">
      <!-- Title & Description -->
      <h1 class="text-2xl font-bold text-gray-900 mb-2">{{ vote.title }}</h1>
      <p class="text-gray-500 mb-6">{{ vote.description }}</p>

      <!-- Stats Box -->
      <div class="bg-gray-50 rounded-xl p-5 mb-8">
        <div class="flex justify-between items-center">
          <div>
            <div class="text-sm text-gray-500 mb-1">총 참여상태</div>
            <div class="font-medium text-gray-900">
              <span class="text-xl font-bold">{{ totalVotes }}</span>명
              {{ vote.status === 'OPEN' ? '진행중' : '마감' }}
            </div>
          </div>
          <div class="text-right">
            <div class="text-sm text-gray-500 mb-1">마지막 업데이트</div>
            <div class="font-medium text-gray-900">{{ lastUpdated }}</div>
          </div>
        </div>
      </div>

      <!-- Results -->
      <div class="space-y-4 mb-10">
        <div
          v-for="(option, index) in sortedOptions"
          :key="option.id"
          class="border border-gray-200 rounded-xl p-5"
        >
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center gap-3">
              <span class="text-xl font-bold text-gray-400">{{ index + 1 }}</span>
              <span
                v-if="index === 0 && (option.voteCount || 0) > 0"
                class="px-2 py-0.5 text-xs font-medium bg-gray-900 text-white rounded"
              >
                1위
              </span>
              <span class="font-medium text-gray-900">{{ option.content }}</span>
            </div>
            <div class="text-right">
              <span class="text-xl font-bold text-gray-900">{{ option.voteCount || 0 }}표</span>
              <span class="text-gray-500 ml-2">{{ getPercentage(option.voteCount || 0) }}%</span>
            </div>
          </div>

          <!-- Progress Bar -->
          <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
            <div
              class="h-full bg-gray-900 rounded-full transition-all duration-500"
              :style="{ width: `${getPercentage(option.voteCount || 0)}%` }"
            ></div>
          </div>
        </div>
      </div>

      <!-- Pie Chart Section -->
      <div class="border-t border-gray-200 pt-8">
        <h3 class="font-medium text-gray-900 mb-6">비율 차트</h3>

        <div class="flex items-center justify-center gap-8">
          <!-- Simple Donut Chart -->
          <div class="relative w-40 h-40">
            <svg viewBox="0 0 100 100" class="w-full h-full -rotate-90">
              <circle
                cx="50"
                cy="50"
                r="40"
                fill="none"
                stroke="#e5e7eb"
                stroke-width="20"
              />
              <circle
                v-for="(option, index) in sortedOptions"
                :key="option.id"
                cx="50"
                cy="50"
                r="40"
                fill="none"
                :stroke="chartColors[index % chartColors.length]"
                stroke-width="20"
                :stroke-dasharray="`${(option.voteCount || 0) / totalVotes * 251.2} 251.2`"
                :stroke-dashoffset="-sortedOptions.slice(0, index).reduce((sum, o) => sum + (o.voteCount || 0) / totalVotes * 251.2, 0)"
              />
            </svg>
            <div class="absolute inset-0 flex flex-col items-center justify-center">
              <span class="text-xs text-gray-500">총 참여</span>
              <span class="text-2xl font-bold text-gray-900">{{ totalVotes }}</span>
            </div>
          </div>

          <!-- Legend -->
          <div class="space-y-2">
            <div
              v-for="(option, index) in sortedOptions"
              :key="option.id"
              class="flex items-center gap-2 text-sm"
            >
              <div
                class="w-3 h-3 rounded-sm"
                :style="{ backgroundColor: chartColors[index % chartColors.length] }"
              ></div>
              <span class="text-gray-600">{{ option.content }}</span>
              <span class="text-gray-900 font-medium">
                {{ option.voteCount || 0 }}표 ({{ getPercentage(option.voteCount || 0) }}%)
              </span>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

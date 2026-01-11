<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { voteApi } from '@/api'

const router = useRouter()

const title = ref('')
const description = ref('')
const options = ref(['', ''])
const voteType = ref('SINGLE')
const deadlineDate = ref('')
const deadlineTime = ref('')
const loading = ref(false)
const error = ref('')

const addOption = () => {
  options.value.push('')
}

const removeOption = (index) => {
  if (options.value.length > 2) {
    options.value.splice(index, 1)
  }
}

const handleSubmit = async () => {
  error.value = ''

  const validOptions = options.value.filter(o => o.trim())
  if (validOptions.length < 2) {
    error.value = '최소 2개의 선택지가 필요합니다.'
    return
  }

  if (!deadlineDate.value || !deadlineTime.value) {
    error.value = '마감 일시를 입력해주세요.'
    return
  }

  loading.value = true

  try {
    const deadline = `${deadlineDate.value}T${deadlineTime.value}:00`

    await voteApi.createVote({
      title: title.value,
      description: description.value,
      options: validOptions,
      voteType: voteType.value,
      deadline
    })

    router.push('/votes')
  } catch (e) {
    error.value = e.response?.data?.message || '투표 생성에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
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
        <span class="text-lg font-medium">투표 만들기</span>
        <div class="w-16"></div>
      </div>
    </header>

    <!-- Content -->
    <main class="max-w-2xl mx-auto px-4 py-8">
      <div class="mb-8">
        <h2 class="text-2xl font-bold text-gray-900 mb-2">새 투표 만들기</h2>
        <p class="text-gray-500">투표를 생성하고 링크를 공유하세요</p>
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-8">
        <!-- 제목 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">제목</label>
          <input
            v-model="title"
            type="text"
            required
            placeholder="점심 메뉴 투표"
            class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent transition"
          />
        </div>

        <!-- 설명 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">
            설명
            <span class="text-gray-400 font-normal">선택사항</span>
          </label>
          <textarea
            v-model="description"
            rows="3"
            placeholder="오늘 점심 뭐 먹을까요? 다수결로 결정합니다."
            class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent transition resize-none"
          ></textarea>
        </div>

        <!-- 선택지 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-3">선택지</label>
          <div class="space-y-3">
            <div
              v-for="(option, index) in options"
              :key="index"
              class="flex items-center gap-3"
            >
              <span class="w-6 text-gray-400 text-sm">{{ index + 1 }}</span>
              <input
                v-model="options[index]"
                type="text"
                :placeholder="`선택지 ${index + 1}`"
                class="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent transition"
              />
              <button
                v-if="options.length > 2"
                type="button"
                @click="removeOption(index)"
                class="p-2 text-gray-400 hover:text-gray-600 transition"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
              </button>
            </div>
          </div>

          <button
            type="button"
            @click="addOption"
            class="mt-3 w-full py-3 border border-dashed border-gray-300 rounded-lg text-gray-500 hover:border-gray-400 hover:text-gray-600 transition"
          >
            + 선택지 추가
          </button>
        </div>

        <!-- 투표 방식 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-3">투표 방식</label>
          <div class="grid grid-cols-2 gap-3">
            <button
              type="button"
              @click="voteType = 'SINGLE'"
              :class="[
                'py-4 px-4 border rounded-lg text-left transition',
                voteType === 'SINGLE'
                  ? 'border-gray-900 bg-gray-50'
                  : 'border-gray-300 hover:border-gray-400'
              ]"
            >
              <div class="font-medium text-gray-900">단일 선택</div>
              <div class="text-sm text-gray-500 mt-1">하나만 선택</div>
            </button>
            <button
              type="button"
              @click="voteType = 'MULTIPLE'"
              :class="[
                'py-4 px-4 border rounded-lg text-left transition',
                voteType === 'MULTIPLE'
                  ? 'border-gray-900 bg-gray-50'
                  : 'border-gray-300 hover:border-gray-400'
              ]"
            >
              <div class="font-medium text-gray-900">복수 선택</div>
              <div class="text-sm text-gray-500 mt-1">여러 개 선택</div>
            </button>
          </div>
        </div>

        <!-- 마감 일시 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-3">마감 일시</label>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs text-gray-500 mb-1">날짜</label>
              <input
                v-model="deadlineDate"
                type="date"
                required
                class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent transition"
              />
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">시간</label>
              <input
                v-model="deadlineTime"
                type="time"
                required
                class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent transition"
              />
            </div>
          </div>
        </div>

        <!-- Error -->
        <div v-if="error" class="text-red-500 text-sm text-center">
          {{ error }}
        </div>

        <!-- Buttons -->
        <div class="flex gap-3 pt-4">
          <router-link
            to="/votes"
            class="flex-1 py-3 text-center border border-gray-300 text-gray-700 font-medium rounded-lg hover:bg-gray-50 transition"
          >
            취소
          </router-link>
          <button
            type="submit"
            :disabled="loading"
            class="flex-1 py-3 bg-gray-900 text-white font-medium rounded-lg hover:bg-gray-800 disabled:opacity-50 disabled:cursor-not-allowed transition"
          >
            {{ loading ? '생성 중...' : '투표 생성하기' }}
          </button>
        </div>
      </form>
    </main>
  </div>
</template>

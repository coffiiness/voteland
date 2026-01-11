import axios from 'axios'
import router from '@/router'

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  signup(data) {
    return api.post('/users/signup', data)
  },
  login(data) {
    return api.post('/users/login', data)
  },
  getMe() {
    return api.get('/users/me')
  },
  deleteMe() {
    return api.delete('/users/me')
  }
}

export const voteApi = {
  getVotes() {
    return api.get('/votes')
  },
  getVote(id) {
    return api.get(`/votes/${id}`)
  },
  createVote(data) {
    return api.post('/votes', data)
  },
  submitVote(id, optionId) {
    return api.post(`/votes/${id}/vote`, { optionId })
  },
  getVoteResult(id) {
    return api.get(`/votes/${id}/result`)
  }
}

export default api

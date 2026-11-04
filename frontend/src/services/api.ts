import axios from 'axios'
import { installSessionInterceptors } from './session'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
})

installSessionInterceptors(api)

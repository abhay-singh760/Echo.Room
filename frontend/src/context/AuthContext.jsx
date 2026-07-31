import React, { createContext, useContext, useState, useCallback } from 'react'
import api from '../api/axios.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('echoroom_user')
    return raw ? JSON.parse(raw) : null
  })
  const [token, setToken] = useState(() => localStorage.getItem('echoroom_token'))

  const persist = (authResponse) => {
    localStorage.setItem('echoroom_token', authResponse.token)
    localStorage.setItem('echoroom_user', JSON.stringify(authResponse))
    setToken(authResponse.token)
    setUser(authResponse)
  }

  const login = useCallback(async (username, password) => {
    const { data } = await api.post('/auth/login', { username, password })
    persist(data)
    return data
  }, [])

  const register = useCallback(async (username, email, password) => {
    const { data } = await api.post('/auth/register', { username, email, password })
    persist(data)
    return data
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('echoroom_token')
    localStorage.removeItem('echoroom_user')
    setToken(null)
    setUser(null)
  }, [])

  const isAdmin = user?.roles?.includes('ROLE_ADMIN')

  return (
    <AuthContext.Provider value={{ user, token, isAdmin, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

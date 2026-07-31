import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const update = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await register(form.username, form.email, form.password)
      navigate('/rooms')
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold">
            <span className="text-white">Echo</span>
            <span className="text-primary-accent">Room</span>
          </h1>
          <p className="text-slate-500 text-sm mt-2">Create your account</p>
        </div>

        <form onSubmit={handleSubmit} className="bg-surface-card border border-surface-border rounded-2xl p-6 flex flex-col gap-4">
          {error && <p className="text-sm text-red-400 bg-red-500/10 rounded-lg px-3 py-2">{error}</p>}

          <div>
            <label className="text-xs text-slate-400">Username</label>
            <input
              value={form.username}
              onChange={update('username')}
              minLength={3}
              maxLength={24}
              required
              className="w-full mt-1 bg-black/20 border border-surface-border rounded-lg px-3 py-2 text-sm outline-none focus:border-primary-accent"
            />
          </div>
          <div>
            <label className="text-xs text-slate-400">Email</label>
            <input
              type="email"
              value={form.email}
              onChange={update('email')}
              required
              className="w-full mt-1 bg-black/20 border border-surface-border rounded-lg px-3 py-2 text-sm outline-none focus:border-primary-accent"
            />
          </div>
          <div>
            <label className="text-xs text-slate-400">Password</label>
            <input
              type="password"
              value={form.password}
              onChange={update('password')}
              minLength={6}
              required
              className="w-full mt-1 bg-black/20 border border-surface-border rounded-lg px-3 py-2 text-sm outline-none focus:border-primary-accent"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="mt-2 bg-primary hover:bg-primary-light transition-colors text-white text-sm font-medium py-2.5 rounded-lg disabled:opacity-60"
          >
            {loading ? 'Creating account…' : 'Create account'}
          </button>

          <p className="text-center text-xs text-slate-500">
            Already have an account?{' '}
            <Link to="/login" className="text-primary-accent hover:underline">
              Sign in
            </Link>
          </p>
        </form>
      </div>
    </div>
  )
}

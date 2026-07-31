import React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import Avatar from './Avatar.jsx'

export default function NavBar() {
  const { user, isAdmin, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <nav className="sticky top-0 z-20 flex items-center justify-between px-6 py-3 bg-surface-card border-b border-surface-border">
      <Link to="/rooms" className="flex items-center gap-2">
        <span className="text-xl font-bold">
          <span className="text-white">Echo</span>
          <span className="text-primary-accent">Room</span>
        </span>
      </Link>

      <div className="flex items-center gap-4">
        {isAdmin && (
          <Link
            to="/admin"
            className="text-sm font-medium text-primary-accent hover:text-white transition-colors"
          >
            Admin Dashboard
          </Link>
        )}
        <div className="flex items-center gap-2">
          <Avatar username={user?.username} size={32} />
          <span className="text-sm text-slate-300">{user?.username}</span>
        </div>
        <button
          onClick={handleLogout}
          className="text-sm px-3 py-1.5 rounded-lg border border-surface-border text-slate-300 hover:bg-surface-border transition-colors"
        >
          Log out
        </button>
      </div>
    </nav>
  )
}

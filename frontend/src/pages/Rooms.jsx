import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios.js'

export default function Rooms() {
  const navigate = useNavigate()
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [showJoin, setShowJoin] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [roomCode, setRoomCode] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const loadRooms = async () => {
    setLoading(true)
    try {
      const { data } = await api.get('/rooms/mine')
      setRooms(data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadRooms()
  }, [])

  const createRoom = async (e) => {
    e.preventDefault()
    setError('')
    try {
      const { data } = await api.post('/rooms', { name, description })
      setShowCreate(false)
      setName('')
      setDescription('')
      navigate(`/rooms/${data.id}`)
    } catch (err) {
      setError(err.response?.data?.message || 'Could not create room')
    }
  }

  const joinRoom = async (e) => {
    e.preventDefault()
    setError('')
    setMessage('')
    try {
      const { data } = await api.post('/rooms/join', { roomCode: roomCode.toUpperCase() })
      setMessage(data.message)
      setRoomCode('')
    } catch (err) {
      setError(err.response?.data?.message || 'Could not send join request')
    }
  }

  return (
    <div className="max-w-4xl mx-auto px-6 py-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-white">Your Rooms</h1>
          <p className="text-slate-500 text-sm mt-1">Rooms you own or have been approved to join.</p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => { setShowJoin(true); setShowCreate(false); setError(''); setMessage('') }}
            className="px-4 py-2 rounded-lg border border-surface-border text-sm hover:bg-surface-card"
          >
            Join a room
          </button>
          <button
            onClick={() => { setShowCreate(true); setShowJoin(false); setError('') }}
            className="px-4 py-2 rounded-lg bg-primary hover:bg-primary-light text-white text-sm font-medium"
          >
            + Create room
          </button>
        </div>
      </div>

      {showCreate && (
        <form onSubmit={createRoom} className="bg-surface-card border border-surface-border rounded-xl p-4 mb-6 flex flex-col gap-3">
          {error && <p className="text-sm text-red-400">{error}</p>}
          <input
            placeholder="Room name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            className="bg-black/20 border border-surface-border rounded-lg px-3 py-2 text-sm outline-none focus:border-primary-accent"
          />
          <input
            placeholder="Description (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="bg-black/20 border border-surface-border rounded-lg px-3 py-2 text-sm outline-none focus:border-primary-accent"
          />
          <div className="flex gap-2">
            <button type="submit" className="px-4 py-2 rounded-lg bg-primary text-white text-sm font-medium">Create</button>
            <button type="button" onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-lg text-sm text-slate-400">Cancel</button>
          </div>
        </form>
      )}

      {showJoin && (
        <form onSubmit={joinRoom} className="bg-surface-card border border-surface-border rounded-xl p-4 mb-6 flex flex-col gap-3">
          {error && <p className="text-sm text-red-400">{error}</p>}
          {message && <p className="text-sm text-emerald-400">{message}</p>}
          <input
            placeholder="Room code, e.g. AB12CD"
            value={roomCode}
            onChange={(e) => setRoomCode(e.target.value)}
            required
            className="bg-black/20 border border-surface-border rounded-lg px-3 py-2 text-sm outline-none focus:border-primary-accent uppercase"
          />
          <p className="text-xs text-slate-500">The room owner must approve your request before you can chat.</p>
          <div className="flex gap-2">
            <button type="submit" className="px-4 py-2 rounded-lg bg-primary text-white text-sm font-medium">Request to join</button>
            <button type="button" onClick={() => setShowJoin(false)} className="px-4 py-2 rounded-lg text-sm text-slate-400">Cancel</button>
          </div>
        </form>
      )}

      {loading ? (
        <p className="text-slate-500 text-sm">Loading rooms…</p>
      ) : rooms.length === 0 ? (
        <div className="text-center py-16 text-slate-500">
          <p>You haven't joined any rooms yet.</p>
          <p className="text-sm mt-1">Create one or join with a room code to get started.</p>
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2">
          {rooms.map((room) => (
            <button
              key={room.id}
              onClick={() => navigate(`/rooms/${room.id}`)}
              className="text-left bg-surface-card border border-surface-border rounded-xl p-4 hover:border-primary-accent transition-colors"
            >
              <div className="flex items-center justify-between">
                <h3 className="font-semibold text-white">{room.name}</h3>
                <span className="text-[10px] font-mono text-slate-500">{room.roomCode}</span>
              </div>
              {room.description && <p className="text-sm text-slate-500 mt-1 line-clamp-2">{room.description}</p>}
              <div className="flex items-center gap-3 mt-3 text-xs text-slate-500">
                <span>{room.memberCount} members</span>
                <span className="text-emerald-400">{room.onlineCount} online</span>
                {room.pendingCount > 0 && <span className="text-amber-400">{room.pendingCount} pending</span>}
                {room.status === 'CLOSED' && <span className="text-red-400">Closed</span>}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

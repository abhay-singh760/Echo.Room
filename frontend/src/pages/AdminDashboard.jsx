import React, { useEffect, useState } from 'react'
import api from '../api/axios.js'

function StatCard({ label, value, accent }) {
  return (
    <div className="bg-surface-card border border-surface-border rounded-xl p-4">
      <p className="text-xs text-slate-500 uppercase tracking-wide">{label}</p>
      <p className={`text-2xl font-bold mt-1 ${accent || 'text-white'}`}>{value}</p>
    </div>
  )
}

export default function AdminDashboard() {
  const [stats, setStats] = useState(null)
  const [rooms, setRooms] = useState([])
  const [filter, setFilter] = useState('ALL')
  const [closing, setClosing] = useState(null)

  const load = async () => {
    const [statsRes, roomsRes] = await Promise.all([api.get('/admin/stats'), api.get('/admin/rooms')])
    setStats(statsRes.data)
    setRooms(roomsRes.data)
  }

  useEffect(() => {
    load()
  }, [])

  const closeRoom = async (roomId) => {
    const reason = prompt('Reason for closing this room (optional):') || ''
    setClosing(roomId)
    try {
      await api.post(`/admin/rooms/${roomId}/close`, { reason })
      await load()
    } finally {
      setClosing(null)
    }
  }

  const filtered = rooms.filter((r) => filter === 'ALL' || r.status === filter)

  return (
    <div className="max-w-6xl mx-auto px-6 py-8">
      <h1 className="text-2xl font-bold text-white mb-1">Admin Dashboard</h1>
      <p className="text-slate-500 text-sm mb-6">Monitor all rooms across EchoRoom and shut down abusive or inactive ones.</p>

      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 mb-8">
          <StatCard label="Users" value={stats.totalUsers} />
          <StatCard label="Total rooms" value={stats.totalRooms} />
          <StatCard label="Active rooms" value={stats.activeRooms} accent="text-emerald-400" />
          <StatCard label="Closed rooms" value={stats.closedRooms} accent="text-red-400" />
          <StatCard label="Messages" value={stats.totalMessages} />
          <StatCard label="Possibly inactive" value={stats.inactiveRoomCandidates} accent="text-amber-400" />
        </div>
      )}

      <div className="flex items-center gap-2 mb-3">
        {['ALL', 'ACTIVE', 'CLOSED'].map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`text-xs px-3 py-1.5 rounded-full border ${
              filter === f ? 'bg-primary text-white border-primary' : 'border-surface-border text-slate-400'
            }`}
          >
            {f === 'ALL' ? 'All rooms' : f.charAt(0) + f.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      <div className="bg-surface-card border border-surface-border rounded-xl overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-xs text-slate-500 uppercase tracking-wide border-b border-surface-border">
              <th className="px-4 py-3">Room</th>
              <th className="px-4 py-3">Code</th>
              <th className="px-4 py-3">Owner</th>
              <th className="px-4 py-3">Members</th>
              <th className="px-4 py-3">Online</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Last activity</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((room) => (
              <tr key={room.id} className="border-b border-surface-border last:border-0">
                <td className="px-4 py-3 font-medium text-white">{room.name}</td>
                <td className="px-4 py-3 font-mono text-xs text-slate-500">{room.roomCode}</td>
                <td className="px-4 py-3 text-slate-400">{room.ownerUsername}</td>
                <td className="px-4 py-3 text-slate-400">{room.memberCount}</td>
                <td className="px-4 py-3 text-emerald-400">{room.onlineCount}</td>
                <td className="px-4 py-3">
                  <span className={`text-xs px-2 py-0.5 rounded-full ${room.status === 'ACTIVE' ? 'bg-emerald-400/10 text-emerald-400' : 'bg-red-400/10 text-red-400'}`}>
                    {room.status}
                  </span>
                </td>
                <td className="px-4 py-3 text-xs text-slate-500">
                  {room.lastActivityAt ? new Date(room.lastActivityAt).toLocaleString() : '—'}
                </td>
                <td className="px-4 py-3 text-right">
                  {room.status === 'ACTIVE' && (
                    <button
                      onClick={() => closeRoom(room.id)}
                      disabled={closing === room.id}
                      className="text-xs px-3 py-1 rounded-lg bg-red-500/10 text-red-400 hover:bg-red-500/20 disabled:opacity-50"
                    >
                      {closing === room.id ? 'Closing…' : 'Close room'}
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-center text-slate-600">No rooms match this filter.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

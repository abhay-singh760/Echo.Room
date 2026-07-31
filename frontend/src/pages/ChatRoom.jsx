import React, { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import api from '../api/axios.js'
import { useAuth } from '../context/AuthContext.jsx'
import { createStompClient } from '../socket/stompClient.js'
import MessageBubble from '../components/MessageBubble.jsx'
import TypingIndicator from '../components/TypingIndicator.jsx'
import JoinRequests from '../components/JoinRequests.jsx'
import Avatar from '../components/Avatar.jsx'

const TYPING_TIMEOUT_MS = 2500

export default function ChatRoom() {
  const { roomId } = useParams()
  const { user, token } = useAuth()
  const navigate = useNavigate()

  const [room, setRoom] = useState(null)
  const [messages, setMessages] = useState([])
  const [draft, setDraft] = useState('')
  const [typingUsers, setTypingUsers] = useState([])
  const [connected, setConnected] = useState(false)
  const [banner, setBanner] = useState('')
  const [showRoster, setShowRoster] = useState(false)
  const [uploading, setUploading] = useState(false)

  const clientRef = useRef(null)
  const scrollRef = useRef(null)
  const typingTimeoutRef = useRef(null)
  const fileInputRef = useRef(null)

  const isOwner = room && user && room.ownerUsername === user.username

  const loadRoom = useCallback(async () => {
    try {
      const { data } = await api.get(`/rooms/${roomId}`)
      setRoom(data)
      setMessages(data.messages || [])
    } catch (err) {
      setBanner(err.response?.data?.message || 'Could not load room')
    }
  }, [roomId])

  useEffect(() => {
    loadRoom()
  }, [loadRoom])

  useEffect(() => {
    if (!token) return

    const client = createStompClient(token, {
      onConnect: (c) => {
        setConnected(true)

        c.subscribe(`/topic/room/${roomId}`, (frame) => {
          const msg = JSON.parse(frame.body)
          setMessages((prev) => {
            const idx = prev.findIndex((m) => m.id === msg.id)
            if (idx === -1) return [...prev, msg]
            const next = [...prev]
            next[idx] = msg
            return next
          })
        })

        c.subscribe(`/topic/room/${roomId}/typing`, (frame) => {
          const evt = JSON.parse(frame.body)
          setTypingUsers((prev) => {
            const others = prev.filter((u) => u !== evt.username)
            return evt.typing && evt.username !== user.username ? [...others, evt.username] : others
          })
        })

        c.subscribe(`/topic/room/${roomId}/presence`, () => {
          loadRoom()
        })

        c.subscribe(`/topic/room/${roomId}/roster`, (frame) => {
          setRoom(JSON.parse(frame.body))
        })

        c.subscribe('/user/queue/room-events', (frame) => {
          const evt = JSON.parse(frame.body)
          setBanner(evt.message)
          if (evt.type === 'JOIN_REQUEST' && evt.roomId === roomId) loadRoom()
          if (evt.type === 'MEMBER_REMOVED' || evt.type === 'JOIN_DENIED') {
            setTimeout(() => navigate('/rooms'), 1500)
          }
        })

        c.publish({
          destination: `/app/room/${roomId}/presence`,
          body: JSON.stringify({ roomId, online: true }),
        })
      },
      onDisconnect: () => setConnected(false),
    })

    client.activate()
    clientRef.current = client

    return () => {
      if (client.connected) {
        client.publish({
          destination: `/app/room/${roomId}/presence`,
          body: JSON.stringify({ roomId, online: false }),
        })
      }
      client.deactivate()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId, token])

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages])

  const sendTyping = (isTyping) => {
    clientRef.current?.publish({
      destination: `/app/room/${roomId}/typing`,
      body: JSON.stringify({ roomId, typing: isTyping }),
    })
  }

  const handleDraftChange = (e) => {
    setDraft(e.target.value)
    sendTyping(true)
    if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current)
    typingTimeoutRef.current = setTimeout(() => sendTyping(false), TYPING_TIMEOUT_MS)
  }

  const sendMessage = (e) => {
    e.preventDefault()
    if (!draft.trim() || !clientRef.current?.connected) return
    clientRef.current.publish({
      destination: `/app/room/${roomId}/send`,
      body: JSON.stringify({ roomId, type: 'TEXT', content: draft.trim() }),
    })
    setDraft('')
    sendTyping(false)
  }

  const handleFileUpload = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    try {
      const formData = new FormData()
      formData.append('file', file)
      const { data } = await api.post('/files/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      const isImage = /\.(png|jpe?g|gif|webp)$/i.test(data.name)
      clientRef.current?.publish({
        destination: `/app/room/${roomId}/send`,
        body: JSON.stringify({
          roomId,
          type: isImage ? 'IMAGE' : 'FILE',
          attachmentUrl: data.url,
          attachmentName: data.name,
        }),
      })
    } catch (err) {
      setBanner(err.response?.data?.message || 'Upload failed')
    } finally {
      setUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  const editMessage = (messageId, newContent) => {
    clientRef.current?.publish({
      destination: `/app/room/${roomId}/edit`,
      body: JSON.stringify({ roomId, messageId, newContent }),
    })
  }

  const deleteMessage = (messageId) => {
    clientRef.current?.publish({
      destination: `/app/room/${roomId}/delete`,
      body: JSON.stringify({ roomId, messageId }),
    })
  }

  const reactToMessage = (messageId, emoji) => {
    clientRef.current?.publish({
      destination: `/app/room/${roomId}/react`,
      body: JSON.stringify({ roomId, messageId, emoji }),
    })
  }

  const approveRequest = async (username) => {
    await api.post(`/rooms/${roomId}/approve/${username}`)
    loadRoom()
  }

  const denyRequest = async (username) => {
    await api.post(`/rooms/${roomId}/deny/${username}`)
    loadRoom()
  }

  const removeMember = async (username) => {
    if (!confirm(`Remove ${username} from the room?`)) return
    await api.delete(`/rooms/${roomId}/members/${username}`)
    loadRoom()
  }

  if (!room) {
    return (
      <div className="max-w-3xl mx-auto px-6 py-10">
        {banner ? (
          <div className="text-center text-slate-400">
            <p>{banner}</p>
            <button onClick={() => navigate('/rooms')} className="mt-4 text-primary-accent text-sm">Back to rooms</button>
          </div>
        ) : (
          <p className="text-slate-500 text-sm">Loading room…</p>
        )}
      </div>
    )
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-6 grid grid-cols-1 lg:grid-cols-[1fr_260px] gap-4">
      <div className="flex flex-col h-[calc(100vh-120px)] bg-surface-card border border-surface-border rounded-2xl overflow-hidden">
        <div className="flex items-center justify-between px-4 py-3 border-b border-surface-border">
          <div>
            <button onClick={() => navigate('/rooms')} className="text-xs text-slate-500 hover:text-white mb-1">← All rooms</button>
            <div className="flex items-center gap-2">
              <h2 className="font-semibold text-white">{room.name}</h2>
              <span className="text-[10px] font-mono text-slate-500 bg-black/20 px-1.5 py-0.5 rounded">{room.roomCode}</span>
              <span className={`w-1.5 h-1.5 rounded-full ${connected ? 'bg-emerald-400' : 'bg-slate-600'}`} title={connected ? 'Connected' : 'Connecting…'} />
            </div>
          </div>
          <button onClick={() => setShowRoster((s) => !s)} className="lg:hidden text-xs text-slate-400 border border-surface-border rounded-lg px-2 py-1">
            Members
          </button>
        </div>

        {banner && (
          <div className="px-4 py-2 text-xs text-amber-400 bg-amber-400/10 border-b border-surface-border">{banner}</div>
        )}

        <div ref={scrollRef} className="chat-scroll flex-1 overflow-y-auto px-4 py-3">
          {messages.length === 0 ? (
            <p className="text-center text-sm text-slate-600 mt-10">No messages yet. Say hello 👋</p>
          ) : (
            messages.map((m) => (
              <MessageBubble
                key={m.id}
                message={m}
                currentUsername={user.username}
                isOwner={isOwner}
                onEdit={editMessage}
                onDelete={deleteMessage}
                onReact={reactToMessage}
              />
            ))
          )}
        </div>

        <TypingIndicator usernames={typingUsers} />

        <form onSubmit={sendMessage} className="flex items-center gap-2 px-4 py-3 border-t border-surface-border">
          <input type="file" ref={fileInputRef} onChange={handleFileUpload} className="hidden" />
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploading}
            className="shrink-0 w-9 h-9 flex items-center justify-center rounded-full border border-surface-border text-slate-400 hover:text-white hover:border-primary-accent"
            title="Attach a file or image"
          >
            {uploading ? '…' : '📎'}
          </button>
          <input
            value={draft}
            onChange={handleDraftChange}
            placeholder="Type a message…"
            className="flex-1 bg-black/20 border border-surface-border rounded-full px-4 py-2 text-sm outline-none focus:border-primary-accent"
          />
          <button
            type="submit"
            disabled={!draft.trim()}
            className="shrink-0 w-9 h-9 flex items-center justify-center rounded-full bg-primary text-white disabled:opacity-40"
          >
            ➤
          </button>
        </form>
      </div>

      <aside className={`${showRoster ? 'block' : 'hidden'} lg:block`}>
        {isOwner && <JoinRequests pending={room.pendingUsernames} onApprove={approveRequest} onDeny={denyRequest} />}

        <div className="bg-surface-card border border-surface-border rounded-xl p-3">
          <p className="text-xs font-semibold text-slate-400 mb-2 uppercase tracking-wide">
            Members ({room.members.length})
          </p>
          <div className="flex flex-col gap-2">
            {room.members.map((m) => (
              <div key={m} className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Avatar username={m} size={26} online={room.onlineMembers.includes(m)} />
                  <span className="text-sm">
                    {m} {m === room.ownerUsername && <span className="text-[10px] text-primary-accent">(owner)</span>}
                  </span>
                </div>
                {isOwner && m !== room.ownerUsername && (
                  <button onClick={() => removeMember(m)} className="text-xs text-slate-600 hover:text-red-400">Remove</button>
                )}
              </div>
            ))}
          </div>
        </div>
      </aside>
    </div>
  )
}

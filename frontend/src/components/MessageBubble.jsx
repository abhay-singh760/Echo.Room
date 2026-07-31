import React, { useState } from 'react'
import Avatar from './Avatar.jsx'

const QUICK_REACTIONS = ['👍', '❤️', '😂', '😮', '🙏']

function formatTime(iso) {
  try {
    return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  } catch {
    return ''
  }
}

export default function MessageBubble({ message, currentUsername, isOwner, onEdit, onDelete, onReact }) {
  const [showActions, setShowActions] = useState(false)
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(message.content || '')

  if (message.type === 'SYSTEM') {
    return (
      <div className="flex justify-center my-2">
        <span className="text-xs text-slate-500 bg-surface-card px-3 py-1 rounded-full">{message.content}</span>
      </div>
    )
  }

  const isMine = message.senderUsername === currentUsername
  const canModify = isMine || isOwner

  const submitEdit = () => {
    if (draft.trim() && draft !== message.content) {
      onEdit(message.id, draft.trim())
    }
    setEditing(false)
  }

  return (
    <div
      className={`flex gap-3 my-2 ${isMine ? 'flex-row-reverse' : ''}`}
      onMouseEnter={() => setShowActions(true)}
      onMouseLeave={() => setShowActions(false)}
    >
      <Avatar username={message.senderUsername} size={32} />
      <div className={`max-w-[70%] ${isMine ? 'items-end' : 'items-start'} flex flex-col`}>
        <div className="flex items-baseline gap-2">
          <span className="text-xs font-medium text-slate-400">{message.senderUsername}</span>
          <span className="text-[10px] text-slate-600">{formatTime(message.timestamp)}</span>
          {message.edited && <span className="text-[10px] text-slate-600 italic">edited</span>}
        </div>

        <div
          className={`relative mt-1 px-3.5 py-2 rounded-2xl text-sm leading-relaxed ${
            message.deleted
              ? 'bg-transparent border border-dashed border-surface-border text-slate-500 italic'
              : isMine
              ? 'bg-primary text-white rounded-tr-sm'
              : 'bg-surface-card text-slate-100 rounded-tl-sm'
          }`}
        >
          {message.deleted ? (
            'This message was deleted'
          ) : editing ? (
            <div className="flex flex-col gap-2 min-w-[200px]">
              <textarea
                autoFocus
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                className="bg-black/20 rounded-lg p-2 text-sm outline-none resize-none"
                rows={2}
              />
              <div className="flex gap-2 text-xs">
                <button onClick={submitEdit} className="text-primary-accent font-medium">Save</button>
                <button onClick={() => setEditing(false)} className="text-slate-400">Cancel</button>
              </div>
            </div>
          ) : (
            <>
              {message.type === 'IMAGE' && message.attachmentUrl && (
                <img
                  src={message.attachmentUrl}
                  alt={message.attachmentName || 'shared image'}
                  className="rounded-lg mb-1 max-h-64 object-cover"
                />
              )}
              {message.type === 'FILE' && message.attachmentUrl && (
                <a
                  href={message.attachmentUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="flex items-center gap-2 underline decoration-dotted"
                >
                  📎 {message.attachmentName || 'Download file'}
                </a>
              )}
              {message.content && <p className="whitespace-pre-wrap break-words">{message.content}</p>}
            </>
          )}
        </div>

        {!message.deleted && message.reactions && Object.keys(message.reactions).length > 0 && (
          <div className="flex gap-1 mt-1 flex-wrap">
            {Object.entries(message.reactions).map(([emoji, users]) => (
              <button
                key={emoji}
                onClick={() => onReact(message.id, emoji)}
                className={`text-xs px-1.5 py-0.5 rounded-full border ${
                  users.includes(currentUsername)
                    ? 'bg-primary/30 border-primary-accent'
                    : 'bg-surface-card border-surface-border'
                }`}
              >
                {emoji} {users.length}
              </button>
            ))}
          </div>
        )}

        {showActions && !message.deleted && !editing && (
          <div className={`flex gap-2 mt-1 text-xs text-slate-500 ${isMine ? 'flex-row-reverse' : ''}`}>
            {QUICK_REACTIONS.map((emoji) => (
              <button key={emoji} onClick={() => onReact(message.id, emoji)} className="hover:scale-125 transition-transform">
                {emoji}
              </button>
            ))}
            {isMine && <button onClick={() => setEditing(true)} className="hover:text-white">Edit</button>}
            {canModify && <button onClick={() => onDelete(message.id)} className="hover:text-red-400">Delete</button>}
          </div>
        )}
      </div>
    </div>
  )
}

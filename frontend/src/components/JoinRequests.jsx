import React from 'react'
import Avatar from './Avatar.jsx'

export default function JoinRequests({ pending, onApprove, onDeny }) {
  if (!pending || pending.length === 0) return null

  return (
    <div className="p-3 bg-surface-card border border-surface-border rounded-xl mb-3">
      <p className="text-xs font-semibold text-slate-400 mb-2 uppercase tracking-wide">
        Pending join requests ({pending.length})
      </p>
      <div className="flex flex-col gap-2">
        {pending.map((username) => (
          <div key={username} className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Avatar username={username} size={26} />
              <span className="text-sm">{username}</span>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => onApprove(username)}
                className="text-xs px-2 py-1 rounded-md bg-primary-accent/20 text-primary-accent hover:bg-primary-accent/30"
              >
                Approve
              </button>
              <button
                onClick={() => onDeny(username)}
                className="text-xs px-2 py-1 rounded-md bg-red-500/10 text-red-400 hover:bg-red-500/20"
              >
                Deny
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

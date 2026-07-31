import React from 'react'

export default function TypingIndicator({ usernames }) {
  if (!usernames || usernames.length === 0) return <div className="h-5" />

  const text =
    usernames.length === 1
      ? `${usernames[0]} is typing…`
      : `${usernames.slice(0, 2).join(', ')}${usernames.length > 2 ? ` +${usernames.length - 2}` : ''} are typing…`

  return (
    <div className="h-5 px-4 text-xs text-slate-500 flex items-center gap-1.5">
      <span className="flex gap-0.5">
        <span className="w-1 h-1 rounded-full bg-primary-accent animate-bounce [animation-delay:-0.3s]" />
        <span className="w-1 h-1 rounded-full bg-primary-accent animate-bounce [animation-delay:-0.15s]" />
        <span className="w-1 h-1 rounded-full bg-primary-accent animate-bounce" />
      </span>
      {text}
    </div>
  )
}

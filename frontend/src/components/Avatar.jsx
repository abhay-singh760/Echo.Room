import React from 'react'

const PALETTE = ['#028090', '#00A896', '#02C39A', '#F96167', '#B85042', '#6D2E46', '#1E2761', '#50808E']

function colorFor(name = '') {
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return PALETTE[Math.abs(hash) % PALETTE.length]
}

export default function Avatar({ username, size = 36, online }) {
  const initial = username ? username[0].toUpperCase() : '?'
  return (
    <div className="relative shrink-0" style={{ width: size, height: size }}>
      <div
        className="flex items-center justify-center rounded-full font-semibold text-white"
        style={{ width: size, height: size, backgroundColor: colorFor(username), fontSize: size * 0.42 }}
      >
        {initial}
      </div>
      {online !== undefined && (
        <span
          className={`absolute bottom-0 right-0 rounded-full border-2 border-surface-card ${
            online ? 'bg-emerald-400' : 'bg-slate-500'
          }`}
          style={{ width: size * 0.32, height: size * 0.32 }}
        />
      )}
    </div>
  )
}

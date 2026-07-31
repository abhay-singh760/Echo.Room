import { Client } from '@stomp/stompjs'

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws'

/**
 * Thin wrapper around @stomp/stompjs. The JWT is sent as a native STOMP
 * "Authorization" header on CONNECT -- the backend derives the user's
 * identity from that verified token, never from anything the client
 * puts in a message payload.
 */
export function createStompClient(token, { onConnect, onDisconnect } = {}) {
  const wsUrl = WS_URL.replace(/^http/, 'ws')

  const client = new Client({
    brokerURL: wsUrl,
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    reconnectDelay: 4000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => onConnect && onConnect(client),
    onWebSocketClose: () => onDisconnect && onDisconnect(),
    onStompError: (frame) => {
      console.error('STOMP error:', frame.headers?.message, frame.body)
    },
  })

  return client
}

import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import Rooms from './pages/Rooms.jsx'
import ChatRoom from './pages/ChatRoom.jsx'
import AdminDashboard from './pages/AdminDashboard.jsx'
import NavBar from './components/NavBar.jsx'

function ProtectedRoute({ children, adminOnly = false }) {
  const { user, isAdmin } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (adminOnly && !isAdmin) return <Navigate to="/rooms" replace />
  return children
}

export default function App() {
  const { user } = useAuth()

  return (
    <div className="min-h-screen bg-surface">
      {user && <NavBar />}
      <Routes>
        <Route path="/login" element={user ? <Navigate to="/rooms" replace /> : <Login />} />
        <Route path="/register" element={user ? <Navigate to="/rooms" replace /> : <Register />} />
        <Route
          path="/rooms"
          element={
            <ProtectedRoute>
              <Rooms />
            </ProtectedRoute>
          }
        />
        <Route
          path="/rooms/:roomId"
          element={
            <ProtectedRoute>
              <ChatRoom />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin"
          element={
            <ProtectedRoute adminOnly>
              <AdminDashboard />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to={user ? '/rooms' : '/login'} replace />} />
      </Routes>
    </div>
  )
}

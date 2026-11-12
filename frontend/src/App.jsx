import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import Navbar from './Navbar';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Events from './pages/Events';
import EventForm from './pages/EventForm';
import MyRegistrations from './pages/MyRegistrations';
import Reports from './pages/Reports';
import ForgotPassword from './pages/ForgotPassword';
import Profile from './pages/Profile';

function ProtectedRoute({ children, roles }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) {
    return <div className="container"><div className="card">You don't have permission to view this page.</div></div>;
  }
  return children;
}

export default function App() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
        <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
        <Route path="/events" element={<ProtectedRoute><Events /></ProtectedRoute>} />
        <Route path="/events/new" element={<ProtectedRoute roles={['admin']}><EventForm /></ProtectedRoute>} />
        <Route path="/events/:id/edit" element={<ProtectedRoute roles={['admin']}><EventForm /></ProtectedRoute>} />
        <Route path="/my-registrations" element={<ProtectedRoute roles={['student']}><MyRegistrations /></ProtectedRoute>} />
        <Route path="/reports" element={<ProtectedRoute roles={['faculty', 'admin']}><Reports /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
}

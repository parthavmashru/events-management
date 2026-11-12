import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../AuthContext';

export default function Dashboard() {
  const { user } = useAuth();

  return (
    <div className="container">
      <div className="card">
        <h2>Welcome, {user?.name} 👋</h2>
        <p>Role: <span className={`badge badge-${user?.role}`}>{user?.role}</span></p>
        <ul>
          <li><Link to="/events">Browse events</Link></li>
          {user?.role === 'student' && <li><Link to="/my-registrations">View my registrations</Link></li>}
          {user?.role === 'admin' && <li><Link to="/events/new">Create a new event</Link></li>}
          {(user?.role === 'faculty' || user?.role === 'admin') && <li><Link to="/reports">View participation reports</Link></li>}
        </ul>
      </div>
    </div>
  );
}

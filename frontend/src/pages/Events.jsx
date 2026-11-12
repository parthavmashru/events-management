import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api';
import { useAuth } from '../AuthContext';

export default function Events() {
  const [events, setEvents] = useState([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const { user } = useAuth();

  const loadEvents = async () => {
    try {
      const res = await api.get('/events');
      setEvents(res.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to load events');
    }
  };

  useEffect(() => {
    loadEvents();
  }, []);

  const handleRegister = async (eventId) => {
    setError('');
    setMessage('');
    try {
      await api.post(`/registrations/${eventId}`);
      setMessage('Successfully registered!');
      loadEvents();
    } catch (err) {
      setError(err.response?.data?.error || 'Registration failed');
    }
  };

  const handleDelete = async (eventId) => {
    if (!window.confirm('Delete this event?')) return;
    try {
      await api.delete(`/events/${eventId}`);
      loadEvents();
    } catch (err) {
      setError(err.response?.data?.error || 'Delete failed');
    }
  };

  return (
    <div className="container">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>Upcoming Events</h2>
        {user?.role === 'admin' && <Link className="btn" to="/events/new">+ New Event</Link>}
      </div>
      {error && <div className="error-text">{error}</div>}
      {message && <div className="success-text">{message}</div>}

      {events.length === 0 && <div className="card">No events yet.</div>}

      {events.map((event) => (
        <div className="card" key={event.id}>
          <h3>{event.title}</h3>
          <p>{event.description}</p>
          <p><strong>Location:</strong> {event.location || 'TBA'}</p>
          <p><strong>Date:</strong> {new Date(event.date).toLocaleString()}</p>
          <p><strong>Capacity:</strong> {event.capacity}</p>
          <p><strong>Organizer:</strong> {event.organizer?.name}</p>
          <div style={{ display: 'flex', gap: 8 }}>
            {user?.role === 'student' && (
              <button className="btn" onClick={() => handleRegister(event.id)}>Register</button>
            )}
            {user?.role === 'admin' && (
              <>
                <Link className="btn btn-secondary" to={`/events/${event.id}/edit`}>Edit</Link>
                <button className="btn btn-danger" onClick={() => handleDelete(event.id)}>Delete</button>
              </>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}

import React, { useEffect, useState } from 'react';
import api from '../api';

export default function MyRegistrations() {
  const [regs, setRegs] = useState([]);
  const [error, setError] = useState('');

  const load = async () => {
    try {
      const res = await api.get('/registrations/my');
      setRegs(res.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to load registrations');
    }
  };

  useEffect(() => { load(); }, []);

  const handleCancel = async (eventId) => {
    try {
      await api.delete(`/registrations/${eventId}`);
      load();
    } catch (err) {
      setError(err.response?.data?.error || 'Cancel failed');
    }
  };

  return (
    <div className="container">
      <h2>My Registrations</h2>
      {error && <div className="error-text">{error}</div>}
      {regs.length === 0 && <div className="card">You haven't registered for any events yet.</div>}
      {regs.map((reg) => (
        <div className="card" key={reg.id}>
          <h3>{reg.Event?.title}</h3>
          <p><strong>Date:</strong> {reg.Event ? new Date(reg.Event.date).toLocaleString() : ''}</p>
          <p><strong>Status:</strong> <span className="badge badge-student">{reg.status}</span></p>
          {reg.status === 'registered' && (
            <button className="btn btn-danger" onClick={() => handleCancel(reg.Event.id)}>Cancel Registration</button>
          )}
        </div>
      ))}
    </div>
  );
}

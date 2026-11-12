import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../api';

export default function EventForm() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [form, setForm] = useState({
    title: '', description: '', location: '', date: '', capacity: 50
  });
  const [error, setError] = useState('');

  useEffect(() => {
    if (isEdit) {
      api.get(`/events/${id}`).then((res) => {
        const e = res.data;
        setForm({
          title: e.title,
          description: e.description || '',
          location: e.location || '',
          date: e.date ? e.date.slice(0, 16) : '',
          capacity: e.capacity
        });
      });
    }
  }, [id, isEdit]);

  const handleChange = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (isEdit) {
        await api.put(`/events/${id}`, form);
      } else {
        await api.post('/events', form);
      }
      navigate('/events');
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to save event');
    }
  };

  return (
    <div className="container" style={{ maxWidth: 520 }}>
      <div className="card">
        <h2>{isEdit ? 'Edit Event' : 'Create Event'}</h2>
        {error && <div className="error-text">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Title</label>
            <input value={form.title} onChange={handleChange('title')} required />
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea rows={3} value={form.description} onChange={handleChange('description')} />
          </div>
          <div className="form-group">
            <label>Location</label>
            <input value={form.location} onChange={handleChange('location')} />
          </div>
          <div className="grid-2">
            <div className="form-group">
              <label>Date & time</label>
              <input type="datetime-local" value={form.date} onChange={handleChange('date')} required />
            </div>
            <div className="form-group">
              <label>Capacity</label>
              <input type="number" min={1} value={form.capacity} onChange={handleChange('capacity')} required />
            </div>
          </div>
          <button className="btn" type="submit">{isEdit ? 'Save Changes' : 'Create Event'}</button>
        </form>
      </div>
    </div>
  );
}

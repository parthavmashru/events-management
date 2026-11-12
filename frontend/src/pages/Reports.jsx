import React, { useEffect, useState } from 'react';
import api from '../api';

export default function Reports() {
  const [report, setReport] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/reports/participation')
      .then((res) => setReport(res.data))
      .catch((err) => setError(err.response?.data?.error || 'Failed to load report'));
  }, []);

  return (
    <div className="container">
      <h2>Event Participation Report</h2>
      {error && <div className="error-text">{error}</div>}
      <div className="card">
        <table>
          <thead>
            <tr>
              <th>Event</th>
              <th>Date</th>
              <th>Capacity</th>
              <th>Registered</th>
            
              <th>Fill Rate</th>
            </tr>
          </thead>
          <tbody>
            {report.map((r) => (
              <tr key={r.eventId}>
                <td>{r.title}</td>
                <td>{new Date(r.date).toLocaleDateString()}</td>
                <td>{r.capacity}</td>
                <td>{r.registeredCount}</td>
               
                <td>{r.fillRate}%</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

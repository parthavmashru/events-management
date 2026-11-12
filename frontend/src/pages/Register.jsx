import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import PasswordInput from '../PasswordInput';

export default function Register() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('student');
  const [facultyId, setFacultyId] = useState('');
  const [adminId, setAdminId] = useState('');
  const [error, setError] = useState('');
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleRoleChange = (e) => {
    setRole(e.target.value);
    setFacultyId('');
    setAdminId('');
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const extra = {};
      if (role === 'faculty') extra.facultyId = facultyId.trim();
      if (role === 'admin') extra.adminId = adminId.trim();
      await register(name, email, password, role, extra);
      navigate('/events');
    } catch (err) {
      setError(err.response?.data?.error || 'Registration failed');
    }
  };

  return (
    <div className="container" style={{ maxWidth: 420 }}>
      <div className="card">
        <h2>Create an account</h2>
        {error && <div className="error-text">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Full name</label>
            <input value={name} onChange={(e) => setName(e.target.value)} required />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </div>
          <div className="form-group">
            <label>Password</label>
            <PasswordInput
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={6}
              autoComplete="new-password"
            />
          </div>
          <div className="form-group">
            <label>Role</label>
            <select value={role} onChange={handleRoleChange}>
              <option value="student">Student</option>
              <option value="faculty">Faculty</option>
              <option value="admin">Admin</option>
            </select>
          </div>
          {role === 'faculty' && (
            <div className="form-group">
              <label>Faculty ID</label>
              <input
                value={facultyId}
                onChange={(e) => setFacultyId(e.target.value)}
                placeholder="Enter Faculty ID"
                required
              />
            </div>
          )}
          {role === 'admin' && (
            <div className="form-group">
              <label>Admin ID</label>
              <input
                value={adminId}
                onChange={(e) => setAdminId(e.target.value)}
                placeholder="Enter Admin ID"
                required
              />
            </div>
          )}
          <button className="btn" type="submit">Register</button>
        </form>
        <p style={{ marginTop: 14 }}>Already have an account? <Link to="/login"><u>Login here</u></Link></p>
      </div>
    </div>
  );
}
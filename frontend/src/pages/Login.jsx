import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import PasswordInput from '../PasswordInput';

export default function Login() {
  const [role, setRole] = useState('student');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [facultyId, setFacultyId] = useState('');
  const [adminId, setAdminId] = useState('');
  const [error, setError] = useState('');
  const { login } = useAuth();
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
      await login(email, password, role, extra);
      navigate('/events');
    } catch (err) {
      setError(err.response?.data?.error || 'Login failed');
    }
  };

  return (
    <div className="container" style={{ maxWidth: 420 }}>
      <div className="card">
        <h2>Login</h2>
        {error && <div className="error-text">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="login-role">Login As</label>
            <select id="login-role" value={role} onChange={handleRoleChange}>
              <option value="student">Student</option>
              <option value="faculty">Faculty</option>
              <option value="admin">Admin</option>
            </select>
          </div>
          <div className="form-group">
            <label htmlFor="login-email">Email</label>
            <input id="login-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </div>
          <div className="form-group">
            <label htmlFor="login-password">Password</label>
            <PasswordInput
              id="login-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
            />
          </div>
          <div style={{ marginBottom: 14, textAlign: 'right' }}>
            <Link to="/forgot-password" style={{ fontSize: '0.88rem', color: '#1e3a5f', textDecoration: 'underline' }}>
              Forgot Password?
            </Link>
          </div>
          {role === 'faculty' && (
            <div className="form-group">
              <label htmlFor="login-faculty-id">Faculty ID</label>
              <input
                id="login-faculty-id"
                value={facultyId}
                onChange={(e) => setFacultyId(e.target.value)}
                placeholder="Enter Faculty ID"
                required
              />
            </div>
          )}
          {role === 'admin' && (
            <div className="form-group">
              <label htmlFor="login-admin-id">Admin ID</label>
              <input
                id="login-admin-id"
                value={adminId}
                onChange={(e) => setAdminId(e.target.value)}
                placeholder="Enter Admin ID"
                required
              />
            </div>
          )}
          <button className="btn" type="submit">Login</button>
        </form>
        <p style={{ marginTop: 14 }}>No account? <Link to="/register"><u>Register here</u></Link></p>
      </div>
    </div>
  );
}

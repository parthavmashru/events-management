import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api';
import PasswordInput from '../PasswordInput';

export default function ForgotPassword() {
  const [step, setStep] = useState('request'); // 'request' | 'reset' | 'done'
  const [email, setEmail] = useState('');
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleRequestReset = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setSubmitting(true);
    try {
      const res = await api.post('/auth/forgot-password', { email: email.trim() });
      setMessage(res.data.message);
      setStep('reset');
    } catch (err) {
      setError(err.response?.data?.error || 'Unable to process password reset request');
    } finally {
      setSubmitting(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setSubmitting(true);
    try {
      const res = await api.post('/auth/reset-password', {
        token: token.trim(),
        newPassword,
        confirmPassword
      });
      setMessage(res.data.message);
      setToken('');
      setNewPassword('');
      setConfirmPassword('');
      setStep('done');
    } catch (err) {
      setError(err.response?.data?.error || 'Password reset failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container" style={{ maxWidth: 440 }}>
      <div className="card">
        <h2>Reset Password</h2>
        {error && <div className="error-text">{error}</div>}
        {message && <div className="success-text">{message}</div>}

        {step === 'request' && (
          <form onSubmit={handleRequestReset}>
            <p style={{ color: '#475467', fontSize: '0.92rem', marginTop: 0 }}>
              Enter your registered email address to request a single-use password reset token.
            </p>
            <div className="info-box">
              Note: Email delivery (SMTP) is not configured in this local environment. When a registered email is submitted, the time-limited reset token is written to the backend server console logs rather than sent by email.
            </div>
            <div className="form-group">
              <label htmlFor="forgot-email">Registered Email</label>
              <input
                id="forgot-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <button className="btn" type="submit" disabled={submitting}>
              {submitting ? 'Submitting...' : 'Request Reset Token'}
            </button>
            <p style={{ marginTop: 14, fontSize: '0.9rem' }}>
              Already have a reset token?{' '}
              <button
                type="button"
                onClick={() => {
                  setError('');
                  setStep('reset');
                }}
                style={{ background: 'none', border: 'none', padding: 0, color: '#1e3a5f', textDecoration: 'underline', cursor: 'pointer', fontSize: '0.9rem' }}
              >
                Enter token
              </button>
            </p>
          </form>
        )}

        {step === 'reset' && (
          <form onSubmit={handleResetPassword}>
            <div className="form-group">
              <label htmlFor="reset-token">Reset Token</label>
              <input
                id="reset-token"
                value={token}
                onChange={(e) => setToken(e.target.value)}
                placeholder="Paste your single-use reset token"
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="reset-new-password">New Password</label>
              <PasswordInput
                id="reset-new-password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                minLength={6}
                autoComplete="new-password"
              />
            </div>
            <div className="form-group">
              <label htmlFor="reset-confirm-password">Confirm New Password</label>
              <PasswordInput
                id="reset-confirm-password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                minLength={6}
                autoComplete="new-password"
              />
            </div>
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
              <button className="btn" type="submit" disabled={submitting}>
                {submitting ? 'Resetting...' : 'Reset Password'}
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setError('');
                  setMessage('');
                  setStep('request');
                }}
              >
                Request Another Token
              </button>
            </div>
          </form>
        )}

        {step === 'done' && (
          <div>
            <Link to="/login" className="btn">Back to Login</Link>
          </div>
        )}

        {step !== 'done' && (
          <p style={{ marginTop: 16 }}>
            Remembered your password? <Link to="/login"><u>Back to Login</u></Link>
          </p>
        )}
      </div>
    </div>
  );
}

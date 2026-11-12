import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../api';
import { useAuth, getInitials } from '../AuthContext';

function resizeImageToDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const img = new Image();
      img.onload = () => {
        const maxSize = 256;
        let { width, height } = img;
        if (width > maxSize || height > maxSize) {
          if (width >= height) {
            height = Math.round((height * maxSize) / width);
            width = maxSize;
          } else {
            width = Math.round((width * maxSize) / height);
            height = maxSize;
          }
        }
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);
        const outputType = file.type === 'image/png' ? 'image/png' : 'image/jpeg';
        resolve(canvas.toDataURL(outputType, 0.85));
      };
      img.onerror = () => reject(new Error('Could not load selected image'));
      img.src = reader.result;
    };
    reader.onerror = () => reject(new Error('Could not read selected file'));
    reader.readAsDataURL(file);
  });
}

export default function Profile() {
  const { user, updateUser } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [profile, setProfile] = useState(user);
  const [isEditing, setIsEditing] = useState(searchParams.get('edit') === 'true');
  const [name, setName] = useState(user?.name || '');
  const [profilePicture, setProfilePicture] = useState(user?.profilePicture || '');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setIsEditing(searchParams.get('edit') === 'true');
  }, [searchParams]);

  useEffect(() => {
    let active = true;
    api.get('/profile')
      .then((res) => {
        if (!active) return;
        setProfile(res.data);
        setName(res.data.name || '');
        setProfilePicture(res.data.profilePicture || '');
        updateUser(res.data);
      })
      .catch((err) => {
        if (!active) return;
        setError(err.response?.data?.error || 'Failed to load profile');
      });
    return () => { active = false; };
  }, []);

  const handleFileChange = async (e) => {
    setError('');
    const file = e.target.files?.[0];
    if (!file) return;
    if (!['image/png', 'image/jpeg', 'image/webp', 'image/gif'].includes(file.type)) {
      setError('Please choose a PNG, JPG, GIF, or WEBP image.');
      return;
    }
    try {
      const dataUrl = await resizeImageToDataUrl(file);
      setProfilePicture(dataUrl);
    } catch (err) {
      setError(err.message || 'Unable to process image');
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setSaving(true);
    try {
      const res = await api.put('/profile', {
        name: name.trim(),
        profilePicture: profilePicture || ''
      });
      setProfile(res.data);
      setName(res.data.name);
      setProfilePicture(res.data.profilePicture || '');
      updateUser(res.data);
      setSuccess('Profile updated successfully.');
      setSearchParams({});
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setError('');
    setSuccess('');
    setName(profile?.name || '');
    setProfilePicture(profile?.profilePicture || '');
    setSearchParams({});
  };

  const displayedName = isEditing ? name : profile?.name;
  const displayedPicture = isEditing ? profilePicture : profile?.profilePicture;
  const roleLabel = profile?.role ? profile.role.charAt(0).toUpperCase() + profile.role.slice(1) : '';

  return (
    <div className="container" style={{ maxWidth: 560 }}>
      <div className="card">
        <div className="profile-header">
          <div className="profile-avatar-large" aria-label="Profile avatar">
            {displayedPicture ? (
              <img src={displayedPicture} alt={displayedName || 'User'} className="profile-avatar-img" />
            ) : (
              <span>{getInitials(displayedName)}</span>
            )}
          </div>
          <div>
            <h2 style={{ margin: '0 0 6px' }}>{displayedName || 'My Profile'}</h2>
            {profile?.role && (
              <span className={`badge badge-${profile.role}`}>{roleLabel}</span>
            )}
          </div>
        </div>

        {error && <div className="error-text">{error}</div>}
        {success && <div className="success-text">{success}</div>}

        {!isEditing ? (
          <div>
            <div className="form-group">
              <label>Full Name</label>
              <div style={{ padding: '8px 0', fontWeight: 500 }}>{profile?.name}</div>
            </div>
            <div className="form-group">
              <label>Email</label>
              <div style={{ padding: '8px 0', color: '#475467' }}>{profile?.email}</div>
            </div>
            <div className="form-group">
              <label>Role</label>
              <div style={{ padding: '8px 0', color: '#475467' }}>{roleLabel}</div>
            </div>
            <button
              type="button"
              className="btn"
              onClick={() => {
                setSuccess('');
                setError('');
                setSearchParams({ edit: 'true' });
              }}
            >
              Edit Profile
            </button>
          </div>
        ) : (
          <form onSubmit={handleSave}>
            <div className="form-group">
              <label htmlFor="profile-name">Full Name</label>
              <input
                id="profile-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                minLength={2}
                maxLength={100}
              />
            </div>
            <div className="form-group">
              <label htmlFor="profile-email">Email</label>
              <input id="profile-email" value={profile?.email || ''} disabled />
            </div>
            <div className="form-group">
              <label htmlFor="profile-role">Role</label>
              <input id="profile-role" value={roleLabel} disabled />
            </div>
            <div className="form-group">
              <label htmlFor="profile-picture">Profile Picture</label>
              <input
                id="profile-picture"
                type="file"
                accept="image/png,image/jpeg,image/webp,image/gif"
                onChange={handleFileChange}
              />
              {profilePicture && (
                <button
                  type="button"
                  className="btn btn-secondary"
                  style={{ marginTop: 8, padding: '6px 12px', fontSize: '0.85rem' }}
                  onClick={() => setProfilePicture('')}
                >
                  Remove Picture (Use Initials)
                </button>
              )}
            </div>
            <div style={{ display: 'flex', gap: 10 }}>
              <button type="submit" className="btn" disabled={saving}>
                {saving ? 'Saving...' : 'Save Changes'}
              </button>
              <button type="button" className="btn btn-secondary" onClick={handleCancel}>
                Cancel
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

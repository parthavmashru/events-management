import React, { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth, getInitials } from './AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [showLogoutDialog, setShowLogoutDialog] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    const handleOutsideClick = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setMenuOpen(false);
      }
    };
    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        setMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleOutsideClick);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick);
      document.removeEventListener('keydown', handleEscape);
    };
  }, []);

  const handleConfirmLogout = () => {
    setShowLogoutDialog(false);
    setMenuOpen(false);
    logout();
    navigate('/login');
  };

  const handleNavigateProfile = (editMode) => {
    setMenuOpen(false);
    navigate(editMode ? '/profile?edit=true' : '/profile');
  };

  return (
    <>
      <nav className="navbar">
        <Link to="/" className="brand">🎓 Event Platform</Link>
        <div className="navbar-links">
          {user ? (
            <>
              <Link to="/events">Events</Link>
              {user.role === 'student' && <Link to="/my-registrations">My Registrations</Link>}
              {user.role === 'admin' && <Link to="/events/new">Create Event</Link>}
              {(user.role === 'faculty' || user.role === 'admin') && <Link to="/reports">Reports</Link>}
              <div className="profile-menu-wrapper" ref={menuRef}>
                <button
                  type="button"
                  className="profile-avatar-btn"
                  onClick={() => setMenuOpen((prev) => !prev)}
                  aria-label="User profile menu"
                  aria-haspopup="true"
                  aria-expanded={menuOpen}
                  title={user.name}
                >
                  {user.profilePicture ? (
                    <img src={user.profilePicture} alt={user.name} className="profile-avatar-img" />
                  ) : (
                    <span>{getInitials(user.name)}</span>
                  )}
                </button>
                {menuOpen && (
                  <div className="profile-dropdown" role="menu">
                    <div className="profile-dropdown-header">
                      <div className="profile-dropdown-name">{user.name}</div>
                      <span className={`badge badge-${user.role}`}>{user.role}</span>
                    </div>
                    <button
                      type="button"
                      className="profile-dropdown-item"
                      role="menuitem"
                      onClick={() => handleNavigateProfile(false)}
                    >
                      My Profile
                    </button>
                    <button
                      type="button"
                      className="profile-dropdown-item"
                      role="menuitem"
                      onClick={() => handleNavigateProfile(true)}
                    >
                      Edit Profile
                    </button>
                    <button
                      type="button"
                      className="profile-dropdown-item danger"
                      role="menuitem"
                      onClick={() => {
                        setMenuOpen(false);
                        setShowLogoutDialog(true);
                      }}
                    >
                      Logout
                    </button>
                  </div>
                )}
              </div>
            </>
          ) : (
            <>
              <Link to="/login">Login</Link>
              <Link to="/register">Register</Link>
            </>
          )}
        </div>
      </nav>

      {showLogoutDialog && (
        <div className="modal-backdrop" onClick={() => setShowLogoutDialog(false)}>
          <div className="modal-card" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <h3>Confirm Logout</h3>
            <p>Are you sure you want to log out?</p>
            <div className="modal-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setShowLogoutDialog(false)}>
                Cancel
              </button>
              <button type="button" className="btn btn-danger" onClick={handleConfirmLogout}>
                Yes, Logout
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

import React, { useState, useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Box, CircularProgress } from '@mui/material';

// Pages
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import NotificationList from './pages/NotificationList';
import NotificationDetail from './pages/NotificationDetail'; // Import the new component

// Components
import Layout from './components/Layout';

// Services
import { checkAuthStatus, validateTokenWithBackend } from './services/authService';
import {
  connectToRealtimeNotifications,
  disconnectFromRealtimeNotifications
} from './services/notificationService';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState(null);

  useEffect(() => {
    const checkAuth = async () => {
      try {
        // First, check local token
          const userData = await checkAuthStatus();
          if (userData) {
            // Now, validate with backend (checkAuthStatus already does basic validation)
            // For a more robust check, explicitly call validateTokenWithBackend
            const backendUser = await validateTokenWithBackend();
            if (backendUser) {
              setIsAuthenticated(true);
              setUser(backendUser);
              // Connect to WebSocket after user is authenticated and data is loaded
              if (backendUser.id) {
                connectToRealtimeNotifications(backendUser.id);
              }
              return;
            }
          }
      } catch (error) {
        console.error('Authentication check failed:', error);
      }
      setIsAuthenticated(false);
      setUser(null);
      setLoading(false);
    };
    checkAuth().finally(() => setLoading(false));

    // Cleanup WebSocket on component unmount, though App typically doesn't unmount
    return () => {
      disconnectFromRealtimeNotifications();
    };
  }, []);

  const handleLogin = (userData) => {
      setIsAuthenticated(true);
      setUser(userData);
      if (userData && userData.id) {
        connectToRealtimeNotifications(userData.id);
      }
  };

  const handleLogout = () => {
      localStorage.removeItem('token');
      setIsAuthenticated(false);
      setUser(null);
      disconnectFromRealtimeNotifications();
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Routes>
      <Route 
        path="/login" 
        element={isAuthenticated ? <Navigate to="/" /> : <Login onLogin={handleLogin} />} 
      />
      <Route 
        path="/register" 
        element={isAuthenticated ? <Navigate to="/" /> : <Register />} 
      />
      <Route 
        path="/" 
        element={
          isAuthenticated ? (
            <Layout user={user} onLogout={handleLogout}>
              <Dashboard user={user} />
            </Layout>
          ) : (
            <Navigate to="/login" />
          )
        } 
      />
      <Route 
        path="/notifications" 
        element={
          isAuthenticated ? (
            <Layout user={user} onLogout={handleLogout}>
              <NotificationList user={user} />
            </Layout>
          ) : (
            <Navigate to="/login" />
          )
        } 
      />
      <Route 
        path="/notifications/:id" // Add route for notification details 
        element={ 
          isAuthenticated ? ( 
            <Layout user={user} onLogout={handleLogout}> 
              <NotificationDetail /> 
            </Layout> 
          ) : ( 
            <Navigate to="/login" /> 
          ) 
        } 
      /> 
    </Routes>
  );
}

export default App;
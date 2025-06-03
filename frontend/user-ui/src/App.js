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
    let isMounted = true;

    const performAuthCheck = async () => {
      let resolvedUser = null;
      let resolvedIsAuthenticated = false;
      try {
        const localUserData = await checkAuthStatus();
        if (localUserData) {
          const backendValidatedUser = await validateTokenWithBackend();
          if (backendValidatedUser) {
            resolvedUser = backendValidatedUser;
            resolvedIsAuthenticated = true;
          }
        }
      } catch (error) {
        console.error('App.js: Error during performAuthCheck:', error);
        // resolvedUser and resolvedIsAuthenticated remain in their default "logged out" state
      }
      return { resolvedUser, resolvedIsAuthenticated };
    };

    performAuthCheck().then(({ resolvedUser, resolvedIsAuthenticated }) => {
      if (isMounted) {
        console.log('App.js: Current user ID:', user?.id, 'Resolved user ID:', resolvedUser?.id);
        const currentUserRoles = JSON.stringify(user?.roles);
        const resolvedUserRoles = JSON.stringify(resolvedUser?.roles);

        let userChanged = false;
        if (user?.id !== resolvedUser?.id ||
            user?.name !== resolvedUser?.name ||
            currentUserRoles !== resolvedUserRoles ||
            (user === null && resolvedUser !== null) ||
            (user !== null && resolvedUser === null)) {
          userChanged = true;
        }

        if (userChanged) {
          console.log('App.js: Calling setUser.');
          setUser(resolvedUser);
        } else {
          console.log('App.js: Not calling setUser, user data is the same.');
        }

        if (isAuthenticated !== resolvedIsAuthenticated) {
          console.log('App.js: Calling setIsAuthenticated to:', resolvedIsAuthenticated);
          setIsAuthenticated(resolvedIsAuthenticated);
        } else {
          console.log('App.js: Not calling setIsAuthenticated, auth state is the same.');
        }

        console.log('App.js: Calling setLoading(false).');
        setLoading(false);
      }
    });

    return () => {
      isMounted = false;
    };
  }, []);

  // New useEffect for SSE Connection Management
  useEffect(() => {
    if (user && user.id) {
      console.log('App.js: User authenticated, connecting to SSE...');
      connectToRealtimeNotifications(user.id);

      return () => {
        console.log('App.js: User logged out or App unmounting, disconnecting from SSE...');
        disconnectFromRealtimeNotifications();
      };
    } else {
      // Optional: Handle case where user is null or lacks id, ensuring disconnection
      // console.log('App.js: No authenticated user, ensuring SSE is disconnected.');
      // disconnectFromRealtimeNotifications(); // This might be redundant if logout always clears user
    }
  }, [user]);

  const handleLogin = (newUserData) => {
    // Only update state if necessary to maintain object reference stability
    if (!isAuthenticated) {
      setIsAuthenticated(true);
    }
    if (user === null || user.id !== newUserData.id || user.name !== newUserData.name || JSON.stringify(user.roles) !== JSON.stringify(newUserData.roles)) {
      setUser(newUserData);
    }
    // SSE connection will be handled by the new useEffect reacting to 'user' state change
  };

  const handleLogout = () => {
      localStorage.removeItem('token');
      // Only update state if necessary
      if (isAuthenticated) {
        setIsAuthenticated(false);
      }
      if (user !== null) {
        setUser(null);
      }
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
        path="/dashboard" 
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
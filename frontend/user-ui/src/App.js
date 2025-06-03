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

  console.log('App.js: Rendering. Current user.id from state:', user?.id);

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
        console.log('App.js performAuthCheck.then: Processing resolved auth state. ResolvedUser.id:', resolvedUser?.id, 'ResolvedIsAuthenticated:', resolvedIsAuthenticated);

        setIsAuthenticated(currentIsAuthInState => {
          console.log('App.js performAuthCheck.then: setIsAuthenticated check. CurrentInState:', currentIsAuthInState, 'Resolved:', resolvedIsAuthenticated);
          if (currentIsAuthInState === resolvedIsAuthenticated) {
            console.log('App.js performAuthCheck.then: setIsAuthenticated - no change needed.');
            return currentIsAuthInState;
          }
          console.log('App.js performAuthCheck.then: setIsAuthenticated - changing to:', resolvedIsAuthenticated);
          return resolvedIsAuthenticated;
        });

        setUser(currentUserInState => {
          console.log('App.js performAuthCheck.then: setUser check. CurrentUserInState.id:', currentUserInState?.id, 'ResolvedUser.id:', resolvedUser?.id);

          const rolesMatch = JSON.stringify(currentUserInState?.roles) === JSON.stringify(resolvedUser?.roles);

          const userIsEffectivelyTheSame =
            (currentUserInState === null && resolvedUser === null) ||
            (currentUserInState !== null && resolvedUser !== null &&
             currentUserInState.id === resolvedUser.id &&
             currentUserInState.name === resolvedUser.name &&
             rolesMatch);

          if (userIsEffectivelyTheSame) {
            console.log('App.js performAuthCheck.then: setUser - user data is effectively the same or both null. Not changing user state reference.');
            return currentUserInState;
          }

          console.log('App.js performAuthCheck.then: setUser - user data is different or involves a null/object transition. Setting new user state.');
          return resolvedUser;
        });

        console.log('App.js performAuthCheck.then: Calling setLoading(false).');
        setLoading(false);
      }
    });

    return () => {
      isMounted = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
    console.log('App.js: handleLogin triggered. newUserData.id:', newUserData?.id, 'Current App user.id:', user?.id, 'App loading state:', loading, 'App isAuthenticated:', isAuthenticated);

    if (loading) {
      console.log('App.js: handleLogin - Initial auth (loading=true). Returning early, performAuthCheck will handle auth.');
      return;
    }

    // Existing logic for conditional state updates follows
    // Only update state if necessary to maintain object reference stability
    if (!isAuthenticated) {
      // This log is being added for clarity as per the example logic, though the original didn't have it.
      console.log('App.js: handleLogin - Calling setIsAuthenticated(true).');
      setIsAuthenticated(true);
    } else {
      // This log is being added for clarity
      console.log('App.js: handleLogin - setIsAuthenticated not called, isAuthenticated is already true.');
    }

    const currentUserRolesString = JSON.stringify(user?.roles);
    const newUserDataRolesString = JSON.stringify(newUserData?.roles);

    if (user === null || user.id !== newUserData.id || user.name !== newUserData.name || currentUserRolesString !== newUserDataRolesString) {
      // This log is being added for clarity
      console.log('App.js: handleLogin - User data is different or current user was null. Calling setUser.');
      setUser(newUserData);
    } else {
      // This log is being added for clarity
      console.log('App.js: handleLogin - setUser not called, user data is considered the same.');
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
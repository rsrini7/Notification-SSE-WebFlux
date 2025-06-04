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

const normalizeUser = (userData) => {
  if (!userData) {
    return null;
  }
  // Create a shallow copy to avoid mutating the original object.
  const normalized = { ...userData };

  if (normalized.roles && Array.isArray(normalized.roles)) {
    // Assuming roles is an array of strings or simple sortable values.
    // If roles are objects, a more complex sorting logic based on a key (e.g., role.name) would be needed.
    // For now, let's assume they are strings or directly sortable.
    normalized.roles = [...normalized.roles].sort(); 
  } else {
    // If roles are not an array or undefined, ensure it's a consistent empty array for comparison.
    normalized.roles = [];
  }
  return normalized;
};

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState(null);

  const userForChildren = React.useMemo(() => {
    if (!user) return null;
    // Construct a new object with only the specific fields children need.
    // This ensures that if 'user' state in App.js has other, perhaps unstable,
    // utility fields, they don't cause the memoized object to change reference unnecessarily.
    return {
      id: user.id,
      name: user.name,
      roles: user.roles // Assuming roles array reference is stable if data is same
    };
  }, [user?.id, user?.name, JSON.stringify(user?.roles)]); // Depend on primitive/stable values
  // Note: If user itself is null, userForChildren will be null.
  // The dependency on JSON.stringify(user?.roles) is to ensure it changes if roles content changes

// At the very top of the App function component body
console.log('App.js: Rendering. User state id:', user?.id, 'Auth:', isAuthenticated, 'Loading:', loading);
console.log('App.js: userForChildren id:', userForChildren?.id, 'userForChildren ref:', userForChildren); // Log the memoized version too


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
          const normalizedCurrentUser = normalizeUser(currentUserInState);
          const normalizedResolvedUser = normalizeUser(resolvedUser);

          console.log('App.js PAT: Comparing normalizedCurrentUser:', JSON.stringify(normalizedCurrentUser));
          console.log('App.js PAT: With normalizedResolvedUser:', JSON.stringify(normalizedResolvedUser));

          const idMatch = normalizedCurrentUser?.id === normalizedResolvedUser?.id;
          const nameMatch = normalizedCurrentUser?.name === normalizedResolvedUser?.name;
          
          // Compare stringified roles AFTER normalization
          const rolesCurrentStr = JSON.stringify(normalizedCurrentUser?.roles || []);
          const rolesResolvedStr = JSON.stringify(normalizedResolvedUser?.roles || []);
          const rolesMatchDetailed = rolesCurrentStr === rolesResolvedStr;
        
          console.log(`App.js PAT Details: idMatch: ${idMatch} (Current: ${normalizedCurrentUser?.id}, Resolved: ${normalizedResolvedUser?.id})`);
          console.log(`App.js PAT Details: nameMatch: ${nameMatch} (Current: ${normalizedCurrentUser?.name}, Resolved: ${normalizedResolvedUser?.name})`);
          console.log(`App.js PAT Details: rolesMatch: ${rolesMatchDetailed} (Current: ${rolesCurrentStr}, Resolved: ${rolesResolvedStr})`);
        
          const userIsEffectivelyTheSame =
            (normalizedCurrentUser === null && normalizedResolvedUser === null) ||
            (normalizedCurrentUser !== null && normalizedResolvedUser !== null && idMatch && nameMatch && rolesMatchDetailed);
        
          if (userIsEffectivelyTheSame) {
            console.log('App.js PAT: setUser - user data IS effectively the same. Returning current state (NO CHANGE).');
            return currentUserInState; // Return original currentUserInState to preserve reference if truly same
          }
          
          console.log('App.js PAT: setUser - user data IS DIFFERENT or involves null transition. Returning (normalized) resolvedUser.');
          // Important: Store the normalized version if it's different, or the original resolvedUser if normalization is only for comparison.
          // For consistency, let's store the normalized version.
          return normalizedResolvedUser; 
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
    if (!newUserData || typeof newUserData.id === 'undefined') {
        console.error('App.js: handleLogin called with invalid or incomplete newUserData.', newUserData);
        return;
    }
    console.log('App.js: handleLogin called with newUserData:', JSON.stringify(newUserData, null, 2));
    console.log('App.js: handleLogin triggered. newUserData.id:', newUserData.id, 'Current App user.id before update attempt:', user?.id, 'App loading state:', loading, 'App isAuthenticated:', isAuthenticated);
  
    if (loading) {
      console.log('App.js: handleLogin - Still in loading state. performAuthCheck is expected to handle initial user setup. Aborting handleLogin.');
      return;
    }
  
    setIsAuthenticated(currentIsAuth => {
      if (!currentIsAuth) {
        console.log('App.js: handleLogin - isAuthenticated was false. Setting to true.');
        return true;
      }
      return currentIsAuth;
    });
  
    // --- This is the ONLY setUser logic that should be in handleLogin for this test ---
    console.log('App.js: handleLogin - Preparing for DIRECT setUser call.');
    const normalizedDataToSet = normalizeUser(newUserData);
    console.log('App.js: handleLogin - Directly calling setUser with (normalized):', JSON.stringify(normalizedDataToSet));
    setUser(normalizedDataToSet);
    // --- End of diagnostic setUser logic ---
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
            <Layout user={userForChildren} onLogout={handleLogout}>
              <Dashboard user={userForChildren} />
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
            <Layout user={userForChildren} onLogout={handleLogout}>
              <Dashboard user={userForChildren} />
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
            <Layout user={userForChildren} onLogout={handleLogout}>
              <NotificationList user={userForChildren} />
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
            <Layout user={userForChildren} onLogout={handleLogout}> 
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
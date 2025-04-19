import axios from 'axios';
import jwtDecode from 'jwt-decode';

const API_URL = '/api/auth';

/**
 * Register a new user
 */
export const register = async (username, password) => {
  try {
    const response = await axios.post(`${API_URL}/register`, { username, password });
    return response.data;
  } catch (error) {
    console.error('Registration failed:', error);
    throw error;
  }
};

/**
 * Authenticate user with backend and get JWT token
 */
export const login = async (username, password) => {
  try {
    // Call the actual backend API endpoint
    const response = await axios.post(`${API_URL}/login`, { username, password });
    
    // Get the token from the response
    const token = response.data.token;
    
    // Store the token in localStorage
    localStorage.setItem('token', token);
    
    // Decode the token to get user information
    const userData = jwtDecode(token);
    
    return {
      id: userData.sub,
      name: userData.sub, // Using username as name since that's what we have
      roles: userData.roles
    };
  } catch (error) {
    console.error('Login failed:', error);
    throw error;
  }
};

export const checkAuthStatus = () => {
  const token = localStorage.getItem('token');
  
  if (!token) {
    return null;
  }
  
  try {
    // Decode the token
    const userData = jwtDecode(token);
    
    // Check if token is expired
    const currentTime = Date.now() / 1000;
    if (userData.exp && userData.exp < currentTime) {
      localStorage.removeItem('token');
      return null;
    }
    
    return {
      id: userData.sub,
      name: userData.name,
      roles: userData.roles
    };
  } catch (error) {
    console.error('Token validation failed:', error);
    localStorage.removeItem('token');
    return null;
  }
};

// Validate token with backend
export const validateTokenWithBackend = async () => {
  const token = localStorage.getItem('token');
  if (!token) return null;
  try {
    const response = await axios.get('/api/auth/validate', {
      headers: { Authorization: `Bearer ${token}` }
    });
    // Response should have user info (token, username, roles)
    return {
      id: response.data.username,
      name: response.data.username,
      roles: response.data.roles
    };
  } catch (error) {
    localStorage.removeItem('token');
    return null;
  }
};

export const getAuthHeader = () => {
  const token = localStorage.getItem('token');
  
  if (token) {
    return { Authorization: `Bearer ${token}` };
  } else {
    return {};
  }
};
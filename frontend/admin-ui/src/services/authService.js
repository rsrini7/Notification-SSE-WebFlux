import axios from 'axios';
import jwt_decode from 'jwt-decode';

const API_URL = '/api/auth';

// Login function for admin users
export const login = async (username, password, isAdmin = true) => {
  try {
    const response = await axios.post(`${API_URL}/login`, {
      username,
      password,
      role: isAdmin ? 'ADMIN' : 'USER'
    });
    
    const { token, user } = response.data;
    
    // Store the token in localStorage
    localStorage.setItem('token', token);
    
    return user;
  } catch (error) {
    console.error('Login error:', error);
    throw error;
  }
};

// Get the authentication header with JWT token
export const getAuthHeader = () => {
  const token = localStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
};

// Check if the user is authenticated
export const checkAuthStatus = async () => {
  const token = localStorage.getItem('token');
  
  if (!token) {
    return null;
  }
  
  try {
    // Decode the token to check if it's expired
    const decoded = jwt_decode(token);
    const currentTime = Date.now() / 1000;
    
    if (decoded.exp < currentTime) {
      // Token is expired
      localStorage.removeItem('token');
      return null;
    }
    
    // Verify token with backend
    const response = await axios.get(`${API_URL}/validate`, {
      headers: getAuthHeader()
    });
    
    return response.data;
  } catch (error) {
    console.error('Token validation error:', error);
    localStorage.removeItem('token');
    return null;
  }
};

// Logout function
export const logout = () => {
  localStorage.removeItem('token');
};
import axios from 'axios';
import jwtDecode from 'jwt-decode';

const API_URL = '/api/auth';

// For demo purposes, we'll simulate authentication
// In a real application, this would connect to the backend auth endpoints
export const login = async (username, password) => {
  try {
    // In a real app, this would be an actual API call
    // const response = await axios.post(`${API_URL}/login`, { username, password });
    
    // For demo, we'll simulate a successful login with a mock token
    const mockToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwibmFtZSI6IkpvaG4gRG9lIiwicm9sZXMiOlsiVVNFUiJdLCJpYXQiOjE1MTYyMzkwMjJ9.sKlQsOUDWP7BXKvKwDV-KfGXHGqiBSYi-lGHEJJELTM';
    
    // Store the token in localStorage
    localStorage.setItem('token', mockToken);
    
    // Decode the token to get user information
    const userData = jwtDecode(mockToken);
    
    return {
      id: userData.sub,
      name: userData.name,
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

export const getAuthHeader = () => {
  const token = localStorage.getItem('token');
  
  if (token) {
    return { Authorization: `Bearer ${token}` };
  } else {
    return {};
  }
};
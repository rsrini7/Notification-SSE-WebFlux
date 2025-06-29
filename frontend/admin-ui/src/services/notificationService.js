import axios from 'axios';
import { getAuthHeader } from './authService';

const ADMIN_BACKEND_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080';
const ADMIN_API_URL = `${ADMIN_BACKEND_URL}/api/admin/notifications`;

// Get notification statistics for admin dashboard
export const getNotificationStats = async () => {
  try {
    const headers = getAuthHeader();
    if (!headers || !headers.Authorization) {
      throw new Error('AUTH_REQUIRED');
    }

    const response = await axios.get(`${ADMIN_API_URL}/stats`, {
      headers: headers
    });
    
    // Handle potential null keys in the response data
    const sanitizedData = Object.entries(response.data).reduce((acc, [key, value]) => {
      if (key !== null) {
        acc[key] = value;
      }
      return acc;
    }, {});
    
    return sanitizedData;
  } catch (error) {
    if (error.message === 'AUTH_REQUIRED' || error.response?.status === 401) {
      throw new Error('Authentication required. Please log in.');
    }
    console.error('Error fetching notification stats:', error);
    throw error;
  }
};

// Update other methods with similar error handling
export const getRecentNotifications = async (limit = 5) => {
  try {
    const headers = getAuthHeader();
    if (!headers || !headers.Authorization) {
      throw new Error('AUTH_REQUIRED');
    }

    const response = await axios.get(`${ADMIN_API_URL}/recent?limit=${limit}`, {
      headers: headers
    });
    return response.data;
  } catch (error) {
    if (error.message === 'AUTH_REQUIRED' || error.response?.status === 401) {
      throw new Error('Authentication required. Please log in.');
    }
    console.error('Error fetching recent notifications:', error);
    throw error;
  }
};

// Send a notification to specific users
export const sendNotification = async (notificationData) => {
  try {
    const eventId = crypto.randomUUID(); // Generate UUID
    const payload = { ...notificationData, eventId }; // Add to payload
    const response = await axios.post(`${ADMIN_API_URL}/send`, payload, { // Use payload
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error sending notification:', error);
    throw error;
  }
};

// Send a broadcast notification to all users
export const sendBroadcastNotification = async (notificationData) => {
  try {
    const eventId = crypto.randomUUID(); // Generate UUID
    const payload = { ...notificationData, eventId }; // Add to payload
    const response = await axios.post(`${ADMIN_API_URL}/broadcast`, payload, { // Use payload and correct ADMIN_API_URL
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error sending broadcast notification:', error);
    throw error;
  }
};

// Get all notification types
export const getNotificationTypes = async () => {
  try {
    const response = await axios.get(`${ADMIN_API_URL}/types`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching notification types:', error);
    throw error;
  }
};

// Get all users for targeting notifications
export const getUsers = async () => {
  try {
    const response = await axios.get(`${ADMIN_BACKEND_URL}/api/users`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching users:', error);
    throw error;
  }
};

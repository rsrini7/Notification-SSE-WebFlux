import axios from 'axios';
import { getAuthHeader } from './authService';

const API_URL = '/api/notifications';
const ADMIN_API_URL = '/api/admin/notifications';

// Get notification statistics for admin dashboard
export const getNotificationStats = async () => {
  try {
    const response = await axios.get(`${ADMIN_API_URL}/stats`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching notification stats:', error);
    throw error;
  }
};

// Get recent notifications for admin dashboard
export const getRecentNotifications = async (limit = 5) => {
  try {
    const response = await axios.get(`${ADMIN_API_URL}/recent?limit=${limit}`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching recent notifications:', error);
    throw error;
  }
};

// Send a notification to specific users
export const sendNotification = async (notificationData) => {
  try {
    const response = await axios.post(`${ADMIN_API_URL}/send`, notificationData, {
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
    const response = await axios.post(`${ADMIN_API_URL}/broadcast`, notificationData, {
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
    const response = await axios.get(`${API_URL}/types`, {
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
    const response = await axios.get('/api/users', {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching users:', error);
    throw error;
  }
};
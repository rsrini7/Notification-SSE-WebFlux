import axios from 'axios';
import { getAuthHeader } from './authService';
import { 
  connectToSse,
  subscribeToSse,
  disconnectFromSse
} from './sseService'; // Updated import

const BACKEND_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080';

// SSE connection for real-time notifications
export const connectToRealtimeNotifications = (userId) => { // Renamed for clarity
  return connectToSse(userId);
};

export const subscribeToRealtimeNotifications = (callback) => { // Renamed for clarity
  return subscribeToSse(callback);
};

export const disconnectFromRealtimeNotifications = () => { // Renamed for clarity
  disconnectFromSse();
};

// API functions for fetching notifications
export const fetchNotifications = async (params = {}) => {
  try {
    const response = await axios.get('/api/notifications', {
      params: {
        page: 0,
        size: 10,
        sort: 'createdAt,desc',
        ...params
      },
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching notifications:', error);
    throw error;
  }
};

export const markAsRead = async (notificationId) => {
  try {
    const response = await axios.put(
      `${BACKEND_URL}/api/notifications/${notificationId}/read`,
      {},
      { headers: getAuthHeader() }
    );
    return response.data;
  } catch (error) {
    console.error('Error marking notification as read:', error);
    throw error;
  }
};

export const markAllAsRead = async () => {
  try {
    const response = await axios.put(
      `${BACKEND_URL}/api/notifications/read-all`,
      {},
      { headers: getAuthHeader() }
    );
    return response.data;
  } catch (error) {
    console.error('Error marking all notifications as read:', error);
    throw error;
  }
};

// Get all notifications for a user
export const getNotifications = async (userId, page = 0, size = 10) => {
  try {
    const response = await axios.get(`${BACKEND_URL}/api/notifications/user/${userId}?page=${page}&size=${size}`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching notifications:', error);
    throw error;
  }
};

// Get unread notifications count
export const getUnreadCount = async (userId) => {
  try {
    const response = await axios.get(`${BACKEND_URL}/api/notifications/user/${userId}/unread-count`, {
      headers: getAuthHeader()
    });
    return response.data.count;
  } catch (error) {
    console.error('Error fetching unread count:', error);
    throw error;
  }
};

// Get all notification types
export const getNotificationTypes = async () => {
  try {
    const response = await axios.get(`${BACKEND_URL}/api/notifications/types`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching notification types:', error);
    throw error;
  }
};


// Get notification by ID
export const getNotificationById = async (notificationId) => {
  try {
    const response = await axios.get(`${BACKEND_URL}/api/notifications/${notificationId}`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching notification:', error);
    throw error;
  }
};

// Get unread notifications for a user
export const getUnreadNotifications = async (userId, page = 0, size = 10) => {
  try {
    const response = await axios.get(`${BACKEND_URL}/api/notifications/user/${userId}/unread?page=${page}&size=${size}`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching unread notifications:', error);
    throw error;
  }
};

// Get notifications by type for a user
export const getNotificationsByType = async (userId, notificationType, page = 0, size = 10) => {
  try {
    const response = await axios.get(`${BACKEND_URL}/api/notifications/user/${userId}/type/${notificationType}?page=${page}&size=${size}`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error(`Error fetching ${notificationType} notifications:`, error);
    throw error;
  }
};

// Search notifications by keyword
export const searchNotifications = async (userId, keyword, page = 0, size = 10) => {
  try {
    const response = await axios.get(`${BACKEND_URL}/api/notifications/user/${userId}/search?q=${encodeURIComponent(keyword)}&page=${page}&size=${size}`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error searching notifications:', error);
    throw error;
  }
};

// Mark a notification as read
export const markNotificationAsRead = async (id, userId) => {
  try {
    const response = await axios.put(`${BACKEND_URL}/api/notifications/${id}/read?userId=${userId}`, {}, {
      headers: getAuthHeader()
    });
    return response.status === 200;
  } catch (error) {
    console.error(`Error marking notification ${id} as read:`, error);
    throw error;
  }
};

// Mark all notifications as read for a user
export const markAllNotificationsAsRead = async (userId) => {
  try {
    // The backend endpoint is ${BACKEND_URL}/api/notifications/user/{userId}/read-all
    const response = await axios.put(`${BACKEND_URL}/api/notifications/user/${userId}/read-all`, {}, {
      headers: getAuthHeader()
    });
    return response.status === 200;
  } catch (error) {
    console.error('Error marking all notifications as read:', error);
    throw error;
  }
};

// Delete a notification
export const deleteNotification = async (id, userId) => {
  try {
    const response = await axios.delete(`${BACKEND_URL}/api/notifications/${id}?userId=${userId}`, {
      headers: getAuthHeader()
    });
    return response.status === 200;
  } catch (error) {
    console.error(`Error deleting notification with ID ${id}:`, error);
    throw error;
  }
};

// Count unread notifications for a user
export const countUnreadNotifications = async (userId, signal) => { // Added signal parameter
  try {
    const response = await axios.get(`${BACKEND_URL}/api/notifications/user/${userId}/unread/count`, {
      headers: getAuthHeader(),
      signal: signal // Pass the signal to axios
    });
    return response.data;
  } catch (error) {
    if (axios.isCancel(error)) {
      console.log('Request canceled:', error.message);
    } else {
      console.error('Error counting unread notifications:', error);
    }
    throw error; // Re-throw error so callers can handle it, including cancellations
  }
};

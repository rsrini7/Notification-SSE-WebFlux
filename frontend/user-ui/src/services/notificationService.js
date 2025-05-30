import axios from 'axios';
import { getAuthHeader } from './authService';
import { 
  connectToWebSocket as wsConnect, 
  subscribeToNotifications as wsSubscribe, 
  disconnectFromWebSocket as wsDisconnect 
} from './websocketService';

// WebSocket connection for real-time notifications
export const connectToWebSocket = (userId) => {
  return wsConnect(userId);
};

export const subscribeToNotifications = (callback) => {
  return wsSubscribe(callback);
};

export const disconnectFromWebSocket = () => {
  wsDisconnect();
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
      `/api/notifications/${notificationId}/read`,
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
      '/api/notifications/read-all',
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
    const response = await axios.get(`/api/notifications/user/${userId}?page=${page}&size=${size}`, {
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
    const response = await axios.get(`/api/notifications/user/${userId}/unread-count`, {
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
    const response = await axios.get('/api/notifications/types', {
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
    const response = await axios.get(`/api/notifications/${notificationId}`, {
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
    const response = await axios.get(`/api/notifications/user/${userId}/unread?page=${page}&size=${size}`, {
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
    const response = await axios.get(`/api/notifications/user/${userId}/type/${notificationType}?page=${page}&size=${size}`, {
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
    const response = await axios.get(`/api/notifications/user/${userId}/search?q=${encodeURIComponent(keyword)}&page=${page}&size=${size}`, {
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
    const response = await axios.put(`/api/notifications/${id}/read?userId=${userId}`, {}, {
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
    // The backend endpoint is /api/notifications/user/{userId}/read-all
    const response = await axios.put(`/api/notifications/user/${userId}/read-all`, {}, {
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
    const response = await axios.delete(`/api/notifications/${id}?userId=${userId}`, {
      headers: getAuthHeader()
    });
    return response.status === 200;
  } catch (error) {
    console.error(`Error deleting notification with ID ${id}:`, error);
    throw error;
  }
};

// Count unread notifications for a user
export const countUnreadNotifications = async (userId) => {
  try {
    const response = await axios.get(`/api/notifications/user/${userId}/unread/count`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error counting unread notifications:', error);
    throw error;
  }
};
// WebSocket service is now implemented as a class above
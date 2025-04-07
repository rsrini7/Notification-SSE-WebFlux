import axios from 'axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getAuthHeader } from './authService';

const API_URL = '/api/notifications';

// Get all notifications for a user
export const getNotifications = async (userId, page = 0, size = 10) => {
  try {
    const response = await axios.get(`${API_URL}/user/${userId}?page=${page}&size=${size}`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching notifications:', error);
    throw error;
  }
};

// Get unread notifications for a user
export const getUnreadNotifications = async (userId, page = 0, size = 10) => {
  try {
    const response = await axios.get(`${API_URL}/user/${userId}/unread?page=${page}&size=${size}`, {
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
    const response = await axios.get(`${API_URL}/user/${userId}/type/${notificationType}?page=${page}&size=${size}`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error(`Error fetching notifications of type ${notificationType}:`, error);
    throw error;
  }
};

// Search notifications for a user
export const searchNotifications = async (userId, searchTerm, page = 0, size = 10) => {
  try {
    const response = await axios.get(`${API_URL}/user/${userId}/search?searchTerm=${encodeURIComponent(searchTerm)}&page=${page}&size=${size}`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error searching notifications:', error);
    throw error;
  }
};

// Get a notification by ID
export const getNotificationById = async (id) => {
  try {
    const response = await axios.get(`${API_URL}/${id}`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error(`Error fetching notification with ID ${id}:`, error);
    throw error;
  }
};

// Mark a notification as read
export const markNotificationAsRead = async (id, userId) => {
  try {
    const response = await axios.put(`${API_URL}/${id}/read?userId=${userId}`, {}, {
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
    const response = await axios.put(`${API_URL}/user/${userId}/read-all`, {}, {
      headers: getAuthHeader()
    });
    return response.data; // Returns count of marked notifications
  } catch (error) {
    console.error('Error marking all notifications as read:', error);
    throw error;
  }
};

// Count unread notifications for a user
export const countUnreadNotifications = async (userId) => {
  try {
    const response = await axios.get(`${API_URL}/user/${userId}/unread/count`, {
      headers: getAuthHeader()
    });
    return response.data;
  } catch (error) {
    console.error('Error counting unread notifications:', error);
    throw error;
  }
};

// WebSocket connection for real-time notifications
let stompClient = null;

export const connectToWebSocket = (userId, onMessageReceived) => {
  if (stompClient) {
    disconnectFromWebSocket();
  }

  const socket = new SockJS('/ws');
  stompClient = new Client({
    webSocketFactory: () => socket,
    debug: (str) => {
      console.log(str);
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });

  stompClient.onConnect = (frame) => {
    console.log('Connected to WebSocket:', frame);
    
    // Subscribe to user-specific notifications
    stompClient.subscribe(`/user/${userId}/notifications`, (message) => {
      const notification = JSON.parse(message.body);
      onMessageReceived(notification);
    });
    
    // Subscribe to broadcast notifications
    stompClient.subscribe('/topic/broadcast', (message) => {
      const notification = JSON.parse(message.body);
      onMessageReceived(notification);
    });
  };

  stompClient.onStompError = (frame) => {
    console.error('WebSocket error:', frame);
  };

  stompClient.activate();
};

export const disconnectFromWebSocket = () => {
  if (stompClient && stompClient.connected) {
    stompClient.deactivate();
    console.log('Disconnected from WebSocket');
  }
};
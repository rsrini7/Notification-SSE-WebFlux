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
let isConnected = false;
const subscribers = new Set(); // Store multiple onMessageReceived callbacks
let currentUserId = null;

export const subscribeToNotifications = (callback) => {
   subscribers.add(callback);
   return () => subscribers.delete(callback); // Return an unsubscribe function
  };

export const connectToWebSocket = (userId) => {
  if (stompClient && stompClient.active && userId === currentUserId) {
    console.log('WebSocket already connected for user:', userId);
    return; // Already connected or connecting for the same user
  }

  if (stompClient && stompClient.active) {
    stompClient.deactivate(); // Deactivate if connected for a different user
  }

  currentUserId = userId;

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
    isConnected = true;

    // Subscribe to user-specific notifications
    stompClient.subscribe(`/user/${userId}/queue/notifications`, (message) => {
      const notification = JSON.parse(message.body);
      subscribers.forEach(callback => callback(notification));
    });
  };

  stompClient.onDisconnect = () => {
      console.log('Disconnected from WebSocket');
      isConnected = false;
  };

  stompClient.onStompError = (frame) => {
    console.error('WebSocket error:', frame);
    isConnected = false;
  };
  
  stompClient.activate();
};



export const disconnectFromWebSocket = () => {
  if (stompClient && stompClient.active) {
    stompClient.deactivate();
  }
  subscribers.clear();
  stompClient = null;
  isConnected = false;
  currentUserId = null;
};
import axios from 'axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getAuthHeader } from './authService';

// Simple WebSocket service with singleton pattern
class WebSocketService {
  constructor() {
    this.client = null;
    this.subscribers = new Set();
    this.isConnected = false;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 5000;
    this.userId = null;
    this.subscription = null;
  }

  // Initialize WebSocket connection
  connect(userId) {
    if (this.client?.connected && this.userId === userId) {
      console.log('WebSocket already connected for user:', userId);
      return Promise.resolve();
    }

    // Disconnect existing connection if any
    this.disconnect();

    this.userId = userId;
    console.log('Connecting WebSocket for user:', userId);

    return new Promise((resolve, reject) => {
      try {
        const socket = new SockJS('/ws');
        
        this.client = new Client({
          webSocketFactory: () => socket,
          debug: (str) => {
            // Filter out heartbeat messages
            if (!str.includes('>>>') && !str.includes('<<<')) {
              console.log(str);
            }
          },
          reconnectDelay: this.reconnectDelay,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
        });

        this.client.onConnect = (frame) => {
          console.log('WebSocket connected:', frame);
          this.isConnected = true;
          this.reconnectAttempts = 0;
          this.subscribeToNotifications();
          resolve();
        };

        this.client.onStompError = (frame) => {
          console.error('STOMP error:', frame);
          this.handleReconnection();
          reject(new Error('STOMP connection error'));
        };

        this.client.onWebSocketClose = () => {
          console.log('WebSocket closed');
          this.isConnected = false;
          this.handleReconnection();
        };

        this.client.activate();
      } catch (error) {
        console.error('WebSocket connection error:', error);
        this.handleReconnection();
        reject(error);
      }
    });
  }


  // Handle reconnection logic
  handleReconnection() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
      console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      
      setTimeout(() => {
        if (this.userId) {
          this.connect(this.userId).catch(console.error);
        }
      }, delay);
    } else {
      console.error('Max reconnection attempts reached');
    }
  }

  // Subscribe to user notifications
  subscribeToNotifications() {
    if (!this.client || !this.client.connected || !this.userId) {
      console.error('Cannot subscribe: WebSocket not connected');
      return;
    }

    // Unsubscribe from previous subscription if exists
    if (this.subscription) {
      try {
        this.subscription.unsubscribe();
      } catch (e) {
        console.warn('Error unsubscribing:', e);
      }
      this.subscription = null;
    }

    try {
      const destination = `/user/${this.userId}/queue/notifications`;
      this.subscription = this.client.subscribe(
        destination,
        (message) => this.handleIncomingMessage(message),
        { id: `sub-${Date.now()}` }
      );
      console.log('Subscribed to:', destination);
    } catch (error) {
      console.error('Subscription error:', error);
    }
  }

  // Handle incoming messages
  handleIncomingMessage(message) {
    try {
      if (!message || !message.body) {
        console.warn('Received empty message');
        return;
      }
      
      const notification = JSON.parse(message.body);
      console.log('Received notification:', notification);
      
      // Notify all subscribers
      this.subscribers.forEach(callback => {
        try {
          callback(notification);
        } catch (error) {
          console.error('Error in notification callback:', error);
        }
      });
    } catch (error) {
      console.error('Error processing message:', error, message);
    }
  }

  // Add a subscriber
  subscribe(callback) {
    this.subscribers.add(callback);
    console.log('Added subscriber, total:', this.subscribers.size);
    
    // Return unsubscribe function
    return () => {
      this.subscribers.delete(callback);
      console.log('Removed subscriber, remaining:', this.subscribers.size);
    };
  }

  // Disconnect WebSocket
  disconnect() {
    if (this.subscription) {
      try {
        this.subscription.unsubscribe();
      } catch (e) {
        console.warn('Error unsubscribing:', e);
      }
      this.subscription = null;
    }

    if (this.client) {
      if (this.client.connected) {
        try {
          this.client.deactivate();
        } catch (e) {
          console.warn('Error deactivating client:', e);
        }
      }
      this.client = null;
    }

    this.isConnected = false;
    this.userId = null;
    this.reconnectAttempts = 0;
    console.log('WebSocket disconnected');
  }
}

// Export a singleton instance
const webSocketService = new WebSocketService();

// Clean up on page unload
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    webSocketService.disconnect();
  });
}

const API_URL = '/api/notifications';

// Export WebSocket service functions
export const connectToWebSocket = (userId) => webSocketService.connect(userId);
export const subscribeToNotifications = (callback) => webSocketService.subscribe(callback);
export const disconnectFromWebSocket = () => webSocketService.disconnect();

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



// WebSocket service is now implemented as a class above
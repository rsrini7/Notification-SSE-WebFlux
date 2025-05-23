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
const subscribers = new Set();
let currentUserId = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;
let activeSubscription = null;
let isConnecting = false;

// Function to handle new messages
const handleIncomingMessage = (message) => {
  try {
    if (!message || !message.body) {
      console.warn('Received empty or invalid WebSocket message');
      return;
    }
    console.log('Received WebSocket message:', message);
    const notification = JSON.parse(message.body);
    subscribers.forEach(callback => {
      try {
        callback(notification);
      } catch (error) {
        console.error('Error in notification callback:', error);
      }
    });
  } catch (error) {
    console.error('Error processing WebSocket message:', error);
  }
};

// Subscribe to notifications
export const subscribeToNotifications = (callback) => {
  console.log('Adding WebSocket subscriber');
  subscribers.add(callback);
  
  // Return an unsubscribe function
  return () => {
    console.log('Removing WebSocket subscriber');
    subscribers.delete(callback);
  };
};

// Subscribe to user notifications
const subscribeToUserNotifications = (client, userId) => {
  if (!client || !client.connected) {
    console.error('Cannot subscribe: WebSocket not connected');
    return null;
  }

  try {
    // Unsubscribe from any existing subscription
    if (activeSubscription) {
      try {
        activeSubscription.unsubscribe();
      } catch (e) {
        console.warn('Error unsubscribing from previous subscription:', e);
      }
    }

    // Create new subscription
    const subscription = client.subscribe(
      `/user/${userId}/queue/notifications`,
      handleIncomingMessage,
      { id: `sub-${Date.now()}` } // Use timestamp to ensure unique ID
    );
    
    console.log('Subscribed to notifications with ID:', subscription.id);
    activeSubscription = subscription;
    return subscription;
  } catch (error) {
    console.error('Error subscribing to notifications:', error);
    return null;
  }
};

// Connect to WebSocket server
export const connectToWebSocket = (userId) => {
  if (!userId) {
    console.error('Cannot connect to WebSocket: No user ID provided');
    return Promise.reject(new Error('No user ID provided'));
  }

  // If already connected for this user, just ensure subscription
  if (stompClient?.connected && userId === currentUserId) {
    console.log('WebSocket already connected for user:', userId);
    return Promise.resolve(stompClient);
  }

  // If already connecting, don't start a new connection
  if (isConnecting) {
    console.log('WebSocket connection already in progress');
    return Promise.resolve(stompClient);
  }

  // Disconnect if connected to a different user
  if (stompClient) {
    console.log('Disconnecting from previous WebSocket connection');
    disconnectFromWebSocket();
  }

  currentUserId = userId;
  isConnecting = true;
  console.log('Initializing WebSocket connection for user:', userId);

  return new Promise((resolve, reject) => {
    try {
      const socket = new SockJS('/ws');
      
      stompClient = new Client({
        webSocketFactory: () => socket,
        debug: (str) => {
          // Filter out heartbeat messages from logs
          if (!str.includes('>>>') && !str.includes('<<<')) {
            console.log(str);
          }
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: (frame) => {
          console.log('Successfully connected to WebSocket');
          isConnected = true;
          isConnecting = false;
          reconnectAttempts = 0;

          // Subscribe to user-specific notifications
          const subscription = subscribeToUserNotifications(stompClient, userId);
          if (subscription) {
            console.log('Successfully subscribed to notifications');
          } else {
            console.error('Failed to subscribe to notifications');
          }
          
          resolve(stompClient);
        },
        onDisconnect: (frame) => {
          console.log('Disconnected from WebSocket');
          isConnected = false;
          isConnecting = false;
        },
        onStompError: (frame) => {
          console.error('WebSocket STOMP error:', frame);
          isConnected = false;
          isConnecting = false;
          
          // Attempt to reconnect with exponential backoff
          if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000);
            console.log(`Attempting to reconnect in ${delay}ms (attempt ${reconnectAttempts + 1}/${MAX_RECONNECT_ATTEMPTS})`);
            reconnectAttempts++;
            setTimeout(() => connectToWebSocket(userId), delay);
          } else {
            console.error('Max reconnection attempts reached');
            reject(new Error('Max reconnection attempts reached'));
          }
        },
        onWebSocketClose: (event) => {
          console.log('WebSocket closed:', event);
          isConnected = false;
          isConnecting = false;
        },
        onWebSocketError: (event) => {
          console.error('WebSocket error:', event);
          isConnected = false;
          isConnecting = false;
          reject(event);
        }
      });

      // Activate the client
      console.log('Activating WebSocket client');
      stompClient.activate();
    } catch (error) {
      console.error('Error initializing WebSocket:', error);
      isConnecting = false;
      reject(error);
    }
  });
};

// Disconnect from WebSocket server
export const disconnectFromWebSocket = () => {
  if (stompClient) {
    console.log('Disconnecting from WebSocket');
    isConnecting = false;
    
    // Clear any active subscription
    if (activeSubscription) {
      try {
        activeSubscription.unsubscribe();
      } catch (e) {
        console.warn('Error unsubscribing:', e);
      }
      activeSubscription = null;
    }

    // Disconnect the client
    if (stompClient.connected) {
      try {
        stompClient.deactivate()
          .then(() => {
            console.log('Successfully disconnected from WebSocket');
          })
          .catch(error => {
            console.error('Error during WebSocket deactivation:', error);
          })
          .finally(() => {
            stompClient = null;
          });
      } catch (error) {
        console.error('Error during WebSocket deactivation:', error);
        stompClient = null;
      }
    } else {
      stompClient = null;
    }
  }
  
  isConnected = false;
  currentUserId = null;
};

// Clean up on page unload
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    disconnectFromWebSocket();
  });
}
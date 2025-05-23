import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
  constructor() {
    this.stompClient = null;
    this.subscribers = [];
    this.isConnected = false;
    this.userId = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectTimeout = null;
  }

  connect(userId) {
    if (this.stompClient?.connected && this.userId === userId) {
      console.log('WebSocket already connected for user:', userId);
      return Promise.resolve();
    }

    this.disconnect();
    this.userId = userId;
    
    return new Promise((resolve, reject) => {
      try {
        console.log('Connecting WebSocket for user:', userId);
        
        // Create a new SockJS connection with error handling
        const backendUrl = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080';
        const socket = new SockJS(`${backendUrl}/ws`);
        
        const token = localStorage.getItem('token');
        
        // Add error handler for SockJS
        socket.onerror = (error) => {
          console.error('SockJS error:', error);
          reject(new Error('Failed to connect to WebSocket server'));
        };
        
        // Create a new STOMP client
        this.stompClient = new Client({
          webSocketFactory: () => socket,
          debug: (str) => {
            // Filter out heartbeat messages from logs
            if (!str.includes('>>>') && !str.includes('<<<') && !str.includes('heartbeat')) {
              console.log('STOMP:', str);
            }
          },
          reconnectDelay: 5000,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          connectHeaders: {
            'user-id': userId,
            'accept-version': '1.2,1.1,1.0',
            'heart-beat': '4000,4000',
            ...(token && { 'Authorization': `Bearer ${token}` }),
          },
          logRawCommunication: true,
          onStompError: (frame) => {
            console.error('Broker reported error: ' + frame.headers['message']);
            console.error('Additional details: ' + frame.body);
            this.handleReconnection();
          },
          onWebSocketClose: (event) => {
            console.log('WebSocket closed:', event);
            this.isConnected = false;
            this.handleReconnection();
          },
          onWebSocketError: (error) => {
            console.error('WebSocket error:', error);
            this.handleReconnection();
          }
        });

        // Set up connection callbacks
        this.stompClient.onConnect = (frame) => {
          console.log('STOMP connection established');
          this.isConnected = true;
          this.reconnectAttempts = 0;
          
          // Subscribe to the user's notification queue
          this.subscribeToNotifications();
          resolve();
        };

        this.stompClient.onStompError = (frame) => {
          console.error('STOMP error:', frame);
          this.handleReconnection();
        };

        this.stompClient.onWebSocketClose = () => {
          console.log('WebSocket connection closed');
          this.isConnected = false;
          this.handleReconnection();
        };

        // Activate the client
        this.stompClient.activate();
      } catch (error) {
        console.error('WebSocket connection error:', error);
        reject(error);
      }
    });
  }

  subscribeToNotifications() {
    if (!this.stompClient?.connected || !this.userId) {
      console.error('Cannot subscribe: WebSocket not connected');
      return;
    }

    try {
      const destination = `/user/${this.userId}/queue/notifications`;
      
      this.stompClient.subscribe(
        destination,
        (message) => {
          try {
            const notification = JSON.parse(message.body);
            console.log('Received notification:', notification);
            this.notifySubscribers(notification);
          } catch (error) {
            console.error('Error processing notification:', error);
          }
        },
        { id: `sub-${Date.now()}` }
      );
      
      console.log('Subscribed to:', destination);
    } catch (error) {
      console.error('Subscription error:', error);
    }
  }

  handleReconnection() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      return;
    }

    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }

    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
    this.reconnectAttempts++;
    
    console.log(`Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
    
    this.reconnectTimeout = setTimeout(() => {
      if (this.userId) {
        this.connect(this.userId).catch(console.error);
      }
    }, delay);
  }

  subscribe(callback) {
    if (typeof callback !== 'function') {
      console.error('Subscriber must be a function');
      return () => {};
    }
    
    this.subscribers.push(callback);
    console.log('Added subscriber, total:', this.subscribers.length);
    
    // Return unsubscribe function
    return () => {
      this.subscribers = this.subscribers.filter(cb => cb !== callback);
      console.log('Removed subscriber, remaining:', this.subscribers.length);
    };
  }

  notifySubscribers(notification) {
    this.subscribers.forEach(callback => {
      try {
        callback(notification);
      } catch (error) {
        console.error('Error in subscriber callback:', error);
      }
    });
  }

  disconnect() {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }

    if (this.stompClient) {
      try {
        if (this.stompClient.connected) {
          this.stompClient.deactivate();
        }
        this.stompClient = null;
      } catch (error) {
        console.error('Error disconnecting WebSocket:', error);
      }
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

export const connectToWebSocket = (userId) => webSocketService.connect(userId);
export const subscribeToNotifications = (callback) => webSocketService.subscribe(callback);
export const disconnectFromWebSocket = () => webSocketService.disconnect();

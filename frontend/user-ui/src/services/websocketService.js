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
        
        const backendUrl = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080';
        const token = localStorage.getItem('token');
        
        // Create headers object for both SockJS and STOMP
        const headers = {
          'user-id': userId,
          'accept-version': '1.2,1.1,1.0',
          'heart-beat': '4000,4000',
        };

        // Add Authorization header if token exists
        if (token) {
          headers['Authorization'] = `Bearer ${token}`;
        }
        
        // Create a new STOMP client with enhanced configuration
        this.stompClient = new Client({
          webSocketFactory: () => {
            // Create SockJS with explicit transports and headers
            const socket = new SockJS(`${backendUrl}/ws`, null, {
              transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
              headers: token ? { 'Authorization': `Bearer ${token}` } : {}
            });
            
            // Add error handler for SockJS
            socket.onerror = (error) => {
              console.error('SockJS connection error:', error);
              reject(new Error('Failed to connect to WebSocket server'));
              this.handleReconnection();
            };
            
            return socket;
          },
          debug: (str) => {
            console.log('STOMP_RAW:', str); // Log the raw string without any filtering
          },
          reconnectDelay: 5000,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          connectHeaders: headers,
          logRawCommunication: true,
          onStompError: (frame) => {
            console.error('STOMP error:', frame);
            console.error('Broker reported error:', frame.headers['message']);
            console.error('Additional details:', frame.body);
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
          
          console.log('In onConnect: this.stompClient.connected =', this.stompClient?.connected);
          console.log('In onConnect: frame object =', frame); 
          
          // Subscribe to the user's notification queue
          console.log('Attempting to subscribe via setTimeout...');
          setTimeout(() => {
            console.log('Inside setTimeout for subscription: this.stompClient.connected =', this.stompClient?.connected);
            if (this.stompClient?.connected) {
              this.subscribeToNotifications();
            } else {
              console.error('Inside setTimeout: STOMP client still not connected, cannot subscribe.');
              // Optionally, trigger another reconnect or error handling if this happens
            }
            resolve(); // Resolve the promise after attempting subscription
          }, 200); // 200ms delay, can be adjusted
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

      // Subscribe to broadcast topic
      const broadcastDestination = '/topic/broadcasts'; 
      this.stompClient.subscribe(
        broadcastDestination,
        (message) => {
          try {
            const broadcastEvent = JSON.parse(message.body);
            console.log('Received broadcast event:', broadcastEvent);
            this.notifySubscribers(broadcastEvent); 
          } catch (error) {
            console.error('Error processing broadcast event:', error);
          }
        },
        { id: `sub-broadcast-${Date.now()}` }
      );
      console.log('Subscribed to broadcast topic:', broadcastDestination);
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
// if (typeof window !== 'undefined') {
//   window.addEventListener('beforeunload', () => {
//     webSocketService.disconnect();
//   });
// }

export const connectToWebSocket = (userId) => webSocketService.connect(userId);
export const subscribeToNotifications = (callback) => webSocketService.subscribe(callback);
export const disconnectFromWebSocket = () => webSocketService.disconnect();

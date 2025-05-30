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
    this.isConnecting = false; // Initialize the flag
    this.connectionPromise = null; // Store the active connection promise
  }

  connect(userId) {
    if (this.isConnecting) {
      console.log('WebSocket connection attempt already in progress. Returning existing promise.');
      return this.connectionPromise || Promise.reject(new Error('Connection attempt in progress, but promise not found.'));
    }

    if (this.stompClient?.connected && this.userId === userId) {
      console.log('WebSocket already connected for user:', userId);
      return Promise.resolve();
    }

    this.isConnecting = true;
    // Disconnect any existing session *before* assigning a new connectionPromise
    this.disconnect(); // Also resets isConnecting, so set it true again for the new attempt.
    this.isConnecting = true; 
    this.userId = userId;
    
    this.connectionPromise = new Promise((resolve, reject) => {
      try {
        console.log('Connecting WebSocket for user:', userId);
        
        const backendUrl = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080';
        const token = localStorage.getItem('token');
        
        const headers = {
          'user-id': userId,
          'accept-version': '1.2,1.1,1.0',
          'heart-beat': '4000,4000',
        };

        if (token) {
          headers['Authorization'] = `Bearer ${token}`;
        }
        
        this.stompClient = new Client({
          webSocketFactory: () => {
            const socket = new SockJS(`${backendUrl}/ws`, null, {
              transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
              headers: token ? { 'Authorization': `Bearer ${token}` } : {}
            });
            
            socket.onerror = (error) => {
              console.error('SockJS connection error:', error);
              this.isConnecting = false;
              this.connectionPromise = null;
              // No automatic reject(new Error(...)) here as handleReconnection will be called by onWebSocketClose or onStompError
              // However, if this is the *first point of failure* for this attempt, rejecting might be needed.
              // For now, let handleReconnection deal with it.
              this.handleReconnection();
            };
            
            return socket;
          },
          debug: (str) => {
            console.log('STOMP_RAW:', str);
          },
          reconnectDelay: 5000, // STOMP.js built-in reconnect delay, not used if we manage manually with handleReconnection
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          connectHeaders: headers,
          logRawCommunication: true,
          onStompError: (frame) => {
            console.error('STOMP error:', frame);
            if (frame.headers && frame.headers['message']) {
              console.error('Broker reported error:', frame.headers['message']);
            }
            if (frame.body) {
              console.error('Additional details:', frame.body);
            }
            this.isConnecting = false;
            this.connectionPromise = null;
            this.handleReconnection(); // Attempt to reconnect
            reject(new Error(`STOMP error: ${frame.headers ? frame.headers['message'] : 'Unknown STOMP error'}`));
          },
          onWebSocketClose: (event) => {
            console.log('WebSocket closed:', event);
            this.isConnected = false; // Explicitly set based on this event
            if (this.isConnecting) { // If it was closed during an active connecting attempt
                this.isConnecting = false;
                this.connectionPromise = null;
                reject(new Error('WebSocket closed during connection attempt.'));
            }
            this.handleReconnection();
          },
          onWebSocketError: (eventOrError) => { // eventOrError can be an Event or Error object
            console.error('WebSocket error:', eventOrError);
            this.isConnecting = false;
            this.connectionPromise = null;
            this.handleReconnection();
            reject(new Error('WebSocket error.'));
          }
        });

        this.stompClient.onConnect = (frame) => {
          console.log('STOMP connection established');
          this.isConnected = true;
          this.isConnecting = false;
          this.connectionPromise = null;
          this.reconnectAttempts = 0;
          
          console.log('In onConnect: this.stompClient.connected =', this.stompClient?.connected);
          console.log('In onConnect: frame object =', frame); 
          
          console.log('Attempting to subscribe via setTimeout...');
          setTimeout(() => {
            console.log('Inside setTimeout for subscription: this.stompClient.connected =', this.stompClient?.connected);
            if (this.stompClient?.connected) {
              this.subscribeToNotifications();
            } else {
              console.error('Inside setTimeout: STOMP client still not connected, cannot subscribe.');
            }
            resolve(frame); // Resolve the promise with the frame on successful connection
          }, 200); 
        };

        this.stompClient.activate();
      } catch (error) {
        console.error('WebSocket connection error (outer try-catch):', error);
        this.isConnecting = false;
        this.connectionPromise = null;
        reject(error);
      }
    });
    return this.connectionPromise;
  }

  subscribeToNotifications() {
    if (!this.stompClient?.connected || !this.userId) {
      console.error('Cannot subscribe: WebSocket not connected or no user ID.');
      return;
    }

    try {
      // const userQueueDestination = `/user/${this.userId}/queue/notifications`;
      const userQueueDestination = `/user/queue/notifications`;
      this.stompClient.subscribe(
        userQueueDestination,
        (message) => {
          try {
            const notification = JSON.parse(message.body);
            console.log('Received user-specific notification:', notification);
            this.notifySubscribers(notification);
          } catch (error) {
            console.error('Error processing user-specific notification:', error);
          }
        },
        { id: `sub-user-${this.userId}-${Date.now()}` } // More unique subscription ID
      );
      console.log('Subscribed to user-specific queue:', userQueueDestination);

      const broadcastTopicDestination = '/topic/broadcasts'; 
      this.stompClient.subscribe(
        broadcastTopicDestination,
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
      console.log('Subscribed to broadcast topic:', broadcastTopicDestination);
    } catch (error) {
      console.error('Subscription error:', error);
    }
  }

  handleReconnection() {
    if (this.isConnecting) { // Don't attempt another reconnect if one is already in progress
        console.log("Reconnection attempt skipped: another connection is already in progress.");
        return;
    }
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached. Not attempting further reconnections.');
      return;
    }

    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }

    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
    this.reconnectAttempts++;
    
    console.log(`Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
    
    this.reconnectTimeout = setTimeout(() => {
      if (this.userId) { // Only reconnect if userId is still set (i.e., user hasn't logged out)
        this.isConnecting = false; // Reset before calling connect
        this.connect(this.userId).catch(error => {
          console.error('Reconnection attempt failed:', error);
          // isConnecting should be false already if connect promise rejected
        });
      } else {
        console.log("User ID not set, skipping reconnection.");
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
    console.log('Disconnect called. Clearing reconnect timeout and deactivating STOMP client.');
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    
    // No new connection attempts should start after explicit disconnect
    this.reconnectAttempts = this.maxReconnectAttempts; // Prevent further auto-reconnections

    if (this.stompClient) {
      try {
        if (this.stompClient.active) { // Check .active instead of .connected for STOMP.js v5+
          console.log('Deactivating STOMP client...');
          this.stompClient.deactivate();
        }
      } catch (error) {
        console.error('Error deactivating STOMP client:', error);
      }
    }
    this.stompClient = null; // Clear the client
    this.isConnected = false;
    this.isConnecting = false; 
    this.connectionPromise = null;
    // this.userId = null; // Keep userId if you want to allow manual reconnect without re-login
    console.log('WebSocket service state reset.');
  }
}

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
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
  constructor() {
    this.stompClient = null;
    this.subscribers = [];
    this.isConnected = false;
    this.userId = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5; // Max attempts before giving up
    this.reconnectTimeout = null;
    this.isConnecting = false;
    this.connectionPromise = null; 
  }

  connect(userId) {
    if (this.isConnecting && this.connectionPromise) {
      console.log('WebSocket connection attempt already in progress for user:', userId, 'Returning existing promise.');
      return this.connectionPromise;
    }

    if (this.stompClient?.active && this.userId === userId) {
      console.log('WebSocket already connected and active for user:', userId);
      this.isConnecting = false;
      return Promise.resolve();
    }
    
    if (this.stompClient) {
        console.log('Disconnecting previous STOMP client before new connection.');
        this.disconnect(true); // Internal call, don't prevent future reconnections for this new attempt
    }

    this.isConnecting = true;
    this.userId = userId; // Set userId at the beginning of a new connection attempt
    
    this.connectionPromise = new Promise((resolve, reject) => {
      try {
        console.log('Attempting WebSocket connection for user:', userId);
        
        const backendUrl = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080';
        const token = localStorage.getItem('token');
        
        const connectHeaders = { 'user-id': userId };
        if (token) {
          connectHeaders['Authorization'] = `Bearer ${token}`;
        }
        
        this.stompClient = new Client({
          webSocketFactory: () => {
            // Ensure SockJS URL is correctly formed e.g. http://localhost:8080/ws
            // STOMP.js over SockJS often doesn't need 'ws://' prefix for SockJS URL itself.
            const sockJsUrl = `${backendUrl}/ws`; 
            console.log(`Attempting SockJS connection to: ${sockJsUrl}`);
            return new SockJS(sockJsUrl);
          },
          debug: (str) => { /* console.log('STOMP_DEBUG:', str); */ },
          reconnectDelay: 0, // We manage reconnects manually with exponential backoff
          heartbeatIncoming: 20000, // Increased heartbeat
          heartbeatOutgoing: 20000, // Increased heartbeat
          connectHeaders: connectHeaders,
          logRawCommunication: false, // Enable for deep debugging only

          onConnect: (frame) => {
            console.log('STOMP client connected to server.');
            this.isConnected = true;
            this.isConnecting = false;
            // this.connectionPromise = null; // Do not nullify here, resolve/reject handles it
            this.reconnectAttempts = 0; // Reset on successful connection
            
            console.log('STOMP active state in onConnect:', this.stompClient?.active);
            this.performSubscriptions();
            resolve(); 
          },
          onStompError: (frame) => {
            console.error('STOMP error frame:', frame);
            const errorMessage = frame.headers?.['message'] || 'Unknown STOMP error';
            this.isConnected = false;
            if (this.isConnecting) {
                this.isConnecting = false;
                // this.connectionPromise = null;
                reject(new Error('STOMP error: ' + errorMessage));
            }
            this.handleReconnection(); // Attempt to reconnect on STOMP errors
          },
          onWebSocketClose: (event) => {
            console.log('WebSocket connection closed:', event);
            this.isConnected = false;
            const wasConnecting = this.isConnecting;
            this.isConnecting = false;
            // this.connectionPromise = null; // Handled by reject/resolve

            // If it was connecting and didn't resolve/reject yet, reject the promise.
            // This can happen if the WebSocket closes before STOMP fully connects or errors.
            if (wasConnecting) {
                 reject(new Error('WebSocket closed unexpectedly during connection phase.'));
            }
            this.handleReconnection();
          },
          onWebSocketError: (errorEvent) => {
            console.error('WebSocket error event:', errorEvent);
            this.isConnected = false;
            const wasConnecting = this.isConnecting;
            this.isConnecting = false;
            // this.connectionPromise = null;

            if (wasConnecting) {
                reject(new Error('WebSocket error during connection attempt.'));
            }
            this.handleReconnection();
          }
        });

        console.log('Activating STOMP client...');
        this.stompClient.activate();

      } catch (error) {
        console.error('Error during STOMP client setup or activation:', error);
        this.isConnecting = false;
        // this.connectionPromise = null; // Handled by reject
        reject(error); // Reject the main connection promise
      }
    }).finally(() => {
        // Ensure connectionPromise is cleared once resolved or rejected.
        // This allows new, distinct connection attempts to generate new promises.
        this.connectionPromise = null;
    });
    return this.connectionPromise;
  }

  performSubscriptions() {
    if (!this.stompClient?.active || !this.userId) {
      console.error('Cannot perform subscriptions: STOMP client not active or no user ID.');
      return;
    }
    console.log('Performing STOMP subscriptions for user:', this.userId);
    try {
      const userQueueDestination = `/user/queue/notifications`;
      this.stompClient.subscribe(userQueueDestination, (message) => {
        try {
          const notification = JSON.parse(message.body);
          this.notifySubscribers(notification);
        } catch (e) { console.error('Error processing user message:', e); }
      }, { id: `user-sub-${this.userId}` });
      console.log('Subscribed to user queue:', userQueueDestination);

      const broadcastTopicDestination = '/topic/broadcasts'; 
      this.stompClient.subscribe(broadcastTopicDestination, (message) => {
        try {
          const broadcastEvent = JSON.parse(message.body);
          this.notifySubscribers(broadcastEvent); 
        } catch (e) { console.error('Error processing broadcast message:', e); }
      }, { id: `broadcast-sub-${this.userId}` }); // Add userId to broadcast sub id for clarity
      console.log('Subscribed to broadcast topic:', broadcastTopicDestination);

    } catch (error) {
      console.error('Error during STOMP subscription setup:', error);
    }
  }

  handleReconnection() {
    if (this.isConnecting || !this.userId) { 
        console.log("Reconnection attempt skipped: connection ongoing or no user to connect for.");
        return;
    }
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached for user:', this.userId, '. Giving up.');
      this.isConnecting = false;
      return;
    }

    if (this.reconnectTimeout) clearTimeout(this.reconnectTimeout);

    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
    this.reconnectAttempts++;
    
    console.log(`Scheduling STOMP reconnection attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts} in ${delay}ms for user ${this.userId}`);
    
    this.reconnectTimeout = setTimeout(() => {
      console.log('Executing scheduled reconnection for user:', this.userId);
      // connect() will set isConnecting to true.
      this.connect(this.userId).catch(error => {
        console.warn(`Scheduled reconnection attempt ${this.reconnectAttempts} failed:`, error.message);
        // connect() itself handles isConnecting and might call handleReconnection again if appropriate from its error paths
      });
    }, delay);
  }

  subscribe(callback) {
    if (typeof callback !== 'function') {
      console.error('Subscriber must be a function.');
      return () => {};
    }
    this.subscribers.push(callback);
    return () => {
      this.subscribers = this.subscribers.filter(cb => cb !== callback);
    };
  }

  notifySubscribers(notification) {
    this.subscribers.forEach(callback => {
      try { callback(notification); } catch (e) { console.error('Error in subscriber callback:', e); }
    });
  }

  disconnect(isInternalCall = false) {
    console.log(`Disconnect called. Internal: ${isInternalCall}`);
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    
    if (!isInternalCall) {
        this.reconnectAttempts = this.maxReconnectAttempts; // Prevent auto-reconnections if external call
        // this.userId = null; // Clearing userId here means subsequent auto-reconnects in handleReconnection won't have a target.
                           // This seems correct for an explicit, external disconnect.
    }

    if (this.stompClient) {
      if (this.stompClient.active) {
        console.log('Deactivating STOMP client...');
        this.stompClient.deactivate()
          .then(() => console.log('STOMP client deactivated successfully.'))
          .catch(e => console.error("Error during STOMP deactivate:", e));
      } else {
        console.log('STOMP client not active.');
      }
      this.stompClient = null; // Ensure client is nulled after attempting deactivation
    }
    
    this.isConnected = false;
    this.isConnecting = false; 
    if (!isInternalCall) { // Only nullify userId if it's an explicit disconnect from outside
        this.userId = null;
        this.reconnectAttempts = 0; // Reset for future explicit connections
    }
    // Do not nullify connectionPromise here if an external disconnect happens while a connect() is in progress.
    // The promise from connect() should still be allowed to reject or resolve.
    // If connect() fails, it will nullify its own promise.
    console.log('WebSocket service state after disconnect. Connected:', this.isConnected, 'Connecting:', this.isConnecting, 'UserID:', this.userId);
  }
}

const webSocketService = new WebSocketService();

export const connectToWebSocket = (userId) => webSocketService.connect(userId);
export const subscribeToNotifications = (callback) => webSocketService.subscribe(callback);
export const disconnectFromWebSocket = () => webSocketService.disconnect();
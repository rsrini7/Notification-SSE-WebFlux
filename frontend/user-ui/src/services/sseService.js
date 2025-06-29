// sseService.js
class SseService {
    constructor() {
        this.eventSource = null;
        this.subscribers = [];
        this.reconnectionAttempts = 0;
        this.maxReconnectionAttempts = 5; // Example: Max 5 reconnection attempts
        this.reconnectionDelay = 5000; // Example: 5 seconds delay
        this.currentUserId = null; // Added currentUserId property
    }

    connect(userId) {
        const token = localStorage.getItem('token');
        console.log(`SSE Service: connect() called for userId: ${userId}. Current EventSource state: ${this.eventSource ? this.eventSource.readyState : 'null'}, current service userId: ${this.currentUserId}`);

        if (!token) {
            console.error('SSE Service: No JWT token found for SSE connection.');
            this.notifySubscribers({ type: 'SSE_AUTH_ERROR', message: 'No token found' });
            return;
        }

        if (this.eventSource) {
            if (this.currentUserId === userId) {
                if (this.eventSource.readyState === EventSource.OPEN) {
                    console.log(`SSE Service: Connection already OPEN for user ${userId}. Not reconnecting. Notifying subscribers.`);
                    this.notifySubscribers({ type: 'SSE_CONNECTION_ESTABLISHED', userId: this.currentUserId });
                    return;
                }
                if (this.eventSource.readyState === EventSource.CONNECTING) {
                    console.log(`SSE Service: Connection already CONNECTING for user ${userId}. Allowing current attempt to proceed.`);
                    return; 
                }
                console.log(`SSE Service: EventSource for user ${userId} was CLOSED (readyState: ${this.eventSource.readyState}). A new connection will be attempted.`);
                // Proceed to disconnect this closed instance and create a new one.
            } else {
                console.log(`SSE Service: User context changing from ${this.currentUserId} to ${userId}. Disconnecting old EventSource if any.`);
                // Proceed to disconnect for the old user and create a new one for the new user.
            }
            this.disconnect(); // Disconnect previous instance (if it was closed or for a different user)
        }

        const BACKEND_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080';
        const url = `${BACKEND_URL}/api/notifications/events?token=${encodeURIComponent(token)}`;
        console.log(`SSE Service: Creating new EventSource. Connecting to ${url} for user ${userId}`);
        this.eventSource = new EventSource(url);
        this.currentUserId = userId; 

        this.eventSource.onopen = () => {
            console.log(`SSE Service: onopen event fired. Connection ESTABLISHED with server for user ID: ${this.currentUserId}. readyState: ${this.eventSource?.readyState}`);
            this.reconnectionAttempts = 0;
            this.notifySubscribers({ type: 'SSE_CONNECTION_ESTABLISHED', userId: this.currentUserId });
        };

        this.eventSource.onmessage = (event) => {
            console.log(`SSE Service: onmessage event received. Type: ${event.type}, Origin: ${event.origin}, LastEventID: ${event.lastEventId}, Data: ${event.data.substring(0, 100)}...`);
            if (event.type === "KEEPALIVE" || (event.data && event.data.includes("KEEPALIVE_HEARTBEAT"))) { // Adjusted for common KEEPALIVE patterns
                console.log('SSE Service: KEEPALIVE event processed.');
                return; 
            }
            if (event.type === "INIT" || (event.data && event.data.includes("Connection established"))) { // Adjusted
                 console.log('SSE Service: INIT event processed.');
                 // Consider if onopen isn't firing but INIT is, maybe notify subscribers here too,
                 // but ideally onopen should fire.
                 return; 
            }
            if (!event.data || event.data.trim() === "") {
                console.log('SSE Service: Received empty event data string.');
                return;
            }
            try {
                const notification = JSON.parse(event.data);
                console.log('SSE Service: Parsed application notification:', notification);
                this.notifySubscribers({ type: 'NOTIFICATION_RECEIVED', payload: notification });
            } catch (error) {
                console.error('SSE Service: Error parsing JSON from application event data:', error, 'Raw data:', event.data);
            }
        };

        this.eventSource.onerror = (errorEvent) => {
            console.error(`SSE Service: onerror event fired. EventSource failed. readyState at error: ${this.eventSource?.readyState}. Full error event object:`, errorEvent);
            // Log more details if possible, e.g. errorEvent.message, errorEvent.target
            // errorEvent itself is often a generic Event, specific error details might be scarce.
            
            if (this.eventSource) { // Check if eventSource still exists
                 // The browser usually closes it before onerror, but defensive closing is okay.
                 this.eventSource.close(); // Ensure it's closed.
                 console.log('SSE Service: onerror - eventSource explicitly closed.');
            }

            if (this.reconnectionAttempts < this.maxReconnectionAttempts) {
                this.reconnectionAttempts++;
                const retryUserId = this.currentUserId || userId; 
                console.log(`SSE Service: Attempting to reconnect in ${this.reconnectionDelay / 1000}s (Attempt ${this.reconnectionAttempts}/${this.maxReconnectionAttempts}) for userId: ${retryUserId}`);
                // Clear current eventSource reference before retry to ensure a new one is made by connect()
                this.eventSource = null; 
                this.currentUserId = null; // Reset userId so connect can set it fresh
                setTimeout(() => this.connect(retryUserId), this.reconnectionDelay);
            } else {
                console.error(`SSE Service: Max reconnection attempts reached for user: ${this.currentUserId || userId}. Giving up.`);
                this.notifySubscribers({ type: 'SSE_CONNECTION_ERROR', message: 'Connection failed after multiple retries', userId: this.currentUserId || userId });
                this.disconnect(); // Full cleanup
            }
        };
    }

    subscribe(callback) {
        console.log('SSE Service: New subscriber callback being added.');
        this.subscribers.push(callback);
        // ... rest of subscribe ...
        return () => {
            this.subscribers = this.subscribers.filter(sub => sub !== callback);
            console.log('SSE Service: Subscriber removed.');
        };
    }

    notifySubscribers(data) {
        console.log('sseService.js: notifySubscribers called with data:', data, 'Number of subscribers:', this.subscribers.length);
        this.subscribers.forEach(callback => {
            console.log('sseService.js: Notifying one subscriber with:', data);
            try {
                callback(data);
            } catch (error) {
                console.error('SSE Service: Error in subscriber callback:', error);
            }
        });
    }

    disconnect() {
        console.log(`SSE Service: disconnect() called. Current EventSource state: ${this.eventSource?.readyState}, user: ${this.currentUserId}`);
        if (this.eventSource) {
            this.eventSource.close();
            console.log('SSE Service: EventSource explicitly closed in disconnect().');
            this.eventSource = null;
            this.notifySubscribers({ type: 'SSE_CONNECTION_CLOSED', userId: this.currentUserId });
        }
        this.reconnectionAttempts = 0; 
        this.currentUserId = null; 
        console.log('SSE Service: Disconnect finished. Service reset.');
    }
}

const sseServiceInstance = new SseService();

// Exported functions for use in other parts of the application
export const connectToSse = (userId) => sseServiceInstance.connect(userId);
export const subscribeToSse = (callback) => sseServiceInstance.subscribe(callback);
export const disconnectFromSse = () => sseServiceInstance.disconnect();

export default sseServiceInstance; // Export the instance for direct use if needed
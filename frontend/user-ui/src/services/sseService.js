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
        if (!token) {
            console.error('SSE Service: No JWT token found for SSE connection.');
            this.notifySubscribers({ type: 'SSE_AUTH_ERROR', message: 'No token found' });
            return;
        }

        // Check if already connected or connecting for the same user
        if (this.eventSource && this.currentUserId === userId &&
            (this.eventSource.readyState === EventSource.OPEN || this.eventSource.readyState === EventSource.CONNECTING)) {
            console.log(`SSE Service: Connection already active or connecting for user ${userId}.`);
            return;
        }

        // Ensure any existing connection (possibly for a different user or in a bad state) is closed
        this.disconnect();

        // Backend SseController uses token query param. UserID from token.
        const url = `/api/notifications/events?token=${encodeURIComponent(token)}`;
        console.log(`SSE Service: Connecting to ${url} for user ${userId}`);
        this.eventSource = new EventSource(url);
        this.currentUserId = userId; // Set currentUserId

        this.eventSource.onopen = () => {
            console.log('SSE Connection: Established with user ID:', this.currentUserId);
            this.reconnectionAttempts = 0; // Reset reconnection attempts on successful connection
            this.notifySubscribers({ type: 'SSE_CONNECTION_ESTABLISHED', userId: this.currentUserId });
        };

        this.eventSource.onmessage = (event) => {
            // Handle KEEPALIVE event (name: "KEEPALIVE", data: "ping")
            // The 'type' property of the Event object is typically used for the event name.
            if (event.type === "KEEPALIVE" || (event.data === "ping")) {
                console.log('SSE Service: KEEPALIVE event received.');
                // Optionally, could also check event.data if type is not reliably KEEPALIVE
                // For example, if (event.data === "ping") when type is just "message"
                return; 
            }

            // Handle INIT event (name: "INIT", data: "Connection established")
            if (event.type === "INIT" || (event.data === "Connection established")) {
                 console.log('SSE Service: INIT event received.');
                 // this.notifySubscribers({ type: 'SSE_INIT_CONFIRMED', message: event.data });
                 return; 
            }
            
            // Default handling for other messages, expecting JSON
            try {
                // Ensure event.data is not empty or undefined before parsing
                if (!event.data || event.data.trim() === "") {
                    console.log('SSE Service: Received empty event data.');
                    return;
                }
                const notification = JSON.parse(event.data);
                console.log('SSE Service: Received application notification:', notification);
                this.notifySubscribers({ type: 'NOTIFICATION_RECEIVED', payload: notification });
            } catch (error) {
                console.error('SSE Service: Error parsing JSON from application event data:', error, 'Raw data for error:', event.data);
            }
        };

        this.eventSource.onerror = (errorEvent) => { // Renamed parameter to errorEvent for clarity
            console.error('SSE Service: EventSource failed. Full error event:', errorEvent);
            
            // Attempt to log specific properties if they exist
            if (errorEvent) {
                console.error('Error event type:', errorEvent.type);
                if (errorEvent.target && errorEvent.target.readyState) {
                    console.error('EventSource readyState:', errorEvent.target.readyState, '(0=CONNECTING, 1=OPEN, 2=CLOSED)');
                }
                if (errorEvent.message) { // Some error events might have a message property
                    console.error('Error event message:', errorEvent.message);
                }
                // For network errors, some browsers might provide status on the EventSource target itself,
                // but this is not standard. The primary indication of an HTTP error would be the EventSource
                // closing and not providing specific HTTP status codes directly in onerror.
            }

            // It's important to close the current EventSource instance if it's not already closed.
            // The browser usually closes it before onerror, but defensive closing is okay.
            if (this.eventSource) { // Check if eventSource still exists
                 this.eventSource.close();
            }
            // Note: The original code already called this.eventSource.close().
            // The current change is primarily about adding more console.error logs.

            // Attempt to reconnect with delay and max attempts
            if (this.reconnectionAttempts < this.maxReconnectionAttempts) {
                this.reconnectionAttempts++;
                const retryUserId = this.currentUserId || userId; // Use currentUserId if available for retry
                console.log(`SSE Service: Attempting to reconnect in ${this.reconnectionDelay / 1000}s (Attempt ${this.reconnectionAttempts}/${this.maxReconnectionAttempts}) for userId: ${retryUserId}`);
                setTimeout(() => this.connect(retryUserId), this.reconnectionDelay);
            } else {
                console.error('SSE Service: Max reconnection attempts reached. Giving up for user:', this.currentUserId || userId);
                this.notifySubscribers({ type: 'SSE_CONNECTION_ERROR', message: 'Connection failed after multiple retries', userId: this.currentUserId || userId });
                this.disconnect(); // Ensure full reset including currentUserId
            }
        };
    }

    subscribe(callback) {
        this.subscribers.push(callback);
        console.log('SSE Service: New subscriber added.');
        // Optionally, return an unsubscribe function
        return () => {
            this.subscribers = this.subscribers.filter(sub => sub !== callback);
            console.log('SSE Service: Subscriber removed.');
        };
    }

    notifySubscribers(data) {
        console.log('SSE Service: Notifying subscribers with data:', data, 'Number of subscribers:', this.subscribers.length);
        this.subscribers.forEach(callback => {
            try {
                callback(data);
            } catch (error) {
                console.error('SSE Service: Error in subscriber callback:', error);
            }
        });
    }

    disconnect() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
            console.log('SSE Service: Disconnected.');
            // Notify subscribers about the disconnection, potentially including the user ID
            this.notifySubscribers({ type: 'SSE_CONNECTION_CLOSED', userId: this.currentUserId });
        }
        // Clear any pending reconnection timeouts if disconnect is called explicitly
        // This requires managing timeouts with an ID, e.g., this.reconnectTimeoutId
        // For simplicity, this example assumes EventSource auto-reconnect or manual retries are sufficient.
        this.reconnectionAttempts = 0; // Reset attempts on manual disconnect
        this.currentUserId = null; // Clear currentUserId on disconnect
    }
}

const sseServiceInstance = new SseService();

// Exported functions for use in other parts of the application
export const connectToSse = (userId) => sseServiceInstance.connect(userId);
export const subscribeToSse = (callback) => sseServiceInstance.subscribe(callback);
export const disconnectFromSse = () => sseServiceInstance.disconnect();

export default sseServiceInstance; // Export the instance for direct use if needed
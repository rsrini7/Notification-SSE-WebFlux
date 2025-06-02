// sseService.js
class SseService {
    constructor() {
        this.eventSource = null;
        this.subscribers = [];
        this.reconnectionAttempts = 0;
        this.maxReconnectionAttempts = 5; // Example: Max 5 reconnection attempts
        this.reconnectionDelay = 5000; // Example: 5 seconds delay
    }

    connect(userId) { // userId might be redundant if token carries it, but kept for now if API expects it or for logging
    const token = localStorage.getItem('token');
        if (!token) {
            console.error('SSE Connection: No JWT token found.');
            // Potentially notify subscribers about auth failure
            this.notifySubscribers({ type: 'SSE_AUTH_ERROR', message: 'No token found' });
            return;
        }

        // Ensure any existing connection is closed before starting a new one
        this.disconnect();

        // Backend SseController uses token query param. UserID from token.
        const url = `/api/notifications/events?token=${encodeURIComponent(token)}`;
        console.log(`SSE Service: Connecting to ${url}`);
        this.eventSource = new EventSource(url);

        this.eventSource.onopen = () => {
            console.log('SSE Connection: Established with user ID:', userId); // Log with userId if available
            this.reconnectionAttempts = 0; // Reset reconnection attempts on successful connection
            this.notifySubscribers({ type: 'SSE_CONNECTION_ESTABLISHED', userId });
        };

        this.eventSource.onmessage = (event) => {
            try {
                const notification = JSON.parse(event.data);
                console.log('SSE Service: Received notification:', notification);
                this.notifySubscribers({ type: 'NOTIFICATION_RECEIVED', payload: notification });
            } catch (error) {
                console.error('SSE Service: Error parsing event data:', error, 'Raw data:', event.data);
            }
        };

        this.eventSource.onerror = (error) => {
            console.error('SSE Service: EventSource failed:', error);
            this.eventSource.close(); // Close the problematic EventSource

            // Attempt to reconnect with delay and max attempts
            if (this.reconnectionAttempts < this.maxReconnectionAttempts) {
                this.reconnectionAttempts++;
                console.log(`SSE Service: Attempting to reconnect in ${this.reconnectionDelay / 1000}s (Attempt ${this.reconnectionAttempts}/${this.maxReconnectionAttempts})`);
                setTimeout(() => this.connect(userId), this.reconnectionDelay);
            } else {
                console.error('SSE Service: Max reconnection attempts reached. Giving up.');
                this.notifySubscribers({ type: 'SSE_CONNECTION_ERROR', message: 'Connection failed after multiple retries' });
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
            this.notifySubscribers({ type: 'SSE_CONNECTION_CLOSED' });
        }
        // Clear any pending reconnection timeouts if disconnect is called explicitly
        // This requires managing timeouts with an ID, e.g., this.reconnectTimeoutId
        // For simplicity, this example assumes EventSource auto-reconnect or manual retries are sufficient.
        this.reconnectionAttempts = 0; // Reset attempts on manual disconnect
    }
}

const sseServiceInstance = new SseService();

// Exported functions for use in other parts of the application
export const connectToSse = (userId) => sseServiceInstance.connect(userId);
export const subscribeToSse = (callback) => sseServiceInstance.subscribe(callback);
export const disconnectFromSse = () => sseServiceInstance.disconnect();

export default sseServiceInstance; // Export the instance for direct use if needed
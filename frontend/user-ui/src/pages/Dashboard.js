import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Card,
  CardContent,
  CardActions,
  Button,
  Divider,
  Badge,
  CircularProgress,
  Alert
} from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import MarkEmailReadIcon from '@mui/icons-material/MarkEmailRead';
import { 
  getUnreadNotifications,
    countUnreadNotifications,
    markAllNotificationsAsRead,
    subscribeToRealtimeNotifications
} from '../services/notificationService';
import eventBus from '../utils/eventBus';

const Dashboard = ({ user }) => {
  console.log('Dashboard.js: Rendering with User ID:', user?.id);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [markingAllRead, setMarkingAllRead] = useState(false);

  // Effect to log when user prop reference changes
  useEffect(() => {
    console.log('Dashboard.js: "user" prop effect. User ID:', user?.id, 'User object:', user);
  }, [user]);

  const fetchData = useCallback(async () => {
    const currentUserId = user?.id;
    console.log('Dashboard.js: fetchData called for user:', currentUserId);
    if (currentUserId) {
      try {
        setLoading(true);
        const notificationsData = await getUnreadNotifications(currentUserId, 0, 5); // Fetch 5 recent for dashboard
        setNotifications(notificationsData.content || []);

        const count = await countUnreadNotifications(currentUserId);
        setUnreadCount(count);
      } catch (err) {
        console.error('Error fetching dashboard data:', err);
        setError('Failed to load dashboard data. Please try again later.');
      } finally {
        setLoading(false);
      }
    }
  }, [user?.id]); // Depends on user object reference

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleNewNotification = useCallback((event) => {
    const currentUserId = user?.id;
    console.log('Dashboard.js: handleNewNotification received event:', event, 'for user:', currentUserId);

    if (!currentUserId) {
      console.log('Dashboard.js: handleNewNotification - No current user, skipping.');
      return;
    }

    if (event.type === 'NOTIFICATION_RECEIVED' && event.payload) {
      const newNotification = event.payload;
      console.log('Dashboard.js: Processing NOTIFICATION_RECEIVED:', newNotification);

      // Ensure newNotification and its properties are valid before processing
      const notificationId = newNotification?.id;
      if (notificationId === null || typeof notificationId === 'undefined') {
          console.warn('Dashboard.js: Received notification without a valid ID, adding to list:', newNotification);
          setNotifications(prevNotifications => [newNotification, ...(prevNotifications || []).slice(0, 4)]);
          // Increment unread count only if the notification is for the current user
          if (newNotification.userId === currentUserId || newNotification.targetUserIds?.includes(currentUserId) || !newNotification.userId) {
            setUnreadCount(prevCount => prevCount + 1);
          }
          return;
      }
      
      // Add to recent notifications list only if it's for the current user and not a duplicate
      // For dashboard, we might just increment count and let user go to notifications page for full list
      // Or, add to a small list of *recent* notifications if it's for this user
      if (newNotification.userId === currentUserId || newNotification.targetUserIds?.includes(currentUserId) || !newNotification.userId) {
        setNotifications(prevNotifications => {
          const exists = (prevNotifications || []).find(n => n.id === notificationId);
          if (exists) {
            console.log('Dashboard.js: Notification with ID ' + notificationId + ' already exists, not adding duplicate.');
            return prevNotifications;
          }
          console.log('Dashboard.js: Adding new notification ID ' + notificationId + ' to dashboard list.');
          return [newNotification, ...(prevNotifications || []).slice(0, 4)]; // Keep max 5
        });
        // Increment unread count for new, relevant notifications
        setUnreadCount(prevCount => prevCount + 1);
      }

    } else if (event.type === 'SSE_CONNECTION_ESTABLISHED'){
      console.log('Dashboard.js: SSE Connection Established event received.');
    } else if (event.type === 'SSE_CONNECTION_CLOSED'){
      console.log('Dashboard.js: SSE Connection Closed event received.');
    } else {
      console.log('Dashboard.js: Received unhandled SSE event type:', event?.type);
    }
  }, [user?.id]); // Depends on user.id to create a specific version of handler for the current user

  useEffect(() => {
    const currentUserId = user?.id;
    if (!currentUserId) {
      console.log('Dashboard_SubEffect: Skipping setup, no user.id.');
      return;
    }

    console.log('Dashboard_SubEffect_Setup: Subscribing. UserID:', currentUserId, 'CallbackRef:', handleNewNotification);
    const unsubscribe = subscribeToRealtimeNotifications(handleNewNotification);
    
    // Re-fetch data if notificationsUpdated event occurs (e.g., mark all as read)
    eventBus.on('notificationsUpdated', fetchData); 

    return () => {
      console.log('Dashboard_SubEffect_Cleanup: Unsubscribing. UserID:', currentUserId, 'CallbackRef:', handleNewNotification);
      if (unsubscribe) {
        unsubscribe();
      }
      eventBus.off('notificationsUpdated', fetchData);
    };
  }, [user?.id, handleNewNotification, fetchData]); // Dependencies for subscription

  const handleMarkAllAsRead = async () => {
    const currentUserId = user?.id;
    if (!currentUserId) return;

    try {
      setMarkingAllRead(true);
      await markAllNotificationsAsRead(currentUserId);
      setUnreadCount(0);
      // Update local list to reflect read status, though dashboard shows limited items
      setNotifications(prev => prev.map(n => ({ ...n, readStatus: 'READ', read: true }))); 
      eventBus.emit('notificationsUpdated'); // Notify other components like Layout
    } catch (err) {
      console.error('Error marking all as read:', err);
      setError('Failed to mark notifications as read. Please try again.');
    } finally {
      setMarkingAllRead(false);
    }
  };

  return (
    <Box sx={{ flexGrow: 1, p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>
      <Typography variant="subtitle1" gutterBottom>
        Welcome back, {user?.name || 'User'}!
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Grid container spacing={3} sx={{ mt: 2 }}>
        <Grid item xs={12} md={4}>
          <Paper elevation={2} sx={{ p: 2, display: 'flex', flexDirection: 'column', height: '100%' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Typography component="h2" variant="h6" color="primary" gutterBottom>
                Notifications
              </Typography>
              <Badge badgeContent={unreadCount} color="error">
                <NotificationsIcon color="action" />
              </Badge>
            </Box>
            <Typography component="p" variant="h4">
              {loading && !markingAllRead ? <CircularProgress size={24} /> : unreadCount}
            </Typography>
            <Typography color="text.secondary" sx={{ flex: 1 }}>
              unread notifications
            </Typography>
            <Box sx={{
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'flex-start',
              alignItems: 'stretch',
              gap: 1,
              mt: 1,
              width: '100%',
              boxSizing: 'border-box'
            }}>
              <Button
                variant="outlined"
                size="small"
                component={Link}
                to="/notifications"
                fullWidth
              >
                View All
              </Button>
              <Button
                variant="outlined"
                size="small"
                color="secondary"
                onClick={handleMarkAllAsRead}
                disabled={unreadCount === 0 || markingAllRead}
                startIcon={markingAllRead ? <CircularProgress size={16} /> : <MarkEmailReadIcon />}
                fullWidth
              >
                Mark All Read
              </Button>
            </Box>
          </Paper>
        </Grid>
        {/* Add other dashboard widgets here if needed */}
      </Grid>

      <Typography variant="h6" sx={{ mt: 4, mb: 2 }}>
        Recent Notifications (Last 5 Unread)
      </Typography>

      {loading ? (
        <Box display="flex" justifyContent="center" sx={{ my: 4 }}>
          <CircularProgress />
        </Box>
      ) : notifications.length > 0 ? (
        <Grid container spacing={2}>
          {notifications.map((notification) => (
            <Grid item xs={12} key={notification.id || Math.random()}>
              <Card 
                className={`notification-item ${notification.readStatus === 'READ' ? 'notification-read' : 'notification-unread'}`}
                variant="outlined"
              >
                <CardContent>
                  <Typography variant="subtitle1" component="div">
                    {notification.title || 'Notification'}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {notification.content}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Type: {notification.notificationType} | Priority: {notification.priority}
                  </Typography>
                  <Typography variant="caption" display="block" color="text.secondary">
                    {notification.createdAt ? new Date(notification.createdAt).toLocaleString() : 'Date N/A'}
                  </Typography>
                </CardContent>
                <Divider />
                <CardActions>
                  <Button size="small" component={Link} to={`/notifications/${notification.id}`}>
                    View Details
                  </Button>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      ) : (
        <Paper sx={{ p: 3, textAlign: 'center' }}>
          <Typography variant="body1">No recent unread notifications</Typography>
        </Paper>
      )}
    </Box>
  );
};

export default Dashboard;
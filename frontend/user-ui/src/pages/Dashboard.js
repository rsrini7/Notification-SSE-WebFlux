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
import CountrySelector from '../components/CountrySelector'; // Import CountrySelector
import { 
  getUnreadNotifications,
    countUnreadNotifications,
    markAllNotificationsAsRead,
    subscribeToRealtimeNotifications
} from '../services/notificationService';
import eventBus from '../utils/eventBus';

const Dashboard = ({ user }) => {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [markingAllRead, setMarkingAllRead] = useState(false);

  const fetchData = useCallback(async () => {
    if (user && user.id) {
      try {
        setLoading(true);
        // Fetch unread notifications
        const notificationsData = await getUnreadNotifications(user.id, 0, 5);
        setNotifications(notificationsData.content);
        
        // Get count of unread notifications
        const count = await countUnreadNotifications(user.id);
        setUnreadCount(count);
      } catch (err) {
        console.error('Error fetching dashboard data:', err);
        setError('Failed to load notifications. Please try again later.');
      } finally {
        setLoading(false);
      }
    }
  }, [user]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    if (!user || !user.id) return;

    const handleNewNotification = (newNotification) => {
      console.log('Dashboard received new notification via WebSocket:', newNotification);
      
      // Variable to track if the notification is genuinely new for incrementing unread count
      let isTrulyNewNotification = false;

      setNotifications(prevNotifications => {
        // Check if the notification (by id) already exists in the list
        // Ensure newNotification and its id are valid before checking
        const notificationId = newNotification && newNotification.id;
        if (notificationId === null || typeof notificationId === 'undefined') {
            console.warn('Received notification without a valid ID, skipping de-duplication for this one and adding to list:', newNotification);
            // Decide how to handle notifications without an ID, here we add it to avoid losing it
            // but this might not be desired if ID is always expected.
            isTrulyNewNotification = true; // Assume it's new if it has no ID to check against
            return [newNotification, ...prevNotifications.slice(0, 4)];
        }

        const existingNotification = prevNotifications.find(
          notification => notification.id === notificationId
        );

        if (existingNotification) {
          // Notification already exists. For now, we don't add it again.
          // In the future, you might want to update the existing one if newNotification has newer data.
          console.log('Notification with ID ' + notificationId + ' already exists, not adding duplicate.');
          isTrulyNewNotification = false;
          return prevNotifications; 
        } else {
          // Notification is new, add it to the beginning and maintain list size.
          isTrulyNewNotification = true;
          return [newNotification, ...prevNotifications.slice(0, 4)];
        }
      });

      // Only increment unread count if it was a genuinely new notification added to the list.
      if (isTrulyNewNotification) {
        setUnreadCount(prevCount => prevCount + 1);
      }
    };

    const unsubscribe = subscribeToRealtimeNotifications(handleNewNotification);

    return () => unsubscribe(); // Cleanup subscription
  }, [user]);

  const handleMarkAllAsRead = async () => {
    try {
      setMarkingAllRead(true);
      await markAllNotificationsAsRead(user.id);
      setUnreadCount(0);
      setNotifications(prev => prev.map(notification => ({
        ...notification,
        read: true
      })));
      eventBus.emit('notificationsUpdated');
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
        Welcome back, {user.name}!
      </Typography>

      <Box sx={{ my: 2 }}>
        <Typography variant="h6">Country Selector:</Typography>
        <CountrySelector />
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Grid container spacing={3} sx={{ mt: 2 }}>
        <Grid item xs={12} md={4}>
          <Paper elevation={2} sx={{ p: 2, display: 'flex', flexDirection: 'column' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Typography component="h2" variant="h6" color="primary" gutterBottom>
                Notifications
              </Typography>
              <Badge badgeContent={unreadCount} color="error">
                <NotificationsIcon color="action" />
              </Badge>
            </Box>
            <Typography component="p" variant="h4">
              {loading ? <CircularProgress size={24} /> : unreadCount}
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
      </Grid>

      <Typography variant="h6" sx={{ mt: 4, mb: 2 }}>
        Recent Notifications
      </Typography>

      {loading ? (
        <Box display="flex" justifyContent="center" sx={{ my: 4 }}>
          <CircularProgress />
        </Box>
      ) : notifications.length > 0 ? (
        <Grid container spacing={2}>
          {notifications.map((notification) => (
            <Grid item xs={12} key={notification.id}>
              <Card 
                className={`notification-item ${notification.read ? 'notification-read' : 'notification-unread'}`}
                variant="outlined"
              >
                <CardContent>
                  <Typography variant="subtitle1" component="div">
                    {notification.content}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Type: {notification.notificationType}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
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
          <Typography variant="body1">No unread notifications</Typography>
        </Paper>
      )}
    </Box>
  );
};

export default Dashboard;

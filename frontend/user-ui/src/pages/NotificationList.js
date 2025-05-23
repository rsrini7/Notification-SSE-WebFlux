import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Typography,
  Paper,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Divider,
  Chip,
  TextField,
  InputAdornment,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Pagination,
  CircularProgress,
  Alert,
  IconButton,
  Tooltip
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import NotificationsIcon from '@mui/icons-material/Notifications';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import { 
  getNotifications, 
  getUnreadNotifications,
  getNotificationsByType,
  searchNotifications,
  markNotificationAsRead,
  subscribeToNotifications,
  countUnreadNotifications
} from '../services/notificationService';

const NotificationList = ({ user }) => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [filter, setFilter] = useState('all'); // 'all', 'unread', or notification type
  const [searchTerm, setSearchTerm] = useState('');
  const [notificationTypes, setNotificationTypes] = useState([]);
  const [markingAsRead, setMarkingAsRead] = useState({});
  const [unreadCount, setUnreadCount] = useState(0);

  const pageSize = 10;

  // Fetch notifications with proper error handling and loading states
  const fetchNotifications = useCallback(async () => {
    if (!user?.id) return;

    try {
      setLoading(true);
      setError('');

      let response;
      if (searchTerm) {
        response = await searchNotifications(user.id, searchTerm, page - 1, pageSize);
      } else if (filter === 'all') {
        response = await getNotifications(user.id, page - 1, pageSize);
      } else if (filter === 'unread') {
        response = await getUnreadNotifications(user.id, page - 1, pageSize);
      } else {
        response = await getNotificationsByType(user.id, filter, page - 1, pageSize);
      }

      setNotifications(response.content);
      setTotalPages(response.totalPages);
      
      // Update unread count if we're on the first page
      if (page === 1) {
        const count = await countUnreadNotifications(user.id);
        setUnreadCount(count);
      }
    } catch (err) {
      console.error('Error fetching notifications:', err);
      setError('Failed to load notifications. Please try again later.');
    } finally {
      setLoading(false);
    }
  }, [user?.id, filter, searchTerm, page, pageSize]);

  useEffect(() => {
    fetchNotifications();
  }, [page, filter, fetchNotifications]);

  // Reset to page 1 when filter or search term changes
  useEffect(() => {
    setPage(1);
  }, [filter, searchTerm]);

  // Set up WebSocket subscription and initial data fetch
  useEffect(() => {
    if (!user?.id) return;
    
    const handleNewNotification = (newNotification) => {
      console.log('NotificationList received new notification via WebSocket:', newNotification);
      
      // Only process if we're on the first page
      if (page === 1) {
        let shouldAdd = false;
        
        if (searchTerm) {
          // If there's an active search, prepend if the new notification matches the search term
          if (newNotification.content.toLowerCase().includes(searchTerm.toLowerCase()) ||
              (newNotification.title && newNotification.title.toLowerCase().includes(searchTerm.toLowerCase()))) {
            shouldAdd = true;
          }
        } else {
          // No active search, check filters
          if (filter === 'all') {
            shouldAdd = true;
          } else if (filter === 'unread' && newNotification.readStatus === 'UNREAD') {
            shouldAdd = true;
          } else if (filter === newNotification.notificationType) {
            shouldAdd = true;
          }
        }

        if (shouldAdd) {
          setNotifications(prev => {
            // Check if notification already exists to prevent duplicates
            const exists = prev.some(n => n.id === newNotification.id);
            if (!exists) {
              return [newNotification, ...prev.slice(0, pageSize - 1)];
            }
            return prev;
          });
          
          // Update unread count if the notification is unread
          if (newNotification.readStatus === 'UNREAD') {
            setUnreadCount(prev => prev + 1);
          }
        }
      }
    };
    
    // Subscribe to WebSocket updates
    const unsubscribe = subscribeToNotifications(handleNewNotification);
    
    // Cleanup subscription on unmount
    return () => {
      if (unsubscribe) unsubscribe();
    };
  }, [user?.id, filter, searchTerm, page, pageSize]);

  const handlePageChange = (event, value) => {
    setPage(value);
  };

  const handleFilterChange = (event) => {
    setFilter(event.target.value);
    setPage(1); // Reset to first page when changing filters
  };

  const handleMarkAsRead = async (notificationId) => {
    try {
      setMarkingAsRead(prev => ({ ...prev, [notificationId]: true }));
      await markNotificationAsRead(notificationId, user.id);
      
      // Update local state
      setNotifications(prev => 
        prev.map(notification => 
          notification.id === notificationId 
            ? { ...notification, readStatus: 'READ' } 
            : notification
        )
      );
      
      // Update unread count
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (err) {
      console.error('Error marking notification as read:', err);
      setError('Failed to mark notification as read. Please try again.');
    } finally {
      setMarkingAsRead(prev => ({ ...prev, [notificationId]: false }));
    }
  };

  const handleSearch = (event) => {
    event.preventDefault();
    fetchNotifications();
  };

  return (
    <Box sx={{ flexGrow: 1, p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Notifications
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Paper sx={{ p: 2, mb: 3 }}>
        <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, gap: 2, mb: 2 }}>
          <FormControl variant="outlined" sx={{ minWidth: 200 }}>
            <InputLabel id="filter-label">Filter</InputLabel>
            <Select
              labelId="filter-label"
              id="filter-select"
              value={filter}
              onChange={handleFilterChange}
              label="Filter"
            >
              <MenuItem value="all">All Notifications</MenuItem>
              <MenuItem value="unread">Unread Only</MenuItem>
              <Divider />
              {notificationTypes.map(type => (
                <MenuItem key={type} value={type}>{type}</MenuItem>
              ))}
            </Select>
          </FormControl>

          <Box component="form" onSubmit={handleSearch} sx={{ display: 'flex', flexGrow: 1 }}>
            <TextField
              fullWidth
              variant="outlined"
              placeholder="Search notifications..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
            />
          </Box>
        </Box>
      </Paper>

      {loading ? (
        <Box display="flex" justifyContent="center" sx={{ my: 4 }}>
          <CircularProgress />
        </Box>
      ) : notifications.length > 0 ? (
        <Paper>
          <List>
            {notifications.map((notification, index) => (
              <React.Fragment key={notification.id}>
                {index > 0 && <Divider />}
                <ListItem 
                  alignItems="flex-start"
                  className={`notification-item ${notification.read ? 'notification-read' : 'notification-unread'}`}
                  secondaryAction={
                    !notification.read && (
                      <Tooltip title="Mark as read">
                        <IconButton 
                          edge="end" 
                          aria-label="mark as read"
                          onClick={() => handleMarkAsRead(notification.id)}
                          disabled={markingAsRead[notification.id]}
                        >
                          {markingAsRead[notification.id] ? 
                            <CircularProgress size={24} /> : 
                            <DoneAllIcon />}
                        </IconButton>
                      </Tooltip>
                    )
                  }
                >
                  <ListItemIcon>
                    {notification.read ? 
                      <NotificationsIcon color="disabled" /> : 
                      <NotificationsActiveIcon color="primary" />}
                  </ListItemIcon>
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Typography variant="subtitle1" component="span">
                          {notification.content}
                        </Typography>
                        <Chip 
                          label={notification.notificationType} 
                          size="small" 
                          color={notification.readStatus === 'READ' ? "default" : "primary"}
                          variant={notification.readStatus === 'READ' ? "outlined" : "filled"}
                        />
                      </Box>
                    }
                    secondary={
                      <React.Fragment>
                        <Typography
                          sx={{ display: 'block' }}
                          component="span"
                          variant="body2"
                          color="text.secondary"
                        >
                          From: {notification.sourceService}
                        </Typography>
                        <Typography
                          component="span"
                          variant="caption"
                          color="text.secondary"
                        >
                          {new Date(notification.createdAt).toLocaleString()}
                        </Typography>
                      </React.Fragment>
                    }
                  />
                </ListItem>
              </React.Fragment>
            ))}
          </List>
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
            <Pagination 
              count={totalPages} 
              page={page} 
              onChange={handlePageChange} 
              color="primary" 
            />
          </Box>
        </Paper>
      ) : (
        <Paper sx={{ p: 3, textAlign: 'center' }}>
          <Typography variant="body1">
            {searchTerm ? 'No notifications match your search' : 'No notifications found'}
          </Typography>
        </Paper>
      )}
    </Box>
  );
};

export default NotificationList;
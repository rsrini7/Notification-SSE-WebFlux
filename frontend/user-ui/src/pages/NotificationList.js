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
  subscribeToNotifications
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

  const pageSize = 10;

  const fetchNotifications = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      let response;

      // Adjust page index for API (0-based)
      const pageIndex = page - 1;

      if (searchTerm) {
        response = await searchNotifications(user.id, searchTerm, pageIndex, pageSize);
      } else if (filter === 'all') {
        response = await getNotifications(user.id, pageIndex, pageSize);
      } else if (filter === 'unread') {
        response = await getUnreadNotifications(user.id, pageIndex, pageSize);
      } else {
        // Filter by notification type
        response = await getNotificationsByType(user.id, filter, pageIndex, pageSize);
      }

      setNotifications(response.content);
      setTotalPages(response.totalPages);

      // Extract unique notification types for the filter dropdown
      if (filter === 'all' && page === 1) {
        const types = [...new Set(response.content.map(n => n.notificationType))];
        setNotificationTypes(types);
      }
    } catch (err) {
      console.error('Error fetching notifications:', err);
      setError('Failed to load notifications. Please try again later.');
    } finally {
      setLoading(false);
    }
  }, [user.id, page, filter, searchTerm, pageSize]);

  useEffect(() => {
    fetchNotifications();
  }, [page, filter, fetchNotifications]);

  // Reset to page 1 when filter or search term changes
  useEffect(() => {
    setPage(1);
  }, [filter, searchTerm]);

  useEffect(() => {
    if (!user || !user.id) return;

    const handleNewNotification = (newNotification) => {
      console.log('NotificationList received new notification via WebSocket:', newNotification);

      // Add to list if on page 1 and matches current filter
      if (page === 1) {
        let shouldAdd = false;
        if (searchTerm) {
          // If there's an active search, prepend if the new notification matches the search term
          // This is a simple check; more complex matching might be needed depending on search logic
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
          setNotifications(prev => [newNotification, ...prev.slice(0, pageSize - 1)]);
        }
      }
    };
    const unsubscribe = subscribeToNotifications(handleNewNotification);
    return () => unsubscribe(); // Cleanup subscription
  }, [user, page, filter, searchTerm, pageSize]);

  const handlePageChange = (event, value) => {
    setPage(value);
  };

  const handleFilterChange = (event) => {
    setFilter(event.target.value);
    setPage(1); // Reset to first page when changing filters
  };

  const handleSearch = (event) => {
    event.preventDefault();
    fetchNotifications();
  };

  const handleMarkAsRead = async (notificationId) => {
    try {
      setMarkingAsRead(prev => ({ ...prev, [notificationId]: true }));
      await markNotificationAsRead(notificationId, user.id);
      
      // Update the notification in the list
      setNotifications(prev => 
        prev.map(notification => 
          notification.id === notificationId 
            ? { ...notification, read: true } 
            : notification
        )
      );
    } catch (err) {
      console.error(`Error marking notification ${notificationId} as read:`, err);
      setError('Failed to mark notification as read. Please try again.');
    } finally {
      setMarkingAsRead(prev => ({ ...prev, [notificationId]: false }));
    }
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
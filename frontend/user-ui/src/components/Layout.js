import React, { useState, useEffect, useCallback } from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  AppBar,
  Box,
  Toolbar,
  Typography,
  Drawer,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Divider,
  IconButton,
  Badge,
  Menu,
  MenuItem,
  useMediaQuery,
  useTheme
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import DashboardIcon from '@mui/icons-material/Dashboard';
import NotificationsIcon from '@mui/icons-material/Notifications';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import LogoutIcon from '@mui/icons-material/Logout';
import {
  countUnreadNotifications,
  subscribeToRealtimeNotifications
} from '../services/notificationService';
import eventBus from '../utils/eventBus';

const drawerWidth = 240;

const Layout = ({ children, user, onLogout }) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);
  const [unreadCount, setUnreadCount] = useState(0);
  const location = useLocation();

  const fetchAndUpdateUnreadCount = useCallback(async () => {
    const currentUserId = user?.id;
    console.log('Layout.js: fetchAndUpdateUnreadCount - currentUserId:', currentUserId);
    if (currentUserId) {
      try {
        const count = await countUnreadNotifications(currentUserId);
        console.log('Layout.js: fetchAndUpdateUnreadCount - About to setUnreadCount. Fetched count:', count);
        setUnreadCount(count); // setUnreadCount is stable from useState
      } catch (error) {
        console.error('Error fetching unread count for Layout:', error);
      }
    } else {
      console.log('Layout.js: fetchAndUpdateUnreadCount - No user or user.id, skipping fetch.');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    fetchAndUpdateUnreadCount(); // Initial fetch

    // Subscribe to WebSocket updates for new notifications
    const handleNewNotification = (event) => { // Parameter name is 'event' based on typical SSE handler
      console.log('Layout.js: handleNewNotification invoked with event:', event);
      // A new notification arrived, re-fetch the count
      console.log('Layout.js: Calling fetchAndUpdateUnreadCount due to new notification event.');
      fetchAndUpdateUnreadCount();
    };

    const unsubscribeWs = subscribeToRealtimeNotifications(handleNewNotification);
    // Subscribe to custom event for manual refresh
    eventBus.on('notificationsUpdated', fetchAndUpdateUnreadCount);

    return () => {
      unsubscribeWs(); // Cleanup WebSocket subscription
      eventBus.off('notificationsUpdated', fetchAndUpdateUnreadCount); // Cleanup eventBus subscription
    };
  }, [user, fetchAndUpdateUnreadCount]); // Dependencies remain the same

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleProfileMenuOpen = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    handleMenuClose();
    onLogout();
  };

  const drawer = (
    <div>
      <Toolbar>
        <Typography variant="h6" noWrap component="div">
          Notification System
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        <ListItem 
          button 
          component={Link} 
          to="/" 
          selected={location.pathname === '/'}
          onClick={() => isMobile && setMobileOpen(false)}
        >
          <ListItemIcon>
            <DashboardIcon />
          </ListItemIcon>
          <ListItemText primary="Dashboard" />
        </ListItem>
        <ListItem 
          button 
          component={Link} 
          to="/notifications" 
          selected={location.pathname === '/notifications'}
          onClick={() => isMobile && setMobileOpen(false)}
        >
          <ListItemIcon>
            <Badge badgeContent={unreadCount} color="error">
              <NotificationsIcon />
            </Badge>
          </ListItemIcon>
          <ListItemText primary="Notifications" />
        </ListItem>
      </List>
    </div>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{
          width: { md: `calc(100% - ${drawerWidth}px)` },
          ml: { md: `${drawerWidth}px` },
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            {location.pathname === '/' ? 'Dashboard' : 'Notifications'}
          </Typography>
          <IconButton
            size="large"
            edge="end"
            aria-label="account of current user"
            aria-controls="menu-appbar"
            aria-haspopup="true"
            onClick={handleProfileMenuOpen}
            color="inherit"
          >
            <AccountCircleIcon />
          </IconButton>
          <Menu
            id="menu-appbar"
            anchorEl={anchorEl}
            anchorOrigin={{
              vertical: 'bottom',
              horizontal: 'right',
            }}
            keepMounted
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
            open={Boolean(anchorEl)}
            onClose={handleMenuClose}
          >
            <MenuItem disabled>
              <Typography variant="body2">{user.name || "Unknown User"}</Typography>
            </MenuItem>
            <Divider />
            <MenuItem onClick={handleLogout}>
              <ListItemIcon>
                <LogoutIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText>Logout</ListItemText>
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>
      <Box
        component="nav"
        sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}
        aria-label="mailbox folders"
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true, // Better open performance on mobile.
          }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>
      <Box
        component="main"
        sx={{ flexGrow: 1, p: 3, width: { md: `calc(100% - ${drawerWidth}px)` } }}
      >
        <Toolbar /> {/* This is for spacing below the AppBar */}
        {children}
      </Box>
    </Box>
  );
};

export default Layout;
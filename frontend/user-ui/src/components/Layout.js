import React, { useState, useEffect, useCallback } from 'react';
import { Link, useLocation } from 'react-router-dom';
import axios from 'axios'; // Add this if not present
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
} from '../services/notificationService'; // Assuming sseService is wrapped here
import eventBus from '../utils/eventBus';

const drawerWidth = 240;

const Layout = ({ children, user, onLogout }) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);
  const [unreadCount, setUnreadCount] = useState(0);
  const location = useLocation();

  const fetchAndUpdateUnreadCount = useCallback(async (signal) => { // Added signal parameter
    const currentUserId = user?.id;
    // console.log('Layout.js: fetchAndUpdateUnreadCount running for user:', currentUserId);

    const token = localStorage.getItem('token');
    if (!token) {
      // console.log('Layout.js: fetchAndUpdateUnreadCount - No token found, aborting fetch.');
      setUnreadCount(0);
      return;
    }

    if (currentUserId) {
      try {
        // console.log('Layout.js: fetchAndUpdateUnreadCount - Fetching count with signal.');
        const count = await countUnreadNotifications(currentUserId, signal); // Pass signal
        setUnreadCount(count);
      } catch (error) {
        if (axios.isCancel(error)) { // Import axios if not already, or check error name/type
          // console.log('Layout.js: fetchAndUpdateUnreadCount - Request canceled.');
        } else {
          console.error('Error fetching unread count for Layout:', error);
        }
        // Do not set unread count to 0 on error necessarily,
        // unless it's a specific requirement for cancellation or other errors.
        // If the request is cancelled, the count might remain stale, which is often acceptable.
      }
    } else {
      // console.log('Layout.js: fetchAndUpdateUnreadCount - No user or user.id, skipping fetch.');
      setUnreadCount(0);
    }
  }, [user?.id]); // Dependency on user.id is key. Signal itself doesn't need to be a dep.

  // useEffect for fetching initial count and subscribing to updates
  useEffect(() => {
    const controller = new AbortController();

    const eventBusHandler = () => {
      fetchAndUpdateUnreadCount(controller.signal);
    };

    if (user?.id) {
      fetchAndUpdateUnreadCount(controller.signal); // Initial fetch

      const handleNewNotification = (event) => { // For SSE
        fetchAndUpdateUnreadCount(controller.signal);
      };

      const unsubscribeWs = subscribeToRealtimeNotifications(handleNewNotification);
      eventBus.on('notificationsUpdated', eventBusHandler);

      return () => {
        controller.abort();
        if (unsubscribeWs) {
          unsubscribeWs();
        }
        eventBus.off('notificationsUpdated', eventBusHandler); // Correctly remove the named handler
      };
    } else {
      setUnreadCount(0);
      return () => {
        controller.abort();
      };
    }
  }, [user?.id, fetchAndUpdateUnreadCount]); // fetchAndUpdateUnreadCount is memoized

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleProfileMenuOpen = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogoutClick = () => { // Renamed to avoid conflict with onLogout prop if any confusion
    handleMenuClose();
    onLogout(); // Call the onLogout prop passed from App.js
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
            {/* Determine page title based on location or children props */}
            {location.pathname === '/' ? 'Dashboard' : 
             location.pathname.startsWith('/notifications') ? 'Notifications' : 
             'App'}
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
              {/* Ensure user object is checked before accessing name */}
              <Typography variant="body2">{user?.name || "User"}</Typography>
            </MenuItem>
            <Divider />
            <MenuItem onClick={handleLogoutClick}>
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
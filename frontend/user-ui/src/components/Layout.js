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

  const fetchAndUpdateUnreadCount = useCallback(async () => {
    console.log('Layout.js: fetchAndUpdateUnreadCount running for user:', user?.id);
    const currentUserId = user?.id; // Capture user.id from closure

    if (currentUserId) {
      try {
        const count = await countUnreadNotifications(currentUserId);
        console.log('Layout.js: fetchAndUpdateUnreadCount - About to setUnreadCount. Fetched count:', count);
        setUnreadCount(count);
      } catch (error) {
        console.error('Error fetching unread count for Layout:', error);
      }
    } else {
      console.log('Layout.js: fetchAndUpdateUnreadCount - No user or user.id, skipping fetch.');
    }
  }, []); // Empty dependency array makes this callback stable

  useEffect(() => {
    // Ensure user.id is available before trying to fetch or subscribe
    if (user?.id) {
      console.log('Layout_SubEffect_Setup: UserID:', user?.id, 'Callback_fetchAndUpdate_ref:', fetchAndUpdateUnreadCount);
      fetchAndUpdateUnreadCount(); // Initial fetch

      const handleNewNotification = (event) => {
        console.log('Layout.js: handleNewNotification invoked with event:', event);
        console.log('Layout.js: Calling fetchAndUpdateUnreadCount due to new notification event.');
        fetchAndUpdateUnreadCount();
      };

      const unsubscribeWs = subscribeToRealtimeNotifications(handleNewNotification);
      eventBus.on('notificationsUpdated', fetchAndUpdateUnreadCount);

      return () => {
        console.log('Layout_SubEffect_Cleanup: UserID:', user?.id, 'Callback_fetchAndUpdate_ref:', fetchAndUpdateUnreadCount);
        if (unsubscribeWs) {
          unsubscribeWs();
        }
        eventBus.off('notificationsUpdated', fetchAndUpdateUnreadCount);
      };
    } else {
      console.log('Layout_SubEffect: Skipping setup, no user.id.');
      // Optionally, reset unreadCount if user logs out or user.id becomes unavailable
      setUnreadCount(0); 
    }
  }, [user?.id, fetchAndUpdateUnreadCount]); // Depends on user (for user.id) and stable fetchAndUpdateUnreadCount

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
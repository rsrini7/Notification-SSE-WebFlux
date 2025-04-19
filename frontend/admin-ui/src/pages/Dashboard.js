import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Grid,
  Paper,
  Card,
  CardContent,
  List,
  ListItem,
  ListItemText,
  Divider,
  CircularProgress,
  Alert,
  Button
} from '@mui/material';
import { format } from 'date-fns';
import { useNavigate } from 'react-router-dom';
import { getNotificationStats, getRecentNotifications } from '../services/notificationService';

const Dashboard = ({ user }) => {
  const navigate = useNavigate();
  const [stats, setStats] = useState(null);
  const [recentNotifications, setRecentNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        const [statsData, recentData] = await Promise.all([
          getNotificationStats(),
          getRecentNotifications(5)
        ]);
        setStats(statsData);
        setRecentNotifications(recentData);
      } catch (err) {
        console.error('Error fetching dashboard data:', err);
        if (err.message.includes('Authentication required')) {
          // Clear token and redirect to login
          localStorage.removeItem('token');
          navigate('/login');
        } else {
          setError('Failed to load dashboard data. Please try again later.');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, [navigate]);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="50vh">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error">{error}</Alert>;
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Admin Dashboard
      </Typography>
      <Typography variant="subtitle1" color="textSecondary" paragraph>
        Welcome back, {user?.name || user?.username}!
      </Typography>

      <Button
        variant="contained"
        color="primary"
        sx={{ mb: 2 }}
        onClick={() => navigate('/notifications/broadcast')}
      >
        Send Broadcast Notification
      </Button>

      <Grid container spacing={3} sx={{ mt: 2 }}>
        {/* Stats Cards */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" color="primary" gutterBottom>
                Total Notifications
              </Typography>
              <Typography variant="h3">
                {stats?.totalCount || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" color="primary" gutterBottom>
                Sent Today
              </Typography>
              <Typography variant="h3">
                {stats?.todayCount || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" color="primary" gutterBottom>
                Read Rate
              </Typography>
              <Typography variant="h3">
                {stats?.readRate ? `${stats.readRate}%` : 'N/A'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Recent Notifications */}
        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Recent Notifications
            </Typography>
            {recentNotifications.length > 0 ? (
              <List>
                {recentNotifications.map((notification, index) => (
                  <React.Fragment key={notification.id}>
                    <ListItem alignItems="flex-start">
                      <ListItemText
                        primary={notification.title}
                        secondary={
                          <React.Fragment>
                            <Typography
                              component="span"
                              variant="body2"
                              color="textPrimary"
                            >
                              {notification.type} - 
                            </Typography>
                            {notification.content ? notification.content.substring(0, 100) : ''}
                            {notification.content && notification.content.length > 100 ? '...' : ''}
                            <Typography
                              component="div"
                              variant="caption"
                              color="textSecondary"
                              sx={{ mt: 1 }}
                            >
                              Sent: {format(new Date(notification.createdAt), 'PPpp')}
                              {notification.isBroadcast && ' • Broadcast'}
                            </Typography>
                          </React.Fragment>
                        }
                      />
                    </ListItem>
                    {index < recentNotifications.length - 1 && <Divider />}
                  </React.Fragment>
                ))}
              </List>
            ) : (
              <Typography variant="body2" color="textSecondary">
                No notifications have been sent yet.
              </Typography>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;

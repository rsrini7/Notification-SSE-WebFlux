import React, { useState, useEffect } from 'react';
import { useParams, Link as RouterLink } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  CircularProgress,
  Alert,
  Button,
  Divider,
  Chip,
  Grid
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { getNotificationById } from '../services/notificationService';
import { format } from 'date-fns';

const NotificationDetail = () => {
  const { id } = useParams();
  const [notification, setNotification] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const tryParseJson = (jsonString) => {
    if (typeof jsonString !== 'string') {
        // If it's already an object (e.g., parsed by axios or backend), stringify it
        return JSON.stringify(jsonString, null, 2);
    }
    try {
      const obj = JSON.parse(jsonString);
      return JSON.stringify(obj, null, 2); // Pretty print if it's valid JSON
    } catch (e) {
      return jsonString; // Return as is if not valid JSON (e.g. plain string)
    }
  };

  useEffect(() => {
    const fetchNotification = async () => {
      if (!id) {
        setError('Notification ID is missing.');
        setLoading(false);
        return;
      }
      try {
        setLoading(true);
        const data = await getNotificationById(id);
        setNotification(data);
      } catch (err) {
        console.error(`Error fetching notification ${id}:`, err);
        setError('Failed to load notification details. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    fetchNotification();
  }, [id]);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="50vh">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>;
  }

  if (!notification) {
    return <Alert severity="info" sx={{ mt:2 }}>Notification not found.</Alert>;
  }

  return (
    <Box sx={{ p: { xs: 2, md: 3 } }}>
      <Button
        component={RouterLink}
        to="/" // Back to Dashboard
        startIcon={<ArrowBackIcon />}
        sx={{ mb: 2 }}
      >
        Back to Dashboard
      </Button>
      <Paper elevation={3} sx={{ p: { xs: 2, md: 3 } }}>
        <Typography variant="h4" component="h1" gutterBottom>
          {notification.title || 'Notification Details'}
        </Typography>
        <Divider sx={{ my: 2 }} />

        <Box sx={{ mb: 2 }}>
          <Typography variant="subtitle1" component="strong" gutterBottom>Content:</Typography>
          <Typography variant="body1" sx={{ mt: 1, p: 2, backgroundColor: 'grey.100', borderRadius: 1, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
            {notification.content}
          </Typography>
        </Box>

        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid item xs={12} sm={6}><Typography variant="body2"><strong>ID:</strong> {notification.id}</Typography></Grid>
          <Grid item xs={12} sm={6}><Typography variant="body2"><strong>User ID:</strong> {notification.userId}</Typography></Grid>
          <Grid item xs={12} sm={6}><Typography variant="body2" component="div"><strong>Type:</strong> <Chip label={notification.notificationType || 'N/A'} size="small" /></Typography></Grid>
          <Grid item xs={12} sm={6}><Typography variant="body2" component="div"><strong>Priority:</strong> <Chip label={notification.priority || 'N/A'} size="small" color={notification.priority === 'CRITICAL' ? 'error' : (notification.priority === 'HIGH' ? 'warning' : 'default')} /></Typography></Grid>
          <Grid item xs={12} sm={6}><Typography variant="body2"><strong>Source:</strong> {notification.sourceService || 'N/A'}</Typography></Grid>
          <Grid item xs={12} sm={6}><Typography variant="body2" component="div"><strong>Status:</strong> <Chip label={notification.readStatus || 'N/A'} size="small" color={notification.readStatus === 'UNREAD' ? 'primary' : 'default'} /></Typography></Grid>
          <Grid item xs={12}><Typography variant="body2"><strong>Created At:</strong> {notification.createdAt ? format(new Date(notification.createdAt), 'PPPppp') : 'N/A'}</Typography></Grid>
        </Grid>

        {notification.metadata && (
          <Box sx={{ mb: 2 }}>
            <Typography variant="subtitle1" component="strong" gutterBottom>Metadata:</Typography>
            <Paper variant="outlined" sx={{ p: 2, mt: 1, overflowX: 'auto', backgroundColor: 'grey.50', maxHeight: '200px' }}>
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                {tryParseJson(notification.metadata)}
              </pre>
            </Paper>
          </Box>
        )}
      </Paper>
    </Box>
  );
};

export default NotificationDetail;
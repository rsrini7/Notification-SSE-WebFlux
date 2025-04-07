import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormControlLabel,
  Switch,
  Chip,
  OutlinedInput,
  Checkbox,
  ListItemText,
  Alert,
  Snackbar,
  Card,
  CardContent,
  Divider,
  Grid,
  CircularProgress
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { 
  getNotificationTypes, 
  getUsers, 
  sendNotification, 
  sendBroadcastNotification 
} from '../services/notificationService';

const ITEM_HEIGHT = 48;
const ITEM_PADDING_TOP = 8;
const MenuProps = {
  PaperProps: {
    style: {
      maxHeight: ITEM_HEIGHT * 4.5 + ITEM_PADDING_TOP,
      width: 250,
    },
  },
};

const NotificationForm = ({ user }) => {
  const { control, handleSubmit, watch, reset, formState: { errors } } = useForm({
    defaultValues: {
      title: '',
      content: '',
      type: '',
      isBroadcast: false,
      isCritical: false,
      selectedUsers: []
    }
  });

  const [notificationTypes, setNotificationTypes] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');
  const [typesLoading, setTypesLoading] = useState(true);
  const [usersLoading, setUsersLoading] = useState(true);

  // Watch form values for preview
  const watchTitle = watch('title');
  const watchContent = watch('content');
  const watchType = watch('type');
  const watchIsBroadcast = watch('isBroadcast');
  const watchIsCritical = watch('isCritical');

  useEffect(() => {
    const fetchFormData = async () => {
      try {
        // Fetch notification types
        setTypesLoading(true);
        const typesData = await getNotificationTypes();
        setNotificationTypes(typesData);
        setTypesLoading(false);

        // Fetch users for targeting
        setUsersLoading(true);
        const usersData = await getUsers();
        setUsers(usersData);
        setUsersLoading(false);
      } catch (err) {
        console.error('Error fetching form data:', err);
        setError('Failed to load form data. Please try again later.');
        setTypesLoading(false);
        setUsersLoading(false);
      }
    };

    fetchFormData();
  }, []);

  const onSubmit = async (data) => {
    setLoading(true);
    setError('');
    
    try {
      if (data.isBroadcast) {
        // Send broadcast notification to all users
        await sendBroadcastNotification({
          title: data.title,
          content: data.content,
          type: data.type,
          isCritical: data.isCritical
        });
      } else {
        // Send notification to selected users
        await sendNotification({
          targetUserIds: data.selectedUsers,
          sourceService: "admin-ui",
          notificationType: data.type,
          priority: data.isCritical ? "CRITICAL" : "NORMAL",
          content: data.content,
          metadata: {},
          tags: []
        });
      }
      
      setSuccess(true);
      reset(); // Reset form after successful submission
    } catch (err) {
      console.error('Error sending notification:', err);
      setError(err.response?.data?.message || 'Failed to send notification. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleCloseSnackbar = () => {
    setSuccess(false);
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Send Notification
      </Typography>
      
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      
      <Grid container spacing={3}>
        <Grid item xs={12} md={7}>
          <Paper 
            elevation={3} 
            sx={{ p: 3 }} 
            className="notification-form-card"
          >
            <Box component="form" onSubmit={handleSubmit(onSubmit)} className="notification-form">
              <Controller
                name="title"
                control={control}
                rules={{ required: 'Title is required' }}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Notification Title"
                    fullWidth
                    margin="normal"
                    error={!!errors.title}
                    helperText={errors.title?.message}
                  />
                )}
              />
              
              <Controller
                name="content"
                control={control}
                rules={{ required: 'Content is required' }}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Notification Content"
                    fullWidth
                    multiline
                    rows={4}
                    margin="normal"
                    error={!!errors.content}
                    helperText={errors.content?.message}
                  />
                )}
              />
              
              <Controller
                name="type"
                control={control}
                rules={{ required: 'Notification type is required' }}
                render={({ field }) => (
                  <FormControl fullWidth margin="normal" error={!!errors.type}>
                    <InputLabel id="notification-type-label">Notification Type</InputLabel>
                    <Select
                      {...field}
                      labelId="notification-type-label"
                      label="Notification Type"
                      disabled={typesLoading}
                    >
                      {typesLoading ? (
                        <MenuItem disabled>Loading types...</MenuItem>
                      ) : (
                        notificationTypes.map((type) => (
                          <MenuItem key={type} value={type}>
                            {type}
                          </MenuItem>
                        ))
                      )}
                    </Select>
                    {errors.type && (
                      <Typography variant="caption" color="error">
                        {errors.type.message}
                      </Typography>
                    )}
                  </FormControl>
                )}
              />
              
              <Box sx={{ mt: 2 }}>
                <Controller
                  name="isCritical"
                  control={control}
                  render={({ field }) => (
                    <FormControlLabel
                      control={
                        <Switch
                          checked={field.value}
                          onChange={(e) => field.onChange(e.target.checked)}
                        />
                      }
                      label="Mark as Critical"
                    />
                  )}
                />
              </Box>
              
              <Box sx={{ mt: 2 }}>
                <Controller
                  name="isBroadcast"
                  control={control}
                  render={({ field }) => (
                    <FormControlLabel
                      control={
                        <Switch
                          checked={field.value}
                          onChange={(e) => field.onChange(e.target.checked)}
                        />
                      }
                      label="Broadcast to All Users"
                    />
                  )}
                />
              </Box>
              
              {!watchIsBroadcast && (
                <Controller
                  name="selectedUsers"
                  control={control}
                  rules={{ 
                    validate: value => 
                      watchIsBroadcast || value.length > 0 || 'Please select at least one user' 
                  }}
                  render={({ field }) => (
                    <FormControl 
                      fullWidth 
                      margin="normal" 
                      error={!!errors.selectedUsers}
                      disabled={usersLoading || watchIsBroadcast}
                    >
                      <InputLabel id="selected-users-label">Select Users</InputLabel>
                      <Select
                        {...field}
                        labelId="selected-users-label"
                        label="Select Users"
                        multiple
                        input={<OutlinedInput label="Select Users" />}
                        renderValue={(selected) => (
                          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                            {selected.map((userId) => {
                              const selectedUser = users.find(u => u.id === userId);
                              return (
                                <Chip 
                                  key={userId} 
                                  label={selectedUser ? selectedUser.username : userId} 
                                />
                              );
                            })}
                          </Box>
                        )}
                        MenuProps={MenuProps}
                      >
                        {usersLoading ? (
                          <MenuItem disabled>Loading users...</MenuItem>
                        ) : (
                          users.map((user) => (
                            <MenuItem key={user.id} value={user.id}>
                              <Checkbox checked={field.value.indexOf(user.id) > -1} />
                              <ListItemText primary={user.username} secondary={user.email} />
                            </MenuItem>
                          ))
                        )}
                      </Select>
                      {errors.selectedUsers && (
                        <Typography variant="caption" color="error">
                          {errors.selectedUsers.message}
                        </Typography>
                      )}
                    </FormControl>
                  )}
                />
              )}
              
              <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  variant="contained"
                  color="primary"
                  type="submit"
                  disabled={loading}
                  startIcon={loading && <CircularProgress size={20} color="inherit" />}
                >
                  {loading ? 'Sending...' : 'Send Notification'}
                </Button>
              </Box>
            </Box>
          </Paper>
        </Grid>
        
        <Grid item xs={12} md={5}>
          <Typography variant="h6" gutterBottom>
            Notification Preview
          </Typography>
          <Card className="notification-preview">
            <CardContent>
              {(watchTitle || watchContent) ? (
                <>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                    <Typography variant="subtitle1" fontWeight="bold">
                      {watchTitle || 'Notification Title'}
                    </Typography>
                    {watchIsCritical && (
                      <Chip 
                        label="CRITICAL" 
                        color="error" 
                        size="small" 
                      />
                    )}
                  </Box>
                  
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    Type: {watchType || 'Not specified'}
                  </Typography>
                  
                  <Divider sx={{ my: 1 }} />
                  
                  <Typography variant="body1">
                    {watchContent || 'Notification content will appear here...'}
                  </Typography>
                  
                  <Box sx={{ mt: 2, display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="caption" color="text.secondary">
                      {new Date().toLocaleString()}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {watchIsBroadcast ? 'Broadcast to all users' : 'Sent to selected users'}
                    </Typography>
                  </Box>
                </>
              ) : (
                <Typography variant="body2" color="text.secondary" align="center">
                  Fill out the form to see a preview of your notification
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      
      <Snackbar
        open={success}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
      >
        <Alert onClose={handleCloseSnackbar} severity="success" sx={{ width: '100%' }} className="success-animation">
          Notification sent successfully!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default NotificationForm;
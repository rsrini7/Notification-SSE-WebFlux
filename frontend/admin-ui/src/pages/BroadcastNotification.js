import React, { useState } from 'react';
import { sendBroadcastNotification } from '../services/notificationService';
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
  Alert,
  Snackbar,
  Card,
  CardContent,
  Divider,
  Grid,
  CircularProgress
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';

const BroadcastNotification = () => {
  const { control, handleSubmit, watch, reset, formState: { errors } } = useForm({
    defaultValues: {
      title: '',
      message: '',
      priority: 'NORMAL'
    }
  });

  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  // Watch form values for preview
  const watchTitle = watch('title');
  const watchMessage = watch('message');
  const watchPriority = watch('priority');

  const onSubmit = async (data) => {
    setLoading(true);
    setError('');
    try {
      // Ensure all fields are properly formatted and no null values
      const payload = {
        title: data.title.trim(),
        message: data.message.trim(),
        priority: data.priority,
        metadata: {}, // Add empty metadata object if needed
        tags: [] // Add empty tags array if needed
      };
      
      await sendBroadcastNotification(payload);
      setSuccess(true);
      reset();
    } catch (err) {
      console.error('Error sending broadcast:', err);
      setError('Failed to send broadcast notification. Please try again.');
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
        Send Broadcast Notification
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Grid container spacing={3}>
        <Grid item xs={12} md={7}>
          <Paper elevation={3} sx={{ p: 3 }}>
            <Box component="form" onSubmit={handleSubmit(onSubmit)}>
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
                name="message"
                control={control}
                rules={{ required: 'Message is required' }}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Message Content"
                    fullWidth
                    multiline
                    rows={4}
                    margin="normal"
                    error={!!errors.message}
                    helperText={errors.message?.message}
                  />
                )}
              />

              <Controller
                name="priority"
                control={control}
                rules={{ required: 'Priority is required' }}
                render={({ field }) => (
                  <FormControl fullWidth margin="normal" error={!!errors.priority}>
                    <InputLabel id="priority-label">Priority</InputLabel>
                    <Select
                      {...field}
                      labelId="priority-label"
                      label="Priority"
                    >
                      <MenuItem value="NORMAL">Normal</MenuItem>
                      <MenuItem value="HIGH">High</MenuItem>
                      <MenuItem value="CRITICAL">Critical</MenuItem>
                    </Select>
                  </FormControl>
                )}
              />

              <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  variant="contained"
                  color="primary"
                  type="submit"
                  disabled={loading}
                  startIcon={loading && <CircularProgress size={20} color="inherit" />}
                >
                  {loading ? 'Sending...' : 'Send Broadcast'}
                </Button>
              </Box>
            </Box>
          </Paper>
        </Grid>

        <Grid item xs={12} md={5}>
          <Typography variant="h6" gutterBottom>
            Broadcast Preview
          </Typography>
          <Card>
            <CardContent>
              {(watchTitle || watchMessage) ? (
                <>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                    <Typography variant="subtitle1" fontWeight="bold">
                      {watchTitle || 'Broadcast Title'}
                    </Typography>
                    {watchPriority === 'CRITICAL' && (
                      <Alert severity="error" sx={{ py: 0 }}>CRITICAL</Alert>
                    )}
                    {watchPriority === 'HIGH' && (
                      <Alert severity="warning" sx={{ py: 0 }}>HIGH</Alert>
                    )}
                  </Box>

                  <Divider sx={{ my: 1 }} />

                  <Typography variant="body1">
                    {watchMessage || 'Broadcast message will appear here...'}
                  </Typography>

                  <Box sx={{ mt: 2 }}>
                    <Typography variant="caption" color="text.secondary">
                      Will be sent to all users • {new Date().toLocaleString()}
                    </Typography>
                  </Box>
                </>
              ) : (
                <Typography variant="body2" color="text.secondary" align="center">
                  Fill out the form to see a preview of your broadcast
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
        <Alert onClose={handleCloseSnackbar} severity="success" sx={{ width: '100%' }}>
          Broadcast notification sent successfully!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default BroadcastNotification;

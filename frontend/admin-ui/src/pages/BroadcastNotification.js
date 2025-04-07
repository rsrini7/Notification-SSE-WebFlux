import React, { useState } from 'react';
import { sendBroadcastNotification } from '../services/notificationService';

const BroadcastNotification = () => {
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [priority, setPriority] = useState('NORMAL');
  const [status, setStatus] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setStatus(null);
    try {
      await sendBroadcastNotification({ title, message, priority });
      setStatus('Broadcast notification sent successfully.');
      setTitle('');
      setMessage('');
      setPriority('NORMAL');
    } catch (error) {
      setStatus('Failed to send broadcast notification.');
    }
  };

  return (
    <div>
      <h2>Send Broadcast Notification</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Title:</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
        </div>
        <div>
          <label>Message:</label>
          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            required
          />
        </div>
        <div>
          <label>Priority:</label>
          <select
            value={priority}
            onChange={(e) => setPriority(e.target.value)}
          >
            <option value="NORMAL">Normal</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
        </div>
        <button type="submit">Send Broadcast</button>
      </form>
      {status && <p>{status}</p>}
    </div>
  );
};

export default BroadcastNotification;

const { createProxyMiddleware } = require('http-proxy-middleware');
module.exports = function(app) {
  app.use(
    '/api', // Or your specific SSE path
    createProxyMiddleware({
      target: 'http://localhost:8080',
      changeOrigin: true,
      ws: true // Often helps with SSE
    })
  );
};
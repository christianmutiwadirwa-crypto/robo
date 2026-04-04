const express = require('express');
const http = require('http');
const { Server } = require('socket.io');

const app = express();
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: '*' } });
const PORT = process.env.PORT || 3000;

// Middleware
app.use(express.json());
app.use(express.static('public')); // Serve the web dashboard

// Store recent readings
const readings = [];
const MAX_READINGS = 1000;
let totalReceived = 0;

// HTTP Endpoint to receive accelerometer data from Android app
app.post('/data', (req, res) => {
  const { x, y, z, timestamp } = req.body;

  // Validate data
  if (typeof x === 'number' && typeof y === 'number' && typeof z === 'number') {
    const reading = {
      x: parseFloat(x.toFixed(4)),
      y: parseFloat(y.toFixed(4)),
      z: parseFloat(z.toFixed(4)),
      timestamp: timestamp || Date.now(),
      receivedAt: new Date().toISOString()
    };

    readings.push(reading);
    totalReceived++;

    // Keep only recent readings in memory
    if (readings.length > MAX_READINGS) {
      readings.shift();
    }

    // Broadcast reading to all connected web clients
    io.emit('new_reading', reading);

    if (totalReceived % 10 === 0) {
      console.log(`[${reading.receivedAt}] X: ${reading.x.toFixed(4)}m/s², Y: ${reading.y.toFixed(4)}m/s², Z: ${reading.z.toFixed(4)}m/s²`);
    }
    res.json({ success: true, message: 'Data received', id: totalReceived });
  } else {
    res.status(400).json({
      success: false,
      message: 'Invalid data format. Expected: {x: number, y: number, z: number, timestamp?: number}'
    });
  }
});

// Status endpoint
app.get('/status', (req, res) => {
  const lastReading = readings[readings.length - 1];
  res.json({
    status: 'running',
    serverTime: new Date().toISOString(),
    totalReadings: readings.length,
    totalReceived: totalReceived,
    lastReading: lastReading || null,
    uptime: process.uptime()
  });
});

// Get recent readings
app.get('/readings', (req, res) => {
  const limit = Math.min(parseInt(req.query.limit) || 100, readings.length);
  const offset = Math.max(0, readings.length - limit);
  res.json(readings.slice(offset));
});

// Get statistics
app.get('/stats', (req, res) => {
  if (readings.length === 0) {
    return res.json({ error: 'No readings available' });
  }

  const xs = readings.map(r => r.x);
  const ys = readings.map(r => r.y);
  const zs = readings.map(r => r.z);

  const avg = (arr) => arr.reduce((a, b) => a + b, 0) / arr.length;
  const min = (arr) => Math.min(...arr);
  const max = (arr) => Math.max(...arr);
  const std = (arr) => {
    const mean = avg(arr);
    const variance = arr.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) / arr.length;
    return Math.sqrt(variance);
  };

  res.json({
    x: { avg: avg(xs), min: min(xs), max: max(xs), std: std(xs) },
    y: { avg: avg(ys), min: min(ys), max: max(ys), std: std(ys) },
    z: { avg: avg(zs), min: min(zs), max: max(zs), std: std(zs) },
    samples: readings.length
  });
});

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(500).json({ error: 'Internal server error' });
});

// Start server
server.listen(PORT, '0.0.0.0', () => {
  console.log('\n╔════════════════════════════════════════════════════════════╗');
  console.log('║     Accelerometer Data Server Running                     ║');
  console.log('╚════════════════════════════════════════════════════════════╝\n');
  console.log(`📡 Server listening on http://0.0.0.0:${PORT}`);
  console.log(`\n📊 Available Endpoints:`);
  console.log(`   POST   /data         - Receive accelerometer data`);
  console.log(`   GET    /status       - Server status`);
  console.log(`   GET    /readings     - Get recent readings (limit=100)`);
  console.log(`   GET    /stats        - Statistics (avg, min, max, std)`);
  console.log(`   GET    /health       - Health check\n`);
  console.log(`🌍 Test locally: http://localhost:${PORT}/status`);
  console.log(`\n📋 Server is waiting for connections...\n`);
});

// Graceful shutdown
process.on('SIGINT', () => {
  console.log('\n\n🛑 Shutting down server...');
  console.log(`📊 Total readings received: ${totalReceived}`);
  process.exit(0);
});

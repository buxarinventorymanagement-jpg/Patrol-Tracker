const express = require('express');
const cors = require('cors');
const { PrismaClient } = require('@prisma/client');

const app = express();
const prisma = new PrismaClient();

app.use(cors());
app.use(express.json());

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Patrol Tracker Node.js API is online', timestamp: new Date().toISOString() });
});

app.get('/api/checkpoints', async (req, res) => {
  try {
    const checkpoints = await prisma.checkpoint.findMany();
    res.json(checkpoints);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/checkpoints', async (req, res) => {
  try {
    const checkpoint = await prisma.checkpoint.create({ data: req.body });
    res.json(checkpoint);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/users', async (req, res) => {
  try {
    const users = await prisma.user.findMany();
    res.json(users);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/users', async (req, res) => {
  try {
    const user = await prisma.user.create({ data: req.body });
    res.json(user);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/duties', async (req, res) => {
  try {
    const duties = await prisma.dutyAllocation.findMany({
      include: { user: true }
    });
    res.json(duties);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/duties', async (req, res) => {
  try {
    const duty = await prisma.dutyAllocation.create({ data: req.body });
    res.json(duty);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/logs', async (req, res) => {
  try {
    const logs = await prisma.scanLog.findMany({
      include: { user: true, checkpoint: true, duty: true },
      orderBy: { scan_time: 'desc' }
    });
    res.json(logs);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/scan', async (req, res) => {
  try {
    const { qr_id, user_id, duty_id, latitude, longitude, notes } = req.body;

    const checkpoint = await prisma.checkpoint.findUnique({
      where: { qr_code_data: qr_id }
    });

    if (!checkpoint) {
      return res.status(404).json({ error: 'Invalid QR Code' });
    }

    const log = await prisma.scanLog.create({
      data: {
        checkpoint_id: checkpoint.checkpoint_id,
        user_id,
        duty_id,
        qr_id,
        latitude,
        longitude,
        notes,
        status: 'On-Time',
        patrol_status: 'Active Patrol'
      }
    });

    res.json(log);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

const PORT = process.env.PORT || 3000;
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`Patrol Tracker server running on http://localhost:${PORT}`);
  });
}

module.exports = app;

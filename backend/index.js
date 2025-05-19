const express = require('express');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
const { Pool } = require('pg');

const app = express();
const PORT = 5000;

// PostgreSQL connection
const pool = new Pool({
  user: 'postgres',
  host: 'localhost',
  database: 'stepup',
  password: 'Yen17082003@',
  port: 5432,
});

// Test DB connection when server starts
pool.query('SELECT NOW()', (err, res) => {
  if (err) {
    console.error('❌ Database connection failed:', err);
  } else {
    console.log('✅ Database connected. Current time from DB:', res.rows[0].now);
  }
});

// Middlewares
app.use(cors());
app.use(express.json());
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Multer config to store images in /uploads
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, 'uploads/');
  },
  filename: function (req, file, cb) {
    const ext = path.extname(file.originalname);
    const uniqueName = Date.now() + '-' + Math.round(Math.random() * 1E9) + ext;
    cb(null, uniqueName);
  }
});
const upload = multer({ storage: storage });

// ✅ API to test DB connection from browser/Postman
app.get('/db-check', async (req, res) => {
  try {
    const result = await pool.query('SELECT NOW()');
    res.send(`✅ DB OK: ${result.rows[0].now}`);
  } catch (err) {
    console.error('❌ DB check failed:', err);
    res.status(500).send('❌ DB connection failed.');
  }
});

// API to add a new place
app.post('/places', upload.single('image'), async (req, res) => {
  const { name, date, steps } = req.body;
  const imagePath = req.file ? `/uploads/${req.file.filename}` : null;

  try {
    const result = await pool.query(
      'INSERT INTO places (name, date_saved, steps, image_path) VALUES ($1, $2, $3, $4) RETURNING *',
      [name, date, steps, imagePath]
    );
    res.status(201).json(result.rows[0]);
  } catch (error) {
    console.error('Error inserting place:', error);
    res.status(500).json({ error: 'Failed to save place' });
  }
});

// API to get all places
app.get('/places', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM places ORDER BY id DESC');
    res.json(result.rows);
  } catch (error) {
    console.error('Error fetching places:', error);
    res.status(500).json({ error: 'Failed to fetch places' });
  }
});

app.listen(PORT, () => {
  console.log(`🚀 Server is running on http://localhost:${PORT}`);
});

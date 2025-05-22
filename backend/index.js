const express = require('express');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
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
app.use(cors());
app.use(express.json());

app.use((req, res, next) => {
  console.log(`Incoming request: ${req.method} ${req.url}`);
  console.log('Headers:', req.headers);
  console.log('Body:', req.body);
  next();
});
// Middlewares
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Ensure upload folder exists
const uploadFolder = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadFolder)) {
  fs.mkdirSync(uploadFolder);
}

// Multer config to store images in /uploads
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, uploadFolder);
  },
  filename: function (req, file, cb) {
    const ext = path.extname(file.originalname);
    const uniqueName = Date.now() + '-' + Math.round(Math.random() * 1E9) + ext;
    cb(null, uniqueName);
  }
});
const upload = multer({ storage: storage });

// Endpoint to upload an image only
app.post('/upload-image', upload.single('file'), (req, res) => {
  if (!req.file) {
    return res.status(400).json({ error: 'No file uploaded' });
  }
  // Respond with the filename so client can save it later with the place
  res.json({ filename: req.file.filename });
});

// Endpoint to save a place/memory WITHOUT image file, expects JSON body with image filename
app.post('/places', async (req, res) => {
  const {name, date_saved, steps_taken, image_url} = req.body; // imageName is string filename from /upload-image
  console.log('Received data:', {name, date_saved, steps_taken});
  console.log('Raw body:', req.body);
  if (!name) console.log("Missing: name");
  if (!date_saved) console.log("Missing: date_saved");
  if (!steps_taken) console.log("Missing: steps_taken");

  if (!name || !date_saved || !steps_taken) {
    console.log('❌ Missing one or more required fields');
    return res.status(400).json({ error: 'Missing required fields' });
  }


  try {
    const result = await pool.query(
    'INSERT INTO places (name, date_saved, steps_taken) VALUES ($1, $2, $3) RETURNING *',
      [name, date_saved, steps_taken]
    );
    res.status(201).json(result.rows[0]);
  } catch (error) {
    console.error('Error saving place to DB:', error);
    res.status(500).json({ error: 'Failed to save place' });
  }
});

// Endpoint to get all places
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
process.stdin.resume();


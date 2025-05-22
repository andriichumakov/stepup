const express = require('express');
const router = express.Router();
const pool = require('../db');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

// Folder to store uploaded images (ensure it exists)
const uploadFolder = path.join(__dirname, '..', 'uploads');
if (!fs.existsSync(uploadFolder)) {
  fs.mkdirSync(uploadFolder);
}

// Set up multer for image uploads
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, uploadFolder);
  },
  filename: function (req, file, cb) {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, uniqueSuffix + path.extname(file.originalname));
  }
});

const upload = multer({ storage: storage });

// GET all saved places
router.get('/', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM places ORDER BY id DESC');
    res.json(result.rows);
  } catch (err) {
    console.error('Error fetching places:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST a new place
router.post('/', upload.single('image'), async (req, res) => {
  const { name, date_saved, steps_taken} = req.body;
  const image = req.file ? req.file.filename : null;

  try {
    const result = await pool.query(
    'INSERT INTO places (name, date_saved, steps_taken) VALUES ($1, $2, $3, $4) RETURNING *',
      [name, date_saved, steps_taken, image_url]
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('Error saving place:', err);
    res.status(500).json({ error: 'Failed to save place' });
  }
});

module.exports = router;

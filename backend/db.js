const { Pool } = require('pg');

// Replace these values with your actual PostgreSQL config
const pool = new Pool({
  user: 'postgres',
  host: 'localhost',
  database: 'stepup',
  password: 'Yen17082003@',
  port: 5432,
});

module.exports = pool;

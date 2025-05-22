// test-api.js
const fetch = require('node-fetch');

async function testPost() {
  const response = await fetch('http://localhost:5000/places', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: "Sample Place",
      date_saved: "2025-05-21",
      steps_taken: 1234,
    }),
  });
  const data = await response.json();
  console.log(data);
}

testPost();

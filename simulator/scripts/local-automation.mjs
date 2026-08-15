import fetch from 'node-fetch'; // Polyfilled or natively available in Node 18+

const PROJECT_ID = process.env.SEED_FIREBASE_PROJECT_ID || 'smart-home-a99ed';
const REGION = 'asia-south1';
const EMULATOR_HOST = '127.0.0.1:5001';

const ENDPOINTS = [
  `http://${EMULATOR_HOST}/${PROJECT_ID}/${REGION}/enforceSafetyCutoffs`,
  `http://${EMULATOR_HOST}/${PROJECT_ID}/${REGION}/enforceLightSchedules`
];

console.log('Starting Local Automation Polling...');
console.log(`Polling every 60 seconds against project: ${PROJECT_ID}\n`);

async function triggerAutomations() {
  console.log(`[${new Date().toISOString()}] Triggering scheduled functions...`);
  
  for (const url of ENDPOINTS) {
    try {
      // In Firebase v2, scheduled functions are accessible via HTTP GET/POST on the emulator
      const response = await fetch(url);
      if (response.ok) {
        console.log(`✅ Successfully triggered ${url.split('/').pop()}`);
      } else {
        console.error(`❌ Failed to trigger ${url.split('/').pop()}: ${response.status} ${response.statusText}`);
      }
    } catch (err) {
      console.error(`❌ Error triggering ${url.split('/').pop()}: ${err.message}`);
    }
  }
  console.log('---');
}

// Trigger immediately on start
triggerAutomations();

// Poll every 60 seconds to emulate the cron job schedule
setInterval(triggerAutomations, 60000);

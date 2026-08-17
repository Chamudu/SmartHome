const PROJECT_ID = process.env.AUTOMATION_FIREBASE_PROJECT_ID || 'demo-smart-home';
const REGION = 'asia-south1';
const FUNCTIONS_EMULATOR_HOST = '127.0.0.1:5001';
const RUN_ONCE = process.argv.includes('--once');

const FUNCTIONS = [
  'enforceSafetyCutoffs',
  'enforceLightSchedules',
];

console.log('Starting Local Automation Polling...');
console.log(`Polling every 60 seconds against project: ${PROJECT_ID}\n`);

async function triggerAutomations() {
  console.log(`[${new Date().toISOString()}] Triggering scheduled functions...`);
  let failed = false;

  for (const functionName of FUNCTIONS) {
    // Firebase CLI exposes v2 background functions with a generated `-0` suffix locally.
    const url = `http://${FUNCTIONS_EMULATOR_HOST}/${PROJECT_ID}/${REGION}/${functionName}-0`;
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: '{}',
      });
      if (response.ok) {
        console.log(`Successfully triggered ${functionName}`);
      } else {
        failed = true;
        console.error(`Failed to trigger ${functionName}: ${response.status} ${response.statusText}`);
      }
    } catch (err) {
      failed = true;
      console.error(`Error triggering ${functionName}: ${err.message}`);
    }
  }
  console.log('---');
  if (failed && RUN_ONCE) process.exitCode = 1;
}

await triggerAutomations();

if (!RUN_ONCE) {
  // Poll every 60 seconds to emulate the production scheduler.
  setInterval(triggerAutomations, 60000);
}

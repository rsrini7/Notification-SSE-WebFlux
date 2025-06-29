// k6-script.js
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data'; // For sharing user credentials
import sse from "k6/x/sse"

// --- Metrics ---
const loginTime = new Trend('login_time');
const apiCallTime = new Trend('api_call_time');
const errors = new Rate('errors');

// SSE Metrics
const sseTimeToFirstEvent = new Trend('sse_time_to_first_event', true);
const sseTimeBetweenEvents = new Trend('sse_time_between_events', true);
const sseEventDataSize = new Trend('sse_event_data_size');
const sseEventParseErrors = new Counter('sse_event_parse_errors');
const sseEventParsedSuccessfully = new Rate('sse_event_parsed_successfully');

// --- Load User Credentials (example) ---
const users = new SharedArray('users', function () {
  return JSON.parse(open('./users.json')); // Assuming users.json has [{username, password}]
});

export const options = {
  stages: [
    { duration: '1m', target: 20 },  // Ramp up to 20 users
    { duration: '1m', target: 50 }, // Ramp up to 50 users
    { duration: '1m', target: 80 }, // Ramp up to 80 users
    { duration: '1m', target: 100 }, // Steady state at 100 users
    { duration: '1m', target: 0 },    // Ramp down
  ],
  thresholds: {
    'http_req_failed': ['rate<0.01'], // Less than 1% errors
    'http_req_duration': ['p(95)<1000'], // 95% of requests < 1s
    'login_time': ['p(95)<500'],
    'api_call_time': ['p(95)<1000'], // 95% of API calls < 1s
    'sse_time_between_events': ['p(95)<1000'], // 95% of events should arrive within 1s of each other.
    'sse_event_data_size': ['p(95)<1000'], // 95% of event data size should be less than 1000 bytes.
    'sse_event_parse_errors': ['count<100'], // No more than 100 parsing errors in total.
    'sse_event_parsed_successfully': ['rate>0.95'], // 95% of events should be parsed successfully.
    'sse_time_to_first_event': ['p(95)<1000'], // 95% of first events should arrive within 1s of connection.
  },
  // insecureSkipTLSVerify: true, // If using self-signed certs in test env
};

// --- Helper to get a random user ---
function getRandomUser() {
  return users[Math.floor(Math.random() * users.length)];
}

export default function () {
  const user = getRandomUser(); // Or VU ID based user selection

  const username = user.username;
  const password = user.password; // Ensure these users exist in your DB
  let authToken = null;

  group('User Login', function () {
    const loginPayload = JSON.stringify({ username: username, password: password });
    const loginParams = { headers: { 'Content-Type': 'application/json' } };
    const loginRes = http.post(`${__ENV.BASE_URL}/api/auth/login`, loginPayload, loginParams);

    const checkRes = check(loginRes, {
      'login successful': (r) => r.status === 200,
      'got auth token': (r) => r.json('token') !== null,
    });
    errors.add(!checkRes);
    loginTime.add(loginRes.timings.duration);

    if (!checkRes) {
      console.error(`Login failed for ${username}: ${loginRes.status} - ${loginRes.body}`);
      return; // Stop this VU iteration if login fails
    }
    authToken = loginRes.json('token');
  });

  if (!authToken) return;

  group('SSE Connection & Activity', function () {
    const sseUrl = `${__ENV.BASE_URL}/api/notifications/events?token=${authToken}`;
    const sseParams = {
      headers: { 'Accept': 'text/event-stream' },
    };

    const response = sse.open(sseUrl, sseParams, function (client) {
        let startTime;
        let lastEventTime;
        let isFirstEvent = true;

        client.on('open', function open() {
            console.log('connected')
            startTime = Date.now();
            lastEventTime = startTime;
        })

        client.on('event', function (event) {
            console.log(`event id=${event.id}, name=${event.name}, data=${event.data}`)

            const receivedTime = Date.now();
      
            // -- Add Trend Samples --
            if (isFirstEvent) {
              sseTimeToFirstEvent.add(receivedTime - startTime);
              isFirstEvent = false;
            }
            sseTimeBetweenEvents.add(receivedTime - lastEventTime);
            sseEventDataSize.add(event.data.length);
            lastEventTime = receivedTime; // Update for the next event

            // Try to parse the data only if it looks like a JSON object
            if (event.data && event.data.trim().startsWith('{')) {
                try {
                    JSON.parse(event.data);
                    sseEventParsedSuccessfully.add(true);
                } catch (e) {
                    sseEventParseErrors.add(1);
                    sseEventParsedSuccessfully.add(false);
                    console.error(`Failed to parse JSON: ${event.data}`);
                }
            } else {
                if(!event.data || event.data.trim() === '') {
                    sseEventParseErrors.add(1);
                    sseEventParsedSuccessfully.add(false);
                } else {
                    sseEventParsedSuccessfully.add(true);
                }
            }

            if (parseInt(event.id) === 3) {
                client.close()
            }
        })

        client.on('error', function (e) {
            console.log('An unexpected error occurred: ', e.error())
        })
    })

    check(response, {"status is 200": (r) => r && r.status === 200})

  });

  group('API Calls & Notifications', function () {
    
    // Simplified API call simulation (these are not SSE specific but typical user actions)
    for (let i = 0; i < 2; i++) { // Simulate a few API calls
      sleep(Math.random() * 5 + 1); // Think time 1-6s

      const apiRes = http.get(`${__ENV.BASE_URL}/api/notifications/user/${username}`, {
        headers: { 'Authorization': `Bearer ${authToken}` },
      });

      check(apiRes, { 'fetch notifications ok': (r) => r.status === 200 });
      apiCallTime.add(apiRes.timings.duration);
      errors.add(apiRes.status !== 200);
    }
    // Keep the "session" open for a while to simulate user being online
    sleep(30 + Math.random() * 30); // User stays "connected" for 30-60s
  });

}
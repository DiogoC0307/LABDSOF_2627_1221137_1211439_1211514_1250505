// IPMA stand-in for tests and the Compose profile `evaluation` (SCRUM-76, SCRUM-144, §6.5).
// Same paths as https://api.ipma.pt. Mode: IPMA_SIM_MODE=ok|unavailable|delayed|incomplete|invalid.
// IPMA_SIM_RCM (1-5) rewrites the corps municipality in an ok or delayed body. Never calls the real service.
const http = require('http');
const fs = require('fs');
const path = require('path');

const fixtures = path.join(__dirname, 'fixtures');
const mode = (process.env.IPMA_SIM_MODE || 'ok').trim().toLowerCase();
const dico = process.env.IPMA_SIM_DICO || '1312';
const rcmOverride = process.env.IPMA_SIM_RCM;
const port = Number(process.env.PORT || process.env.IPMA_SIM_PORT || 8090);

const RCM = /^\/open-data\/forecast\/meteorology\/rcm\/rcm-d[012]\.json$/;
const WARNINGS = '/open-data/forecast/warnings/warnings_www.json';

function read(name) {
  return fs.readFileSync(path.join(fixtures, name));
}

function rcmResponse() {
  if (mode === 'unavailable') {
    return { status: 503, type: 'text/plain; charset=utf-8', body: Buffer.from('unavailable') };
  }
  if (mode === 'invalid') {
    return { status: 200, type: 'text/plain; charset=utf-8', body: read('invalid.json') };
  }
  const file = mode === 'incomplete' ? 'rcm-d0-municipality-missing.json' : 'rcm-d0.json';
  const json = JSON.parse(read(file).toString('utf8'));
  if (mode === 'delayed') {
    json.dataPrev = '2020-01-01';
    json.dataRun = '2019-12-31';
    json.fileDate = '2020-01-01 00:00:00';
  }
  if (rcmOverride && json.local && json.local[dico] && json.local[dico].data) {
    json.local[dico].data.rcm = Number(rcmOverride);
  }
  return {
    status: 200,
    type: 'application/json; charset=utf-8',
    body: Buffer.from(JSON.stringify(json)),
  };
}

function warningsResponse() {
  if (mode === 'unavailable') {
    return { status: 503, type: 'text/plain; charset=utf-8', body: Buffer.from('unavailable') };
  }
  if (mode === 'invalid') {
    return { status: 200, type: 'text/plain; charset=utf-8', body: read('invalid.json') };
  }
  return {
    status: 200,
    type: 'application/json; charset=utf-8',
    body: read('warnings_www.json'),
  };
}

const server = http.createServer((request, response) => {
  const url = request.url.split('?')[0];
  let answer;
  if (RCM.test(url)) {
    answer = rcmResponse();
  } else if (url === WARNINGS) {
    answer = warningsResponse();
  } else {
    answer = { status: 404, type: 'text/plain; charset=utf-8', body: Buffer.from('not found') };
  }
  response.writeHead(answer.status, { 'Content-Type': answer.type });
  response.end(answer.body);
});

server.listen(port, '0.0.0.0', () => {
  const address = server.address();
  console.log(`listening ${address.port}`);
});

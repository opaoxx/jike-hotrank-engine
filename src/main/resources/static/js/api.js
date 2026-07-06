/* ================================================================
   Jike HotRank Engine — API Client & SSE Layer
   ================================================================ */

const API_BASE = '/api';

/**
 * Generic fetch wrapper that unwraps ApiResponse<T>.
 * Throws on network error or ApiResponse.code !== 0.
 */
async function apiFetch(url, options = {}) {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || `HTTP ${res.status}`);
  }
  const json = await res.json();
  if (json.code !== 0) {
    throw new Error(json.message || `API error code=${json.code}`);
  }
  return json.data;
}

/* ---- Ranking ---- */

function fetchRanking(type, params = {}) {
  const urls = {
    global:     `${API_BASE}/ranking/global`,
    circle:     `${API_BASE}/ranking/circle/${params.circleId || 1}`,
    newcomer:   `${API_BASE}/ranking/newcomer`,
    surging:    `${API_BASE}/ranking/surging`,
    personalized: `${API_BASE}/ranking/personalized`
  };
  let url = urls[type] || urls.global;
  // circle type uses path param, others use query string
  if (type !== 'circle') {
    const cleanParams = { ...params };
    delete cleanParams.circleId;
    const cleanQs = new URLSearchParams(cleanParams).toString();
    url = cleanQs ? `${url}?${cleanQs}` : url;
  } else {
    const { circleId, ...rest } = params;
    const restQs = new URLSearchParams(rest).toString();
    url = restQs ? `${url}?${restQs}` : url;
  }
  return apiFetch(url);
}

/* ---- Topic ---- */

function fetchTopicDetail(topicId) {
  return apiFetch(`${API_BASE}/topic/${topicId}`);
}

function blockTopic(topicId) {
  return apiFetch(`${API_BASE}/topic/${topicId}/block`, { method: 'POST' });
}

function unblockTopic(topicId) {
  return apiFetch(`${API_BASE}/topic/${topicId}/unblock`, { method: 'POST' });
}

/* ---- Interaction ---- */

function submitInteraction(body) {
  return apiFetch(`${API_BASE}/interaction`, {
    method: 'POST',
    body: JSON.stringify(body)
  });
}

/* ---- Analysis ---- */

function fetchAnalysis(endpoint, params = {}) {
  const qs = new URLSearchParams(params).toString();
  const url = qs ? `${API_BASE}/analysis/${endpoint}?${qs}` : `${API_BASE}/analysis/${endpoint}`;
  return apiFetch(url);
}

/* ---- Anti-Spam ---- */

function fetchAntiSpamReport() {
  return apiFetch(`${API_BASE}/anti-spam/report`);
}

/* ---- Cache Stats ---- */

function fetchCacheStats() {
  return apiFetch(`${API_BASE}/perf/cache-comparison`);
}

/* ---- SSE Connection ---- */

/**
 * Connect to the SSE ranking notification stream.
 * @param {Object} handlers - { onConnected, onRankingUpdated, onTopNEntered, onError }
 * @returns {Function} cleanup function to close the connection
 */
function connectSSE(handlers = {}) {
  const es = new EventSource(`${API_BASE}/notifications/rankings/stream`);

  es.addEventListener('connected', (e) => {
    const data = JSON.parse(e.data);
    if (handlers.onConnected) handlers.onConnected(data);
  });

  es.addEventListener('ranking-updated', (e) => {
    const data = JSON.parse(e.data);
    if (handlers.onRankingUpdated) handlers.onRankingUpdated(data);
  });

  es.addEventListener('top-n-entered', (e) => {
    const data = JSON.parse(e.data);
    if (handlers.onTopNEntered) handlers.onTopNEntered(data);
  });

  es.onerror = () => {
    if (handlers.onError) handlers.onError();
  };

  // Return cleanup
  return () => es.close();
}

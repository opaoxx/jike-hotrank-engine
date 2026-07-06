/* ================================================================
   Jike HotRank Engine — Dashboard UI Controller
   ================================================================ */

const state = {
  activeTab: 'overview',
  refreshTimer: null,
  refreshSeconds: 30,
  sseConnected: false,
  sseCleanup: null,
  charts: {},
  circleList: []
};

/* ---- Init ---- */
document.addEventListener('DOMContentLoaded', () => {
  bindTabs();
  bindModal();
  bindInteractionForm();
  startRefreshTimer();
  startSSE();
  renderTab('overview');
});

/* ---- Tab Switching ---- */
function bindTabs() {
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      state.activeTab = btn.dataset.tab;
      resetRefreshTimer();
      renderTab(state.activeTab);
    });
  });
}

function renderTab(tab) {
  destroyCharts();
  const content = document.getElementById('main-content');
  content.innerHTML = '<div class="loading"><div class="spinner"></div> Loading...</div>';

  const renderers = {
    overview: renderOverview,
    global: () => renderRankingTab('global', 'Global Hot Ranking', 50),
    circle: renderCircleTab,
    newcomer: () => renderRankingTab('newcomer', 'Newcomer Ranking', 10),
    surging: () => renderRankingTab('surging', 'Surging Ranking', 10)
  };

  (renderers[tab] || renderers.overview)();
}

/* ---- Overview Tab ---- */
async function renderOverview() {
  const content = document.getElementById('main-content');
  try {
    const data = await fetchAnalysis('overview');
    const hd = data.heatDistribution || {};
    const intStats = data.interactionStats || {};
    const ca = data.circleActivity || {};
    const cs = data.cacheStats || {};

    content.innerHTML = `
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-label">Total Topics</div>
          <div class="stat-value">${hd.topicCount || 0}</div>
          <div class="stat-sub">Max: ${(hd.maxScore || 0).toFixed(1)}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Interactions (${intStats.hours || 24}h)</div>
          <div class="stat-value">${(intStats.total || 0).toLocaleString()}</div>
          <div class="stat-sub">${(intStats.periodStart || '').slice(0,16)} ~ ${(intStats.periodEnd || '').slice(11,16)}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Avg Score</div>
          <div class="stat-value">${(hd.avgScore || 0).toFixed(1)}</div>
          <div class="stat-sub">Median: ${(hd.medianScore || 0).toFixed(1)}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Cache Hit Rate</div>
          <div class="stat-value">${((cs.hitRate || 0) * 100).toFixed(1)}%</div>
          <div class="stat-sub">Hits: ${cs.hits || 0} / Misses: ${cs.misses || 0}</div>
        </div>
      </div>

      <div class="charts-grid">
        <div class="panel">
          <div class="panel-header">Heat Distribution</div>
          <div class="panel-body">
            <div class="chart-container"><canvas id="chart-heat"></canvas></div>
          </div>
        </div>
        <div class="panel">
          <div class="panel-header">Interaction Breakdown</div>
          <div class="panel-body">
            <div class="chart-container"><canvas id="chart-interaction"></canvas></div>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-header">Circle Activity</div>
        <div class="panel-body no-padding" id="circle-activity-table"></div>
      </div>
    `;

    renderHeatChart(hd);
    renderInteractionChart(intStats);
    renderCircleActivityTable(ca);
  } catch (e) {
    content.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠</div>Failed to load overview: ${esc(e.message)}</div>`;
  }
}

function renderHeatChart(hd) {
  const ranges = (hd.ranges || []).map(r => r.range);
  const counts = (hd.ranges || []).map(r => r.count);
  state.charts.heat = new Chart(document.getElementById('chart-heat'), {
    type: 'bar',
    data: {
      labels: ranges,
      datasets: [{
        label: 'Topics',
        data: counts,
        backgroundColor: ['#1e40af','#2563eb','#3b82f6','#60a5fa','#93c5fd'],
        borderRadius: 4
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        y: { beginAtZero: true, ticks: { color: '#94a3b8' }, grid: { color: '#1e293b' } },
        x: { ticks: { color: '#94a3b8' }, grid: { display: false } }
      }
    }
  });
}

function renderInteractionChart(intStats) {
  const byType = intStats.byType || [];
  state.charts.interaction = new Chart(document.getElementById('chart-interaction'), {
    type: 'doughnut',
    data: {
      labels: byType.map(t => t.name),
      datasets: [{
        data: byType.map(t => t.count),
        backgroundColor: ['#3b82f6', '#22c55e', '#eab308', '#ef4444'],
        borderColor: '#1e293b',
        borderWidth: 2
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom',
          labels: { color: '#94a3b8', padding: 16, usePointStyle: true }
        }
      }
    }
  });
}

function renderCircleActivityTable(ca) {
  const items = ca.items || [];
  if (!items.length) {
    document.getElementById('circle-activity-table').innerHTML = '<div class="empty-state"><div class="empty-icon">📭</div>No circle data</div>';
    return;
  }
  // Cache circle list for the Circle tab dropdown
  state.circleList = items;
  const rows = items.map((c, i) => `
    <tr>
      <td class="rank-col"><span class="rank-badge ${i < 3 ? 'rank-'+(i+1) : 'rank-other'}">${c.rank || i+1}</span></td>
      <td>${esc(c.circleName)}</td>
      <td class="count-col">${c.topicCount || 0}</td>
      <td class="score-col">${(c.avgScore || 0).toFixed(1)}</td>
      <td class="count-col">${(c.interactionCount || 0).toLocaleString()}</td>
    </tr>
  `).join('');
  document.getElementById('circle-activity-table').innerHTML = `
    <table class="data-table">
      <thead><tr><th class="rank-col">#</th><th>Circle</th><th class="count-col">Topics</th><th class="score-col">Avg Score</th><th class="count-col">Interactions</th></tr></thead>
      <tbody>${rows}</tbody>
    </table>`;
}

/* ---- Ranking Tab (shared for global/newcomer/surging) ---- */
async function renderRankingTab(type, title, defaultLimit) {
  const content = document.getElementById('main-content');
  try {
    const data = await fetchRanking(type, { limit: defaultLimit });
    const items = data.items || [];
    content.innerHTML = `
      <div class="panel">
        <div class="panel-header">${title} <span style="font-weight:400;font-size:13px;color:var(--text-muted)">${items.length} topics · updated ${(data.updateTime || '').slice(11,19)}</span></div>
        <div class="panel-body no-padding" id="ranking-table"></div>
      </div>
    `;
    renderRankingTable(items, 'ranking-table');
  } catch (e) {
    content.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠</div>Failed to load ranking: ${esc(e.message)}</div>`;
  }
}

/* ---- Circle Tab ---- */
async function renderCircleTab() {
  const content = document.getElementById('main-content');
  // Ensure we have circle list
  if (!state.circleList.length) {
    try {
      const ca = await fetchAnalysis('circle-activity');
      state.circleList = ca.items || [];
    } catch (e) { /* ignore */ }
  }

  const options = state.circleList.map(c => `<option value="${c.circleId}">${esc(c.circleName)}</option>`).join('');
  content.innerHTML = `
    <div class="panel">
      <div class="panel-header">
        Circle Hot Ranking
        <select id="circle-select" style="width:200px">${options}</select>
      </div>
      <div class="panel-body no-padding" id="ranking-table">
        <div class="loading"><div class="spinner"></div> Loading...</div>
      </div>
    </div>
  `;

  const select = document.getElementById('circle-select');
  select.addEventListener('change', () => loadCircleRanking(select.value));

  if (state.circleList.length) {
    loadCircleRanking(state.circleList[0].circleId);
  }
}

async function loadCircleRanking(circleId) {
  try {
    const data = await fetchRanking('circle', { circleId, limit: 20 });
    const items = data.items || [];
    document.getElementById('ranking-table').innerHTML = items.length
      ? renderRankingTableHtml(items)
      : '<div class="empty-state"><div class="empty-icon">📭</div>No topics in this circle</div>';
    bindRankingRowClicks();
  } catch (e) {
    document.getElementById('ranking-table').innerHTML = `<div class="empty-state"><div class="empty-icon">⚠</div>${esc(e.message)}</div>`;
  }
}

/* ---- Ranking Table Rendering ---- */
function renderRankingTable(items, containerId) {
  const el = document.getElementById(containerId);
  if (!items.length) {
    el.innerHTML = '<div class="empty-state"><div class="empty-icon">📭</div>No ranking data</div>';
    return;
  }
  el.innerHTML = renderRankingTableHtml(items);
  bindRankingRowClicks();
}

function renderRankingTableHtml(items) {
  const rows = items.map(item => `
    <tr data-topic-id="${item.topicId}" class="ranking-row">
      <td class="rank-col"><span class="rank-badge ${item.rank <= 3 ? 'rank-'+item.rank : 'rank-other'}">${item.rank}</span></td>
      <td><strong>${esc(item.title)}</strong></td>
      <td>${esc(item.circleName || '—')}</td>
      <td class="score-col">${(item.score || 0).toFixed(1)}</td>
      <td class="count-col">${(item.interactionCount || 0).toLocaleString()}</td>
    </tr>
  `).join('');
  return `
    <table class="data-table">
      <thead><tr><th class="rank-col">#</th><th>Title</th><th>Circle</th><th class="score-col">Score</th><th class="count-col">Interactions</th></tr></thead>
      <tbody>${rows}</tbody>
    </table>`;
}

function bindRankingRowClicks() {
  document.querySelectorAll('.ranking-row').forEach(row => {
    row.addEventListener('click', () => {
      const topicId = parseInt(row.dataset.topicId);
      openTopicModal(topicId);
    });
  });
}

/* ---- Topic Modal ---- */
function bindModal() {
  document.getElementById('modal-close').addEventListener('click', closeTopicModal);
  document.getElementById('modal-overlay').addEventListener('click', (e) => {
    if (e.target === document.getElementById('modal-overlay')) closeTopicModal();
  });
}

async function openTopicModal(topicId) {
  const overlay = document.getElementById('modal-overlay');
  const body = document.getElementById('modal-body');
  overlay.classList.add('active');
  body.innerHTML = '<div class="loading"><div class="spinner"></div> Loading topic...</div>';
  document.getElementById('modal-footer').innerHTML = '';

  try {
    const topic = await fetchTopicDetail(topicId);
    const statusLabels = { 0: 'Blocked', 1: 'Normal', 2: 'Under Review' };
    const statusClasses = { 0: 'status-blocked', 1: 'status-normal', 2: 'status-review' };
    const statusLabel = statusLabels[topic.status] || 'Unknown';
    const statusClass = statusClasses[topic.status] || '';

    body.innerHTML = `
      <div class="field"><div class="field-label">Title</div><div class="field-value" style="font-size:18px;font-weight:600">${esc(topic.title)}</div></div>
      <div class="field"><div class="field-label">Status</div><div class="field-value"><span class="status-badge ${statusClass}">${statusLabel}</span></div></div>
      <div class="field"><div class="field-label">Content</div><div class="field-value" style="color:var(--text-secondary)">${esc(topic.content || '(no content)')}</div></div>
      <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:16px;margin-top:16px">
        <div class="field"><div class="field-label">Circle ID</div><div class="field-value">${topic.circleId}</div></div>
        <div class="field"><div class="field-label">Author ID</div><div class="field-value">${topic.authorId}</div></div>
        <div class="field"><div class="field-label">Publish Time</div><div class="field-value">${(topic.publishTime || '').slice(0,19)}</div></div>
        <div class="field"><div class="field-label">Current Score</div><div class="field-value" style="font-weight:600;color:var(--green)">${(topic.currentScore || 0).toFixed(4)}</div></div>
        <div class="field"><div class="field-label">Interactions</div><div class="field-value">${(topic.interactionCount || 0).toLocaleString()}</div></div>
        <div class="field"><div class="field-label">Topic ID</div><div class="field-value">${topic.id}</div></div>
      </div>
    `;

    const footer = document.getElementById('modal-footer');
    const btn = topic.status === 0
      ? `<button class="btn btn-primary" onclick="unblockTopicAction(${topic.id})">✓ Unblock Topic</button>`
      : `<button class="btn btn-danger" onclick="blockTopicAction(${topic.id})">🚫 Block Topic</button>`;
    footer.innerHTML = btn + `<button class="btn" onclick="closeTopicModal()">Close</button>`;

    // Store current topic for action callbacks
    window._modalTopic = topic;
  } catch (e) {
    body.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠</div>${esc(e.message)}</div>`;
  }
}

function closeTopicModal() {
  document.getElementById('modal-overlay').classList.remove('active');
}

async function blockTopicAction(topicId) {
  try {
    await blockTopic(topicId);
    showToast('Topic blocked successfully', 'success');
    closeTopicModal();
    resetRefreshTimer();
    renderTab(state.activeTab);
  } catch (e) {
    showToast(`Block failed: ${e.message}`, 'error');
  }
}

async function unblockTopicAction(topicId) {
  try {
    await unblockTopic(topicId);
    showToast('Topic unblocked successfully', 'success');
    closeTopicModal();
    resetRefreshTimer();
    renderTab(state.activeTab);
  } catch (e) {
    showToast(`Unblock failed: ${e.message}`, 'error');
  }
}

/* ---- Interaction Simulator ---- */
function bindInteractionForm() {
  document.getElementById('interaction-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('interaction-submit');
    const resultEl = document.getElementById('interaction-result');
    btn.disabled = true;
    btn.textContent = 'Submitting...';
    resultEl.innerHTML = '';

    const body = {
      topicId: parseInt(document.getElementById('sim-topicId').value),
      userId: parseInt(document.getElementById('sim-userId').value),
      interactionType: parseInt(document.getElementById('sim-type').value),
      deviceFingerprint: document.getElementById('sim-device').value || undefined,
      ipAddress: document.getElementById('sim-ip').value || undefined
    };

    try {
      const result = await submitInteraction(body);
      resultEl.innerHTML = `<div style="color:var(--green);margin-top:8px">✓ Interaction recorded! ID=${esc(result.id)}, weight=${esc(result.weightMultiplier)}</div>`;
      showToast('Interaction recorded', 'success');
      resetRefreshTimer();
      renderTab(state.activeTab);
    } catch (err) {
      resultEl.innerHTML = `<div style="color:var(--red);margin-top:8px">✗ ${esc(err.message)}</div>`;
      showToast(err.message, 'error');
    } finally {
      btn.disabled = false;
      btn.textContent = 'Submit Interaction';
    }
  });
}

/* ---- SSE ---- */
function startSSE() {
  if (state.sseCleanup) state.sseCleanup();

  state.sseCleanup = connectSSE({
    onConnected: (data) => {
      state.sseConnected = true;
      updateSSEIndicator(true);
      if (data.subscriberCount !== undefined) {
        document.getElementById('sse-text').textContent = `SSE: ● connected (${data.subscriberCount})`;
      }
    },
    onRankingUpdated: (data) => {
      showToast(`Ranking updated: ${data.updatedTopicCount} topics recalculated`, 'info');
      resetRefreshTimer();
      renderTab(state.activeTab);
    },
    onTopNEntered: (data) => {
      showToast(`🏆 Top ${data.threshold} entered: "${data.title}" (${data.score})`, 'info');
    },
    onError: () => {
      state.sseConnected = false;
      updateSSEIndicator(false);
      document.getElementById('sse-text').textContent = 'SSE: ● disconnected';
      // Auto-reconnect after delay
      setTimeout(startSSE, 5000);
    }
  });
}

function updateSSEIndicator(connected) {
  const dot = document.getElementById('sse-dot');
  dot.className = 'sse-dot ' + (connected ? 'connected' : 'disconnected');
}

/* ---- Refresh Timer ---- */
function startRefreshTimer() {
  state.refreshTimer = setInterval(() => {
    if (state.refreshSeconds <= 1) {
      state.refreshSeconds = 30;
      renderTab(state.activeTab);
    } else {
      state.refreshSeconds--;
    }
    updateTimerDisplay();
  }, 1000);
}

function resetRefreshTimer() {
  state.refreshSeconds = 30;
  updateTimerDisplay();
}

function updateTimerDisplay() {
  document.getElementById('refresh-timer').textContent = `Refresh: ${state.refreshSeconds}s`;
}

/* ---- Charts ---- */
function destroyCharts() {
  Object.values(state.charts).forEach(c => c.destroy());
  state.charts = {};
}

/* ---- Toast ---- */
function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(() => { toast.remove(); }, 4000);
}

/* ---- Utilities ---- */
function esc(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

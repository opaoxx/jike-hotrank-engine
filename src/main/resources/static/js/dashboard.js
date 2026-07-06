/* ================================================================
   即刻热点榜单引擎 — Dashboard UI 控制器
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

/* ---- 初始化 ---- */
document.addEventListener('DOMContentLoaded', () => {
  bindTabs();
  bindModal();
  bindInteractionForm();
  startRefreshTimer();
  startSSE();
  renderTab('overview');
});

/* ---- Tab 切换 ---- */
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
  content.innerHTML = '<div class="loading"><div class="spinner"></div> 加载中…</div>';

  const renderers = {
    overview: renderOverview,
    global:   () => renderRankingTab('global',   '全站热榜', 50),
    circle:   renderCircleTab,
    newcomer: () => renderRankingTab('newcomer', '新星榜',   10),
    surging:  () => renderRankingTab('surging',  '飙升榜',   10)
  };

  (renderers[tab] || renderers.overview)();
}

/* ---- 总览 Tab ---- */
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
          <div class="stat-label">话题总数</div>
          <div class="stat-value">${hd.topicCount || 0}</div>
          <div class="stat-sub">最高热度：${(hd.maxScore || 0).toFixed(1)}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">互动次数（${intStats.hours || 24}h）</div>
          <div class="stat-value">${(intStats.total || 0).toLocaleString()}</div>
          <div class="stat-sub">${(intStats.periodStart || '').slice(0,16)} ~ ${(intStats.periodEnd || '').slice(11,16)}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">平均热度</div>
          <div class="stat-value">${(hd.avgScore || 0).toFixed(1)}</div>
          <div class="stat-sub">中位数：${(hd.medianScore || 0).toFixed(1)}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">缓存命中率</div>
          <div class="stat-value">${((cs.hitRate || 0) * 100).toFixed(1)}%</div>
          <div class="stat-sub">命中 ${cs.hits || 0} / 未命中 ${cs.misses || 0}</div>
        </div>
      </div>

      <div class="charts-grid">
        <div class="panel">
          <div class="panel-header">热度分布</div>
          <div class="panel-body">
            <div class="chart-container"><canvas id="chart-heat"></canvas></div>
          </div>
        </div>
        <div class="panel">
          <div class="panel-header">互动类型分布</div>
          <div class="panel-body">
            <div class="chart-container"><canvas id="chart-interaction"></canvas></div>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-header">圈子活跃度</div>
        <div class="panel-body no-padding" id="circle-activity-table"></div>
      </div>
    `;

    renderHeatChart(hd);
    renderInteractionChart(intStats);
    renderCircleActivityTable(ca);
  } catch (e) {
    content.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠</div>加载总览失败：${esc(e.message)}</div>`;
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
        label: '话题数',
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
        y: { beginAtZero: true, ticks: { color: '#94a3b8', font: { size: 18 } }, grid: { color: '#1e293b' } },
        x: { ticks: { color: '#94a3b8', font: { size: 18 } }, grid: { display: false } }
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
          labels: { color: '#94a3b8', padding: 16, usePointStyle: true, font: { size: 19 } }
        }
      }
    }
  });
}

function renderCircleActivityTable(ca) {
  const items = ca.items || [];
  if (!items.length) {
    document.getElementById('circle-activity-table').innerHTML = '<div class="empty-state"><div class="empty-icon">📭</div>暂无圈子数据</div>';
    return;
  }
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
      <thead><tr><th class="rank-col">#</th><th>圈子</th><th class="count-col">话题数</th><th class="score-col">平均热度</th><th class="count-col">互动数</th></tr></thead>
      <tbody>${rows}</tbody>
    </table>`;
}

/* ---- 榜单 Tab（全站 / 新星 / 飙升 共用） ---- */
async function renderRankingTab(type, title, defaultLimit) {
  const content = document.getElementById('main-content');
  try {
    const data = await fetchRanking(type, { limit: defaultLimit });
    const items = data.items || [];
    content.innerHTML = `
      <div class="panel">
        <div class="panel-header">${title} <span style="font-weight:400;font-size:19px;color:var(--text-muted)">${items.length} 个话题 · 更新于 ${(data.updateTime || '').slice(11,19)}</span></div>
        <div class="panel-body no-padding" id="ranking-table"></div>
      </div>
    `;
    renderRankingTable(items, 'ranking-table');
  } catch (e) {
    content.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠</div>加载榜单失败：${esc(e.message)}</div>`;
  }
}

/* ---- 圈子 Tab ---- */
async function renderCircleTab() {
  const content = document.getElementById('main-content');
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
        圈子热榜
        <select id="circle-select" style="width:240px;font-size:20px">${options}</select>
      </div>
      <div class="panel-body no-padding" id="ranking-table">
        <div class="loading"><div class="spinner"></div> 加载中…</div>
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
      : '<div class="empty-state"><div class="empty-icon">📭</div>该圈子暂无话题</div>';
    bindRankingRowClicks();
  } catch (e) {
    document.getElementById('ranking-table').innerHTML = `<div class="empty-state"><div class="empty-icon">⚠</div>${esc(e.message)}</div>`;
  }
}

/* ---- 榜单表格渲染 ---- */
function renderRankingTable(items, containerId) {
  const el = document.getElementById(containerId);
  if (!items.length) {
    el.innerHTML = '<div class="empty-state"><div class="empty-icon">📭</div>暂无排行数据</div>';
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
      <thead><tr><th class="rank-col">#</th><th>话题</th><th>圈子</th><th class="score-col">热度</th><th class="count-col">互动数</th></tr></thead>
      <tbody>${rows}</tbody>
    </table>`;
}

function bindRankingRowClicks() {
  document.querySelectorAll('.ranking-row').forEach(row => {
    row.addEventListener('click', () => {
      openTopicModal(parseInt(row.dataset.topicId));
    });
  });
}

/* ---- 话题详情弹窗 ---- */
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
  body.innerHTML = '<div class="loading"><div class="spinner"></div> 加载话题中…</div>';
  document.getElementById('modal-footer').innerHTML = '';

  try {
    const topic = await fetchTopicDetail(topicId);
    const statusLabels = { 0: '已屏蔽', 1: '正常', 2: '待审核' };
    const statusClasses = { 0: 'status-blocked', 1: 'status-normal', 2: 'status-review' };
    const statusLabel = statusLabels[topic.status] || '未知';
    const statusClass = statusClasses[topic.status] || '';

    body.innerHTML = `
      <div class="field"><div class="field-label">标题</div><div class="field-value" style="font-size:24px;font-weight:600">${esc(topic.title)}</div></div>
      <div class="field"><div class="field-label">状态</div><div class="field-value"><span class="status-badge ${statusClass}">${statusLabel}</span></div></div>
      <div class="field"><div class="field-label">内容</div><div class="field-value" style="color:var(--text-secondary)">${esc(topic.content || '（无内容）')}</div></div>
      <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:16px;margin-top:16px">
        <div class="field"><div class="field-label">圈子 <span translate="no">ID</span></div><div class="field-value">${topic.circleId}</div></div>
        <div class="field"><div class="field-label">作者 <span translate="no">ID</span></div><div class="field-value">${topic.authorId}</div></div>
        <div class="field"><div class="field-label">发布时间</div><div class="field-value">${(topic.publishTime || '').slice(0,19)}</div></div>
        <div class="field"><div class="field-label">当前热度</div><div class="field-value" style="font-weight:600;color:var(--green)">${(topic.currentScore || 0).toFixed(4)}</div></div>
        <div class="field"><div class="field-label">互动数</div><div class="field-value">${(topic.interactionCount || 0).toLocaleString()}</div></div>
        <div class="field"><div class="field-label">话题 <span translate="no">ID</span></div><div class="field-value">${topic.id}</div></div>
      </div>
    `;

    const footer = document.getElementById('modal-footer');
    const btn = topic.status === 0
      ? `<button class="btn btn-primary" onclick="unblockTopicAction(${topic.id})">✓ 取消屏蔽</button>`
      : `<button class="btn btn-danger" onclick="blockTopicAction(${topic.id})">🚫 屏蔽话题</button>`;
    footer.innerHTML = btn + `<button class="btn" onclick="closeTopicModal()">关闭</button>`;

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
    showToast('话题已屏蔽', 'success');
    closeTopicModal();
    resetRefreshTimer();
    renderTab(state.activeTab);
  } catch (e) {
    showToast(`屏蔽失败：${e.message}`, 'error');
  }
}

async function unblockTopicAction(topicId) {
  try {
    await unblockTopic(topicId);
    showToast('话题已取消屏蔽', 'success');
    closeTopicModal();
    resetRefreshTimer();
    renderTab(state.activeTab);
  } catch (e) {
    showToast(`取消屏蔽失败：${e.message}`, 'error');
  }
}

/* ---- 互动模拟器 ---- */
function bindInteractionForm() {
  document.getElementById('interaction-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('interaction-submit');
    const resultEl = document.getElementById('interaction-result');
    btn.disabled = true;
    btn.textContent = '提交中…';
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
      resultEl.innerHTML = `<div style="color:var(--green);margin-top:8px">✓ 互动已记录！<span translate="no">ID</span>=${esc(result.id)}，权重=${esc(result.weightMultiplier)}</div>`;
      showToast('互动已记录', 'success');
      resetRefreshTimer();
      renderTab(state.activeTab);
    } catch (err) {
      resultEl.innerHTML = `<div style="color:var(--red);margin-top:8px">✗ ${esc(err.message)}</div>`;
      showToast(err.message, 'error');
    } finally {
      btn.disabled = false;
      btn.textContent = '提交互动';
    }
  });
}

/* ---- SSE 实时推送 ---- */
function startSSE() {
  if (state.sseCleanup) state.sseCleanup();

  state.sseCleanup = connectSSE({
    onConnected: (data) => {
      state.sseConnected = true;
      updateSSEIndicator(true);
      const sub = data.subscriberCount !== undefined ? `（${data.subscriberCount} 个订阅者）` : '';
      document.getElementById('sse-text').textContent = `实时推送：● 已连接 ${sub}`;
    },
    onRankingUpdated: (data) => {
      showToast(`榜单已更新：${data.updatedTopicCount} 个话题已重新计算`, 'info');
      resetRefreshTimer();
      renderTab(state.activeTab);
    },
    onTopNEntered: (data) => {
      showToast(`🏆 新晋前 ${data.threshold} 名：「${data.title}」(${data.score})`, 'info');
    },
    onError: () => {
      state.sseConnected = false;
      updateSSEIndicator(false);
      document.getElementById('sse-text').textContent = '实时推送：● 已断开';
      setTimeout(startSSE, 5000);
    }
  });
}

function updateSSEIndicator(connected) {
  const dot = document.getElementById('sse-dot');
  dot.className = 'sse-dot ' + (connected ? 'connected' : 'disconnected');
}

/* ---- 自动刷新 ---- */
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
  document.getElementById('refresh-timer').textContent = `刷新：${state.refreshSeconds}s`;
}

/* ---- 图表 ---- */
function destroyCharts() {
  Object.values(state.charts).forEach(c => c.destroy());
  state.charts = {};
}

/* ---- Toast 通知 ---- */
function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(() => { toast.remove(); }, 4000);
}

/* ---- 工具函数 ---- */
function esc(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

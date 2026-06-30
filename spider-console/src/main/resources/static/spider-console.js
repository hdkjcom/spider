const i18n = {
  zh: {
    nav: {
      overview: '概览',
      clients: '客户端',
      reports: '最近上报',
      breakers: '熔断器',
      services: '服务'
    },
    section: { monitor: '监控', resources: '资源' },
    label: { refresh: '刷新' },
    action: { refresh: '刷新' },
    filter: { allServices: '全部服务', search: '搜索客户端、服务或方法' },
    title: {
      overview: ['概览', '调用、错误、延迟和治理状态'],
      clients: ['客户端', '按服务、客户端和方法查看调用质量'],
      reports: ['最近上报', '服务上报的最新指标快照'],
      breakers: ['熔断器', '熔断器实例与当前状态'],
      services: ['服务', '已向控制台上报的服务']
    },
    panel: {
      clientHealth: '客户端健康度',
      governance: '治理状态',
      governanceNote: '熔断、追踪与上报状态',
      recentReports: '最近上报',
      recentReportsNote: '最新 8 条指标快照',
      clientDetails: '客户端明细',
      breakers: '熔断器状态',
      services: '已上报服务'
    },
    health: { connecting: '连接中', refreshing: '刷新中', running: '运行中', disconnected: '连接异常' },
    metric: {
      calls: '总调用',
      successRate: '成功率',
      failures: '失败',
      retries: '重试',
      fallbacks: '降级',
      avgLatency: '平均延迟',
      clientMethods: '个客户端方法',
      successful: '次成功',
      needsAttention: '需要关注',
      noFailures: '无失败上报',
      totalRetries: '累计重试次数',
      fallbackInvocations: 'Fallback 触发次数',
      services: '个服务',
      breakers: '个熔断器'
    },
    table: {
      client: '客户端',
      method: '方法',
      calls: '调用',
      success: '成功',
      failure: '失败',
      retry: '重试',
      fallback: '降级',
      avgLatency: '平均延迟',
      p99: 'P99',
      successRate: '成功率',
      time: '时间',
      service: '服务',
      clientMethods: '客户端方法',
      failures: '失败'
    },
    empty: {
      noReportTitle: '暂无调用数据',
      noReportText: '发起一次 Spider 调用后即可看到调用、延迟和错误信息。',
      noRecentTitle: '暂无最近上报',
      noRecentText: '客户端上报指标快照后会显示在这里。',
      noBreakerTitle: '暂无熔断器数据',
      noBreakerText: '客户端上报熔断状态后会显示在这里。',
      noServicesTitle: '暂无服务',
      noServicesText: '收到服务指标上报后会自动出现。',
      connectionTitle: '控制台连接异常',
      unableRead: '无法读取控制台数据'
    },
    note: {
      clientRisk: '条客户端方法记录，按风险优先排序',
      clientRecords: '条客户端方法记录',
      noClientRecords: '暂无客户端记录',
      latestReports: '最近 {n} 条上报',
      noReports: '暂无上报',
      breakers: '个熔断器',
      noBreakers: '暂无熔断器',
      services: '个服务',
      noServices: '暂无服务'
    },
    state: {
      circuitBreakers: '熔断器',
      failures: '错误累计',
      snapshots: '数据快照',
      allClosed: '全部关闭或未激活',
      open: '{n} 个 OPEN',
      halfOpen: '{n} 个 HALF_OPEN',
      failuresReported: '存在失败调用',
      noFailures: '当前无失败',
      waitingReports: '等待上报',
      requestsBlocked: '请求被熔断保护',
      recoveryProbe: '正在探测恢复',
      requestsAllowed: '允许请求通过'
    },
    langToggle: 'English'
  },
  en: {
    nav: {
      overview: 'Overview',
      clients: 'Clients',
      reports: 'Reports',
      breakers: 'Breakers',
      services: 'Services'
    },
    section: { monitor: 'Monitor', resources: 'Resources' },
    label: { refresh: 'Refresh' },
    action: { refresh: 'Refresh' },
    filter: { allServices: 'All services', search: 'Search client, service, or method' },
    title: {
      overview: ['Overview', 'Calls, errors, latency, and governance status'],
      clients: ['Clients', 'Call quality by service, client, and method'],
      reports: ['Recent Reports', 'Latest metric snapshots reported by services'],
      breakers: ['Circuit Breakers', 'Breaker instances and current state'],
      services: ['Services', 'Services that have reported to the console']
    },
    panel: {
      clientHealth: 'Client Health',
      governance: 'Governance Status',
      governanceNote: 'Circuit breakers, tracing, and reports',
      recentReports: 'Recent Reports',
      recentReportsNote: 'Latest 8 metric snapshots',
      clientDetails: 'Client Details',
      breakers: 'Circuit Breakers',
      services: 'Reported Services'
    },
    health: { connecting: 'Connecting', refreshing: 'Refreshing', running: 'Running', disconnected: 'Disconnected' },
    metric: {
      calls: 'Calls',
      successRate: 'Success Rate',
      failures: 'Failures',
      retries: 'Retries',
      fallbacks: 'Fallbacks',
      avgLatency: 'Avg Latency',
      clientMethods: 'client methods',
      successful: 'successful',
      needsAttention: 'Needs attention',
      noFailures: 'No failures',
      totalRetries: 'Total retry attempts',
      fallbackInvocations: 'Fallback invocations',
      services: 'services',
      breakers: 'breakers'
    },
    table: {
      client: 'Client',
      method: 'Method',
      calls: 'Calls',
      success: 'Success',
      failure: 'Failure',
      retry: 'Retry',
      fallback: 'Fallback',
      avgLatency: 'Avg Latency',
      p99: 'P99',
      successRate: 'Success Rate',
      time: 'Time',
      service: 'Service',
      clientMethods: 'Client Methods',
      failures: 'Failures'
    },
    empty: {
      noReportTitle: 'No call data yet',
      noReportText: 'Make a Spider call and the call, latency, and error data will appear here.',
      noRecentTitle: 'No recent reports',
      noRecentText: 'Metric snapshots will appear after SpiderReporter posts data.',
      noBreakerTitle: 'No breaker data',
      noBreakerText: 'Breaker states will appear after clients report them.',
      noServicesTitle: 'No services',
      noServicesText: 'Services appear after they report metrics.',
      connectionTitle: 'Console connection failed',
      unableRead: 'Unable to read console data'
    },
    note: {
      clientRisk: 'client method records, risk-first order',
      clientRecords: 'client method records',
      noClientRecords: 'No client records',
      latestReports: 'Latest {n} reports',
      noReports: 'No reports',
      breakers: 'breakers',
      noBreakers: 'No breakers',
      services: 'services',
      noServices: 'No services'
    },
    state: {
      circuitBreakers: 'Circuit Breakers',
      failures: 'Failures',
      snapshots: 'Snapshots',
      allClosed: 'All closed or inactive',
      open: '{n} OPEN',
      halfOpen: '{n} HALF_OPEN',
      failuresReported: 'Failures reported',
      noFailures: 'No failures',
      waitingReports: 'Waiting for reports',
      requestsBlocked: 'Requests are blocked',
      recoveryProbe: 'Recovery probe in progress',
      requestsAllowed: 'Requests are allowed'
    },
    langToggle: '中文'
  }
};

const state = {
  data: { clients: {}, services: [], circuitBreakers: {}, snapshotCount: 0, recentReports: [] },
  view: 'overview',
  service: '',
  query: '',
  loading: false,
  lang: localStorage.getItem('spider.console.lang') || 'zh'
};

document.getElementById('refreshBtn').addEventListener('click', load);
document.getElementById('langToggle').addEventListener('click', () => {
  state.lang = state.lang === 'zh' ? 'en' : 'zh';
  localStorage.setItem('spider.console.lang', state.lang);
  applyLanguage();
  render();
});
document.getElementById('searchBox').addEventListener('input', e => {
  state.query = e.target.value.trim().toLowerCase();
  render();
});
document.getElementById('serviceFilter').addEventListener('change', e => {
  state.service = e.target.value;
  render();
});
document.getElementById('mobileView').addEventListener('change', e => setView(e.target.value));
document.querySelectorAll('[data-view]').forEach(el => {
  el.addEventListener('click', () => setView(el.dataset.view));
});

function t(path, params) {
  const value = path.split('.').reduce((obj, key) => obj && obj[key], i18n[state.lang]) || path;
  if (!params) return value;
  return Object.keys(params).reduce((text, key) => text.replace(`{${key}}`, params[key]), value);
}

function applyLanguage() {
  document.documentElement.lang = state.lang === 'zh' ? 'zh-CN' : 'en';
  document.querySelectorAll('[data-i18n]').forEach(el => {
    el.textContent = t(el.dataset.i18n);
  });
  document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
    el.setAttribute('placeholder', t(el.dataset.i18nPlaceholder));
  });
  document.getElementById('langToggle').textContent = t('langToggle');
  setView(state.view);
}

async function load() {
  if (state.loading) return;
  state.loading = true;
  setHealth('warn', t('health.refreshing'));
  try {
    const response = await fetch('/spider/api/dashboard', { cache: 'no-store' });
    if (!response.ok) throw new Error('HTTP ' + response.status);
    state.data = await response.json();
    state.data.recentReports = state.data.recentReports || [];
    setHealth('ok', t('health.running'));
    document.getElementById('lastRefresh').textContent = new Date().toLocaleTimeString();
    render();
  } catch (err) {
    setHealth('error', t('health.disconnected'));
    renderEmptyShell(err.message || t('empty.unableRead'));
  } finally {
    state.loading = false;
  }
}

function setHealth(kind, text) {
  const dot = document.getElementById('healthDot');
  dot.className = 'dot' + (kind === 'warn' ? ' warn' : kind === 'error' ? ' error' : '');
  document.getElementById('healthText').textContent = text;
}

function setView(view) {
  state.view = view;
  document.querySelectorAll('.nav-button').forEach(btn => btn.classList.toggle('active', btn.dataset.view === view));
  document.querySelectorAll('.tab').forEach(btn => btn.classList.toggle('active', btn.dataset.view === view));
  document.getElementById('mobileView').value = view;
  Object.keys(i18n[state.lang].title).forEach(key => {
    const el = document.getElementById('view-' + key);
    if (el) el.hidden = key !== view;
  });
  document.getElementById('pageTitle').textContent = t(`title.${view}`)[0];
  document.getElementById('pageSubtitle').textContent = t(`title.${view}`)[1];
}

function render() {
  const clients = filteredClients();
  const allClients = Object.values(state.data.clients || {});
  const services = state.data.services || [];
  const breakers = state.data.circuitBreakers || {};
  renderServiceFilter(services);
  renderNav(allClients, services, breakers);
  renderSummary(allClients, services, breakers);
  renderClientTables(clients);
  renderGovernance(allClients, breakers);
  renderReports();
  renderBreakers(breakers);
  renderServices(services, allClients);
  setView(state.view);
}

function filteredClients() {
  return Object.values(state.data.clients || {}).filter(client => {
    const service = client.service || '';
    const name = client.client || '';
    const method = client.method || '';
    const matchesService = !state.service || service === state.service;
    const haystack = (service + ' ' + name + ' ' + method).toLowerCase();
    const matchesQuery = !state.query || haystack.includes(state.query);
    return matchesService && matchesQuery;
  }).sort((a, b) => (b.failure || 0) - (a.failure || 0) || (b.calls || 0) - (a.calls || 0));
}

function renderServiceFilter(services) {
  const filter = document.getElementById('serviceFilter');
  const current = filter.value;
  filter.innerHTML = `<option value="">${t('filter.allServices')}</option>` + services.map(s => `<option value="${esc(s)}">${esc(s)}</option>`).join('');
  filter.value = services.includes(current) ? current : '';
  state.service = filter.value;
}

function renderNav(clients, services, breakers) {
  document.getElementById('navTotal').textContent = compact(sum(clients, 'calls'));
  document.getElementById('navClients').textContent = clients.length;
  document.getElementById('navReports').textContent = (state.data.recentReports || []).length;
  document.getElementById('navBreakers').textContent = Object.keys(breakers).length;
  document.getElementById('navServices').textContent = services.length;
}

function renderSummary(clients, services, breakers) {
  const calls = sum(clients, 'calls');
  const success = sum(clients, 'success');
  const failure = sum(clients, 'failure');
  const retries = sum(clients, 'retries');
  const fallbacks = sum(clients, 'fallbacks');
  const avg = calls ? (sum(clients, 'totalLatencyMs') / calls).toFixed(1) : '0.0';
  const rate = calls ? (success * 100 / calls) : null;
  document.getElementById('summary').innerHTML = [
    metric(t('metric.calls'), compact(calls), `${clients.length} ${t('metric.clientMethods')}`),
    metric(t('metric.successRate'), rate === null ? 'N/A' : rate.toFixed(1) + '%', `${compact(success)} ${t('metric.successful')}`, rate !== null && rate < 95 ? 'bad' : 'good'),
    metric(t('metric.failures'), compact(failure), failure ? t('metric.needsAttention') : t('metric.noFailures'), failure ? 'bad' : 'good'),
    metric(t('metric.retries'), compact(retries), t('metric.totalRetries'), retries ? 'orange' : ''),
    metric(t('metric.fallbacks'), compact(fallbacks), t('metric.fallbackInvocations'), fallbacks ? 'orange' : ''),
    metric(t('metric.avgLatency'), avg + ' ms', `${services.length} ${t('metric.services')} / ${Object.keys(breakers).length} ${t('metric.breakers')}`, avg > 500 ? 'orange' : '')
  ].join('');
}

function metric(label, value, foot, color) {
  return `<div class="metric"><div class="metric-label">${label}</div><div class="metric-value ${color || ''}">${value}</div><div class="metric-foot">${foot}</div></div>`;
}

function renderClientTables(clients) {
  const html = clientRows(clients);
  document.getElementById('clientNote').textContent = clients.length ? `${clients.length} ${t('note.clientRisk')}` : t('note.noClientRecords');
  document.getElementById('clientDetailNote').textContent = clients.length ? `${clients.length} ${t('note.clientRecords')}` : t('note.noClientRecords');
  document.getElementById('overviewTable').innerHTML = html;
  document.getElementById('clientTable').innerHTML = html;
}

function clientRows(clients) {
  if (!clients.length) {
    return `<div class="empty"><div><strong>${t('empty.noReportTitle')}</strong><span>${t('empty.noReportText')}</span></div></div>`;
  }
  return `<table>
    <thead><tr>
      <th>${t('table.client')}</th><th>${t('table.method')}</th><th class="num">${t('table.calls')}</th><th class="num">${t('table.success')}</th><th class="num">${t('table.failure')}</th>
      <th class="num">${t('table.retry')}</th><th class="num">${t('table.fallback')}</th><th class="num">${t('table.avgLatency')}</th><th class="num">${t('table.p99')}</th><th class="num">${t('table.successRate')}</th>
    </tr></thead>
    <tbody>${clients.map(client => {
      const rate = parseRate(client.successRate, client.calls, client.success);
      return `<tr>
        <td><div class="primary-cell"><strong>${esc(client.client || '-')}</strong><span>${esc(client.service || '-')}</span></div></td>
        <td><span class="badge">${esc(client.method || '*')}</span></td>
        <td class="num">${fmt(client.calls)}</td>
        <td class="num good">${fmt(client.success)}</td>
        <td class="num ${client.failure ? 'bad' : ''}">${fmt(client.failure)}</td>
        <td class="num ${client.retries ? 'orange' : ''}">${fmt(client.retries)}</td>
        <td class="num ${client.fallbacks ? 'orange' : ''}">${fmt(client.fallbacks)}</td>
        <td class="num">${client.avgLatencyMs || '0'} ms</td>
        <td class="num">${fmt(client.p99)} ms</td>
        <td class="num">${rateBar(rate)}</td>
      </tr>`;
    }).join('')}</tbody>
  </table>`;
}

function renderGovernance(clients, breakers) {
  const open = Object.values(breakers).filter(v => v === 'OPEN').length;
  const half = Object.values(breakers).filter(v => v === 'HALF_OPEN').length;
  const errors = sum(clients, 'failure');
  document.getElementById('governancePanel').innerHTML = [
    stateItem(t('state.circuitBreakers'), `${Object.keys(breakers).length}`, open ? 'bad' : half ? 'orange' : 'good', open ? t('state.open', { n: open }) : half ? t('state.halfOpen', { n: half }) : t('state.allClosed')),
    stateItem(t('state.failures'), compact(errors), errors ? 'bad' : 'good', errors ? t('state.failuresReported') : t('state.noFailures')),
    stateItem(t('state.snapshots'), compact(state.data.snapshotCount || 0), '', state.data.time ? new Date(state.data.time).toLocaleString() : t('state.waitingReports'))
  ].join('');
}

function stateItem(title, value, color, note) {
  return `<div class="state-item"><strong>${title}</strong><span class="badge ${color || ''}"><span class="dot ${color === 'bad' ? 'error' : color === 'orange' ? 'warn' : ''}"></span>${value}</span><div class="metric-foot">${note}</div></div>`;
}

function renderReports() {
  const reports = state.data.recentReports || [];
  document.getElementById('reportNote').textContent = reports.length ? t('note.latestReports', { n: reports.length }) : t('note.noReports');
  document.getElementById('recentOverviewTable').innerHTML = reportRows(reports.slice(0, 8));
  document.getElementById('reportTable').innerHTML = reportRows(reports);
}

function reportRows(reports) {
  if (!reports.length) {
    return `<div class="empty"><div><strong>${t('empty.noRecentTitle')}</strong><span>${t('empty.noRecentText')}</span></div></div>`;
  }
  return `<table>
    <thead><tr>
      <th>${t('table.time')}</th><th>${t('table.service')}</th><th>${t('table.client')}</th><th>${t('table.method')}</th><th class="num">${t('table.calls')}</th>
      <th class="num">${t('table.failure')}</th><th class="num">${t('table.retry')}</th><th class="num">${t('table.fallback')}</th><th class="num">${t('table.p99')}</th><th class="num">${t('table.successRate')}</th>
    </tr></thead>
    <tbody>${reports.map(report => {
      const rate = parseRate(report.successRate, report.calls, report.success);
      return `<tr>
        <td>${formatTime(report.reportTime)}</td>
        <td>${esc(report.service || '-')}</td>
        <td><strong>${esc(report.client || '-')}</strong></td>
        <td><span class="badge">${esc(report.method || '*')}</span></td>
        <td class="num">${fmt(report.calls)}</td>
        <td class="num ${report.failure ? 'bad' : ''}">${fmt(report.failure)}</td>
        <td class="num ${report.retries ? 'orange' : ''}">${fmt(report.retries)}</td>
        <td class="num ${report.fallbacks ? 'orange' : ''}">${fmt(report.fallbacks)}</td>
        <td class="num">${fmt(report.p99)} ms</td>
        <td class="num">${rateBar(rate)}</td>
      </tr>`;
    }).join('')}</tbody>
  </table>`;
}

function renderBreakers(breakers) {
  const entries = Object.entries(breakers);
  document.getElementById('breakerNote').textContent = entries.length ? `${entries.length} ${t('note.breakers')}` : t('note.noBreakers');
  if (!entries.length) {
    document.getElementById('breakerPanel').innerHTML = `<div class="empty"><div><strong>${t('empty.noBreakerTitle')}</strong><span>${t('empty.noBreakerText')}</span></div></div>`;
    return;
  }
  document.getElementById('breakerPanel').innerHTML = `<div class="state-grid">${entries.map(([name, status]) => {
    const color = status === 'OPEN' ? 'bad' : status === 'HALF_OPEN' ? 'orange' : 'good';
    return stateItem(esc(name), esc(status), color, status === 'OPEN' ? t('state.requestsBlocked') : status === 'HALF_OPEN' ? t('state.recoveryProbe') : t('state.requestsAllowed'));
  }).join('')}</div>`;
}

function renderServices(services, clients) {
  document.getElementById('serviceNote').textContent = services.length ? `${services.length} ${t('note.services')}` : t('note.noServices');
  if (!services.length) {
    document.getElementById('serviceTable').innerHTML = `<div class="empty"><div><strong>${t('empty.noServicesTitle')}</strong><span>${t('empty.noServicesText')}</span></div></div>`;
    return;
  }
  const rows = services.map(service => {
    const scoped = clients.filter(c => c.service === service);
    return `<tr><td><strong>${esc(service)}</strong></td><td class="num">${scoped.length}</td><td class="num">${fmt(sum(scoped, 'calls'))}</td><td class="num ${sum(scoped, 'failure') ? 'bad' : ''}">${fmt(sum(scoped, 'failure'))}</td></tr>`;
  }).join('');
  document.getElementById('serviceTable').innerHTML = `<table><thead><tr><th>${t('table.service')}</th><th class="num">${t('table.clientMethods')}</th><th class="num">${t('table.calls')}</th><th class="num">${t('table.failures')}</th></tr></thead><tbody>${rows}</tbody></table>`;
}

function renderEmptyShell(message) {
  document.getElementById('overviewTable').innerHTML = `<div class="empty"><div><strong>${t('empty.connectionTitle')}</strong><span>${esc(message)}</span></div></div>`;
}

function rateBar(rate) {
  const value = Number.isFinite(rate) ? Math.max(0, Math.min(100, rate)) : 0;
  const cls = value >= 99 ? '' : value >= 95 ? 'warn' : 'error';
  const label = Number.isFinite(rate) ? value.toFixed(1) + '%' : 'N/A';
  return `<div class="bar"><div class="track"><div class="fill ${cls}" style="width:${value}%"></div></div><span class="rate-badge ${value >= 99 ? 'good' : value >= 95 ? 'orange' : 'bad'}">${label}</span></div>`;
}

function parseRate(raw, calls, success) {
  const parsed = parseFloat(raw);
  if (Number.isFinite(parsed)) return parsed;
  return calls ? success * 100 / calls : NaN;
}

function sum(items, key) { return items.reduce((total, item) => total + Number(item[key] || 0), 0); }
function fmt(value) { return Number(value || 0).toLocaleString(); }
function compact(value) {
  const n = Number(value || 0);
  if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M';
  if (n >= 1000) return (n / 1000).toFixed(1) + 'K';
  return String(n);
}
function formatTime(value) {
  if (!value) return '-';
  const time = new Date(value);
  return Number.isNaN(time.getTime()) ? '-' : time.toLocaleTimeString();
}
function esc(value) {
  return String(value == null ? '' : value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

applyLanguage();
load();
setInterval(load, 5000);

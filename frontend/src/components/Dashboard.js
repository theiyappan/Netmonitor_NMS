import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { 
    LayoutDashboard, List, Settings, 
    RefreshCw, FileText, Bell, Grid, List as ListIcon, 
    Clock, Server, Wifi, WifiOff, Activity, Power, 
    Check, X, Edit2, Filter, ArrowDownUp, Search, 
    PlayCircle, StopCircle, Network, Loader // <--- Added Loader Icon
} from 'lucide-react';
import InterfaceGraph from './InterfaceGraph';
import NetworkMap from './NetworkMap';
import './Dashboard.css';

// Ensure this matches your backend port
const API_BASE = 'http://localhost:8080/api';

function Dashboard({ data, onInterfaceClick, onRefresh, selectedTimeRange, onTimeRangeChange }) {
  const [activeTab, setActiveTab] = useState('overview');
  const [syncing, setSyncing] = useState(false);
  const [viewMode, setViewMode] = useState('grid');
  
  // --- Logs & Settings ---
  const [logs, setLogs] = useState([]);
  const [loadingLogs, setLoadingLogs] = useState(false);
  const [settings, setSettings] = useState({ thresholdUtil: 90, thresholdError: 0.5 });
  const [savingSettings, setSavingSettings] = useState(false);

  // --- Filters ---
  const [logFilter, setLogFilter] = useState('ALL'); 
  const [statusFilter, setStatusFilter] = useState('ALL'); 
  const [sortBy, setSortBy] = useState('INDEX');           

  // --- IP Editing ---
  const [isEditingIp, setIsEditingIp] = useState(false);
  const [newIp, setNewIp] = useState(data ? data.ipAddress : '');
  const [savingIp, setSavingIp] = useState(false);

  // --- Discovery ---
  const [showDiscovery, setShowDiscovery] = useState(false);
  const [subnet, setSubnet] = useState('192.168.1.0/24');
  const [isScanning, setIsScanning] = useState(false);
  const [scanResults, setScanResults] = useState(null);

  // --- Live Mode & Graph ---
  const [isLive, setIsLive] = useState(false);
  const [selectedInterface, setSelectedInterface] = useState(null);

  // --- Topology Demo Data ---
  const [demoDevices, setDemoDevices] = useState([]);

  // ==================================================================================
  // 1. HELPER CALCULATIONS
  // ==================================================================================
  const calculateCounts = () => {
    if (!data || !data.interfaces) return { up: 0, down: 0 };
    let up = 0, down = 0;
    data.interfaces.forEach(iface => iface.status.toLowerCase() === 'up' ? up++ : down++);
    return { up, down };
  };
  const counts = calculateCounts();

  const calculateLogCounts = () => {
      let c = { ERROR: 0, ALERT: 0, STARTUP: 0, INFO: 0 };
      logs.forEach(log => {
          const lvl = log.level ? log.level.toUpperCase() : 'INFO';
          if (c[lvl] !== undefined) c[lvl]++;
      });
      return c;
  };
  const logCounts = calculateLogCounts();

  // ==================================================================================
  // 2. DATA PROCESSING (Filtering & Sorting)
  // ==================================================================================
  const getProcessedInterfaces = () => {
    if (!data || !data.interfaces) return [];
    let processed = [...data.interfaces];

    if (statusFilter === 'UP') processed = processed.filter(i => i.status.toLowerCase() === 'up');
    else if (statusFilter === 'DOWN') processed = processed.filter(i => i.status.toLowerCase() !== 'up');

    processed.sort((a, b) => {
        if(sortBy === 'RX') return parseFloat(b.rxRate.replace(/[^\d.]/g, '')) - parseFloat(a.rxRate.replace(/[^\d.]/g, ''));
        if(sortBy === 'TX') return parseFloat(b.txRate.replace(/[^\d.]/g, '')) - parseFloat(a.txRate.replace(/[^\d.]/g, ''));
        if(sortBy === 'NAME') return a.interfaceName.localeCompare(b.interfaceName);
        return a.interfaceIndex - b.interfaceIndex;
    });
    return processed;
  };

  const getFilteredLogs = () => {
      if (logFilter === 'ALL') return logs;
      return logs.filter(log => log.level && log.level.toUpperCase() === logFilter);
  };

  const getLogBadgeClass = (level) => {
      if (!level) return 'log-badge';
      switch(level.toUpperCase()) {
          case 'ERROR': return 'log-badge error';
          case 'ALERT': return 'log-badge alert';
          case 'STARTUP': return 'log-badge startup';
          case 'INFO': return 'log-badge info';
          default: return 'log-badge';
      }
  };

  // ==================================================================================
  // 3. EFFECTS (Live Mode & Data Fetching)
  // ==================================================================================
  useEffect(() => {
    let intervalId;
    if (isLive && data && !selectedInterface) {
        intervalId = setInterval(async () => {
            try {
                await axios.post(`${API_BASE}/dashboard/${data.systemName}/scan`);
                setTimeout(() => onRefresh(), 2000);
            } catch (err) { setIsLive(false); }
        }, 5000); 
    }
    return () => clearInterval(intervalId);
  }, [isLive, data, onRefresh, selectedInterface]);

  useEffect(() => {
    if (!data) return;
    if (activeTab === 'events') {
        const hours = selectedTimeRange > 0 ? selectedTimeRange : 24;
        fetchLogs('all', hours);
    }
  }, [activeTab, data, selectedTimeRange]);

  const fetchLogs = async (type, hours) => {
    setLoadingLogs(true);
    try {
        const response = await axios.get(`${API_BASE}/dashboard/${data.systemName}/logs/${type}?hours=${hours}`);
        setLogs(response.data.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp)));
    } catch (err) { console.error(err); } finally { setLoadingLogs(false); }
  };

  // ==================================================================================
  // 4. ACTION HANDLERS
  // ==================================================================================
  const handleCardClick = (iface) => { if(isLive) setIsLive(false); setSelectedInterface(iface); };
  
  const handleSaveSettings = async () => { 
      setSavingSettings(true);
      try { await axios.put(`${API_BASE}/dashboard/${data.systemName}/settings`, settings); alert("Settings saved!"); } 
      catch (err) { alert("Error: " + err.message); } finally { setSavingSettings(false); }
  };

  const handleSyncHistory = async () => {
    if(!data) return;
    setSyncing(true);
    try { 
        await axios.post(`${API_BASE}/dashboard/${data.systemName}/scan`);
        setTimeout(() => { onRefresh(); setSyncing(false); }, 2500);
    } catch (err) { alert("Sync Failed: " + err.message); setSyncing(false); } 
  };

  const handleExportReport = async () => {
      if(!data) return;
      try { 
          const response = await axios.get(`${API_BASE}/dashboard/${data.systemName}/report`, { responseType: 'blob' });
          const url = window.URL.createObjectURL(new Blob([response.data]));
          const link = document.createElement('a'); link.href = url; link.download = `${data.systemName}_report.pdf`;
          document.body.appendChild(link); link.click(); link.remove();
      } catch (err) { alert("Report Failed: " + err.message); }
  };

  const handleTestEmail = async () => {
      if(!data) return;
      try { 
          const res = await axios.post(`${API_BASE}/dashboard/${data.systemName}/test-email`);
          alert(res.data);
      } catch (err) { alert("Email Failed: " + err.message); }
  };

  const handleSaveIp = async () => {
    setSavingIp(true);
    try { await axios.put(`${API_BASE}/dashboard/${data.systemName}/ip`, { ipAddress: newIp }); setIsEditingIp(false); onRefresh(); } 
    catch (err) { alert("Failed to save IP"); } finally { setSavingIp(false); }
  };

  const handleDiscovery = async () => {
      setIsScanning(true); setScanResults(null);
      try {
          const res = await axios.post(`${API_BASE}/dashboard/discovery?cidr=${subnet}&community=public`);
          setScanResults(res.data);
          if(res.data.length > 0) setTimeout(() => onRefresh(), 2000);
      } catch (err) { alert("Discovery Failed. Check console."); } finally { setIsScanning(false); }
  };

  const handleInjectDemoData = () => {
      const fakeDevices = [
          { systemName: 'Core-Switch-01', ipAddress: '192.168.1.1', activeInterfaces: 1 },
          { systemName: 'Router-NY-West', ipAddress: '10.20.1.50', activeInterfaces: 1 },
          { systemName: 'Firewall-Perimeter', ipAddress: '172.16.0.1', activeInterfaces: 0 },
          { systemName: 'Switch-Floor-2', ipAddress: '192.168.1.102', activeInterfaces: 1 },
          { systemName: data.systemName, ipAddress: data.ipAddress, activeInterfaces: data.activeInterfaces } 
      ];
      setDemoDevices(fakeDevices);
  };

  // --- NEW: LOADING SCREEN ---
  if (!data) {
      return (
          <div className="loading-screen-container">
              <div className="loader-box">
                  <div className="spinner-wrapper">
                      <Loader className="spin-slow" size={48} color="#3b82f6" />
                  </div>
                  <h3>Waiting for Data...</h3>
                  <p>Select a device from the sidebar or wait for the initial scan.</p>
              </div>
          </div>
      );
  }

  const filteredInterfaces = getProcessedInterfaces();
  const filteredLogs = getFilteredLogs();
  const active = (new Date() - new Date(data.lastScanTime)) < 600000; 
  
  const mapData = demoDevices.length > 0 ? demoDevices : [{ systemName: data.systemName, ipAddress: data.ipAddress, activeInterfaces: data.activeInterfaces }];

  return (
    <div className="dashboard">
      <div className="header">
        
        {/* ROW 1: Identity & Primary Actions */}
        <div className="header-primary">
            <div className="identity-section">
                <h1 className="system-name">{data.systemName}</h1>
                <div className={`device-status-badge ${active ? 'online' : 'offline'}`}>
                  {active ? <Activity size={16} /> : <Power size={16} />}
                  <span>{active ? 'Active' : 'Offline'}</span>
                </div>
                <div className="ip-section">
                    {isEditingIp ? (
                      <div className="ip-edit-controls">
                        <input className="ip-input-mini" value={newIp} onChange={(e) => setNewIp(e.target.value)} />
                        <button className="icon-btn save" onClick={handleSaveIp}><Check size={14} /></button>
                        <button className="icon-btn cancel" onClick={() => setIsEditingIp(false)}><X size={14} /></button>
                      </div>
                    ) : (
                      <div className="ip-display" onClick={() => setIsEditingIp(true)}>
                        <span className="value">{data.ipAddress || '0.0.0.0'}</span>
                        <Edit2 size={10} className="icon-subtle"/>
                      </div>
                    )}
                </div>
            </div>
            <div className="actions-section">
                <button className={`action-btn ${isLive ? 'live-active' : ''}`} onClick={() => setIsLive(!isLive)}>
                    {isLive ? <StopCircle size={14} /> : <PlayCircle size={14} />} {isLive ? " Stop Live" : " Live"}
                </button>
                <button className="action-btn" onClick={() => setShowDiscovery(true)}><Search size={14} /> Discovery</button>
                <button className="action-btn" onClick={handleSyncHistory} disabled={syncing}>
                    <RefreshCw size={14} className={syncing ? "spin" : ""} /> {syncing ? "Sync..." : "Sync"}
                </button>
                <button className="action-btn" onClick={handleExportReport}><FileText size={14} /> Report</button>
                <button className="action-btn" onClick={handleTestEmail}><Bell size={14} /> Alert</button>
            </div>
        </div>

        {/* ROW 2: Toolbar, Circles & Filters */}
        <div className="header-toolbar">
             <div className="toolbar-left">
                 <div className="tab-switcher">
                    <button className={`tab-btn ${activeTab === 'overview' ? 'active' : ''}`} onClick={() => setActiveTab('overview')}><LayoutDashboard size={16}/> Overview</button>
                    <button className={`tab-btn ${activeTab === 'map' ? 'active' : ''}`} onClick={() => setActiveTab('map')}><Network size={16}/> Map</button>
                    <button className={`tab-btn ${activeTab === 'events' ? 'active' : ''}`} onClick={() => setActiveTab('events')}><List size={16}/> Events</button>
                    <button className={`tab-btn ${activeTab === 'settings' ? 'active' : ''}`} onClick={() => setActiveTab('settings')}><Settings size={16}/> Settings</button>
                 </div>
             </div>

             <div className="toolbar-right">
                 
                 {/* --- MAP TOOLBAR --- */}
                 {activeTab === 'map' && (
                     <div className="filter-group">
                        <button className="action-btn-small" onClick={handleInjectDemoData}>Load Demo Topology</button>
                     </div>
                 )}

                 {/* --- OVERVIEW CIRCLES (Inline) --- */}
                 {activeTab === 'overview' && (
                    <div className="status-circles-inline">
                        <div className={`status-circle-mini red ${statusFilter === 'DOWN' ? 'active' : ''}`} onClick={() => setStatusFilter(statusFilter === 'DOWN' ? 'ALL' : 'DOWN')}>
                            <span className="count">{counts.down}</span><span className="label">DOWN</span>
                        </div>
                        <div className={`status-circle-mini green ${statusFilter === 'UP' ? 'active' : ''}`} onClick={() => setStatusFilter(statusFilter === 'UP' ? 'ALL' : 'UP')}>
                            <span className="count">{counts.up}</span><span className="label">UP</span>
                        </div>
                        <div className="separator-vertical"></div>
                    </div>
                 )}

                 {/* --- EVENTS CIRCLES (Inline) --- */}
                 {activeTab === 'events' && (
                    <div className="status-circles-inline">
                        <div className={`status-circle-mini red ${logFilter === 'ERROR' ? 'active' : ''}`} onClick={() => setLogFilter(logFilter === 'ERROR' ? 'ALL' : 'ERROR')}>
                            <span className="count">{logCounts.ERROR}</span><span className="label">ERROR</span>
                        </div>
                        <div className={`status-circle-mini orange ${logFilter === 'ALERT' ? 'active' : ''}`} onClick={() => setLogFilter(logFilter === 'ALERT' ? 'ALL' : 'ALERT')}>
                            <span className="count">{logCounts.ALERT}</span><span className="label">ALERT</span>
                        </div>
                        <div className={`status-circle-mini purple ${logFilter === 'STARTUP' ? 'active' : ''}`} onClick={() => setLogFilter(logFilter === 'STARTUP' ? 'ALL' : 'STARTUP')}>
                            <span className="count">{logCounts.STARTUP}</span><span className="label">START</span>
                        </div>
                        <div className={`status-circle-mini blue ${logFilter === 'INFO' ? 'active' : ''}`} onClick={() => setLogFilter(logFilter === 'INFO' ? 'ALL' : 'INFO')}>
                            <span className="count">{logCounts.INFO}</span><span className="label">INFO</span>
                        </div>
                        <div className="separator-vertical"></div>
                    </div>
                 )}

                 {activeTab === 'overview' && (
                    <div className="filter-group">
                        <div className="filter-item">
                            <ArrowDownUp size={14} className="icon-subtle"/>
                            <select value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
                                <option value="INDEX">Default</option><option value="RX">Max RX</option><option value="TX">Max TX</option><option value="NAME">Name</option>
                            </select>
                        </div>
                    </div>
                 )}

                 {/* --- GLOBAL TIME SELECTOR --- */}
                 <div className="filter-item time-selector">
                    <Clock size={14} className="icon-subtle" />
                    <select value={selectedTimeRange} onChange={(e) => onTimeRangeChange(Number(e.target.value))}>
                        <option value={0}>Latest Snapshot</option>
                        <option value={1}>Last 1 Hour</option>
                        <option value={6}>Last 6 Hours</option>
                        <option value={12}>Last 12 Hours</option>
                        <option value={24}>Last 24 Hours</option>
                        <option value={168}>Last 7 Days</option>
                        <option value={720}>Last 30 Days</option>
                    </select>
                </div>

                 {activeTab === 'overview' && (
                    <div className="view-switcher">
                        <button className={`view-btn ${viewMode === 'grid' ? 'active' : ''}`} onClick={() => setViewMode('grid')}><Grid size={16} /></button>
                        <button className={`view-btn ${viewMode === 'list' ? 'active' : ''}`} onClick={() => setViewMode('list')}><ListIcon size={16} /></button>
                    </div>
                 )}
             </div>
        </div>
      </div>

      {/* ==================================================================================
          CONTENT AREA
      ================================================================================== */}

      {/* 1. OVERVIEW */}
      {activeTab === 'overview' && (
          <div className={viewMode === 'grid' ? "interfaces-grid" : "list-view-container"}>
            {viewMode === 'grid' ? (
                filteredInterfaces.map((iface) => (
                    <div key={iface.interfaceIndex} className={`interface-card ${iface.status}`} onClick={() => handleCardClick(iface)} style={{cursor: 'pointer'}}>
                      <div className="card-header">
                        <div className="interface-identity">
                            {iface.status === 'up' ? <Wifi size={20} className="icon-up" /> : <WifiOff size={20} className="icon-down" />}
                            <span className="name" title={iface.interfaceName}>{iface.interfaceName}</span>
                        </div>
                        <span className={`status-badge ${iface.status}`}>{iface.status}</span>
                      </div>
                      <div className="card-metrics">
                        <div className="meta-row">
                            <span className="meta-tag">{iface.interfaceType}</span>
                            <span className="meta-speed">{iface.speedFormatted}</span>
                        </div>
                        <div className="traffic-grid">
                            <div className="traffic-col"><span className="traffic-label">RX</span><span className="traffic-value">{iface.rxRate}</span></div>
                            <div className="traffic-col"><span className="traffic-label">TX</span><span className="traffic-value">{iface.txRate}</span></div>
                        </div>
                      </div>
                    </div>
                ))
            ) : (
                <table className="interface-table">
                    <thead><tr><th>ID</th><th>Name</th><th>Status</th><th>Speed</th><th>RX Rate</th><th>TX Rate</th></tr></thead>
                    <tbody>
                        {filteredInterfaces.map((iface) => (
                            <tr key={iface.interfaceIndex} onClick={() => handleCardClick(iface)} style={{cursor: 'pointer'}}>
                                <td className="mono">{iface.interfaceIndex}</td>
                                <td className="name-col"><Server size={14}/> {iface.interfaceName}</td>
                                <td><span className={`status-dot ${iface.status}`}></span> {iface.status}</td>
                                <td className="mono">{iface.speedFormatted}</td>
                                <td className="traffic-cell rx"><span>{iface.rxRate}</span></td>
                                <td className="traffic-cell tx"><span>{iface.txRate}</span></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
          </div>
      )}

      {/* 2. NETWORK MAP */}
      {activeTab === 'map' && (
          <NetworkMap devices={mapData} />
      )}

      {/* 3. EVENTS */}
      {activeTab === 'events' && (
          <div className="logs-container">
              {loadingLogs ? <div className="loading-state">Loading logs...</div> : (
                  <table className="logs-table">
                      <thead><tr><th>Time</th><th>Level</th><th>Source</th><th>Message</th></tr></thead>
                      <tbody>
                          {filteredLogs.map(log => (
                              <tr key={log.id}>
                                  <td className="log-time">{new Date(log.timestamp).toLocaleString()}</td>
                                  <td><span className={getLogBadgeClass(log.level)}>{log.level}</span></td>
                                  <td className="log-source">{log.source}</td>
                                  <td className="log-message">{log.message}</td>
                              </tr>
                          ))}
                          {filteredLogs.length === 0 && <tr><td colSpan="4" className="empty-state">No events found.</td></tr>}
                      </tbody>
                  </table>
              )}
          </div>
      )}

      {/* 4. SETTINGS */}
      {activeTab === 'settings' && (
          <div className="settings-container">
              <div className="settings-card">
                  <h3><Settings size={20}/> Monitoring Thresholds</h3>
                  <div className="setting-row"><label>High Utilization (%)</label><input type="number" value={settings.thresholdUtil} onChange={(e) => setSettings({...settings, thresholdUtil: parseFloat(e.target.value)})} /></div>
                  <div className="setting-row"><label>Error Rate (%)</label><input type="number" value={settings.thresholdError} onChange={(e) => setSettings({...settings, thresholdError: parseFloat(e.target.value)})} /></div>
                  <button className="save-btn" onClick={handleSaveSettings}>Save</button>
              </div>
          </div>
      )}

      {/* ==================================================================================
          MODALS
      ================================================================================== */}

      {showDiscovery && (
        <div className="modal-overlay">
          <div className="modal">
            <h3><Search size={20}/> Network Discovery</h3>
            <div className="form-group"><input value={subnet} onChange={(e) => setSubnet(e.target.value)} placeholder="192.168.1.0/24" className="ip-input" style={{width: '100%', marginBottom: '15px'}}/></div>
            {scanResults && <div className="scan-results">{scanResults.length === 0 ? <p className="no-results">No new devices found.</p> : <ul>{scanResults.map((d, i) => <li key={i}>✅ {d}</li>)}</ul>}</div>}
            <div className="settings-actions">
                <button className="save-btn" onClick={handleDiscovery} disabled={isScanning}>{isScanning ? 'Scanning...' : 'Start Scan'}</button>
                <button className="cancel-btn" onClick={() => setShowDiscovery(false)}>Close</button>
            </div>
          </div>
        </div>
      )}

      {selectedInterface && (
          <InterfaceGraph systemName={data.systemName} interfaceIndex={selectedInterface.interfaceIndex} interfaceName={selectedInterface.interfaceName} onClose={() => setSelectedInterface(null)}/>
      )}
    </div>
  );
}

export default Dashboard;
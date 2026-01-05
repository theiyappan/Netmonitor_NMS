import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Layout, Server, Activity } from 'lucide-react'; // Added Icons
import Dashboard from './components/Dashboard';
import InterfaceGraph from './components/InterfaceGraph';
import './App.css';

const API_BASE = 'http://localhost:8080/api';

function App() {
  const [view, setView] = useState('dashboard'); // 'dashboard' or 'graph'
  
  // Device Selection State
  const [devices, setDevices] = useState([]);
  const [selectedSystem, setSelectedSystem] = useState(null); // Null initially until loaded
  
  const [selectedInterface, setSelectedInterface] = useState(null);
  const [dashboardData, setDashboardData] = useState(null);
  
  // Global Time Range State (Persists between views)
  const [timeRange, setTimeRange] = useState(0); // Default to 0 (Latest Snapshot)

  // 1. Fetch Device List on Load
  useEffect(() => {
    fetchDevices();
  }, []);

  // 2. Fetch Dashboard Data when Device or Time changes
  useEffect(() => {
    if (selectedSystem) {
      fetchDashboardData();
      const interval = setInterval(fetchDashboardData, 30000); // Auto-refresh 30s
      return () => clearInterval(interval);
    }
  }, [selectedSystem, timeRange]);

  const fetchDevices = async () => {
    try {
      const res = await axios.get(`${API_BASE}/dashboard/devices`);
      setDevices(res.data);
      // If we have devices and none is selected, select the first one
      if (res.data.length > 0 && !selectedSystem) {
        setSelectedSystem(res.data[0].systemName);
      }
    } catch (error) {
      console.error("Error fetching devices:", error);
    }
  };

  const fetchDashboardData = async () => {
    if (!selectedSystem) return;
    try {
      const response = await axios.get(
        `${API_BASE}/dashboard/${selectedSystem}?hours=${timeRange}`
      );
      setDashboardData(response.data);
    } catch (error) {
      console.error("Error fetching dashboard:", error);
    }
  };

  const handleInterfaceClick = (index) => {
    setSelectedInterface(index);
    setView('graph');
  };

  const handleBack = () => {
    setView('dashboard');
    setSelectedInterface(null);
  };

  return (
    <div className="app-container">
      {/* --- SIDEBAR (RESTORED) --- */}
      <div className="sidebar">
        <div className="sidebar-header">
           <Activity className="logo-icon" size={24} /> 
           <span className="logo-text">NetMonitor</span>
        </div>
        
        <div className="sidebar-content">
           <div className="section-label">DEVICES</div>
           {devices.length === 0 ? (
             <div className="sidebar-item">No Devices</div>
           ) : (
             devices.map((device) => (
               <button 
                 key={device.id}
                 className={`sidebar-item ${selectedSystem === device.systemName ? 'active' : ''}`}
                 onClick={() => {
                   setSelectedSystem(device.systemName);
                   setView('dashboard'); // Switch back to dashboard when changing device
                 }}
               >
                 <Server size={16} />
                 {device.systemName}
               </button>
             ))
           )}
        </div>
      </div>

      {/* --- MAIN CONTENT --- */}
      <div className="main-content">
        {selectedSystem ? (
          view === 'dashboard' ? (
            <Dashboard 
              data={dashboardData} 
              onInterfaceClick={handleInterfaceClick}
              onRefresh={fetchDashboardData}
              selectedTimeRange={timeRange}
              onTimeRangeChange={setTimeRange}
            />
          ) : (
            <InterfaceGraph 
              systemName={selectedSystem}
              interfaceIndex={selectedInterface}
              onBack={handleBack}
              initialHours={timeRange} // Pass the global time
            />
          )
        ) : (
          <div className="loading-screen">Loading Devices...</div>
        )}
      </div>
    </div>
  );
}

export default App;
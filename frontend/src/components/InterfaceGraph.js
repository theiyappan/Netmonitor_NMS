import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { 
    AreaChart, Area, XAxis, YAxis, CartesianGrid, 
    Tooltip, ResponsiveContainer 
} from 'recharts';
import { X, PlayCircle, StopCircle, Activity, Clock } from 'lucide-react';
import './Dashboard.css';

const API_BASE = 'http://localhost:8080/api';

const InterfaceGraph = ({ systemName, interfaceIndex, interfaceName, onClose }) => {
    // --- STATE ---
    const [graphData, setGraphData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [isLive, setIsLive] = useState(false);
    const [lastUpdated, setLastUpdated] = useState(null);
    const [timeRange, setTimeRange] = useState(24); // Default 24 Hours

    // Poll Timer Reference
    const pollTimer = useRef(null);

    // --- 1. DATA FETCHING ---
    const fetchData = async () => {
        try {
            // Fetch graph data based on selected time range
            const res = await axios.get(`${API_BASE}/dashboard/${systemName}/interface/${interfaceIndex}/graph?hours=${timeRange}`);
            setGraphData(res.data);
            setLastUpdated(new Date());
            setLoading(false);
        } catch (err) {
            console.error("Graph fetch failed", err);
            setLoading(false);
        }
    };

    // --- 2. INITIAL LOAD & TIME RANGE CHANGE ---
    useEffect(() => {
        setLoading(true);
        fetchData();
        // If live mode was active, restart the loop to adapt to new range
        if (isLive) {
            clearInterval(pollTimer.current);
            pollTimer.current = setInterval(handleLiveLoop, 5000);
        }
    }, [timeRange]); // Re-run when user changes time

    // Cleanup on unmount
    useEffect(() => {
        return () => stopLiveMode();
    }, []);

    // --- 3. LIVE MODE LOGIC ---
    const startLiveMode = () => {
        setIsLive(true);
        handleLiveLoop(); // Immediate trigger
        pollTimer.current = setInterval(handleLiveLoop, 5000);
    };

    const stopLiveMode = () => {
        setIsLive(false);
        if (pollTimer.current) clearInterval(pollTimer.current);
    };

    const handleLiveLoop = async () => {
        try {
            // Trigger backend scan
            await axios.post(`${API_BASE}/dashboard/${systemName}/scan`);
            // Wait 2s for backend processing, then refresh graph
            setTimeout(() => fetchData(), 2000);
        } catch (err) {
            console.error("Live loop error", err);
            stopLiveMode();
        }
    };

    // --- 4. FORMATTERS ---
    const formatBps = (value) => {
        if (value >= 1_000_000_000) return (value / 1_000_000_000).toFixed(1) + ' Gbps';
        if (value >= 1_000_000) return (value / 1_000_000).toFixed(1) + ' Mbps';
        if (value >= 1_000) return (value / 1_000).toFixed(1) + ' Kbps';
        return value + ' bps';
    };

    const formatTimeAxis = (timestamp) => {
        const date = new Date(timestamp);
        // If viewing > 2 days, show Date. Otherwise show Time.
        if (timeRange > 48) return date.toLocaleDateString(undefined, {month:'numeric', day:'numeric'});
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    const formatTooltipTime = (timestamp) => {
        return new Date(timestamp).toLocaleString();
    };

    // --- 5. DATA PROCESSING ---
    // Merge RX and TX arrays into one for Recharts
    const mergedData = graphData?.rxData.map((rxPoint, i) => ({
        timestamp: rxPoint.timestamp,
        rx: rxPoint.value,
        tx: graphData.txData[i]?.value || 0
    })) || [];

    return (
        <div className="modal-overlay">
            <div className="graph-modal">
                
                {/* HEADER */}
                <div className="graph-header">
                    <div className="graph-title">
                        <Activity className="icon-blue" size={24}/>
                        <div>
                            <h2>{interfaceName}</h2>
                            <span className="subtitle">
                                {loading ? 'Fetching Data...' : `Traffic Analysis (${timeRange}h)`}
                            </span>
                        </div>
                    </div>

                    <div className="graph-actions">
                        {/* Time Selector */}
                        <div className="graph-time-select">
                            <Clock size={14}/>
                            <select 
                                value={timeRange} 
                                onChange={(e) => setTimeRange(Number(e.target.value))}
                                disabled={isLive} // Lock time range while live
                            >
                                <option value={1}>Last 1 Hour</option>
                                <option value={6}>Last 6 Hours</option>
                                <option value={12}>Last 12 Hours</option>
                                <option value={24}>Last 24 Hours</option>
                                <option value={168}>Last 7 Days</option>
                                <option value={720}>Last 30 Days</option>
                            </select>
                        </div>

                        <span className="last-updated">
                            {isLive && <span className="live-dot"></span>}
                            {lastUpdated ? lastUpdated.toLocaleTimeString() : ''}
                        </span>
                        
                        <button 
                            className={`live-btn ${isLive ? 'active' : ''}`} 
                            onClick={isLive ? stopLiveMode : startLiveMode}
                        >
                            {isLive ? <StopCircle size={16}/> : <PlayCircle size={16}/>}
                            {isLive ? 'Stop Live' : 'Go Live'}
                        </button>

                        <button className="icon-btn close" onClick={onClose}><X size={24}/></button>
                    </div>
                </div>

                {/* GRAPH AREA */}
                <div className="chart-container">
                    {loading && !graphData ? (
                        <div className="graph-loading-state">Loading...</div>
                    ) : (
                        <ResponsiveContainer width="100%" height={350}>
                            <AreaChart data={mergedData}>
                                <defs>
                                    <linearGradient id="colorRx" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3}/>
                                        <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                                    </linearGradient>
                                    <linearGradient id="colorTx" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#10b981" stopOpacity={0.3}/>
                                        <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                                    </linearGradient>
                                </defs>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0"/>
                                <XAxis 
                                    dataKey="timestamp" 
                                    tickFormatter={formatTimeAxis}
                                    tick={{fontSize: 11, fill: '#64748b'}} 
                                    minTickGap={40}
                                />
                                <YAxis 
                                    tickFormatter={formatBps} 
                                    tick={{fontSize: 11, fill: '#64748b'}}
                                    width={70}
                                    domain={['auto', 'auto']} // AUTO SCALING ENABLED
                                />
                                <Tooltip 
                                    labelFormatter={formatTooltipTime}
                                    formatter={(val) => [formatBps(val), ""]}
                                    contentStyle={{borderRadius: '8px', border: 'none', boxShadow: '0 10px 15px -3px rgba(0,0,0,0.1)'}}
                                />
                                <Area 
                                    type="monotone" 
                                    dataKey="rx" 
                                    name="Inbound (RX)"
                                    stroke="#3b82f6" 
                                    strokeWidth={2}
                                    fillOpacity={1} 
                                    fill="url(#colorRx)" 
                                    isAnimationActive={false} // Disable animation for live smoothness
                                />
                                <Area 
                                    type="monotone" 
                                    dataKey="tx" 
                                    name="Outbound (TX)"
                                    stroke="#10b981" 
                                    strokeWidth={2}
                                    fillOpacity={1} 
                                    fill="url(#colorTx)" 
                                    isAnimationActive={false} 
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    )}
                </div>

                {/* STATS FOOTER */}
                {graphData && (
                    <div className="graph-footer">
                        <div className="stat-box">
                            <span className="label">Current Inbound</span>
                            <span className="val rx">{formatBps(mergedData[mergedData.length-1]?.rx || 0)}</span>
                        </div>
                        <div className="stat-box">
                            <span className="label">Current Outbound</span>
                            <span className="val tx">{formatBps(mergedData[mergedData.length-1]?.tx || 0)}</span>
                        </div>
                        <div className="stat-box">
                            <span className="label">Max Speed</span>
                            <span className="val">{formatBps(graphData.summary.speedBps)}</span>
                        </div>
                        <div className="stat-box">
                            <span className="label">Total Errors</span>
                            <span className="val err">{graphData.summary.totalErrors || 0}</span>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default InterfaceGraph;
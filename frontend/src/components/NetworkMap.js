import React, { useEffect, useCallback } from 'react';
import ReactFlow, { 
  Background, 
  Controls, 
  MiniMap, 
  useNodesState, 
  useEdgesState, 
  MarkerType,
  Handle, 
  Position
} from 'reactflow';
import 'reactflow/dist/style.css';
import { 
    Router, Server, Shield, 
    Cloud, Activity, Power, 
    Cpu, Globe, Network 
} from 'lucide-react';

// --- A. CUSTOM NODE COMPONENT ---
// This mimics a "Rack Unit" or Technical Label found in engineering diagrams
const TechNode = ({ data }) => {
    const isOnline = data.status === 'UP';
    const statusColor = isOnline ? '#10b981' : '#ef4444'; // Emerald-500 vs Red-500
    const bgColor = isOnline ? '#ffffff' : '#fef2f2';

    return (
        <div style={{
            minWidth: '220px',
            background: bgColor,
            border: '1px solid #cbd5e1',
            borderRadius: '4px', // Tighter radius for technical look
            borderLeft: `5px solid ${statusColor}`, // Professional status indicator
            boxShadow: '0 2px 4px -1px rgba(0,0,0,0.06)',
            fontFamily: 'Inter, system-ui, sans-serif',
            fontSize: '12px'
        }}>
            {/* Connection Handles (Invisible but necessary for ReactFlow) */}
            <Handle type="target" position={Position.Top} style={{ background: '#94a3b8' }} />
            <Handle type="source" position={Position.Bottom} style={{ background: '#94a3b8' }} />

            {/* Header: Type & Status */}
            <div style={{
                padding: '8px 12px',
                borderBottom: '1px solid #e2e8f0',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                background: '#f8fafc'
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#475569', fontWeight: 600, textTransform: 'uppercase', fontSize: '10px', letterSpacing: '0.5px' }}>
                    {data.icon}
                    {data.type}
                </div>
                {isOnline ? 
                    <Activity size={12} color="#10b981" /> : 
                    <Power size={12} color="#ef4444" />
                }
            </div>

            {/* Body: Name & IP */}
            <div style={{ padding: '10px 12px' }}>
                <div style={{ fontWeight: 700, color: '#1e293b', fontSize: '14px', marginBottom: '4px' }}>
                    {data.label}
                </div>
                <div style={{ 
                    fontFamily: '"JetBrains Mono", "Fira Code", monospace', // Monospace for IPs is crucial for realism
                    color: '#64748b', 
                    background: '#f1f5f9',
                    padding: '2px 6px',
                    borderRadius: '4px',
                    display: 'inline-block',
                    fontSize: '11px',
                    border: '1px solid #e2e8f0'
                }}>
                    {data.ip}
                </div>
            </div>
        </div>
    );
};

// Register custom node types
const nodeTypes = { techNode: TechNode };

const NetworkMap = ({ devices }) => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  useEffect(() => {
    if (!devices || devices.length === 0) return;

    // --- 1. CORE GATEWAY (The "ISP/Cloud" connection) ---
    const coreNode = {
      id: 'core-gateway',
      type: 'techNode', // Use our custom technical node
      data: { 
          label: 'Core-Gateway-01',
          ip: '10.0.0.1',
          type: 'CORE',
          status: 'UP',
          icon: <Cloud size={14} />
      },
      position: { x: 400, y: 50 },
    };

    // --- 2. MAP DEVICES TO NODES ---
    const deviceNodes = devices.map((device, index) => {
      // "Tree" Layout Calculation
      const levelWidth = 800; 
      const spacing = levelWidth / (devices.length + 1);
      const xPos = (index + 1) * spacing + 50; 
      const yPos = 250 + (index % 2) * 80; // Stagger height slightly to avoid connector overlap

      // Determine Icon & Type based on naming convention
      let Icon = <Server size={14} />;
      let typeLabel = "SERVER";
      const name = device.systemName.toUpperCase();
      
      if (name.includes('ROUTER')) { Icon = <Router size={14} />; typeLabel = "ROUTER"; }
      else if (name.includes('FW') || name.includes('FIREWALL')) { Icon = <Shield size={14} />; typeLabel = "FIREWALL"; }
      else if (name.includes('SWITCH')) { Icon = <Cpu size={14} />; typeLabel = "SWITCH"; }

      return {
        id: device.systemName,
        type: 'techNode',
        data: { 
            label: device.systemName,
            ip: device.ipAddress,
            type: typeLabel,
            status: device.activeInterfaces > 0 ? 'UP' : 'DOWN',
            icon: Icon
        },
        position: { x: xPos, y: yPos },
      };
    });

    // --- 3. CREATE TECHNICAL EDGES ---
    const deviceEdges = devices.map((device) => {
        const isOnline = device.activeInterfaces > 0;
        return {
          id: `e-core-${device.systemName}`,
          source: 'core-gateway',
          target: device.systemName,
          type: 'smoothstep', // Orthogonal right-angle lines (Circuit style)
          style: { 
              stroke: isOnline ? '#64748b' : '#cbd5e1', // Slate-500 for active, lighter for inactive
              strokeWidth: 2,
              strokeDasharray: isOnline ? '0' : '4 4', // Dashed if connection is down
          },
          markerEnd: { 
              type: MarkerType.ArrowClosed, 
              color: isOnline ? '#64748b' : '#cbd5e1',
              width: 20, height: 20
          },
        };
    });

    setNodes([coreNode, ...deviceNodes]);
    setEdges(deviceEdges);

  }, [devices, setNodes, setEdges]);

  return (
    <div style={{ 
        width: '100%', 
        height: '650px', 
        background: '#f8fafc', // Slate-50 background (standard for technical tools)
        borderRadius: '8px', 
        border: '1px solid #e2e8f0', 
        position: 'relative',
        overflow: 'hidden'
    }}>
      {/* Label for the Map Section */}
      <div style={{
          position: 'absolute', top: 12, left: 16, zIndex: 10,
          background: 'white', padding: '6px 12px', borderRadius: '6px',
          border: '1px solid #e2e8f0', boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
          display: 'flex', alignItems: 'center', gap: '8px',
          fontSize: '0.85rem', fontWeight: 600, color: '#475569'
      }}>
          <Network size={16} color="#3b82f6"/>
          Live Network Topology
      </div>

      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        fitView
        attributionPosition="bottom-right"
        nodesConnectable={false} // Prevent user from messing up the wiring
        minZoom={0.5}
        maxZoom={1.5}
      >
        {/* Technical Grid Background */}
        <Background 
            variant="dots" 
            color="#94a3b8" 
            gap={25} 
            size={1} 
            style={{ opacity: 0.4 }}
        />
        
        {/* Navigation Controls (Bottom Left, unobtrusive) */}
        <Controls showInteractive={false} position="bottom-left" />
        
        {/* MiniMap (Bottom Right, technical look) */}
        <MiniMap 
            nodeStrokeColor="#cbd5e1"
            nodeColor="#f1f5f9"
            maskColor="rgba(248, 250, 252, 0.7)"
            style={{ 
                border: '1px solid #e2e8f0', 
                borderRadius: '6px', 
                height: 100, 
                width: 150 
            }} 
        />
      </ReactFlow>
    </div>
  );
};

export default NetworkMap;
// src/App.tsx
import React from 'react';
import LogicCommitForm from './components/LogicCommitForm';
// import AuditDashboard from './components/AuditDashboard'; // Future component
// import UniversalRenderer from './components/UniversalRenderer'; // Future component

// The main application entry point (simple for now)
const App: React.FC = () => {
    return (
        <div style={{ maxWidth: '1000px', margin: '0 auto', fontFamily: 'sans-serif' }}>
            <h1>UBOS v0.2 Control Platform</h1>
            <p>React GUI: AI-Generated Logic Deployment Interface</p>
            <hr />
            <LogicCommitForm />
            {/* The Audit Dashboard and Universal Renderer components will be integrated here */}
        </div>
    );
};

export default App;
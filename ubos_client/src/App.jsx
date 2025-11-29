import React from 'react';
import LogicCommitForm from './components/LogicCommitForm';
// import AuditDashboard from './components/AuditDashboard'; // Future component
// import UniversalRenderer from './components/UniversalRenderer'; // Future component

const App = () => {
    return (
        <div style={{ maxWidth: '800px', margin: '0 auto', fontFamily: 'sans-serif' }}>
            <h1>UBOS v0.2 Control Platform</h1>
            <p>Ready to deploy logic via GUI (GPT-4o powered).</p>
            <hr />
            <LogicCommitForm />
            {/* Future Tabs for Audit Dashboard and App Runner will go here */}
        </div>
    );
};

export default App;
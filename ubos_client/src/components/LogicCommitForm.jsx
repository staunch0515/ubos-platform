import React, { useState } from 'react';
import { commitLogic } from '../services/api';

const LogicCommitForm = () => {
    const [slug, setSlug] = useState('logic.new.test');
    const [branch, setBranch] = useState('master');
    const [code, setCode] = useState('return ["status": "ok", "message": "Script runs fine"]');
    const [message, setMessage] = useState('New feature commit via React GUI');
    const [status, setStatus] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setStatus('Committing...');
        try {
            const result = await commitLogic({ type: 'LOGIC', slug, branch, code, message });
            setStatus(`Success: Committed ID ${result.commitId} to branch ${branch}`);
        } catch (error) {
            setStatus(`Error: ${error.message}`);
        }
    };

    return (
        <form onSubmit={handleSubmit}>
            <h2>Logic Injection Console</h2>
            <p style={{color: 'red'}}>{status}</p>
            <div style={{ marginBottom: '10px' }}>
                <label>Slug:</label>
                <input type="text" value={slug} onChange={(e) => setSlug(e.target.value)} required />
            </div>
            <div style={{ marginBottom: '10px' }}>
                <label>Branch:</label>
                <input type="text" value={branch} onChange={(e) => setBranch(e.target.value)} required />
            </div>
            <div style={{ marginBottom: '10px' }}>
                <label>Commit Message:</label>
                <input type="text" value={message} onChange={(e) => setMessage(e.target.value)} required />
            </div>
            <div style={{ marginBottom: '10px' }}>
                <label>Groovy Code:</label>
                <textarea rows="8" value={code} onChange={(e) => setCode(e.target.value)} required style={{ width: '100%' }} />
            </div>
            <button type="submit">Commit to Kernel (Deploy)</button>
        </form>
    );
};

export default LogicCommitForm;
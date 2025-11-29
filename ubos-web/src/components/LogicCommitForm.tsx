// src/components/LogicCommitForm.tsx
import React, { useState } from 'react';
import { commitLogic, ApiException } from '../services/api';
import type { CommitRequest } from '../types/ubos';

// Define component's external properties (if any)
type LogicCommitFormProps = object

const LogicCommitForm: React.FC<LogicCommitFormProps> = () =>
{
	// State is typed using TypeScript's built-in string/boolean types
	const [slug, setSlug] = useState<string>('logic.new.test');
	const [branch, setBranch] = useState<string>('master');
	const [code, setCode] = useState<string>('return ["status": "ok", "message": "Script runs fine"]');
	const [message, setMessage] = useState<string>('New feature commit via React GUI');
	const [status, setStatus] = useState<string>('');
	const [resultData, setResultData] = useState<any>(null);

	const handleSubmit = async (e: React.FormEvent) =>
	{
		e.preventDefault();
		setStatus('Committing...');
		setResultData(null);

		try
		{
			const requestData: CommitRequest = {
				type: 'LOGIC',
				slug: slug,
				branch: branch,
				code: code,
				message: message
			};

			const result = await commitLogic(requestData);

			setStatus(`✅ Success: Committed ID ${result.commitId} to branch ${branch}`);
			setResultData(result);

		} catch (error)
		{
			if (error instanceof ApiException)
			{
				setStatus(`❌ Error ${error.status}: ${error.body.error || error.message}`);
				setResultData(error.body);
			} else
			{
				setStatus(`❌ Error: ${error instanceof Error ? error.message : 'Unknown error'}`);
			}
		}
	};

	return (
		<form onSubmit={handleSubmit} style={{ padding: '20px', border: '1px solid #ccc', borderRadius: '8px' }}>
			<h2>Logic Injection Console</h2>

			<p style={{ fontWeight: 'bold', color: status.startsWith('✅') ? 'green' : 'red' }}>{status}</p>

			{/* Input Fields */}
			<div style={{ marginBottom: '10px' }}>
				<label>Slug (Unique ID):</label>
				<input type="text" value={slug} onChange={(e) => setSlug(e.target.value)} required style={{ width: '100%', padding: '8px' }} />
			</div>
			<div style={{ marginBottom: '10px' }}>
				<label>Branch (Target Version):</label>
				<input type="text" value={branch} onChange={(e) => setBranch(e.target.value)} required style={{ width: '100%', padding: '8px' }} />
			</div>
			<div style={{ marginBottom: '10px' }}>
				<label>Commit Message:</label>
				<input type="text" value={message} onChange={(e) => setMessage(e.target.value)} required style={{ width: '100%', padding: '8px' }} />
			</div>
			<div style={{ marginBottom: '10px' }}>
				<label>Groovy Code:</label>
				<textarea rows={8} value={code} onChange={(e) => setCode(e.target.value)} required style={{ width: '100%', padding: '8px', fontFamily: 'monospace' }} />
			</div>

			<button type="submit" style={{ padding: '10px 20px', backgroundColor: '#007bff', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer' }}>
				Commit to Kernel (Deploy)
			</button>

			{resultData && (
				<div style={{ marginTop: '20px', background: '#f4f4f4', padding: '10px', borderRadius: '5px' }}>
					<strong>API Response:</strong>
					<pre>{JSON.stringify(resultData, null, 2)}</pre>
				</div>
			)}
		</form>
	);
};

export default LogicCommitForm;
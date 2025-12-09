// src/main.tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App' // 👈 引入 App，而不是 Desktop
import './styles/os.scss'

ReactDOM.createRoot(document.getElementById('root')!).render(
	<React.StrictMode>
		<App />
	</React.StrictMode>,
)
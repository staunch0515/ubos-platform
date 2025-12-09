// src/App.tsx
import React, { useEffect, useState } from 'react';
import { useKernel } from './store/kernelStore';
import { Desktop } from './components/Desktop';

// --- 组件 1: 开机画面 ---
const BootScreen = () => (
	<div style={{
		height: '100vh',
		background: 'black',
		color: 'white',
		display: 'flex',
		justifyContent: 'center',
		alignItems: 'center',
		flexDirection: 'column',
		fontFamily: 'Segoe UI, sans-serif'
	}}>
		<h1 style={{ fontSize: '3rem', marginBottom: '1rem', letterSpacing: '2px' }}>UBOS</h1>
		<div className="loader" style={{ fontSize: '1.2rem', opacity: 0.8 }}>System Booting...</div>
	</div>
);

// --- 组件 2: 登录画面 (已添加交互逻辑) ---
const LoginScreen = () =>
{
	// 获取 store 中的登录方法
	const { login } = useKernel();

	// 本地状态管理输入
	const [email, setEmail] = useState('');
	const [password, setPassword] = useState('');
	const [isLoggingIn, setIsLoggingIn] = useState(false);

	const handleLogin = async () =>
	{
		if (!email || !password)
		{
			alert("请输入账号和密码");
			return;
		}

		setIsLoggingIn(true);
		// 调用内核的登录逻辑
		await login(email, password);
		// 注意：如果登录失败，store 内部通常会处理错误或弹窗，这里为了简单先只负责调用
		setIsLoggingIn(false);
	};

	return (
		<div style={{
			height: '100vh',
			background: 'linear-gradient(135deg, #1e1e1e 0%, #0f0f0f 100%)', //稍微好一点的深色背景
			color: 'white',
			display: 'flex',
			justifyContent: 'center',
			alignItems: 'center',
			fontFamily: 'Segoe UI, sans-serif'
		}}>
			<div style={{
				textAlign: 'center',
				width: '320px',
				padding: '40px',
				background: 'rgba(255,255,255,0.05)',
				backdropFilter: 'blur(10px)',
				borderRadius: '8px',
				boxShadow: '0 8px 32px 0 rgba(0, 0, 0, 0.37)'
			}}>
				<h2 style={{ marginBottom: '30px', fontWeight: 300 }}>Logrum Admin</h2>

				<div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
					<input
						type="text"
						placeholder="Username / Email"
						value={email}
						onChange={(e) => setEmail(e.target.value)}
						style={{
							padding: '12px',
							borderRadius: '4px',
							border: '1px solid #444',
							background: 'rgba(0,0,0,0.3)',
							color: 'white',
							outline: 'none'
						}}
					/>
					<input
						type="password"
						placeholder="Password"
						value={password}
						onChange={(e) => setPassword(e.target.value)}
						style={{
							padding: '12px',
							borderRadius: '4px',
							border: '1px solid #444',
							background: 'rgba(0,0,0,0.3)',
							color: 'white',
							outline: 'none'
						}}
					/>
					<button
						onClick={handleLogin}
						disabled={isLoggingIn}
						style={{
							marginTop: '10px',
							padding: '12px',
							borderRadius: '4px',
							border: 'none',
							background: '#0078d4',
							color: 'white',
							cursor: 'pointer',
							fontWeight: 'bold',
							opacity: isLoggingIn ? 0.7 : 1
						}}
					>
						{isLoggingIn ? 'Verifying...' : 'Sign In'}
					</button>
				</div>
			</div>
		</div>
	);
};

// --- 主控制器 ---
function App()
{
	const { status, bootSystem } = useKernel();

	// 按下电源键：启动系统
	useEffect(() =>
	{
		bootSystem();
	}, []);

	// 状态机路由：决定显示哪个“屏幕”
	switch (status)
	{
		case 'BOOTING':
			return <BootScreen />;
		case 'LOGIN_REQUIRED':
			return <LoginScreen />;
		case 'READY':
			return <Desktop />; // 只有在这里才显示桌面
		case 'ERROR':
			return (
				<div style={{
					height: '100vh', background: '#0000aa', color: 'white',
					display: 'flex', justifyContent: 'center', alignItems: 'center', flexDirection: 'column'
				}}>
					<h1>:(</h1>
					<h2>System Error</h2>
					<p>Cannot connect to UBOS Kernel.</p>
					<p>Please check if the Java backend is running on port 8080.</p>
				</div>
			);
		default:
			return null;
	}
}

export default App;
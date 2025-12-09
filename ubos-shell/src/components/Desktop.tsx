import React from 'react';
import { useKernel } from '../store/kernelStore';
import { WindowFrame } from './WindowFrame'; // 假设您有这个文件
import { useOSStore } from '../store/osStore'; // 假设您有这个文件

export const Desktop = () =>
{
	const { desktopConfig } = useKernel();
	const { windows, openApp } = useOSStore();

	// 防御性编程：如果没拿到配置，先渲染黑屏或 Loading
	if (!desktopConfig) return <div style={{ background: 'black', height: '100vh' }}>Loading Desktop...</div>;

	return (
		<div
			className="os-screen"
			style={{
				// 对应后端 Genesis 里的 "wallpaper" 字段
				backgroundImage: `url(${desktopConfig.wallpaper})`,
				backgroundSize: 'cover',
				height: '100vh',
				width: '100vw',
				position: 'relative',
				overflow: 'hidden'
			}}
		>
			{/* 图标层 */}
			<div style={{ padding: 20, display: 'flex', flexDirection: 'column', gap: 20 }}>
				{/* 对应后端 Genesis 里的 "icons" 数组 */}
				{desktopConfig.icons && desktopConfig.icons.map((app: any) => (
					<div
						key={app.id}
						className="os-icon"
						onDoubleClick={() => openApp(app.id, app)}
						style={{
							width: 80, height: 90,
							display: 'flex', flexDirection: 'column', alignItems: 'center',
							cursor: 'pointer', color: 'white', textShadow: '0 1px 2px black'
						}}
					>
						<div style={{ fontSize: 40 }}>{app.icon}</div>
						<div style={{ fontSize: 14, marginTop: 5 }}>{app.title}</div>
					</div>
				))}
			</div>

			{/* 窗口层 */}
			{windows.map((win: any) => <WindowFrame key={win.id} win={win} />)}

			{/* 任务栏 */}
			<div className="os-taskbar" style={{
				position: 'absolute', bottom: 0, width: '100%', height: 48,
				background: 'rgba(0,0,0,0.85)', color: 'white', display: 'flex', alignItems: 'center', padding: '0 16px'
			}}>
				<div>🔷 Start</div>
			</div>
		</div>
	);
};
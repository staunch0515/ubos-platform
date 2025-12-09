import Draggable from 'react-draggable';
import { X, Minus, Square } from 'lucide-react';
import { useOSStore, type WindowState } from '../store/osStore';
import { SchemaRenderer } from '../engine/SchemaRenderer';

export const WindowFrame = ({ win }: { win: WindowState }) =>
{
	const { closeWindow, focusWindow, minimizeWindow } = useOSStore();

	return (
		<Draggable handle=".window-header" onMouseDown={() => focusWindow(win.id)}>
			<div
				className={`os-window ${win.id === useOSStore.getState().activeWindowId ? 'active' : ''}`}
				style={{ zIndex: win.zIndex, display: win.isMinimized ? 'none' : 'flex' }}
			>
				{/* 标题栏 */}
				<div className="window-header">
					<div className="title">
						<span>{win.icon}</span> {win.title}
					</div>
					<div className="controls">
						<button onClick={() => minimizeWindow(win.id)}><Minus size={14} /></button>
						<button><Square size={12} /></button>
						<button className="close" onClick={() => closeWindow(win.id)}><X size={14} /></button>
					</div>
				</div>

				{/* 内容区：交给引擎 */}
				<div className="window-content">
					<SchemaRenderer schema={win.contentSchema} />
				</div>
			</div>
		</Draggable>
	);
};
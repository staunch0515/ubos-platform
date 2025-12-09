import { create } from 'zustand';

// 定义窗口的数据结构
export interface WindowState
{
	id: string;
	title: string;
	icon: string;
	contentSchema: any; // 来自后端的 UI 定义
	isMinimized: boolean;
	zIndex: number;
}

interface OSState
{
	windows: WindowState[];
	activeWindowId: string | null;

	// 动作
	openApp: (appId: string, manifest: any) => void;
	closeWindow: (id: string) => void;
	focusWindow: (id: string) => void;
	minimizeWindow: (id: string) => void;
}

export const useOSStore = create<OSState>((set) => ({
	windows: [],
	activeWindowId: null,

	openApp: (appId, manifest) => set((state) =>
	{
		// 如果已经打开，就聚焦
		const exist = state.windows.find(w => w.id === appId);
		if (exist) return { activeWindowId: appId };

		const newZ = state.windows.length + 1;
		const newWindow: WindowState = {
			id: appId,
			title: manifest.title,
			icon: manifest.icon,
			contentSchema: manifest.ui_schema, // 【关键】加载 UI 定义
			isMinimized: false,
			zIndex: newZ
		};
		return { windows: [...state.windows, newWindow], activeWindowId: appId };
	}),

	closeWindow: (id) => set((state) => ({
		windows: state.windows.filter(w => w.id !== id)
	})),

	focusWindow: (id) => set((state) =>
	{
		const maxZ = Math.max(...state.windows.map(w => w.zIndex), 0);
		return {
			windows: state.windows.map(w => w.id === id ? { ...w, zIndex: maxZ + 1 } : w),
			activeWindowId: id
		};
	}),

	minimizeWindow: (id) => set((state) => ({
		windows: state.windows.map(w => w.id === id ? { ...w, isMinimized: !w.isMinimized } : w)
	}))
}));
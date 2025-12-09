// src/store/kernelStore.ts
import { create } from 'zustand';
import axios from 'axios';

// 定义内核状态接口
interface KernelState
{
	// 系统状态机: 启动中 -> (需要登录) -> 就绪/桌面 -> 崩溃
	status: 'BOOTING' | 'LOGIN_REQUIRED' | 'READY' | 'ERROR';

	// 存放从后端 JSON 拿到的桌面定义 (壁纸、图标列表等)
	desktopConfig: any;

	// 用户凭证
	token: string | null;

	// 核心动作
	bootSystem: () => Promise<void>;
	login: (email: string, pass: string) => Promise<void>;
}

export const useKernel = create<KernelState>((set, get) => ({
	status: 'BOOTING',
	desktopConfig: null,
	token: localStorage.getItem('ubos_token'), // 初始化时尝试读取本地缓存

	// --- 动作 1: 系统启动/握手 ---
	bootSystem: async () =>
	{
		try
		{
			const res = await axios.get('/api/boot');

			// 🔍 调试：看看后端到底给了什么
			console.log("Backend Response:", res.data);

			// ✅ 修正点：后端给的是 desktop_schema，不是 schema
			const desktopSchema = res.data.desktop_schema;

			if (desktopSchema)
			{
				set({
					status: 'READY',
					desktopConfig: desktopSchema // 存进去
				});
			} else
			{
				console.error("缺少 desktop_schema 字段");
				set({ status: 'ERROR' });
			}
		} catch (e)
		{
			console.error("Boot Failed:", e);
			set({ status: 'ERROR' });
		}
	},

	// --- 动作 2: 用户登录 ---
	login: async (email, pass) =>
	{
		try
		{
			// 1. 调用登录接口
			// 对应 Java: AuthController.login()
			// 注意: 参数名必须匹配 Java RequestBody (email, password)
			const res = await axios.post('/api/auth/login', {
				email: email,
				password: pass
			});

			// 2. 获取 Token
			const newToken = res.data.token;

			if (newToken)
			{
				// 3. 持久化 Token
				localStorage.setItem('ubos_token', newToken);
				set({ token: newToken });

				// 4. 【关键】登录成功后，重新执行 Boot 流程
				// 这样后端会识别新身份，返回"管理员版"的桌面图标 (比如多了出题系统)
				await get().bootSystem();
			}

		} catch (e)
		{
			console.error("Login Failed:", e);
			alert("登录失败: 账号或密码错误");
			// 这里的错误会被 LoginScreen 里的 handleLogin 捕获
			throw e;
		}
	}
}));
// src/engine/SchemaRenderer.tsx
import React from 'react';
import axios from 'axios'; // 确保引入 axios

const ComponentRegistry: any = {
	'v-stack': ({ children, style }: any) => (
		<div style={{ display: 'flex', flexDirection: 'column', gap: '15px', padding: '10px', ...style }}>
			{children}
		</div>
	),

	'text': ({ content, size, color }: any) => (
		<div style={{ fontSize: size || '14px', color: color || '#eee' }}>
			{content}
		</div>
	),

	// 【修正点】按钮点击逻辑
	'button': ({ label, action }: any) => (
		<button
			className="os-btn-primary"
			style={{
				padding: '8px 16px', background: '#0078d4', border: 'none',
				borderRadius: '4px', color: 'white', cursor: 'pointer'
			}}
			onClick={async () =>
			{
				if (!action) return alert("没有绑定 Action");

				try
				{
					// 调用后端 RunController
					// 假设 action 格式是 "logic.wallet.withdraw"
					const res = await axios.post(`/api/run/${action}?branch=master`, {});

					alert("内核执行成功: " + JSON.stringify(res.data));
				} catch (e: any)
				{
					console.error(e);
					alert("执行失败: " + (e.response?.data?.error || e.message));
				}
			}}
		>
			{label}
		</button>
	),

	'input': ({ placeholder }: any) => (
		<input
			className="os-input"
			placeholder={placeholder}
			style={{ padding: '8px', borderRadius: '4px', border: '1px solid #555', background: '#333', color: 'white' }}
		/>
	)
};

export const SchemaRenderer = ({ schema }: { schema: any }) =>
{
	// 1. 防御：如果 schema 是空的，显示“无内容”，而不是崩掉
	if (!schema) return <div style={{ padding: 20, color: '#999' }}>Empty App (No Schema)</div>;

	const renderItem = (item: any, index: number) =>
	{
		// 2. 防御：如果 item 本身是坏的
		if (!item || !item.type) return null;

		const Component = ComponentRegistry[item.type];

		// 3. 防御：如果遇到了未知的组件类型
		if (!Component) return <div key={index} style={{ color: 'red', padding: 10 }}>Unknown Component: {item.type}</div>;

		return (
			<Component key={index} {...item.props}>
				{item.children && Array.isArray(item.children)
					? item.children.map((child: any, idx: number) => renderItem(child, `${index}-${idx}`))
					: null}
			</Component>
		);
	};

	// 4. 开始渲染
	try
	{
		return renderItem(schema, 0);
	} catch (e)
	{
		console.error("Render Error:", e);
		return <div style={{ color: 'red' }}>UI Render Error</div>;
	}
};
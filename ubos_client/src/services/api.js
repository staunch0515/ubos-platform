// src/services/api.js

const UBOS_API_URL = "http://localhost:8080/api";

const AUTH_TOKEN_ADMIN = "VALID_TOKEN_STAFF";

const headers = {
    "Content-Type": "application/json",
    // 注入 Auth Context Header (用于测试 STAFF/ADMIN 角色)
    "X-Auth-Token": AUTH_TOKEN_ADMIN
};

// 1. 提交逻辑/视图的 API
export async function commitLogic(data) {
    const response = await fetch(`${UBOS_API_URL}/dev/commit`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({
            // 默认参数和结构 (Groovy code must be nested in the 'code' string)
            type: data.type || 'LOGIC',
            slug: data.slug,
            branch: data.branch || 'master',
            message: data.message || 'Commit via React Client',
            code: data.code,
            // processId is omitted or handled by the Groovy script itself
        }),
    });
    // 强制检查 HTTP 状态码
    if (!response.ok) {
        const errorBody = await response.json();
        throw new Error(`API Error ${response.status}: ${errorBody.error}`);
    }
    return response.json();
}

// 2. 运行逻辑的 API
export async function runLogic(slug, context = {}, branch = 'master') {
    const response = await fetch(`${UBOS_API_URL}/run/${slug}?branch=${branch}`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(context),
    });
    if (!response.ok) {
        const errorBody = await response.json();
        throw new Error(`Execution Failed: ${errorBody.error}`);
    }
    // Logic execution returns raw JSON payload or text
    return response.json();
}

// 3. 审计 API (获取流程列表)
export async function fetchProcesses() {
    const response = await fetch(`${UBOS_API_URL}/audit/processes`, {
        method: 'GET',
        headers: headers
    });
    if (!response.ok) throw new Error("Failed to fetch audit data.");
    return response.json();
}

// 4. 视图 API (获取 SDUI 定义)
export async function fetchViewDefinition(slug) {
    const response = await fetch(`${UBOS_API_URL}/view/${slug}`, {
        method: 'GET',
        headers: headers
    });
    if (!response.ok) throw new Error("View not found or API failed.");

    // The Java backend returns a wrapper {data: {...}, headers: {...}}
    const result = await response.json();

    // We need to unwrap the nested JSON inside 'content' (The final fix!)
    if (result.data && result.data.content) {
        try {
            return JSON.parse(result.data.content);
        } catch (e) {
            throw new Error("Failed to parse inner View JSON.");
        }
    }
    return result;
}
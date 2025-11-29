// src/services/api.ts
import { type CommitRequest, type CommitResponse, type RuntimeContext, type ApiErrorResponse, type UbosApiResponse, ApiException } from '../types/ubos';

const UBOS_API_URL = "http://localhost:8080/api";

const headers: Record<string, string> = {
	"Content-Type": "application/json",
	// ⚠️ Simulates logged-in user for AuthContext check
	"X-Auth-Token": "VALID_TOKEN_STAFF"
};

// --- General Fetch Utility ---
async function fetchApi<T>(endpoint: string, options: RequestInit = {}): Promise<T>
{
	const response = await fetch(`${UBOS_API_URL}/${endpoint}`, options);

	if (!response.ok)
	{
		let errorBody: ApiErrorResponse | string = await response.text();
		try
		{
			errorBody = JSON.parse(errorBody);
		} catch { }

		throw new ApiException(`API Error ${response.status}: ${typeof errorBody === 'object' ? errorBody.error : errorBody}`, response.status, errorBody);
	}

	// Check if the response has content
	if (response.headers.get("content-length") === "0" || response.status === 204)
	{
		return {} as T;
	}

	return response.json() as Promise<T>;
}


// 1. COMMIT LOGIC / VIEW
export async function commitLogic(data: CommitRequest): Promise<CommitResponse>
{
	const rawResponse = await fetchApi<UbosApiResponse<CommitResponse>>('dev/commit', {
		method: 'POST',
		headers: headers,
		body: JSON.stringify({
			type: data.type || 'LOGIC',
			slug: data.slug,
			branch: data.branch || 'master',
			message: data.message || 'Commit via React Client',
			code: data.code,
		}),
	});
	// The Java backend returns the payload wrapped in {data: {...}, headers: {...}} via call_ubos helper
	return rawResponse.data;
}

// 2. RUN LOGIC
export async function runLogic(slug: string, context: RuntimeContext = {}, branch: string = 'master'): Promise<any>
{
	const response = await fetch(`${UBOS_API_URL}/run/${slug}?branch=${branch}`, {
		method: 'POST',
		headers: headers,
		body: JSON.stringify(context),
	});
	if (!response.ok)
	{
		const errorBody = await response.json() as ApiErrorResponse;
		throw new ApiException(`Execution Failed: ${errorBody.error}`, response.status, errorBody);
	}
	// Execution result can be any JSON structure (string, map, etc.)
	return response.json();
}

// 3. AUDIT FETCHERS (For demonstration purposes)
export async function fetchProcesses(): Promise<any[]>
{
	const response = await fetchApi<UbosApiResponse<any[]>>('audit/processes', { headers: headers });
	return response.data || [];
}

// 4. VIEW FETCHERS (Using the actual View API Endpoint)
export async function fetchViewDefinition(slug: string): Promise<any>
{
	const response = await fetch(`${UBOS_API_URL}/view/${slug}`, { headers: headers });
	if (!response.ok) throw new ApiException(`View definition not found for ${slug}`, response.status, {});

	// The Java backend returns the wrapped structure {data: JSON_STRING, headers: {...}}
	const result = await response.json() as { content: string };

	// Final Fix Check: Unwrap the nested JSON string in 'content'
	if (result && result.content)
	{
		try
		{
			return JSON.parse(result.content);
		} catch (e)
		{
			throw new Error("Failed to parse inner View JSON (Corrupted script data).");
		}
	}
	return {};
}

export { ApiException };

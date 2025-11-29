/** Structure for API errors */
export interface ApiErrorResponse
{
	timestamp: string;
	status: number;
	error: string;
	message?: string;
	requestId?: string;
}

/** Payload sent to /api/dev/commit */
export interface CommitRequest
{
	type: 'LOGIC' | 'VIEW' | 'DATA' | 'CRON';
	slug: string;
	branch?: string;
	code: string;
	message?: string;
	// processId is omitted or handled by the Groovy script itself
}

/** Response received from successful /api/dev/commit */
export interface CommitResponse
{
	status: string;
	commitId: number;
	slug: string;
}

/** Standard structure for the context object used in runLogic */
export interface RuntimeContext
{
	[key: string]: any;
}

/** Universal structure for data returned by the backend */
export interface UbosApiResponse<T>
{
	data: T;
	headers: Record<string, string>;
}

// Custom error for our API handling
export class ApiException extends Error
{
	constructor(message: string, public status: number, public body: any)
	{
		super(message);
		this.name = 'ApiException';
	}
}
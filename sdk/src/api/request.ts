import type { PaginationParams } from "../types";

export interface CalendarEngineConfig {
  baseUrl: string;
  getToken?: () => string | null;
  getTenantId?: () => number | string;
  onUnauthorized?: () => void;
}

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public body?: unknown,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export function createRequest(config: CalendarEngineConfig) {
  async function request<T>(
    path: string,
    options: RequestInit = {},
  ): Promise<T> {
    const url = `${config.baseUrl}${path}`;

    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      ...(options.headers as Record<string, string>),
    };

    const token = config.getToken?.();
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    const tenantId = config.getTenantId?.();
    if (tenantId) {
      headers["X-Tenant-Id"] = String(tenantId);
    }

    const response = await fetch(url, {
      ...options,
      headers,
    });

    if (response.status === 204) {
      return undefined as T;
    }

    if (response.status === 401) {
      config.onUnauthorized?.();
      throw new ApiError("Unauthorized", 401);
    }

    if (!response.ok) {
      const body = await response.json().catch(() => null);
      throw new ApiError(
        body?.error || `Request failed with status ${response.status}`,
        response.status,
        body,
      );
    }

    return response.json();
  }

  return request;
}

export function buildPaginationQuery(params?: PaginationParams): string {
  if (!params) return "";

  const parts: string[] = [];
  if (params.limit != null) parts.push(`limit=${params.limit}`);
  if (params.offset != null) parts.push(`offset=${params.offset}`);
  if (params.sortBy) parts.push(`sortBy=${encodeURIComponent(params.sortBy)}`);
  if (params.sortDir) parts.push(`sortDir=${params.sortDir}`);
  if (params.search) parts.push(`search=${encodeURIComponent(params.search)}`);

  return parts.length > 0 ? `?${parts.join("&")}` : "";
}

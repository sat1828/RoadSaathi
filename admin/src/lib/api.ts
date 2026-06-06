import type {
  AuthResponse,
  LoginRequest,
  DashboardSummary,
  AnalyticsData,
  HazardReport,
  HeatmapCluster,
  AssignRequest,
  StatusUpdateRequest,
  HeatmapResponse,
} from './types';

const BASE_URL = import.meta.env.VITE_API_URL || '/api/v1';

async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = localStorage.getItem('token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (res.status === 401) {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login';
    throw new Error('Unauthorized');
  }

  if (!res.ok) {
    const body = await res.text();
    throw new Error(body || `Request failed: ${res.status}`);
  }

  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  login(email: string, password: string): Promise<AuthResponse> {
    return request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password } as LoginRequest),
    });
  },

  register(data: { name: string; email: string; password: string; role?: string }) {
    return request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  getDashboardSummary(): Promise<DashboardSummary> {
    return request<DashboardSummary>('/dashboard/summary');
  },

  getAnalytics(period?: string): Promise<AnalyticsData> {
    const params = period ? `?period=${period}` : '';
    return request<AnalyticsData>(`/analytics${params}`);
  },

  getHazards(params: {
    status?: string;
    hazardType?: string;
    severity?: number;
    page?: number;
    limit?: number;
  } = {}): Promise<HazardReport[]> {
    const qs = new URLSearchParams();
    if (params.status) qs.set('status', params.status);
    if (params.hazardType) qs.set('hazardType', params.hazardType);
    if (params.severity !== undefined) qs.set('severity', String(params.severity));
    if (params.page !== undefined) qs.set('page', String(params.page));
    if (params.limit !== undefined) qs.set('limit', String(params.limit));
    const query = qs.toString();
    return request<HazardReport[]>(`/hazards${query ? `?${query}` : ''}`);
  },

  getHeatmapData(lat: number, lng: number, radius: number): Promise<HeatmapCluster[]> {
    return request<HeatmapResponse>(`/hazards/heatmap?lat=${lat}&lng=${lng}&radius=${radius}`)
      .then((geoJson) =>
        geoJson.features.map((f) => ({
          lat: f.geometry.coordinates[1],
          lng: f.geometry.coordinates[0],
          hazardType: f.properties.hazard_type,
          count: f.properties.count,
          severity: f.properties.severity,
          aiBrief: f.properties.ai_brief,
          nhCorridor: f.properties.nh_corridor,
        }))
      );
  },

  getReport(id: string): Promise<HazardReport> {
    return request<HazardReport>(`/reports/${id}`);
  },

  getTriageQueue(params: {
    page?: number;
    limit?: number;
    status?: string;
    hazardType?: string;
    severity?: number;
  } = {}): Promise<HazardReport[]> {
    const qs = new URLSearchParams();
    if (params.page !== undefined) qs.set('page', String(params.page));
    if (params.limit !== undefined) qs.set('limit', String(params.limit));
    if (params.status) qs.set('status', params.status);
    if (params.hazardType) qs.set('hazardType', params.hazardType);
    if (params.severity !== undefined) qs.set('severity', String(params.severity));
    return request<HazardReport[]>(`/admin/triage?${qs.toString()}`);
  },

  assignReport(id: string, data: AssignRequest): Promise<void> {
    return request<void>(`/admin/reports/${id}/assign`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateReportStatus(id: string, data: StatusUpdateRequest): Promise<void> {
    return request<void>(`/admin/reports/${id}/status`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

export interface User {
  id: string;
  name: string;
  email: string;
  role: 'DRIVER' | 'NHAI_OFFICER' | 'FIELD_ENGINEER';
  token: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  id: string;
  name: string;
  email: string;
  role: string;
  token: string;
}

export type HazardStatus =
  | 'ACTIVE'
  | 'CONFIRMED'
  | 'UNCONFIRMED'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'EXPIRED';

export interface HazardReport {
  id: string;
  hazardType: string;
  latitude: number;
  longitude: number;
  photoUrl?: string;
  severity: number;
  status: HazardStatus;
  nhCorridor?: string;
  reportedAt: string;
  expiresAt: string;
  confirmCount: number;
  aiBrief?: string;
  confidenceScore?: number;
  classificationLabel?: string;
  reporterId?: string;
  assignedEngineerId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardSummary {
  totalReports: number;
  activeReports: number;
  pendingReview: number;
  assigned: number;
  resolvedLastWeek: number;
  avgResponseTimeHours: number;
  uniqueDrivers: number;
  activeCorridors: number;
}

export interface AnalyticsData {
  byType: { hazardType: string; count: number }[];
  byStatus: { status: string; count: number }[];
  dailyTrend: { date: string; count: number }[];
  byCorridor: { corridor: string; count: number }[];
  blackspots: Blackspot[];
}

export interface Blackspot {
  lat: number;
  lng: number;
  hazardType: string;
  count: number;
  avgSeverity: number;
}

export interface HeatmapCluster {
  lat: number;
  lng: number;
  hazardType: string;
  count: number;
  severity: number;
  aiBrief?: string;
  nhCorridor?: string;
}

export interface AssignRequest {
  engineerId: string;
}

export interface StatusUpdateRequest {
  status: string;
}

export interface HeatmapResponse {
  type: string;
  features: {
    type: string;
    geometry: { type: string; coordinates: number[] };
    properties: {
      hazard_type: string;
      count: number;
      severity: number;
      nh_corridor?: string;
      ai_brief?: string;
    };
  }[];
}

import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, Clock, MapPin, Activity } from 'lucide-react';
import { api } from '../lib/api';
import { Card } from '../components/ui/Card';
import { HazardTypeBadge, SeverityBadge, StatusBadge } from '../components/ui/Badge';
import { MapView } from '../components/MapView';
import { Button } from '../components/ui/Button';
import { formatDistanceToNow } from 'date-fns';
import type { HazardReport } from '../lib/types';

const severityOrder = { critical: 0, high: 1, medium: 2, low: 3 };

export default function DashboardPage() {
  const navigate = useNavigate();

  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: () => api.getDashboardSummary(),
    refetchInterval: 30000,
  });

  const { data: reportsData, isLoading: reportsLoading } = useQuery({
    queryKey: ['recent-reports'],
    queryFn: () => api.getReports({ limit: 10, status: 'reported' }),
    refetchInterval: 30000,
  });

  const recentReports = reportsData?.reports || [];
  const markers = recentReports.slice(0, 5).map((r) => ({
    lat: r.latitude,
    lng: r.longitude,
    popup: `<b>${r.hazardType}</b><br/>${r.nhCorridor}<br/>Severity: ${r.severity}`,
    id: r.id,
  }));

  const sortedReports = [...recentReports].sort(
    (a, b) => (severityOrder[a.severity] ?? 99) - (severityOrder[b.severity] ?? 99)
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => navigate('/triage')}>
            View Triage Queue
          </Button>
          <Button onClick={() => navigate('/live-map')}>
            Open Live Map
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <div className="flex items-center gap-3">
            <div className="p-3 bg-primary-50 rounded-lg">
              <Activity className="w-6 h-6 text-primary-600" />
            </div>
            <div>
              <p className="text-sm text-gray-500">Active Reports</p>
              <p className="text-2xl font-bold text-gray-900">
                {summaryLoading ? '...' : summary?.totalActive ?? 0}
              </p>
            </div>
          </div>
        </Card>

        <Card>
          <div className="flex items-center gap-3">
            <div className="p-3 bg-blue-50 rounded-lg">
              <MapPin className="w-6 h-6 text-blue-600" />
            </div>
            <div>
              <p className="text-sm text-gray-500">Reports Today</p>
              <p className="text-2xl font-bold text-gray-900">
                {summaryLoading ? '...' : summary?.reportsToday ?? 0}
              </p>
            </div>
          </div>
        </Card>

        <Card>
          <div className="flex items-center gap-3">
            <div className="p-3 bg-amber-50 rounded-lg">
              <Clock className="w-6 h-6 text-amber-600" />
            </div>
            <div>
              <p className="text-sm text-gray-500">Avg Response Time</p>
              <p className="text-2xl font-bold text-gray-900">
                {summaryLoading ? '...' : `${summary?.avgResponseTime ?? 0}h`}
              </p>
            </div>
          </div>
        </Card>

        <Card>
          <div className="flex items-center gap-3">
            <div className="p-3 bg-red-50 rounded-lg">
              <AlertTriangle className="w-6 h-6 text-red-600" />
            </div>
            <div>
              <p className="text-sm text-gray-500">High Severity</p>
              <p className="text-2xl font-bold text-gray-900">
                {summaryLoading ? '...' : summary?.highSeverityCount ?? 0}
              </p>
            </div>
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-2" header={<h2 className="font-semibold text-gray-900">Recent Reports Map</h2>}>
          <div className="h-80">
            <MapView
              markers={markers}
              onMarkerClick={(id) => navigate(`/reports/${id}`)}
            />
          </div>
        </Card>

        <Card header={<h2 className="font-semibold text-gray-900">Quick Actions</h2>}>
          <div className="space-y-3">
            <Button className="w-full" onClick={() => navigate('/live-map')}>
              <MapPin className="w-4 h-4 mr-2" />
              View Heatmap
            </Button>
            <Button className="w-full" variant="secondary" onClick={() => navigate('/triage')}>
              Triage Queue
            </Button>
            <Button className="w-full" variant="outline" onClick={() => navigate('/analytics')}>
              Analytics
            </Button>
          </div>
        </Card>
      </div>

      <Card header={<h2 className="font-semibold text-gray-900">Recent Reports</h2>}>
        {reportsLoading ? (
          <div className="flex justify-center py-8">
            <div className="w-6 h-6 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
          </div>
        ) : sortedReports.length === 0 ? (
          <p className="text-center py-8 text-gray-500">No recent reports</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Severity</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Location</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Reported</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {sortedReports.map((report) => (
                  <tr key={report.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <HazardTypeBadge type={report.hazardType} />
                    </td>
                    <td className="px-4 py-3">
                      <SeverityBadge severity={report.severity} />
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">{report.nhCorridor}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={report.status} />
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500">
                      {formatDistanceToNow(new Date(report.reportedAt), { addSuffix: true })}
                    </td>
                    <td className="px-4 py-3">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate(`/reports/${report.id}`)}
                      >
                        View
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { api } from '../lib/api';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';

const HAZARD_COLORS: Record<string, string> = {
  pothole: '#ff6f00',
  road_collapse: '#d32f2f',
  waterlogging: '#1976d2',
  accident: '#b71c1c',
  debris: '#757575',
  cattle: '#388e3c',
  other: '#9e9e9e',
};

const PIE_COLORS = ['#ff6f00', '#d32f2f', '#1976d2', '#b71c1c', '#757575', '#388e3c', '#9e9e9e'];

export default function AnalyticsPage() {
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['analytics', startDate, endDate],
    queryFn: () => api.getAnalytics({ startDate: startDate || undefined, endDate: endDate || undefined }),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  const monthlyData = data?.monthlyTrends || [];
  const hazardDistribution = data?.hazardDistribution || {} as Record<string, number>;
  const severityBreakdown = data?.severityBreakdown || {} as Record<string, number>;
  const topBlackspots = data?.topBlackspots || [];

  const corridorMonths = Array.from(new Set(monthlyData.map((d) => d.month))).sort();
  const corridors = Array.from(new Set(monthlyData.map((d) => d.nhCorridor)));

  const lineChartData = corridorMonths.map((month) => {
    const point: Record<string, string | number> = { month };
    corridors.forEach((c) => {
      const entry = monthlyData.find((d) => d.month === month && d.nhCorridor === c);
      point[c] = entry?.count || 0;
    });
    return point;
  });

  const hazardPieData = Object.entries(hazardDistribution).map(([name, value]) => ({
    name: name.replace('_', ' '),
    value,
  }));

  const severityBarData = Object.entries(severityBreakdown).map(([name, value]) => ({
    name,
    count: value,
  }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-bold text-gray-900">Analytics</h1>
        <div className="flex items-center gap-2">
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="text-sm border border-gray-300 rounded-lg px-3 py-2"
          />
          <span className="text-gray-500">to</span>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="text-sm border border-gray-300 rounded-lg px-3 py-2"
          />
          <Button variant="outline" onClick={() => { setStartDate(''); setEndDate(''); }}>
            Reset
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card header={<h2 className="font-semibold text-gray-900">Monthly Hazard Trends by NH Corridor</h2>}>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={lineChartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" fontSize={12} />
                <YAxis fontSize={12} />
                <Tooltip />
                <Legend />
                {corridors.map((c, i) => (
                  <Line
                    key={c}
                    type="monotone"
                    dataKey={c}
                    stroke={PIE_COLORS[i % PIE_COLORS.length]}
                    strokeWidth={2}
                    dot={false}
                  />
                ))}
              </LineChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card header={<h2 className="font-semibold text-gray-900">Hazard Type Distribution</h2>}>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={hazardPieData}
                  cx="50%"
                  cy="50%"
                  labelLine
                  outerRadius={100}
                  dataKey="value"
                  label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                >
                  {hazardPieData.map((entry, idx) => (
                    <Cell key={entry.name} fill={PIE_COLORS[idx % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card header={<h2 className="font-semibold text-gray-900">Reports by Severity</h2>}>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={severityBarData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" fontSize={12} />
                <YAxis fontSize={12} />
                <Tooltip />
                <Bar dataKey="count" fill="#4caf50" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card header={<h2 className="font-semibold text-gray-900">Top 5 Blackspot NH Corridors</h2>}>
          {topBlackspots.length === 0 ? (
            <p className="text-center py-8 text-gray-500">No data available</p>
          ) : (
            <div className="space-y-3">
              {topBlackspots.slice(0, 5).map((spot, idx) => (
                <div
                  key={spot.nhCorridor}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                >
                  <div className="flex items-center gap-3">
                    <span className="text-lg font-bold text-gray-400">#{idx + 1}</span>
                    <div>
                      <p className="font-medium text-sm text-gray-900">{spot.nhCorridor}</p>
                      <p className="text-xs text-gray-500">
                        {spot.totalReports} reports &middot; {spot.criticalCount} critical
                      </p>
                    </div>
                  </div>
                  <Badge variant={spot.avgSeverity > 0.6 ? 'danger' : spot.avgSeverity > 0.3 ? 'warning' : 'success'}>
                    {Math.round(spot.avgSeverity * 100)}% severity
                  </Badge>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}

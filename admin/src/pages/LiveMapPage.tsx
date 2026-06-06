import { useState, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { RefreshCw, Filter } from 'lucide-react';
import { api } from '../lib/api';
import { MapView } from '../components/MapView';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import type { HazardType, HeatmapCluster } from '../lib/types';

const hazardTypes: HazardType[] = [
  'pothole', 'road_collapse', 'waterlogging', 'accident', 'debris', 'cattle', 'other'
];

export default function LiveMapPage() {
  const [selectedTypes, setSelectedTypes] = useState<HazardType[]>([]);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const { data: clusters, isLoading, refetch } = useQuery({
    queryKey: ['heatmap'],
    queryFn: () => api.getHeatmapData(),
    refetchInterval: autoRefresh ? 15000 : false,
  });

  const toggleType = useCallback((type: HazardType) => {
    setSelectedTypes((prev) =>
      prev.includes(type) ? prev.filter((t) => t !== type) : [...prev, type]
    );
  }, []);

  const filteredClusters = (clusters || []).filter(
    (c) => selectedTypes.length === 0 || selectedTypes.includes(c.hazardType)
  );

  const heatmapPoints = filteredClusters.map((c) => ({
    lat: c.lat,
    lng: c.lng,
    intensity: c.intensity,
  }));

  const markers = filteredClusters
    .filter((c) => c.count >= 3)
    .map((c) => ({
      lat: c.lat,
      lng: c.lng,
      id: c.lat.toString() + c.lng.toString(),
      popup: `
        <div style="min-width: 180px;">
          <strong>${c.count} reports</strong><br/>
          Type: ${c.hazardType.replace('_', ' ')}<br/>
          Severity: ${c.severity}<br/>
          ${c.aiBrief ? `<p style="margin-top:4px;font-size:12px;color:#666;">${c.aiBrief}</p>` : ''}
          <div style="margin-top:6px;">
            ${Object.entries(c.types)
              .filter(([, count]) => count > 0)
              .map(([type, count]) => `<span style="margin-right:6px;font-size:11px;">${type}: ${count}</span>`)
              .join('')}
          </div>
        </div>
      `,
    }));

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-bold text-gray-900">Live Hazard Map</h1>
        <div className="flex items-center gap-2">
          <label className="flex items-center gap-2 text-sm text-gray-600">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
              className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            Auto-refresh
          </label>
          <Button variant="outline" onClick={() => refetch()} loading={isLoading}>
            <RefreshCw className="w-4 h-4 mr-1" />
            Refresh
          </Button>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {hazardTypes.map((type) => (
          <button
            key={type}
            onClick={() => toggleType(type)}
            className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
              selectedTypes.length === 0 || selectedTypes.includes(type)
                ? 'bg-primary-100 text-primary-700'
                : 'bg-gray-100 text-gray-500'
            }`}
          >
            {type.replace('_', ' ')}
          </button>
        ))}
        {selectedTypes.length > 0 && (
          <button
            onClick={() => setSelectedTypes([])}
            className="px-3 py-1 rounded-full text-xs font-medium bg-red-50 text-red-600 hover:bg-red-100"
          >
            Clear filters
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
        <div className="lg:col-span-3">
          <Card className="p-0 overflow-hidden">
            <div className="h-[65vh]">
              {isLoading && clusters === undefined ? (
                <div className="flex items-center justify-center h-full">
                  <div className="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
                </div>
              ) : (
                <MapView
                  heatmapData={heatmapPoints}
                  markers={markers}
                  zoom={5}
                />
              )}
            </div>
          </Card>
        </div>

        <div className="space-y-3">
          <Card header={<h3 className="font-semibold text-sm">Cluster Summary</h3>}>
            {isLoading ? (
              <div className="flex justify-center py-4">
                <div className="w-5 h-5 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
              </div>
            ) : filteredClusters.length === 0 ? (
              <p className="text-sm text-gray-500 text-center py-4">No clusters found</p>
            ) : (
              <div className="space-y-2 max-h-96 overflow-y-auto">
                {filteredClusters.slice(0, 20).map((cluster, idx) => (
                  <div key={idx} className="p-2 bg-gray-50 rounded text-sm">
                    <div className="flex items-center justify-between mb-1">
                      <Badge variant={
                        cluster.hazardType === 'road_collapse' || cluster.hazardType === 'accident'
                          ? 'danger'
                          : cluster.hazardType === 'waterlogging'
                          ? 'info'
                          : cluster.hazardType === 'pothole'
                          ? 'warning'
                          : 'default'
                      }>
                        {cluster.hazardType.replace('_', ' ')}
                      </Badge>
                      <span className="text-xs text-gray-500">{cluster.count} reports</span>
                    </div>
                    <p className="text-xs text-gray-600 truncate">
                      {cluster.aiBrief || `${cluster.severity} severity`}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}

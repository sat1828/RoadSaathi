import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import { HazardTypeBadge, SeverityBadge, StatusBadge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Modal } from '../components/ui/Modal';
import type { TriageItem, HazardType } from '../lib/types';

const hazardTypes: HazardType[] = [
  'pothole', 'road_collapse', 'waterlogging', 'accident', 'debris', 'cattle', 'other'
];

const nhCorridors = [
  'NH-1', 'NH-2', 'NH-3', 'NH-4', 'NH-5', 'NH-6', 'NH-7', 'NH-8', 'NH-9', 'NH-10',
  'NH-19', 'NH-27', 'NH-44', 'NH-48', 'NH-66'
];

const severityOrder = { critical: 0, high: 1, medium: 2, low: 3 };

export default function TriagePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [sortBy, setSortBy] = useState('severity');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [filterHazard, setFilterHazard] = useState('');
  const [filterCorridor, setFilterCorridor] = useState('');
  const [assignModal, setAssignModal] = useState<{ open: boolean; reportId: string }>({
    open: false,
    reportId: '',
  });

  const { data, isLoading } = useQuery({
    queryKey: ['triage', page, sortBy, sortOrder, filterHazard, filterCorridor],
    queryFn: () =>
      api.getTriageQueue({
        page,
        limit: 20,
        hazardType: filterHazard || undefined,
        nhCorridor: filterCorridor || undefined,
        sortBy,
        sortOrder,
      }),
  });

  const assignMutation = useMutation({
    mutationFn: ({ reportId, engineerId }: { reportId: string; engineerId: string }) =>
      api.assignReport(reportId, engineerId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['triage'] });
      setAssignModal({ open: false, reportId: '' });
    },
  });

  const items = data?.items || [];
  const total = data?.total || 0;
  const totalPages = Math.ceil(total / 20);

  const handleSort = (key: string) => {
    if (sortBy === key) {
      setSortOrder((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortBy(key);
      setSortOrder('desc');
    }
  };

  const sortedItems = useMemo(() => {
    const sorted = [...items];
    sorted.sort((a, b) => {
      let cmp = 0;
      if (sortBy === 'severity') {
        cmp = (severityOrder[a.severity] ?? 99) - (severityOrder[b.severity] ?? 99);
      } else if (sortBy === 'timeSinceReported') {
        cmp = a.timeSinceReported - b.timeSinceReported;
      } else if (sortBy === 'reportCount') {
        cmp = a.reportCount - b.reportCount;
      }
      return sortOrder === 'asc' ? cmp : -cmp;
    });
    return sorted;
  }, [items, sortBy, sortOrder]);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Triage Queue</h1>
        <div className="flex gap-2">
          <select
            value={filterHazard}
            onChange={(e) => { setFilterHazard(e.target.value); setPage(1); }}
            className="text-sm border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
          >
            <option value="">All Hazards</option>
            {hazardTypes.map((t) => (
              <option key={t} value={t}>{t.replace('_', ' ')}</option>
            ))}
          </select>
          <select
            value={filterCorridor}
            onChange={(e) => { setFilterCorridor(e.target.value); setPage(1); }}
            className="text-sm border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
          >
            <option value="">All Corridors</option>
            {nhCorridors.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>
      </div>

      <Card className="p-0 overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center py-12">
            <div className="w-6 h-6 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
          </div>
        ) : sortedItems.length === 0 ? (
          <div className="text-center py-12 text-gray-500">No triage items found</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Hazard</th>
                  <th
                    className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:bg-gray-100 select-none"
                    onClick={() => handleSort('severity')}
                  >
                    Severity {sortBy === 'severity' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">NH Corridor</th>
                  <th
                    className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:bg-gray-100 select-none"
                    onClick={() => handleSort('timeSinceReported')}
                  >
                    Time Ago {sortBy === 'timeSinceReported' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                  </th>
                  <th
                    className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:bg-gray-100 select-none"
                    onClick={() => handleSort('reportCount')}
                  >
                    Reports {sortBy === 'reportCount' ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Confidence</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {sortedItems.map((item) => (
                  <tr key={item.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <HazardTypeBadge type={item.hazardType} />
                    </td>
                    <td className="px-4 py-3">
                      <SeverityBadge severity={item.severity} />
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">{item.nhCorridor}</td>
                    <td className="px-4 py-3 text-sm text-gray-500">
                      {item.timeSinceReported < 60
                        ? `${item.timeSinceReported}m`
                        : `${Math.floor(item.timeSinceReported / 60)}h`}
                    </td>
                    <td className="px-4 py-3 text-sm font-medium">{item.reportCount}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="w-16 bg-gray-200 rounded-full h-1.5">
                          <div
                            className="bg-primary-500 h-1.5 rounded-full"
                            style={{ width: `${Math.round(item.confidence * 100)}%` }}
                          />
                        </div>
                        <span className="text-xs text-gray-500">
                          {Math.round(item.confidence * 100)}%
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={item.status} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <Button
                          variant="outline"
                          onClick={() => setAssignModal({ open: true, reportId: item.id })}
                        >
                          Assign
                        </Button>
                        <Button
                          variant="outline"
                          onClick={() => navigate(`/reports/${item.id}`)}
                        >
                          Details
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-gray-200 bg-gray-50">
            <p className="text-sm text-gray-500">
              Showing {(page - 1) * 20 + 1} to {Math.min(page * 20, total)} of {total}
            </p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                disabled={page <= 1}
                onClick={() => setPage((p) => Math.max(1, p - 1))}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                disabled={page >= totalPages}
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </Card>

      <Modal
        isOpen={assignModal.open}
        onClose={() => setAssignModal({ open: false, reportId: '' })}
        title="Assign Report to Engineer"
      >
        <div className="space-y-3">
          {['Engineer - North Zone', 'Engineer - East Zone', 'Engineer - West Zone', 'Engineer - South Zone'].map(
            (engineer) => (
              <button
                key={engineer}
                onClick={() =>
                  assignMutation.mutate({
                    reportId: assignModal.reportId,
                    engineerId: engineer.toLowerCase().replace(/\s+/g, '_'),
                  })
                }
                disabled={assignMutation.isPending}
                className="w-full text-left px-4 py-3 rounded-lg border border-gray-200 hover:bg-primary-50 hover:border-primary-300 transition-colors disabled:opacity-50"
              >
                <p className="font-medium text-sm text-gray-900">{engineer}</p>
                <p className="text-xs text-gray-500">Available now</p>
              </button>
            )
          )}
        </div>
      </Modal>
    </div>
  );
}

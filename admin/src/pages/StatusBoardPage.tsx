import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { formatDistanceToNow } from 'date-fns';
import { api } from '../lib/api';
import { HazardTypeBadge, SeverityBadge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import type { HazardReport, ReportStatus } from '../lib/types';

const columns: { status: ReportStatus; title: string }[] = [
  { status: 'reported', title: 'Reported' },
  { status: 'assigned', title: 'Assigned' },
  { status: 'in_progress', title: 'In Progress' },
  { status: 'resolved', title: 'Resolved' },
];

export default function StatusBoardPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['board-reports'],
    queryFn: () => api.getReports({ limit: 100 }),
    refetchInterval: 15000,
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: ReportStatus }) =>
      api.updateReportStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['board-reports'] });
    },
  });

  const reports = data?.reports || [];

  const grouped = columns.map((col) => ({
    ...col,
    items: reports.filter((r) => r.status === col.status),
  }));

  const nextStatus: Record<ReportStatus, ReportStatus | null> = {
    reported: 'assigned',
    assigned: 'in_progress',
    in_progress: 'resolved',
    resolved: null,
    verified: 'assigned',
    expired: null,
    dismissed: null,
  };

  const handleAdvance = (report: HazardReport) => {
    const next = nextStatus[report.status];
    if (next) {
      statusMutation.mutate({ id: report.id, status: next });
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">Status Board</h1>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {grouped.map((column) => (
          <div key={column.status} className="bg-gray-50 rounded-lg p-4">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-gray-900">{column.title}</h3>
              <span className="bg-gray-200 text-gray-700 text-xs font-medium px-2 py-0.5 rounded-full">
                {column.items.length}
              </span>
            </div>

            <div className="space-y-3 min-h-[200px]">
              {column.items.length === 0 && (
                <p className="text-center text-sm text-gray-400 py-8">No reports</p>
              )}

              {column.items.map((report) => (
                <div
                  key={report.id}
                  className="bg-white rounded-lg p-3 shadow-sm border border-gray-200 space-y-2"
                >
                  <div className="flex items-center justify-between">
                    <HazardTypeBadge type={report.hazardType} />
                    <SeverityBadge severity={report.severity} />
                  </div>

                  <p className="text-sm font-medium text-gray-900">{report.nhCorridor}</p>

                  <p className="text-xs text-gray-500">
                    {formatDistanceToNow(new Date(report.reportedAt), { addSuffix: true })}
                  </p>

                  {report.photoUrls && report.photoUrls.length > 0 && (
                    <img
                      src={report.photoUrls[0]}
                      alt="Report"
                      className="w-full h-20 object-cover rounded"
                      onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
                    />
                  )}

                  <div className="flex items-center gap-2 pt-1">
                    {nextStatus[report.status] && (
                      <Button
                        variant="primary"
                        onClick={() => handleAdvance(report)}
                        disabled={statusMutation.isPending}
                      >
                        Move to {nextStatus[report.status]!.replace('_', ' ')}
                      </Button>
                    )}
                    <Button
                      variant="outline"
                      onClick={() => navigate(`/reports/${report.id}`)}
                    >
                      View
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

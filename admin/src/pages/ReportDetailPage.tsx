import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { ArrowLeft, MapPin, Calendar, AlertTriangle } from 'lucide-react';
import { api } from '../lib/api';
import { HazardTypeBadge, SeverityBadge, StatusBadge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Modal } from '../components/ui/Modal';
import { MapView } from '../components/MapView';
import type { ReportStatus } from '../lib/types';

const statusFlow: ReportStatus[] = ['reported', 'assigned', 'in_progress', 'resolved'];

export default function ReportDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [statusModal, setStatusModal] = useState(false);
  const [deleteModal, setDeleteModal] = useState(false);
  const [photoIndex, setPhotoIndex] = useState(0);

  const { data: report, isLoading } = useQuery({
    queryKey: ['report', id],
    queryFn: () => api.getReport(id!),
    enabled: !!id,
  });

  const statusMutation = useMutation({
    mutationFn: (status: ReportStatus) => api.updateReportStatus(id!, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report', id] });
      setStatusModal(false);
    },
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!report) {
    return (
      <div className="text-center py-12">
        <AlertTriangle className="w-12 h-12 text-gray-400 mx-auto mb-4" />
        <h2 className="text-lg font-semibold text-gray-900">Report not found</h2>
        <p className="text-gray-500 mt-1">The report you're looking for doesn't exist.</p>
        <Button className="mt-4" onClick={() => navigate('/triage')}>
          Back to Triage
        </Button>
      </div>
    );
  }

  const currentStatusIdx = statusFlow.indexOf(report.status);

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="outline" onClick={() => navigate(-1)}>
            <ArrowLeft className="w-4 h-4 mr-1" />
            Back
          </Button>
          <h1 className="text-2xl font-bold text-gray-900">Report #{report.id.slice(0, 8)}</h1>
          <StatusBadge status={report.status} />
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => setStatusModal(true)}>
            Change Status
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          {report.photoUrls && report.photoUrls.length > 0 && (
            <Card className="p-0 overflow-hidden">
              <div className="relative bg-gray-900">
                <img
                  src={report.photoUrls[photoIndex]}
                  alt={`Report photo ${photoIndex + 1}`}
                  className="w-full h-72 object-cover"
                  onError={(e) => {
                    (e.target as HTMLImageElement).src = 'https://via.placeholder.com/800x400?text=No+Image';
                  }}
                />
                {report.photoUrls.length > 1 && (
                  <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-2">
                    {report.photoUrls.map((_, idx) => (
                      <button
                        key={idx}
                        onClick={() => setPhotoIndex(idx)}
                        className={`w-2.5 h-2.5 rounded-full transition-colors ${
                          idx === photoIndex ? 'bg-white' : 'bg-white/50'
                        }`}
                      />
                    ))}
                  </div>
                )}
              </div>
            </Card>
          )}

          <Card header={<h2 className="font-semibold text-gray-900">Location</h2>}>
            <div className="h-64">
              <MapView
                center={[report.latitude, report.longitude]}
                zoom={15}
                markers={[
                  {
                    lat: report.latitude,
                    lng: report.longitude,
                    popup: `<b>${report.hazardType}</b><br/>${report.nhCorridor}`,
                  },
                ]}
              />
            </div>
          </Card>
        </div>

        <div className="space-y-6">
          <Card header={<h2 className="font-semibold text-gray-900">Details</h2>}>
            <dl className="space-y-3">
              <div>
                <dt className="text-xs text-gray-500 uppercase">Hazard Type</dt>
                <dd className="mt-1">
                  <HazardTypeBadge type={report.hazardType} />
                </dd>
              </div>
              <div>
                <dt className="text-xs text-gray-500 uppercase">Severity</dt>
                <dd className="mt-1">
                  <SeverityBadge severity={report.severity} />
                </dd>
              </div>
              <div>
                <dt className="text-xs text-gray-500 uppercase">NH Corridor</dt>
                <dd className="mt-1 text-sm font-medium">{report.nhCorridor}</dd>
              </div>
              <div>
                <dt className="text-xs text-gray-500 uppercase">District</dt>
                <dd className="mt-1 text-sm">{report.district}</dd>
              </div>
              <div>
                <dt className="text-xs text-gray-500 uppercase">State</dt>
                <dd className="mt-1 text-sm">{report.state}</dd>
              </div>
              <div>
                <dt className="text-xs text-gray-500 uppercase">Coordinates</dt>
                <dd className="mt-1 text-sm text-gray-600">
                  {report.latitude.toFixed(4)}, {report.longitude.toFixed(4)}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-gray-500 uppercase">Confidence</dt>
                <dd className="mt-1">
                  <div className="flex items-center gap-2">
                    <div className="w-full bg-gray-200 rounded-full h-2">
                      <div
                        className="bg-primary-500 h-2 rounded-full"
                        style={{ width: `${Math.round(report.confidence * 100)}%` }}
                      />
                    </div>
                    <span className="text-sm font-medium">
                      {Math.round(report.confidence * 100)}%
                    </span>
                  </div>
                </dd>
              </div>
              <div>
                <dt className="text-xs text-gray-500 uppercase">Report Count</dt>
                <dd className="mt-1 text-sm font-medium">{report.reportCount}</dd>
              </div>
              <div>
                <dt className="text-xs text-gray-500 uppercase">Reported At</dt>
                <dd className="mt-1 text-sm">
                  {format(new Date(report.reportedAt), 'PPp')}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-gray-500 uppercase">Expires At</dt>
                <dd className="mt-1 text-sm">
                  {format(new Date(report.expiresAt), 'PPp')}
                </dd>
              </div>
              {report.assignedTo && (
                <div>
                  <dt className="text-xs text-gray-500 uppercase">Assigned To</dt>
                  <dd className="mt-1 text-sm font-medium">{report.assignedTo}</dd>
                </div>
              )}
            </dl>
          </Card>

          {report.description && (
            <Card header={<h2 className="font-semibold text-gray-900">Description</h2>}>
              <p className="text-sm text-gray-700">{report.description}</p>
            </Card>
          )}

          <Card header={<h2 className="font-semibold text-gray-900">Status Timeline</h2>}>
            <div className="space-y-3">
              {statusFlow.map((status, idx) => {
                const isActive = idx <= currentStatusIdx;
                const isCurrent = idx === currentStatusIdx;
                return (
                  <div key={status} className="flex items-center gap-3">
                    <div
                      className={`w-3 h-3 rounded-full flex-shrink-0 ${
                        isCurrent
                          ? 'bg-primary-500 ring-2 ring-primary-200'
                          : isActive
                          ? 'bg-primary-500'
                          : 'bg-gray-200'
                      }`}
                    />
                    <span
                      className={`text-sm ${
                        isActive ? 'text-gray-900 font-medium' : 'text-gray-400'
                      }`}
                    >
                      {status.replace('_', ' ').replace(/\b\w/g, (c) => c.toUpperCase())}
                    </span>
                  </div>
                );
              })}
            </div>
          </Card>
        </div>
      </div>

      <Modal
        isOpen={statusModal}
        onClose={() => setStatusModal(false)}
        title="Change Report Status"
      >
        <div className="space-y-2">
          {statusFlow.map((status) => (
            <button
              key={status}
              onClick={() => statusMutation.mutate(status)}
              disabled={statusMutation.isPending || status === report.status}
              className={`w-full text-left px-4 py-3 rounded-lg border transition-colors ${
                status === report.status
                  ? 'bg-primary-50 border-primary-300 text-primary-700'
                  : 'border-gray-200 hover:bg-gray-50 text-gray-700'
              } disabled:opacity-50`}
            >
              <p className="font-medium text-sm">
                {status.replace('_', ' ').replace(/\b\w/g, (c) => c.toUpperCase())}
              </p>
            </button>
          ))}
        </div>
      </Modal>
    </div>
  );
}

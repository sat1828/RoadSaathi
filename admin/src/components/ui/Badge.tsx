import { clsx } from 'clsx';
import type { HazardType, Severity, ReportStatus } from '../../lib/types';

interface BadgeProps {
  children: React.ReactNode;
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info';
  className?: string;
}

const badgeVariants = {
  default: 'bg-gray-100 text-gray-800',
  success: 'bg-green-100 text-green-800',
  warning: 'bg-yellow-100 text-yellow-800',
  danger: 'bg-red-100 text-red-800',
  info: 'bg-blue-100 text-blue-800',
};

export function Badge({ children, variant = 'default', className }: BadgeProps) {
  return (
    <span
      className={clsx(
        'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
        badgeVariants[variant],
        className
      )}
    >
      {children}
    </span>
  );
}

const hazardTypeColors: Record<HazardType, 'warning' | 'danger' | 'info' | 'default' | 'success'> = {
  pothole: 'warning',
  road_collapse: 'danger',
  waterlogging: 'info',
  accident: 'danger',
  debris: 'default',
  cattle: 'success',
  other: 'default',
};

const severityColors: Record<Severity, 'success' | 'warning' | 'danger'> = {
  low: 'success',
  medium: 'warning',
  high: 'danger',
  critical: 'danger',
};

const statusColors: Record<ReportStatus, 'default' | 'success' | 'warning' | 'info' | 'danger'> = {
  reported: 'warning',
  verified: 'info',
  assigned: 'info',
  in_progress: 'warning',
  resolved: 'success',
  expired: 'default',
  dismissed: 'default',
};

export function HazardTypeBadge({ type }: { type: HazardType }) {
  return (
    <Badge variant={hazardTypeColors[type]}>
      {type.replace('_', ' ')}
    </Badge>
  );
}

export function SeverityBadge({ severity }: { severity: Severity }) {
  return (
    <Badge variant={severityColors[severity]}>
      {severity}
    </Badge>
  );
}

export function StatusBadge({ status }: { status: ReportStatus }) {
  return (
    <Badge variant={statusColors[status]}>
      {status.replace('_', ' ')}
    </Badge>
  );
}

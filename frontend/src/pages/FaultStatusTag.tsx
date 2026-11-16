import { AppStatusTag } from '@helex/ui';
import { FAULT_STATUS_LABELS, type FaultStatus } from '../api';

/**
 * Each status with a tone from the library's status tones (Storybook ›
 * AppStatusTag › All tones). The library version in this template has the
 * appointment and encounter tones; the generic ones (success, warning…) are
 * newer and not available here. The tone only sets the colour: Rejected uses
 * 'noshow' because it is red; the tag still reads "Rejected".
 */
const STATUS_TONE = {
  NEW: 'planned',
  IN_PROGRESS: 'in-progress',
  RESOLVED: 'finished',
  REJECTED: 'noshow',
} as const;

/**
 * A report's status as a coloured tag — one component, so the list, the detail
 * panel and the report page show statuses the same way.
 */
export const FaultStatusTag = ({ status }: { status: FaultStatus }) => (
  <AppStatusTag tone={STATUS_TONE[status]}>{FAULT_STATUS_LABELS[status] ?? status}</AppStatusTag>
);

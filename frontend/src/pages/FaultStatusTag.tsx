import { AppTag } from '@helex/ui';
import { FAULT_STATUS_LABELS, type FaultStatus } from '../api';

/**
 * A report's status as a tag — one component, so the list, the detail panel and
 * the report page show statuses the same way. FAULTS.01 § UI asks for
 * `AppStatusTag` with one colour per status; that switch happens here once the
 * library's exact tone names are confirmed.
 */
export const FaultStatusTag = ({ status }: { status: FaultStatus }) => (
  <AppTag>{FAULT_STATUS_LABELS[status] ?? status}</AppTag>
);

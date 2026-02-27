import { useAuth } from '../auth/AuthContext';
import { ROLES } from '../utils/constants';

export function usePermission() {
  const { user, hasRole, hasAnyRole } = useAuth();

  const canCreate = hasAnyRole(ROLES.ADMIN, ROLES.MAKER);
  const canApprove = hasAnyRole(ROLES.ADMIN, ROLES.CHECKER);
  const canViewAll = hasAnyRole(ROLES.ADMIN, ROLES.MAKER, ROLES.CHECKER, ROLES.VIEWER);
  const isAdmin = hasRole(ROLES.ADMIN);

  const canApprovePending = (pendingAction) => {
    if (!canApprove) return false;
    return pendingAction?.makerId !== user?.id;
  };

  const canCancelPending = (pendingAction) => {
    return pendingAction?.makerId === user?.id && pendingAction?.status === 'PENDING';
  };

  return { canCreate, canApprove, canViewAll, isAdmin, canApprovePending, canCancelPending };
}

package com.example.workflow.organization;
import com.example.workflow.common.ApiException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MembershipPolicyTest {
    @Test void ownerCannotRemoveOwner() { assertThrows(ApiException.class,() -> MembershipService.checkManage(Role.OWNER,Role.OWNER,null)); }
    @Test void adminCannotPromoteToAdmin() { assertThrows(ApiException.class,() -> MembershipService.checkManage(Role.ADMIN,Role.MEMBER,Role.ADMIN)); }
    @Test void adminCannotRemoveAdmin() { assertThrows(ApiException.class,() -> MembershipService.checkManage(Role.ADMIN,Role.ADMIN,null)); }
    @Test void memberCannotPromoteAnyone() { assertThrows(ApiException.class,() -> MembershipService.checkManage(Role.MEMBER,Role.MEMBER,Role.MANAGER)); }
    @Test void adminCanPromoteMemberToManager() { assertDoesNotThrow(() -> MembershipService.checkManage(Role.ADMIN,Role.MEMBER,Role.MANAGER)); }
    @Test void ownerCanAddAdmin() { assertDoesNotThrow(() -> MembershipService.checkManage(Role.OWNER,null,Role.ADMIN)); }
}

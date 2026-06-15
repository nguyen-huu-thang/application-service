package vn.xime.application.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import vn.xime.application.common.exception.PublicError;
import vn.xime.application.domain.application.ApplicationStatus;
import vn.xime.application.domain.application.exception.ApplicationNotFoundException;
import vn.xime.application.domain.application.exception.DuplicateApplicationCodeException;
import vn.xime.application.domain.application.exception.InvalidStatusTransitionException;
import vn.xime.application.domain.error.ErrorCode;
import vn.xime.application.domain.permission.PermissionCode;
import vn.xime.application.domain.permission.exception.PermissionAlreadyGrantedException;
import vn.xime.application.domain.permission.exception.PermissionNotGrantedException;

class ExceptionErrorCodeResolverTest {

    @Test
    void appException_returnsItsOwnCode() {
        assertThat(ExceptionErrorCodeResolver.resolve(new PublicError(ErrorCode.BAD_REQUEST)))
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void applicationNotFound() {
        assertThat(ExceptionErrorCodeResolver.resolve(new ApplicationNotFoundException("x")))
                .isEqualTo(ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    void duplicateCode() {
        assertThat(ExceptionErrorCodeResolver.resolve(new DuplicateApplicationCodeException("x")))
                .isEqualTo(ErrorCode.DUPLICATE_APPLICATION_CODE);
    }

    @Test
    void invalidTransition() {
        assertThat(ExceptionErrorCodeResolver.resolve(
                new InvalidStatusTransitionException(ApplicationStatus.PENDING_REVIEW, ApplicationStatus.SUSPENDED)))
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void permissionAlreadyGranted() {
        assertThat(ExceptionErrorCodeResolver.resolve(
                new PermissionAlreadyGrantedException(PermissionCode.DATA_READ_OBJECT)))
                .isEqualTo(ErrorCode.PERMISSION_ALREADY_GRANTED);
    }

    @Test
    void permissionNotGranted() {
        assertThat(ExceptionErrorCodeResolver.resolve(
                new PermissionNotGrantedException(PermissionCode.DATA_READ_OBJECT)))
                .isEqualTo(ErrorCode.PERMISSION_NOT_GRANTED);
    }

    @Test
    void illegalArgument_isValidationFailed() {
        assertThat(ExceptionErrorCodeResolver.resolve(new IllegalArgumentException("bad")))
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void illegalState_isRuleViolation() {
        assertThat(ExceptionErrorCodeResolver.resolve(new IllegalStateException("bad")))
                .isEqualTo(ErrorCode.RULE_VIOLATION);
    }

    @Test
    void unknownException_isUnknown() {
        assertThat(ExceptionErrorCodeResolver.resolve(new RuntimeException("boom")))
                .isEqualTo(ErrorCode.UNKNOWN);
    }
}

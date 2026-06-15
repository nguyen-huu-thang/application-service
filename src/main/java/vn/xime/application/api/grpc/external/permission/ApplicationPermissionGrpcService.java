package vn.xime.application.api.grpc.external.permission;

import com.google.protobuf.Empty;

import io.grpc.stub.StreamObserver;

import vn.xime.application.api.grpc.error.GrpcErrorMapper;
import vn.xime.application.api.grpc.mapper.ApplicationGrpcMapper;
import vn.xime.application.application.dto.permission.GrantSystemPermissionCommand;
import vn.xime.application.application.dto.permission.RevokeSystemPermissionCommand;
import vn.xime.application.application.port.in.permission.GrantSystemPermissionUseCase;
import vn.xime.application.application.port.in.permission.RevokeSystemPermissionUseCase;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;
import vn.xime.application.grpc.external.permission.ApplicationPermissionServiceGrpc;
import vn.xime.application.grpc.external.permission.PermissionRequest;

/**
 * gRPC adapter for granting / revoking system permissions of an application.
 * Adapter gRPC cấp / thu hồi quyền hệ thống của application.
 */
public class ApplicationPermissionGrpcService
        extends ApplicationPermissionServiceGrpc.ApplicationPermissionServiceImplBase {

    private final GrantSystemPermissionUseCase grantUseCase;
    private final RevokeSystemPermissionUseCase revokeUseCase;

    public ApplicationPermissionGrpcService(
            GrantSystemPermissionUseCase grantUseCase,
            RevokeSystemPermissionUseCase revokeUseCase) {
        this.grantUseCase = grantUseCase;
        this.revokeUseCase = revokeUseCase;
    }

    @Override
    public void grantSystemPermission(PermissionRequest request,
                                      StreamObserver<Empty> observer) {
        try {
            ApplicationId appId =
                    ApplicationGrpcMapper.toApplicationId(request.getAppIdentityId());

            grantUseCase.grant(new GrantSystemPermissionCommand(appId, request.getPermission()));

            observer.onNext(Empty.getDefaultInstance());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatus(e));
        }
    }

    @Override
    public void revokeSystemPermission(PermissionRequest request,
                                       StreamObserver<Empty> observer) {
        try {
            ApplicationId appId =
                    ApplicationGrpcMapper.toApplicationId(request.getAppIdentityId());

            revokeUseCase.revoke(new RevokeSystemPermissionCommand(appId, request.getPermission()));

            observer.onNext(Empty.getDefaultInstance());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatus(e));
        }
    }
}

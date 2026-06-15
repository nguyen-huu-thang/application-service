package vn.xime.application.api.grpc.external.application;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import io.grpc.stub.StreamObserver;

import vn.xime.application.api.grpc.error.GrpcErrorMapper;
import vn.xime.application.api.grpc.mapper.ApplicationGrpcMapper;
import vn.xime.application.application.dto.application.ActivateApplicationCommand;
import vn.xime.application.application.dto.application.ApplicationResult;
import vn.xime.application.application.dto.application.ApplicationSummaryResult;
import vn.xime.application.application.dto.application.DisableApplicationCommand;
import vn.xime.application.application.dto.application.GetApplicationQuery;
import vn.xime.application.application.dto.application.ListApplicationsQuery;
import vn.xime.application.application.dto.application.PageResult;
import vn.xime.application.application.dto.application.ReactivateApplicationCommand;
import vn.xime.application.application.dto.application.RegisterApplicationCommand;
import vn.xime.application.application.dto.application.RegisterApplicationResult;
import vn.xime.application.application.dto.application.RetireApplicationCommand;
import vn.xime.application.application.dto.application.SuspendApplicationCommand;
import vn.xime.application.application.port.in.application.ActivateApplicationUseCase;
import vn.xime.application.application.port.in.application.DisableApplicationUseCase;
import vn.xime.application.application.port.in.application.GetApplicationUseCase;
import vn.xime.application.application.port.in.application.ListApplicationsUseCase;
import vn.xime.application.application.port.in.application.ReactivateApplicationUseCase;
import vn.xime.application.application.port.in.application.RegisterApplicationUseCase;
import vn.xime.application.application.port.in.application.RetireApplicationUseCase;
import vn.xime.application.application.port.in.application.SuspendApplicationUseCase;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;
import vn.xime.application.grpc.external.application.ApplicationAdminServiceGrpc;
import vn.xime.application.grpc.external.application.ApplicationIdRequest;
import vn.xime.application.grpc.external.application.ApplicationResponse;
import vn.xime.application.grpc.external.application.ListApplicationsRequest;
import vn.xime.application.grpc.external.application.ListApplicationsResponse;
import vn.xime.application.grpc.external.application.RegisterApplicationRequest;
import vn.xime.application.grpc.external.application.RegisterApplicationResponse;

/**
 * gRPC adapter for the application admin API (register + lifecycle + query).
 * Adapter gRPC cho admin API của application (đăng ký + vòng đời + truy vấn).
 *
 * Chỉ dịch proto -> use case -> proto; mọi exception đẩy qua GrpcErrorMapper. Khởi tạo
 * bằng new ở config (không phải Spring bean) để không tự đăng ký nhầm server khác.
 */
public class ApplicationAdminGrpcService
        extends ApplicationAdminServiceGrpc.ApplicationAdminServiceImplBase {

    private final RegisterApplicationUseCase registerUseCase;
    private final ActivateApplicationUseCase activateUseCase;
    private final SuspendApplicationUseCase suspendUseCase;
    private final ReactivateApplicationUseCase reactivateUseCase;
    private final DisableApplicationUseCase disableUseCase;
    private final RetireApplicationUseCase retireUseCase;
    private final GetApplicationUseCase getUseCase;
    private final ListApplicationsUseCase listUseCase;

    public ApplicationAdminGrpcService(
            RegisterApplicationUseCase registerUseCase,
            ActivateApplicationUseCase activateUseCase,
            SuspendApplicationUseCase suspendUseCase,
            ReactivateApplicationUseCase reactivateUseCase,
            DisableApplicationUseCase disableUseCase,
            RetireApplicationUseCase retireUseCase,
            GetApplicationUseCase getUseCase,
            ListApplicationsUseCase listUseCase) {
        this.registerUseCase = registerUseCase;
        this.activateUseCase = activateUseCase;
        this.suspendUseCase = suspendUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.disableUseCase = disableUseCase;
        this.retireUseCase = retireUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
    }

    @Override
    public void registerApplication(RegisterApplicationRequest request,
                                    StreamObserver<RegisterApplicationResponse> observer) {
        try {
            RegisterApplicationResult result = registerUseCase.register(
                    new RegisterApplicationCommand(
                            request.getApplicationCode(),
                            request.getName(),
                            request.getDescription()));

            observer.onNext(RegisterApplicationResponse.newBuilder()
                    .setIdentityId(ByteString.copyFrom(result.identityId().toBytes()))
                    .setApplicationCode(result.applicationCode())
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatus(e));
        }
    }

    @Override
    public void activateApplication(ApplicationIdRequest request,
                                    StreamObserver<Empty> observer) {
        try {
            activateUseCase.activate(new ActivateApplicationCommand(idOf(request)));
            completeEmpty(observer);
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatus(e));
        }
    }

    @Override
    public void suspendApplication(ApplicationIdRequest request,
                                   StreamObserver<Empty> observer) {
        try {
            suspendUseCase.suspend(new SuspendApplicationCommand(idOf(request)));
            completeEmpty(observer);
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatus(e));
        }
    }

    @Override
    public void reactivateApplication(ApplicationIdRequest request,
                                      StreamObserver<Empty> observer) {
        try {
            reactivateUseCase.reactivate(new ReactivateApplicationCommand(idOf(request)));
            completeEmpty(observer);
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatus(e));
        }
    }

    @Override
    public void disableApplication(ApplicationIdRequest request,
                                   StreamObserver<Empty> observer) {
        try {
            disableUseCase.disable(new DisableApplicationCommand(idOf(request)));
            completeEmpty(observer);
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatus(e));
        }
    }

    @Override
    public void retireApplication(ApplicationIdRequest request,
                                  StreamObserver<Empty> observer) {
        try {
            retireUseCase.retire(new RetireApplicationCommand(idOf(request)));
            completeEmpty(observer);
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatus(e));
        }
    }

    @Override
    public void getApplication(ApplicationIdRequest request,
                               StreamObserver<ApplicationResponse> observer) {
        try {
            ApplicationResult result = getUseCase.get(GetApplicationQuery.byId(idOf(request)));
            observer.onNext(ApplicationGrpcMapper.toProto(result));
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatus(e));
        }
    }

    @Override
    public void listApplications(ListApplicationsRequest request,
                                 StreamObserver<ListApplicationsResponse> observer) {
        try {
            PageResult<ApplicationSummaryResult> page = listUseCase.list(
                    new ListApplicationsQuery(
                            request.getStatusFilter(),
                            request.getPage(),
                            request.getSize()));

            ListApplicationsResponse.Builder builder = ListApplicationsResponse.newBuilder()
                    .setTotal(page.total());

            for (ApplicationSummaryResult summary : page.items()) {
                builder.addApplications(ApplicationResponse.newBuilder()
                        .setIdentityId(ByteString.copyFrom(summary.identityId().toBytes()))
                        .setApplicationCode(summary.applicationCode())
                        .setName(summary.name())
                        .setStatus(summary.status())
                        .build());
            }

            observer.onNext(builder.build());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatus(e));
        }
    }

    // =========================
    // HELPERS
    // =========================

    private static ApplicationId idOf(ApplicationIdRequest request) {
        return ApplicationGrpcMapper.toApplicationId(request.getIdentityId());
    }

    private static void completeEmpty(StreamObserver<Empty> observer) {
        observer.onNext(Empty.getDefaultInstance());
        observer.onCompleted();
    }
}

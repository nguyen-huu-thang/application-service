package vn.xime.application.api.grpc.internal.subject;

import io.grpc.stub.StreamObserver;

import vn.xime.application.api.grpc.error.GrpcErrorMapper;
import vn.xime.application.api.grpc.mapper.ApplicationGrpcMapper;
import vn.xime.application.application.dto.internal.ChangedApplicationsResult;
import vn.xime.application.application.dto.internal.GetSubjectInfoQuery;
import vn.xime.application.application.dto.internal.PollChangedApplicationsQuery;
import vn.xime.application.application.dto.internal.SubjectInfoResult;
import vn.xime.application.application.port.in.internal.GetSubjectInfoUseCase;
import vn.xime.application.application.port.in.internal.PollChangedApplicationsUseCase;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;
import vn.xime.application.grpc.internal.subject.ApplicationSubjectServiceGrpc;
import vn.xime.application.grpc.internal.subject.GetSubjectInfoRequest;
import vn.xime.application.grpc.internal.subject.PollChangedRequest;
import vn.xime.application.grpc.internal.subject.PollChangedResponse;
import vn.xime.application.grpc.internal.subject.SubjectInfoResponse;

/**
 * gRPC adapter for resource-service subject sync (direct lookup + pull).
 * Adapter gRPC cho đồng bộ subject phía resource service (lookup trực tiếp + pull).
 */
public class ApplicationSubjectGrpcService
        extends ApplicationSubjectServiceGrpc.ApplicationSubjectServiceImplBase {

    private final GetSubjectInfoUseCase getSubjectInfoUseCase;
    private final PollChangedApplicationsUseCase pollChangedUseCase;

    public ApplicationSubjectGrpcService(
            GetSubjectInfoUseCase getSubjectInfoUseCase,
            PollChangedApplicationsUseCase pollChangedUseCase) {
        this.getSubjectInfoUseCase = getSubjectInfoUseCase;
        this.pollChangedUseCase = pollChangedUseCase;
    }

    @Override
    public void getSubjectInfo(GetSubjectInfoRequest request,
                               StreamObserver<SubjectInfoResponse> observer) {
        try {
            ApplicationId id = ApplicationGrpcMapper.toApplicationId(request.getIdentityId());

            SubjectInfoResult result = getSubjectInfoUseCase.get(new GetSubjectInfoQuery(id));

            observer.onNext(ApplicationGrpcMapper.toProto(result));
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatus(e));
        }
    }

    @Override
    public void pollChangedApplications(PollChangedRequest request,
                                        StreamObserver<PollChangedResponse> observer) {
        try {
            ChangedApplicationsResult result = pollChangedUseCase.poll(
                    new PollChangedApplicationsQuery(
                            request.getAfterSequence(),
                            request.getLimit()));

            PollChangedResponse.Builder builder = PollChangedResponse.newBuilder()
                    .setMaxSequence(result.maxSequence())
                    .setHasMore(result.hasMore());

            for (SubjectInfoResult subject : result.applications()) {
                builder.addApplications(ApplicationGrpcMapper.toProto(subject));
            }

            observer.onNext(builder.build());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(GrpcErrorMapper.toStatus(e));
        }
    }
}

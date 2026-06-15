package vn.xime.application.config.grpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import vn.xime.application.infrastructure.ssl.GrpcServerCredentialsProvider;
import vn.xime.application.api.grpc.external.application.ApplicationAdminGrpcService;
import vn.xime.application.api.grpc.external.permission.ApplicationPermissionGrpcService;
import vn.xime.application.api.grpc.internal.subject.ApplicationSubjectGrpcService;
import vn.xime.application.application.port.in.application.ActivateApplicationUseCase;
import vn.xime.application.application.port.in.application.DisableApplicationUseCase;
import vn.xime.application.application.port.in.application.GetApplicationUseCase;
import vn.xime.application.application.port.in.application.ListApplicationsUseCase;
import vn.xime.application.application.port.in.application.ReactivateApplicationUseCase;
import vn.xime.application.application.port.in.application.RegisterApplicationUseCase;
import vn.xime.application.application.port.in.application.RetireApplicationUseCase;
import vn.xime.application.application.port.in.application.SuspendApplicationUseCase;
import vn.xime.application.application.port.in.internal.GetSubjectInfoUseCase;
import vn.xime.application.application.port.in.internal.PollChangedApplicationsUseCase;
import vn.xime.application.application.port.in.permission.GrantSystemPermissionUseCase;
import vn.xime.application.application.port.in.permission.RevokeSystemPermissionUseCase;

/**
 * Bootstraps the gRPC server and registers the application gRPC services.
 * Khởi tạo gRPC server và đăng ký các gRPC service của application.
 *
 * Service khởi tạo bằng new (KHÔNG là Spring bean) để không bị auto-register nhầm nơi khác.
 *
 * mTLS: dùng TlsServerCredentials từ GrpcServerCredentialsProvider (cert in-memory từ Trust).
 * @DependsOn đảm bảo cert loaders (root CA init + cert sync bootstrap) chạy xong trước khi
 * build server credentials.
 */
@Configuration
public class GrpcServerConfig {

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    @DependsOn({ "trustCertificateSynchronizationScheduler", "trustRootCertificateInitializer" })
    public Server applicationGrpcServer(
            @Value("${grpc.server.port:9094}") int port,
            GrpcServerCredentialsProvider credentialsProvider,
            // admin
            RegisterApplicationUseCase registerUseCase,
            ActivateApplicationUseCase activateUseCase,
            SuspendApplicationUseCase suspendUseCase,
            ReactivateApplicationUseCase reactivateUseCase,
            DisableApplicationUseCase disableUseCase,
            RetireApplicationUseCase retireUseCase,
            GetApplicationUseCase getUseCase,
            ListApplicationsUseCase listUseCase,
            // permission
            GrantSystemPermissionUseCase grantUseCase,
            RevokeSystemPermissionUseCase revokeUseCase,
            // internal subject sync
            GetSubjectInfoUseCase getSubjectInfoUseCase,
            PollChangedApplicationsUseCase pollChangedUseCase) throws Exception {

        return NettyServerBuilder
                .forPort(port, credentialsProvider.buildServerCredentials())
                .addService(new ApplicationAdminGrpcService(
                        registerUseCase, activateUseCase, suspendUseCase, reactivateUseCase,
                        disableUseCase, retireUseCase, getUseCase, listUseCase))
                .addService(new ApplicationPermissionGrpcService(
                        grantUseCase, revokeUseCase))
                .addService(new ApplicationSubjectGrpcService(
                        getSubjectInfoUseCase, pollChangedUseCase))
                .build();
    }
}

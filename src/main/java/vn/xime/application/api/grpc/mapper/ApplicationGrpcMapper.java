package vn.xime.application.api.grpc.mapper;

import com.google.protobuf.ByteString;

import vn.xime.application.application.dto.application.ApplicationResult;
import vn.xime.application.application.dto.internal.SubjectInfoResult;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;
import vn.xime.application.grpc.external.application.ApplicationResponse;
import vn.xime.application.grpc.internal.subject.SubjectInfoResponse;

/**
 * Maps between protobuf messages and application DTOs.
 * Map giữa message protobuf và DTO của application.
 *
 * Static thuần. proto3 string không nhận null -> rỗng hóa; timestamp đẩy ra epoch millis;
 * id đi qua dây dưới dạng bytes (ByteString).
 */
public final class ApplicationGrpcMapper {

    private ApplicationGrpcMapper() {
    }

    // =========================
    // Wire -> Domain
    // =========================

    public static ApplicationId toApplicationId(ByteString bytes) {
        return new ApplicationId(bytes.toByteArray());
    }

    // =========================
    // DTO -> Wire
    // =========================

    public static ApplicationResponse toProto(ApplicationResult r) {
        return ApplicationResponse.newBuilder()
                .setIdentityId(ByteString.copyFrom(r.identityId().toBytes()))
                .setApplicationCode(r.applicationCode())
                .setName(r.name())
                .setDescription(nullToEmpty(r.description()))
                .setStatus(r.status())
                .setStateVersion(r.stateVersion())
                .setChangeSequence(r.changeSequence())
                .addAllPermissions(r.permissions())
                .setCreatedAt(r.createdAt().toEpochMilli())
                .setUpdatedAt(r.updatedAt().toEpochMilli())
                .build();
    }

    public static SubjectInfoResponse toProto(SubjectInfoResult r) {
        return SubjectInfoResponse.newBuilder()
                .setIdentityId(ByteString.copyFrom(r.identityId().toBytes()))
                .setSubjectType(r.subjectType())
                .setStatus(r.status())
                .setStateVersion(r.stateVersion())
                .setChangeSequence(r.changeSequence())
                .addAllPermissions(r.permissions())
                .setTenantId(nullToEmpty(r.tenantId()))
                .build();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
